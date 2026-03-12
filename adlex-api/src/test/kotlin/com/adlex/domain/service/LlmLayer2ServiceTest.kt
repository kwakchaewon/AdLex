package com.adlex.domain.service

import com.adlex.engine.model.EvaluationResult
import com.adlex.engine.model.Severity
import com.adlex.infra.llm.ClaudeApiClient
import com.adlex.infra.llm.LlmAnalysisResponse
import com.adlex.infra.llm.LlmException
import com.adlex.infra.rag.LawChunkResult
import com.adlex.infra.rag.PrecedentResult
import com.adlex.infra.rag.RagContext
import com.adlex.infra.rag.VectorSearchService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LlmLayer2ServiceTest {

    private val vectorSearchService = mockk<VectorSearchService>()
    private val claudeApiClient = mockk<ClaudeApiClient>()
    private lateinit var service: LlmLayer2Service

    @BeforeEach
    fun setUp() {
        service = LlmLayer2Service(vectorSearchService, claudeApiClient)
    }

    @Test
    fun `RAG 컨텍스트와 함께 Claude API 호출`() {
        val ragContext = RagContext(
            lawChunks = listOf(
                LawChunkResult(1L, "표시·광고의 공정화에 관한 법률", "제3조", null, "허위 표현 금지", null)
            ),
            precedents = listOf(
                PrecedentResult(1L, "2021-001", "공정거래위원회", "과장광고 제재", "허위 표현 제재 사례")
            )
        )
        val capturedRagContext = slot<RagContext>()

        every { vectorSearchService.search(any()) } returns ragContext
        every {
            claudeApiClient.analyze(any(), any(), capture(capturedRagContext), any())
        } returns LlmAnalysisResponse("법적 위험 중간 수준입니다.", 150, 60)

        val result = service.analyze("최고 품질 보장! 100% 환불!", emptyList())

        assertNotNull(result)
        assertEquals("법적 위험 중간 수준입니다.", result!!.analysis)
        assertEquals(1, result.citedLawCount)
        assertEquals(1, result.citedPrecedentCount)
        assertEquals(150, result.inputTokens)
        assertEquals(60, result.outputTokens)
        assertEquals(ragContext, capturedRagContext.captured)
    }

    @Test
    fun `Layer 1 위반 결과가 프롬프트에 포함됨`() {
        val violations = listOf(
            EvaluationResult("RULE-001", Severity.HIGH, "최고 표현 사용 금지", "표시광고법 제3조")
        )
        val capturedPrompt = slot<String>()

        every { vectorSearchService.search(any()) } returns RagContext(emptyList(), emptyList())
        every {
            claudeApiClient.analyze(capture(capturedPrompt), any(), any(), any())
        } returns LlmAnalysisResponse("위반 사항 확인됨", 100, 40)

        service.analyze("최고의 제품!", violations)

        assertTrue(capturedPrompt.captured.contains("RULE-001"))
        assertTrue(capturedPrompt.captured.contains("Layer 1 규칙 엔진 탐지 결과"))
    }

    @Test
    fun `위반 없을 때 프롬프트 내용 확인`() {
        val capturedPrompt = slot<String>()

        every { vectorSearchService.search(any()) } returns RagContext(emptyList(), emptyList())
        every {
            claudeApiClient.analyze(capture(capturedPrompt), any(), any(), any())
        } returns LlmAnalysisResponse("특별한 위반 없음", 80, 30)

        service.analyze("안녕하세요, 새 제품을 소개합니다.", emptyList())

        assertTrue(capturedPrompt.captured.contains("규칙 엔진 위반은 없었으나"))
    }

    @Test
    fun `LlmException 발생 시 null 반환 (장애 격리)`() {
        every { vectorSearchService.search(any()) } returns RagContext(emptyList(), emptyList())
        every { claudeApiClient.analyze(any(), any(), any(), any()) } throws LlmException("API 키 오류")

        val result = service.analyze("테스트 메시지", emptyList())

        assertNull(result)
    }

    @Test
    fun `일반 예외 발생 시 null 반환 (장애 격리)`() {
        every { vectorSearchService.search(any()) } throws RuntimeException("네트워크 오류")

        val result = service.analyze("테스트 메시지", emptyList())

        assertNull(result)
        verify(exactly = 0) { claudeApiClient.analyze(any(), any(), any(), any()) }
    }
}

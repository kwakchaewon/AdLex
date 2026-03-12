package com.adlex.domain.service

import com.adlex.engine.model.Channel
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
import org.junit.jupiter.api.assertThrows

class SuggestionServiceTest {

    private val vectorSearchService = mockk<VectorSearchService>()
    private val claudeApiClient = mockk<ClaudeApiClient>()
    private lateinit var service: SuggestionService

    @BeforeEach
    fun setUp() {
        service = SuggestionService(vectorSearchService, claudeApiClient)
    }

    @Test
    fun `정상적인 수정 제안 반환`() {
        val ragContext = RagContext(
            lawChunks = listOf(
                LawChunkResult(1L, "표시·광고의 공정화에 관한 법률", "제3조", null, "허위 표현 금지", null)
            ),
            precedents = listOf(
                PrecedentResult(1L, "2021-001", "공정거래위원회", "과장광고 제재", null)
            )
        )

        every { vectorSearchService.search(any()) } returns ragContext
        every { claudeApiClient.analyze(any(), any(), any(), any()) } returns LlmAnalysisResponse(
            content = """
                [수정안]
                품질 인증을 받은 제품입니다. 자세한 사항은 홈페이지를 확인해주세요.

                [수정 이유]
                '최고 품질'은 근거 없는 절대적 표현으로 표시광고법 위반 가능성이 있어 제거했습니다.
            """.trimIndent(),
            inputTokens = 200,
            outputTokens = 80
        )

        val result = service.suggest("최고 품질 보장!", Channel.SMS)

        assertEquals("품질 인증을 받은 제품입니다. 자세한 사항은 홈페이지를 확인해주세요.", result.suggested)
        assertTrue(result.rationale.contains("절대적 표현"))
        assertEquals(1, result.citedLawCount)
        assertEquals(1, result.citedPrecedentCount)
    }

    @Test
    fun `hint가 프롬프트에 포함됨`() {
        val capturedPrompt = slot<String>()

        every { vectorSearchService.search(any()) } returns RagContext(emptyList(), emptyList())
        every {
            claudeApiClient.analyze(capture(capturedPrompt), any(), any(), any())
        } returns LlmAnalysisResponse(
            content = "[수정안]\n수정된 내용\n\n[수정 이유]\n이유 설명",
            inputTokens = 100,
            outputTokens = 50
        )

        service.suggest("최고의 제품!", Channel.EMAIL, hint = "의약품 표현 제거")

        assertTrue(capturedPrompt.captured.contains("의약품 표현 제거"))
        assertTrue(capturedPrompt.captured.contains("수정 방향 힌트"))
    }

    @Test
    fun `채널 정보가 프롬프트에 포함됨`() {
        val capturedPrompt = slot<String>()

        every { vectorSearchService.search(any()) } returns RagContext(emptyList(), emptyList())
        every {
            claudeApiClient.analyze(capture(capturedPrompt), any(), any(), any())
        } returns LlmAnalysisResponse(
            content = "[수정안]\n수정 내용\n\n[수정 이유]\n이유",
            inputTokens = 80,
            outputTokens = 40
        )

        service.suggest("광고 메시지", Channel.KAKAO)

        assertTrue(capturedPrompt.captured.contains("KAKAO"))
    }

    @Test
    fun `LLM 응답 파싱 실패 시 전체 내용을 수정안으로 반환`() {
        every { vectorSearchService.search(any()) } returns RagContext(emptyList(), emptyList())
        every { claudeApiClient.analyze(any(), any(), any(), any()) } returns LlmAnalysisResponse(
            content = "파싱 형식 없이 그냥 수정안만 있는 경우",
            inputTokens = 50,
            outputTokens = 20
        )

        val result = service.suggest("원본 메시지", Channel.SMS)

        assertEquals("파싱 형식 없이 그냥 수정안만 있는 경우", result.suggested)
        assertEquals("", result.rationale)
    }

    @Test
    fun `LlmException 발생 시 그대로 전파`() {
        every { vectorSearchService.search(any()) } returns RagContext(emptyList(), emptyList())
        every { claudeApiClient.analyze(any(), any(), any(), any()) } throws LlmException("API 오류")

        assertThrows<LlmException> {
            service.suggest("테스트 메시지", Channel.SMS)
        }
    }

    @Test
    fun `RAG 컨텍스트가 Claude API에 전달됨`() {
        val ragContext = RagContext(
            lawChunks = listOf(
                LawChunkResult(1L, "방문판매법", "제2조", null, "방문판매 정의", null)
            ),
            precedents = emptyList()
        )
        val capturedRagContext = slot<RagContext>()

        every { vectorSearchService.search(any()) } returns ragContext
        every {
            claudeApiClient.analyze(any(), any(), capture(capturedRagContext), any())
        } returns LlmAnalysisResponse("[수정안]\n수정됨\n\n[수정 이유]\n이유", 100, 40)

        service.suggest("방문판매 특가!", Channel.SMS)

        assertEquals(ragContext, capturedRagContext.captured)
        verify(exactly = 1) { vectorSearchService.search("방문판매 특가!") }
    }
}

package com.adlex.domain.service

import com.adlex.api.dto.ReportRequest
import com.adlex.engine.RuleRegistry
import com.adlex.engine.evaluator.RuleEvaluator
import com.adlex.engine.model.Channel
import com.adlex.engine.model.EvaluationContext
import com.adlex.engine.model.EvaluationResult
import com.adlex.engine.model.RuleType
import com.adlex.domain.entity.Rule
import com.adlex.engine.model.Severity
import com.adlex.infra.rag.LawChunkResult
import com.adlex.infra.rag.PrecedentResult
import com.adlex.infra.rag.RagContext
import com.adlex.infra.rag.VectorSearchService
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ReportServiceTest {

    private val ruleRegistry = mockk<RuleRegistry>()
    private val evaluator = mockk<RuleEvaluator>()
    private val vectorSearchService = mockk<VectorSearchService>()
    private val llmLayer2Service = mockk<LlmLayer2Service>()
    private val llmPlanGuard = mockk<LlmPlanGuard>()

    private val service = ReportService(
        ruleRegistry = ruleRegistry,
        evaluators = listOf(evaluator),
        vectorSearchService = vectorSearchService,
        llmLayer2Service = llmLayer2Service,
        llmPlanGuard = llmPlanGuard
    )

    private val rule = Rule(
        code = "ADV_001", name = "허위 표현 금지", type = RuleType.KEYWORD,
        channel = "SMS", severity = Severity.HIGH, pattern = "100% 효과"
    )

    @Test
    fun `위반 없는 메시지 — 준법 리포트 반환`() {
        every { ruleRegistry.getRulesByChannel(Channel.SMS) } returns listOf(rule)
        every { evaluator.supports(any()) } returns true
        every { evaluator.evaluate(any(), any()) } returns null
        every { vectorSearchService.search(any()) } returns RagContext(emptyList(), emptyList())

        val request = ReportRequest(message = "정직한 광고 메시지", channel = Channel.SMS)
        val report = service.generate(1L, request)

        assertTrue(report.compliant)
        assertEquals(0, report.violationCount)
        assertTrue(report.violations.isEmpty())
    }

    @Test
    fun `위반 있는 메시지 — 위반 목록 포함`() {
        val violation = EvaluationResult(
            ruleCode = "ADV_001", severity = Severity.HIGH,
            message = "금지 표현 포함", legalBasis = "표시광고법 제3조"
        )
        every { ruleRegistry.getRulesByChannel(Channel.SMS) } returns listOf(rule)
        every { evaluator.supports(any()) } returns true
        every { evaluator.evaluate(any(), any()) } returns violation
        every { vectorSearchService.search(any()) } returns RagContext(emptyList(), emptyList())

        val report = service.generate(1L, ReportRequest(message = "100% 효과 보장!", channel = Channel.SMS))

        assertFalse(report.compliant)
        assertEquals(1, report.violationCount)
        assertEquals("ADV_001", report.violations[0].ruleCode)
    }

    @Test
    fun `RAG 법령·판례 포함 리포트`() {
        every { ruleRegistry.getRulesByChannel(Channel.SMS) } returns emptyList()
        every { vectorSearchService.search(any()) } returns RagContext(
            lawChunks = listOf(
                LawChunkResult(1L, "표시광고법", "제3조", "부당표시 금지", "내용...", null)
            ),
            precedents = listOf(
                PrecedentResult(1L, "2021나123", "공정거래위원회", "허위광고 사례", "요약...")
            )
        )

        val report = service.generate(1L, ReportRequest(message = "테스트 메시지", channel = Channel.SMS))

        assertEquals(1, report.relatedLaws.size)
        assertEquals("표시광고법", report.relatedLaws[0].lawName)
        assertEquals(1, report.similarPrecedents.size)
        assertEquals("공정거래위원회", report.similarPrecedents[0].authority)
    }

    @Test
    fun `LLM 분석 요청 — 분석 포함 반환`() {
        every { ruleRegistry.getRulesByChannel(Channel.SMS) } returns emptyList()
        every { vectorSearchService.search(any()) } returns RagContext(emptyList(), emptyList())
        every { llmPlanGuard.requireLlmAccess(1L) } just runs
        every { llmLayer2Service.analyze(any(), any()) } returns LlmLayer2Result(
            analysis = "LLM 분석 결과",
            citedLawCount = 2,
            citedPrecedentCount = 1,
            inputTokens = 100,
            outputTokens = 200
        )

        val report = service.generate(
            1L,
            ReportRequest(message = "테스트 메시지", channel = Channel.SMS, includeLlmAnalysis = true)
        )

        assertNotNull(report.llmAnalysis)
        assertEquals("LLM 분석 결과", report.llmAnalysis!!.analysis)
    }

    @Test
    fun `LLM 미요청 — llmAnalysis null`() {
        every { ruleRegistry.getRulesByChannel(Channel.SMS) } returns emptyList()
        every { vectorSearchService.search(any()) } returns RagContext(emptyList(), emptyList())

        val report = service.generate(
            1L,
            ReportRequest(message = "테스트 메시지", channel = Channel.SMS, includeLlmAnalysis = false)
        )

        assertNull(report.llmAnalysis)
    }

    @Test
    fun `RAG 검색 실패 시 빈 법령·판례 반환 (서비스 계속)`() {
        every { ruleRegistry.getRulesByChannel(Channel.SMS) } returns emptyList()
        every { vectorSearchService.search(any()) } throws RuntimeException("벡터 DB 오류")

        val report = service.generate(1L, ReportRequest(message = "테스트", channel = Channel.SMS))

        assertTrue(report.relatedLaws.isEmpty())
        assertTrue(report.similarPrecedents.isEmpty())
    }
}

package com.adlex.domain.service

import com.adlex.api.dto.*
import com.adlex.engine.RuleRegistry
import com.adlex.engine.evaluator.RuleEvaluator
import com.adlex.engine.model.EvaluationContext
import com.adlex.infra.rag.VectorSearchService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * 준법 검사 리포트 생성 서비스.
 *
 * Layer 1(규칙 엔진) 위반 + RAG 관련 법령·유사 판례 + LLM 보조 분석을
 * 하나의 리포트로 통합하여 반환합니다.
 */
@Service
class ReportService(
    private val ruleRegistry: RuleRegistry,
    private val evaluators: List<RuleEvaluator>,
    private val vectorSearchService: VectorSearchService,
    private val llmLayer2Service: LlmLayer2Service? = null,
    private val llmPlanGuard: LlmPlanGuard? = null
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun generate(tenantId: Long, request: com.adlex.api.dto.ReportRequest): ReportResponse {
        val startMs = System.currentTimeMillis()

        // 1. Layer 1: 규칙 엔진 평가
        val context = EvaluationContext(
            message = request.message,
            channel = request.channel
        )
        val rules = ruleRegistry.getRulesByChannel(context.channel)
        val violations = rules.mapNotNull { rule ->
            evaluators.find { it.supports(rule.type) }?.evaluate(context, rule)
        }

        // 2. RAG: 관련 법령·유사 판례 검색
        val ragContext = runCatching { vectorSearchService.search(request.message) }
            .onFailure { log.warn("RAG 검색 실패 (리포트 계속): ${it.message}") }
            .getOrNull()

        // 3. LLM Layer 2 보조 분석 (옵션 + 플랜 검증)
        val llmResult = if (request.includeLlmAnalysis && llmLayer2Service != null) {
            runCatching {
                llmPlanGuard?.requireLlmAccess(tenantId)
                llmLayer2Service.analyze(request.message, violations)
            }.onFailure { log.warn("LLM 분석 실패 (리포트 계속): ${it.message}") }
                .getOrNull()
        } else null

        val processingMs = System.currentTimeMillis() - startMs

        return ReportResponse(
            message = request.message,
            channel = request.channel,
            compliant = violations.isEmpty(),
            violationCount = violations.size,
            violations = violations.map {
                ViolationDto(
                    ruleCode = it.ruleCode,
                    severity = it.severity,
                    message = it.message,
                    legalBasis = it.legalBasis,
                    suggestion = it.suggestion
                )
            },
            relatedLaws = ragContext?.lawChunks?.map { law ->
                RelatedLawDto(
                    lawName = law.lawName,
                    articleNo = law.articleNo,
                    articleTitle = law.articleTitle,
                    content = law.content,
                    sourceUrl = law.sourceUrl
                )
            } ?: emptyList(),
            similarPrecedents = ragContext?.precedents?.map { p ->
                SimilarPrecedentDto(
                    caseNo = p.caseNo,
                    authority = p.authority,
                    title = p.title,
                    summary = p.summary
                )
            } ?: emptyList(),
            llmAnalysis = llmResult?.let {
                LlmAnalysisDto(
                    analysis = it.analysis,
                    citedLawCount = it.citedLawCount,
                    citedPrecedentCount = it.citedPrecedentCount
                )
            },
            generatedAt = Instant.now(),
            processingMs = processingMs
        )
    }
}

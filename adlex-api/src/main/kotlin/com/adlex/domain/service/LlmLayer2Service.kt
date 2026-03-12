package com.adlex.domain.service

import com.adlex.engine.model.EvaluationResult
import com.adlex.infra.llm.ClaudeApiClient
import com.adlex.infra.llm.LlmException
import com.adlex.infra.rag.VectorSearchService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * LLM Layer 2 보조 분석 서비스.
 *
 * Layer 1(규칙 엔진)이 결정론적으로 법조문 위반을 탐지한 후,
 * Layer 2(LLM + RAG)가 과장·허위 표현 등 맥락적 위험을 보조 검토합니다.
 *
 * - VectorSearchService: 광고 메시지와 유사한 법령·판례 검색
 * - ClaudeApiClient: RAG 컨텍스트 주입 후 Claude API 호출
 */
@Service
class LlmLayer2Service(
    private val vectorSearchService: VectorSearchService,
    private val claudeApiClient: ClaudeApiClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 광고 메시지에 대한 LLM 보조 분석 수행.
     *
     * @param message 광고 메시지
     * @param layer1Violations Layer 1 규칙 엔진 위반 결과 (없으면 빈 리스트)
     * @return LLM 분석 결과. API 오류 시 null 반환 (서비스 장애 격리)
     */
    fun analyze(message: String, layer1Violations: List<EvaluationResult>): LlmLayer2Result? {
        return try {
            val ragContext = vectorSearchService.search(message)
            val userPrompt = buildUserPrompt(message, layer1Violations)

            val response = claudeApiClient.analyze(
                userMessage = userPrompt,
                ragContext = ragContext
            )

            LlmLayer2Result(
                analysis = response.content,
                citedLawCount = ragContext.lawChunks.size,
                citedPrecedentCount = ragContext.precedents.size,
                inputTokens = response.inputTokens,
                outputTokens = response.outputTokens
            ).also {
                log.debug("LLM Layer 2 분석 완료: citedLaws=${it.citedLawCount}, citedPrecedents=${it.citedPrecedentCount}")
            }
        } catch (e: LlmException) {
            log.warn("LLM Layer 2 분석 실패 (서비스 계속): ${e.message}")
            null
        } catch (e: Exception) {
            log.error("LLM Layer 2 분석 중 예외 발생", e)
            null
        }
    }

    private fun buildUserPrompt(message: String, violations: List<EvaluationResult>): String =
        buildString {
            appendLine("다음 광고 메시지를 검토해주세요:")
            appendLine()
            appendLine("【광고 메시지】")
            appendLine(message)

            if (violations.isNotEmpty()) {
                appendLine()
                appendLine("【Layer 1 규칙 엔진 탐지 결과】")
                violations.forEach { v ->
                    appendLine("- [${v.severity}] ${v.ruleCode}: ${v.message}")
                    v.legalBasis?.let { appendLine("  근거: $it") }
                }
                appendLine()
                appendLine("위 탐지 결과를 참고하여, 추가로 다음을 분석해주세요:")
                appendLine("1. 과장·허위 표현 여부 및 소비자 오인 가능성")
                appendLine("2. 법적 위험 수준 평가 (상/중/하)")
                appendLine("3. 구체적인 수정 제안")
            } else {
                appendLine()
                appendLine("규칙 엔진 위반은 없었으나, 다음을 추가 검토해주세요:")
                appendLine("1. 과장·허위 표현 여부 및 소비자 오인 가능성")
                appendLine("2. 법적 위험 수준 평가 (상/중/하)")
                appendLine("3. 개선이 필요한 경우 수정 제안")
            }
        }
}

data class LlmLayer2Result(
    val analysis: String,
    val citedLawCount: Int,
    val citedPrecedentCount: Int,
    val inputTokens: Int,
    val outputTokens: Int
)

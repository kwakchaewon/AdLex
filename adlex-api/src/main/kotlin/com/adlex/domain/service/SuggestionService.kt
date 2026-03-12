package com.adlex.domain.service

import com.adlex.engine.model.Channel
import com.adlex.infra.llm.ClaudeApiClient
import com.adlex.infra.llm.LlmException
import com.adlex.infra.rag.VectorSearchService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 광고 메시지 수정 제안 서비스.
 *
 * Layer 1(규칙 엔진) 위반 여부와 무관하게, 사용자가 명시적으로 요청한 경우
 * RAG 컨텍스트를 활용하여 법규 준수 광고 메시지 수정안을 생성합니다.
 */
@Service
class SuggestionService(
    private val vectorSearchService: VectorSearchService,
    private val claudeApiClient: ClaudeApiClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    data class SuggestionResult(
        val suggested: String,
        val rationale: String,
        val citedLawCount: Int,
        val citedPrecedentCount: Int
    )

    /**
     * 수정 제안 생성.
     *
     * @param message 원본 광고 메시지
     * @param channel 광고 채널 (SMS/KAKAO/EMAIL)
     * @param hint 사용자 힌트 (선택)
     * @return 수정 제안 결과
     * @throws LlmException API 오류 시
     */
    fun suggest(message: String, channel: Channel, hint: String? = null): SuggestionResult {
        log.debug("수정 제안 요청: channel=$channel, messageLength=${message.length}")

        val ragContext = vectorSearchService.search(message)
        val userPrompt = buildSuggestionPrompt(message, channel, hint)

        val response = claudeApiClient.analyze(
            userMessage = userPrompt,
            systemPrompt = SUGGESTION_SYSTEM_PROMPT,
            ragContext = ragContext
        )

        val (suggested, rationale) = parseResponse(response.content, message)

        return SuggestionResult(
            suggested = suggested,
            rationale = rationale,
            citedLawCount = ragContext.lawChunks.size,
            citedPrecedentCount = ragContext.precedents.size
        ).also {
            log.debug("수정 제안 완료: citedLaws=${it.citedLawCount}, citedPrecedents=${it.citedPrecedentCount}")
        }
    }

    private fun buildSuggestionPrompt(message: String, channel: Channel, hint: String?): String =
        buildString {
            appendLine("다음 광고 메시지의 법규 준수 수정안을 작성해주세요.")
            appendLine()
            appendLine("【원본 메시지】")
            appendLine(message)
            appendLine()
            appendLine("【채널】: ${channel.name}")
            if (!hint.isNullOrBlank()) {
                appendLine()
                appendLine("【수정 방향 힌트】: $hint")
            }
            appendLine()
            appendLine("다음 형식으로 응답해주세요:")
            appendLine()
            appendLine("[수정안]")
            appendLine("(수정된 광고 메시지 전체 내용)")
            appendLine()
            appendLine("[수정 이유]")
            appendLine("(어떤 표현이 왜 문제였는지, 어떻게 수정했는지 간결하게 설명)")
        }

    /**
     * LLM 응답에서 수정안과 이유 파싱.
     * [수정안] / [수정 이유] 섹션이 없으면 전체 내용을 수정안으로 처리.
     */
    private fun parseResponse(content: String, originalMessage: String): Pair<String, String> {
        val suggestedRegex = Regex("""\[수정안\]\s*\n([\s\S]*?)(?=\[수정 이유\]|$)""")
        val rationaleRegex = Regex("""\[수정 이유\]\s*\n([\s\S]*)""")

        val suggestedMatch = suggestedRegex.find(content)
        val rationaleMatch = rationaleRegex.find(content)

        return if (suggestedMatch != null) {
            val suggested = suggestedMatch.groupValues[1].trim().ifBlank { originalMessage }
            val rationale = rationaleMatch?.groupValues?.get(1)?.trim() ?: ""
            Pair(suggested, rationale)
        } else {
            // 파싱 실패 시 전체 내용을 수정안으로
            Pair(content.trim(), "")
        }
    }

    companion object {
        private const val SUGGESTION_SYSTEM_PROMPT = """당신은 대한민국 마케팅·광고 법규 전문가입니다.
광고 메시지를 법규에 맞게 수정하되, 원래 메시지의 핵심 의도와 마케팅 효과는 최대한 유지하세요.
수정 시 다음 기준을 따르세요:
1. 표시·광고의 공정화에 관한 법률 준수
2. 근거 없는 최고·최상·1위 등 절대적 표현 제거 또는 근거 명시
3. 과장·허위 표현 → 사실에 기반한 표현으로 대체
4. 필수 고지사항 누락 시 추가 권고
5. 채널별 특성(SMS 90자 제한 등) 고려
응답은 반드시 [수정안]과 [수정 이유] 섹션으로 구분하여 한국어로 작성하세요."""
    }
}

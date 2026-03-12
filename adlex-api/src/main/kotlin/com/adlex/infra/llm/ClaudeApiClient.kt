package com.adlex.infra.llm

import com.adlex.infra.rag.RagContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException

/**
 * Anthropic Claude Messages API 클라이언트.
 * POST https://api.anthropic.com/v1/messages
 * Phase 5 LLM Layer 2 보조 분석에 사용됩니다.
 */
@Component
class ClaudeApiClient(
    @Value("\${claude.api-key:}") private val apiKey: String,
    @Value("\${claude.model:claude-opus-4-6}") private val model: String,
    @Value("\${claude.max-tokens:1024}") private val defaultMaxTokens: Int
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val webClient = WebClient.builder()
        .baseUrl("https://api.anthropic.com")
        .defaultHeader("x-api-key", apiKey)
        .defaultHeader("anthropic-version", "2023-06-01")
        .defaultHeader("Content-Type", "application/json")
        .build()

    /**
     * Claude API 호출. RAG 컨텍스트가 있으면 시스템 프롬프트에 주입합니다.
     *
     * @param userMessage 사용자 광고 메시지
     * @param systemPrompt 시스템 프롬프트 (기본값: 법규 검토 보조 지시)
     * @param ragContext 관련 법령·판례 RAG 컨텍스트 (없으면 null)
     * @param maxTokens 최대 출력 토큰 수
     * @return LLM 분석 결과
     * @throws LlmException API 호출 실패 시
     */
    fun analyze(
        userMessage: String,
        systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
        ragContext: RagContext? = null,
        maxTokens: Int = defaultMaxTokens
    ): LlmAnalysisResponse {
        if (apiKey.isBlank()) {
            log.warn("claude.api-key가 설정되지 않았습니다. LLM 분석을 건너뜁니다.")
            return LlmAnalysisResponse(
                content = "Claude API 키가 설정되지 않아 LLM 분석을 수행할 수 없습니다.",
                inputTokens = 0,
                outputTokens = 0
            )
        }

        val effectiveSystem = if (ragContext?.hasContent == true) {
            "$systemPrompt\n\n${ragContext.toPromptText()}"
        } else {
            systemPrompt
        }

        val body = mapOf(
            "model" to model,
            "max_tokens" to maxTokens,
            "system" to effectiveSystem,
            "messages" to listOf(
                mapOf("role" to "user", "content" to userMessage)
            )
        )

        log.debug("Claude API 요청: model=$model, maxTokens=$maxTokens, ragContext=${ragContext?.hasContent}")

        return try {
            @Suppress("UNCHECKED_CAST")
            val response = webClient.post()
                .uri("/v1/messages")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map::class.java)
                .block() ?: throw LlmException("Claude API 응답 없음")

            val content = parseContent(response)
            val usage = response["usage"] as? Map<String, Any> ?: emptyMap()

            LlmAnalysisResponse(
                content = content,
                inputTokens = (usage["input_tokens"] as? Number)?.toInt() ?: 0,
                outputTokens = (usage["output_tokens"] as? Number)?.toInt() ?: 0
            ).also {
                log.debug("Claude API 응답: inputTokens=${it.inputTokens}, outputTokens=${it.outputTokens}")
            }
        } catch (e: WebClientResponseException) {
            log.error("Claude API HTTP 오류: status=${e.statusCode}, body=${e.responseBodyAsString}")
            throw LlmException("Claude API 호출 실패: ${e.statusCode}", e)
        } catch (e: LlmException) {
            throw e
        } catch (e: Exception) {
            log.error("Claude API 호출 중 예외 발생", e)
            throw LlmException("Claude API 호출 중 오류: ${e.message}", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseContent(response: Map<*, *>): String {
        val contentList = response["content"] as? List<Map<String, Any>>
            ?: throw LlmException("Claude API 응답 형식 오류: content 필드 없음")
        return contentList
            .filter { it["type"] == "text" }
            .joinToString("\n") { it["text"] as? String ?: "" }
            .trim()
            .ifBlank { throw LlmException("Claude API 응답에 텍스트 콘텐츠 없음") }
    }

    companion object {
        private const val DEFAULT_SYSTEM_PROMPT = """당신은 대한민국 마케팅·광고 법규 전문가입니다.
광고 메시지의 과장·허위 표현, 법적 위반 가능성을 분석하고 구체적인 개선 방안을 제시하세요.
분석 시 다음 기준을 따르세요:
1. 표시·광고의 공정화에 관한 법률 위반 여부
2. 과장·허위 표현 또는 소비자 오인 유발 가능성
3. 금지된 표현(최고, 최상, 1위 등) 사용 여부
4. 필수 고지사항 누락 여부
응답은 한국어로 간결하게 작성하세요."""
    }
}

data class LlmAnalysisResponse(
    val content: String,
    val inputTokens: Int,
    val outputTokens: Int
)

class LlmException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

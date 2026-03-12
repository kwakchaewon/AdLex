package com.adlex.infra.embedding

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class OpenAiEmbeddingService(
    @Value("\${openai.api-key}") private val apiKey: String,
    @Value("\${openai.embedding.model:text-embedding-3-small}") private val model: String
) : EmbeddingService {

    private val log = LoggerFactory.getLogger(javaClass)

    private val client = WebClient.builder()
        .baseUrl("https://api.openai.com")
        .defaultHeader("Authorization", "Bearer $apiKey")
        .defaultHeader("Content-Type", "application/json")
        .build()

    override fun embed(text: String): String =
        embedBatch(listOf(text)).first()

    override fun embedBatch(texts: List<String>): List<String> {
        if (apiKey.isBlank()) {
            log.warn("openai.api-key가 설정되지 않았습니다. 임베딩을 건너뜁니다.")
            return texts.map { "[${FloatArray(1536) { 0f }.joinToString(",")}]" }
        }

        data class EmbeddingRequest(val model: String, val input: List<String>)
        data class EmbeddingData(val index: Int, val embedding: List<Float>)
        data class EmbeddingResponse(val data: List<EmbeddingData>)

        log.debug("임베딩 요청: ${texts.size}개 텍스트, 모델=$model")

        val response = client.post()
            .uri("/v1/embeddings")
            .bodyValue(mapOf("model" to model, "input" to texts))
            .retrieve()
            .bodyToMono(Map::class.java)
            .block() ?: error("OpenAI API 응답 없음")

        @Suppress("UNCHECKED_CAST")
        val data = response["data"] as List<Map<String, Any>>

        return data
            .sortedBy { (it["index"] as Number).toInt() }
            .map { item ->
                @Suppress("UNCHECKED_CAST")
                val vector = item["embedding"] as List<Number>
                "[${vector.joinToString(",") { it.toFloat().toString() }}]"
            }
    }
}

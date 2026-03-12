package com.adlex.infra.rag

import com.adlex.domain.repository.LawChunkRepository
import com.adlex.domain.repository.PrecedentRepository
import com.adlex.infra.embedding.EmbeddingService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.Duration

/**
 * RAG 벡터 검색 서비스.
 * 1. 쿼리 텍스트 → 임베딩 (Redis 캐시 1시간)
 * 2. pgvector 코사인 유사도 검색 (law_chunks + precedents)
 * 3. RagContext 반환 → LLM 프롬프트에 주입
 */
@Service
class VectorSearchService(
    private val embeddingService: EmbeddingService,
    private val lawChunkRepository: LawChunkRepository,
    private val precedentRepository: PrecedentRepository,
    private val redisTemplate: StringRedisTemplate,
    @Value("\${adlex.rag.law-chunks-top-k:3}") private val lawTopK: Int,
    @Value("\${adlex.rag.precedents-top-k:2}") private val precedentTopK: Int,
    @Value("\${adlex.rag.cache-ttl-minutes:60}") private val cacheTtlMinutes: Long
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val cacheTtl = Duration.ofMinutes(cacheTtlMinutes)

    /**
     * 메시지와 관련된 법령 및 판례를 검색하여 RagContext 반환.
     * @param query 광고 메시지 또는 검색어
     */
    fun search(query: String): RagContext {
        val embedding = getOrCreateEmbedding(query)

        val laws = runCatching {
            lawChunkRepository.findSimilar(embedding, lawTopK).map {
                LawChunkResult(
                    id = it.id,
                    lawName = it.lawName,
                    articleNo = it.articleNo,
                    articleTitle = it.articleTitle,
                    content = it.content,
                    sourceUrl = it.sourceUrl
                )
            }
        }.onFailure { log.warn("law_chunks 유사도 검색 실패: ${it.message}") }
            .getOrDefault(emptyList())

        val precedents = runCatching {
            precedentRepository.findSimilar(embedding, precedentTopK).map {
                PrecedentResult(
                    id = it.id,
                    caseNo = it.caseNo,
                    authority = it.authority,
                    title = it.title,
                    summary = it.summary
                )
            }
        }.onFailure { log.warn("precedents 유사도 검색 실패: ${it.message}") }
            .getOrDefault(emptyList())

        log.debug("RAG 검색 완료: 법령 ${laws.size}개, 판례 ${precedents.size}개")
        return RagContext(lawChunks = laws, precedents = precedents)
    }

    /**
     * 임베딩 캐시 조회 또는 생성.
     * 동일 쿼리에 대한 반복 임베딩 API 호출 방지.
     */
    private fun getOrCreateEmbedding(query: String): String {
        val cacheKey = "rag:emb:${sha256(query)}"
        val cached = redisTemplate.opsForValue().get(cacheKey)
        if (cached != null) return cached

        val embedding = embeddingService.embed(query)
        redisTemplate.opsForValue().set(cacheKey, embedding, cacheTtl)
        return embedding
    }

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(text.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(32)
    }
}

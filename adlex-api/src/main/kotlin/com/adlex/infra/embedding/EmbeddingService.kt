package com.adlex.infra.embedding

/**
 * 텍스트 → 임베딩 벡터 변환 인터페이스.
 * 기본 구현: OpenAiEmbeddingService (text-embedding-3-small, 1536차원)
 */
interface EmbeddingService {
    /** 텍스트를 1536차원 float 벡터로 변환. pgvector 형식 "[0.1, 0.2, ...]" 반환. */
    fun embed(text: String): String

    /** 여러 텍스트를 배치로 임베딩 (API 호출 횟수 최소화) */
    fun embedBatch(texts: List<String>): List<String>
}

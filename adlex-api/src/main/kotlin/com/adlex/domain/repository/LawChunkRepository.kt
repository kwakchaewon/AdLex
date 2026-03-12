package com.adlex.domain.repository

import com.adlex.domain.entity.LawChunk
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface LawChunkRepository : JpaRepository<LawChunk, Long> {

    fun findAllByActiveTrue(): List<LawChunk>

    fun findAllByLawNameAndActiveTrue(lawName: String): List<LawChunk>

    /**
     * 코사인 유사도 기반 유사 법령 청크 검색.
     * embedding 파라미터: "[0.1, 0.2, ...]" 형식의 pgvector 문자열.
     * <=> 연산자: 코사인 거리 (낮을수록 유사)
     */
    @Query(
        value = """
            SELECT id, law_name, article_no, article_title, content, law_date, source_url, active, created_at, updated_at
            FROM law_chunks
            WHERE active = true
              AND embedding IS NOT NULL
            ORDER BY embedding <=> CAST(:embedding AS vector)
            LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findSimilar(
        @Param("embedding") embedding: String,
        @Param("limit") limit: Int = 3
    ): List<LawChunk>

    /** embedding 저장 (임베딩 파이프라인에서 호출) */
    @Modifying
    @Query(
        value = "UPDATE law_chunks SET embedding = CAST(:embedding AS vector), updated_at = now() WHERE id = :id",
        nativeQuery = true
    )
    fun updateEmbedding(@Param("id") id: Long, @Param("embedding") embedding: String)

    fun existsByLawNameAndArticleNo(lawName: String, articleNo: String?): Boolean
}

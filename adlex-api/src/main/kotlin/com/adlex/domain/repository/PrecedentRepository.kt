package com.adlex.domain.repository

import com.adlex.domain.entity.Precedent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PrecedentRepository : JpaRepository<Precedent, Long> {

    fun findAllByActiveTrue(): List<Precedent>

    /**
     * 코사인 유사도 기반 유사 판례 검색.
     * embedding 파라미터: "[0.1, 0.2, ...]" 형식의 pgvector 문자열.
     */
    @Query(
        value = """
            SELECT id, case_no, authority, title, summary, content, decided_at, active, created_at, updated_at
            FROM precedents
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
    ): List<Precedent>

    /** embedding 저장 */
    @Modifying
    @Query(
        value = "UPDATE precedents SET embedding = CAST(:embedding AS vector), updated_at = now() WHERE id = :id",
        nativeQuery = true
    )
    fun updateEmbedding(@Param("id") id: Long, @Param("embedding") embedding: String)

    fun existsByCaseNoAndAuthority(caseNo: String?, authority: String): Boolean
}

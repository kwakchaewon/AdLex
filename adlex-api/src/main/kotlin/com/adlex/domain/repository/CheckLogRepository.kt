package com.adlex.domain.repository

import com.adlex.domain.entity.CheckLog
import com.adlex.engine.model.Channel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface CheckLogRepository : JpaRepository<CheckLog, Long> {

    @Query("""
        SELECT c FROM CheckLog c
        WHERE c.tenantId = :tenantId
          AND (:channel IS NULL OR c.channel = :channel)
          AND (:compliant IS NULL OR c.compliant = :compliant)
          AND (:from IS NULL OR c.createdAt >= :from)
          AND (:to IS NULL OR c.createdAt <= :to)
        ORDER BY c.createdAt DESC
    """)
    fun findWithFilters(
        tenantId: Long,
        channel: Channel?,
        compliant: Boolean?,
        from: Instant?,
        to: Instant?,
        pageable: Pageable
    ): Page<CheckLog>

    fun findByIdAndTenantId(id: Long, tenantId: Long): CheckLog?
}

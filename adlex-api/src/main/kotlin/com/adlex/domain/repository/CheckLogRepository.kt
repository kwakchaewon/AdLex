package com.adlex.domain.repository

import com.adlex.domain.entity.CheckLog
import com.adlex.engine.model.Channel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

interface ChannelStatProjection {
    val channel: String
    val total: Long
    val compliant: Long
}

interface DailyStatProjection {
    val date: java.sql.Date
    val total: Long
    val compliant: Long
}

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

    fun countByTenantIdAndCreatedAtBetween(tenantId: Long, from: Instant, to: Instant): Long

    fun countByTenantIdAndCompliantTrueAndCreatedAtBetween(tenantId: Long, from: Instant, to: Instant): Long

    @Query("""
        SELECT c.channel AS channel,
               COUNT(c) AS total,
               SUM(CASE WHEN c.compliant = true THEN 1 ELSE 0 END) AS compliant
        FROM CheckLog c
        WHERE c.tenantId = :tenantId
          AND c.createdAt BETWEEN :from AND :to
        GROUP BY c.channel
    """)
    fun countByChannel(tenantId: Long, from: Instant, to: Instant): List<ChannelStatProjection>

    @Query(
        value = """
            SELECT DATE(created_at) AS date,
                   COUNT(*) AS total,
                   SUM(CASE WHEN compliant THEN 1 ELSE 0 END) AS compliant
            FROM check_logs
            WHERE tenant_id = :tenantId
              AND created_at BETWEEN :from AND :to
            GROUP BY DATE(created_at)
            ORDER BY date DESC
        """,
        nativeQuery = true
    )
    fun countByDay(tenantId: Long, from: Instant, to: Instant): List<DailyStatProjection>
}

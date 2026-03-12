package com.adlex.domain.entity

import com.adlex.engine.model.Channel
import jakarta.persistence.*

@Entity
@Table(name = "check_logs")
class CheckLog(

    @Column(name = "tenant_id", nullable = false)
    val tenantId: Long,

    @Column(nullable = false, columnDefinition = "TEXT")
    val message: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val channel: Channel,

    @Column(nullable = false)
    val compliant: Boolean,

    @Column(name = "violation_count", nullable = false)
    val violationCount: Int = 0,

    // JSON 배열 문자열: [{"ruleCode":"...","severity":"HIGH",...}]
    @Column(nullable = false, columnDefinition = "jsonb")
    val violations: String = "[]",

    @Column(name = "processing_ms", nullable = false)
    val processingMs: Long

) : BaseEntity()

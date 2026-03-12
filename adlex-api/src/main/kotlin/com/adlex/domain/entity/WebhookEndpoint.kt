package com.adlex.domain.entity

import jakarta.persistence.*

@Entity
@Table(name = "webhook_endpoints")
class WebhookEndpoint(

    @Column(name = "tenant_id", nullable = false)
    val tenantId: Long,

    @Column(nullable = false, length = 2048)
    var url: String,

    /** HMAC-SHA256 서명에 사용할 시크릿 (생성 시 1회 반환 후 해시 저장 불필요 — 평문 보관) */
    @Column(nullable = false, length = 64)
    val secret: String,

    @Column(nullable = false)
    var active: Boolean = true,

    /** 쉼표 구분 이벤트 목록. 현재 지원: check.completed */
    @Column(nullable = false, length = 100)
    var events: String = "check.completed"

) : BaseEntity()

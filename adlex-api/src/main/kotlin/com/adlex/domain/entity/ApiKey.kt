package com.adlex.domain.entity

import jakarta.persistence.*
import java.time.Instant

enum class ApiKeyStatus {
    ACTIVE,
    REVOKED
}

@Entity
@Table(name = "api_keys")
class ApiKey(

    @Column(name = "tenant_id", nullable = false)
    val tenantId: Long,

    @Column(nullable = false, length = 50)
    val name: String,

    @Column(name = "key_hash", nullable = false, unique = true, length = 64)
    val keyHash: String,

    @Column(name = "key_prefix", nullable = false, length = 12)
    val keyPrefix: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var status: ApiKeyStatus = ApiKeyStatus.ACTIVE,

    @Column(name = "last_used_at")
    var lastUsedAt: Instant? = null

) : BaseEntity()

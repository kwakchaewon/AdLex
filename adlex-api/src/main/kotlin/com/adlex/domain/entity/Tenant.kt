package com.adlex.domain.entity

import jakarta.persistence.*

enum class Plan {
    FREE,
    STARTER,
    PRO,
    ENTERPRISE
}

@Entity
@Table(name = "tenants")
class Tenant(

    @Column(nullable = false, unique = true, length = 255)
    val email: String,

    @Column(name = "password_hash", nullable = false, length = 255)
    var passwordHash: String,

    @Column(name = "company_name", length = 100)
    var companyName: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var plan: Plan = Plan.FREE,

    @Column(name = "monthly_quota", nullable = false)
    var monthlyQuota: Int = 100,

    @Column(name = "monthly_used", nullable = false)
    var monthlyUsed: Int = 0

) : BaseEntity()

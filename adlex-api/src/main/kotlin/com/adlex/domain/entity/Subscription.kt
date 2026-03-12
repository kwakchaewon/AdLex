package com.adlex.domain.entity

import jakarta.persistence.*
import java.time.Instant

enum class SubscriptionStatus { PENDING, ACTIVE, CANCELLED, EXPIRED }

@Entity
@Table(name = "subscriptions")
class Subscription(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    val tenant: Tenant,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var plan: Plan,

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "subscription_status", nullable = false)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    var status: SubscriptionStatus = SubscriptionStatus.PENDING,

    @Column(name = "portone_payment_id", length = 100)
    var portonePaymentId: String? = null,

    @Column(name = "portone_customer_uid", length = 100)
    var portoneCustomerUid: String? = null,

    @Column(name = "current_period_start", nullable = false)
    var currentPeriodStart: Instant = Instant.now(),

    @Column(name = "current_period_end", nullable = false)
    var currentPeriodEnd: Instant = Instant.now().plusSeconds(30L * 24 * 3600),

    @Column(name = "cancelled_at")
    var cancelledAt: Instant? = null

) : BaseEntity()

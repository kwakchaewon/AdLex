package com.adlex.domain.repository

import com.adlex.domain.entity.Subscription
import com.adlex.domain.entity.SubscriptionStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SubscriptionRepository : JpaRepository<Subscription, Long> {

    @Query("SELECT s FROM Subscription s WHERE s.tenant.id = :tenantId AND s.status = :status ORDER BY s.createdAt DESC")
    fun findFirstByTenantIdAndStatus(tenantId: Long, status: SubscriptionStatus): List<Subscription>

    @Query("SELECT s FROM Subscription s WHERE s.tenant.id = :tenantId AND s.portonePaymentId = :paymentId")
    fun findByTenantIdAndPaymentId(tenantId: Long, paymentId: String): Subscription?
}

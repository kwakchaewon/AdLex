package com.adlex.domain.repository

import com.adlex.domain.entity.WebhookEndpoint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WebhookEndpointRepository : JpaRepository<WebhookEndpoint, Long> {
    fun findAllByTenantId(tenantId: Long): List<WebhookEndpoint>
    fun findAllByTenantIdAndActiveTrue(tenantId: Long): List<WebhookEndpoint>
    fun findByIdAndTenantId(id: Long, tenantId: Long): WebhookEndpoint?
}

package com.adlex.domain.repository

import com.adlex.domain.entity.ApiKey
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ApiKeyRepository : JpaRepository<ApiKey, Long> {
    fun findByKeyHash(keyHash: String): ApiKey?
    fun findAllByTenantId(tenantId: Long): List<ApiKey>
}

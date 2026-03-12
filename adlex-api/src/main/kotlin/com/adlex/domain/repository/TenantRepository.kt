package com.adlex.domain.repository

import com.adlex.domain.entity.Tenant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TenantRepository : JpaRepository<Tenant, Long> {
    fun findByEmail(email: String): Tenant?
}

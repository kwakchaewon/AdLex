package com.adlex.api.dto

import com.adlex.domain.entity.Plan
import com.adlex.domain.entity.Tenant
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class AuthResponse(
    @Schema(description = "액세스 토큰 (30분)")
    val accessToken: String,

    @Schema(description = "리프레시 토큰 (7일)")
    val refreshToken: String,

    @Schema(description = "토큰 타입", example = "Bearer")
    val tokenType: String = "Bearer"
)

data class MeResponse(
    val id: Long,
    val email: String,
    val companyName: String?,
    val plan: Plan,
    val monthlyQuota: Int,
    val monthlyUsed: Int,
    val createdAt: Instant
) {
    companion object {
        fun from(tenant: Tenant) = MeResponse(
            id = tenant.id,
            email = tenant.email,
            companyName = tenant.companyName,
            plan = tenant.plan,
            monthlyQuota = tenant.monthlyQuota,
            monthlyUsed = tenant.monthlyUsed,
            createdAt = tenant.createdAt
        )
    }
}

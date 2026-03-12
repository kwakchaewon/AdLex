package com.adlex.api.dto

import com.adlex.domain.entity.Plan
import com.adlex.domain.entity.Subscription
import com.adlex.domain.entity.SubscriptionStatus
import com.adlex.domain.entity.Tenant
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "구독 상태 응답")
data class SubscriptionStatusResponse(
    @Schema(description = "구독 ID (없으면 null)") val subscriptionId: Long?,
    @Schema(description = "현재 플랜") val plan: Plan,
    @Schema(description = "구독 상태") val status: SubscriptionStatus?,
    @Schema(description = "현재 구독 기간 종료일 (ISO-8601)") val currentPeriodEnd: String?,
    @Schema(description = "월 검사 한도") val monthlyQuota: Int,
    @Schema(description = "당월 사용량") val monthlyUsed: Int
) {
    companion object {
        fun from(tenant: Tenant, sub: Subscription? = null) = SubscriptionStatusResponse(
            subscriptionId = sub?.id,
            plan = tenant.plan,
            status = sub?.status,
            currentPeriodEnd = sub?.currentPeriodEnd?.toString(),
            monthlyQuota = tenant.monthlyQuota,
            monthlyUsed = tenant.monthlyUsed
        )
    }
}

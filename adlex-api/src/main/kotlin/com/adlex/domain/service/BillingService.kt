package com.adlex.domain.service

import com.adlex.api.advice.BusinessException
import com.adlex.api.advice.ErrorCode
import com.adlex.api.dto.ConfirmRequest
import com.adlex.api.dto.SubscribeRequest
import com.adlex.api.dto.SubscriptionStatusResponse
import com.adlex.domain.entity.Plan
import com.adlex.domain.entity.Subscription
import com.adlex.domain.entity.SubscriptionStatus
import com.adlex.domain.repository.SubscriptionRepository
import com.adlex.domain.repository.TenantRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.Instant

@Service
class BillingService(
    private val tenantRepository: TenantRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val webClient: WebClient
) {
    @Value("\${portone.api-key:}") private val portoneApiKey: String = ""
    @Value("\${portone.api-secret:}") private val portoneApiSecret: String = ""

    companion object {
        val PLAN_QUOTA = mapOf(
            Plan.FREE to 100,
            Plan.STARTER to 1_000,
            Plan.PRO to 10_000,
            Plan.ENTERPRISE to Int.MAX_VALUE
        )
    }

    @Transactional
    fun subscribe(tenantId: Long, req: SubscribeRequest): SubscriptionStatusResponse {
        val tenant = tenantRepository.findById(tenantId).orElseThrow {
            BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다")
        }

        // 기존 ACTIVE 구독 취소
        subscriptionRepository.findFirstByTenantIdAndStatus(tenantId, SubscriptionStatus.ACTIVE)
            .firstOrNull()
            ?.apply { status = SubscriptionStatus.CANCELLED; cancelledAt = Instant.now() }

        if (req.plan == Plan.FREE) {
            tenant.plan = Plan.FREE
            tenant.monthlyQuota = PLAN_QUOTA[Plan.FREE]!!
            tenantRepository.save(tenant)
            return SubscriptionStatusResponse.from(tenant)
        }

        val now = Instant.now()
        val sub = subscriptionRepository.save(
            Subscription(
                tenant = tenant,
                plan = req.plan,
                status = SubscriptionStatus.PENDING,
                portoneCustomerUid = req.customerUid,
                currentPeriodStart = now,
                currentPeriodEnd = now.plusSeconds(30L * 24 * 3600)
            )
        )
        return SubscriptionStatusResponse.from(tenant, sub)
    }

    @Transactional
    fun confirm(tenantId: Long, req: ConfirmRequest): SubscriptionStatusResponse {
        val tenant = tenantRepository.findById(tenantId).orElseThrow {
            BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다")
        }

        val paymentStatus = fetchPortonePaymentStatus(req.impUid)
        if (paymentStatus != "paid") {
            throw BusinessException(ErrorCode.VALIDATION_ERROR, "결제가 완료되지 않았습니다 (status: $paymentStatus)")
        }

        val sub = subscriptionRepository.findFirstByTenantIdAndStatus(tenantId, SubscriptionStatus.PENDING)
            .firstOrNull()
            ?: throw BusinessException(ErrorCode.NOT_FOUND, "대기 중인 구독을 찾을 수 없습니다")

        val now = Instant.now()
        sub.status = SubscriptionStatus.ACTIVE
        sub.portonePaymentId = req.impUid
        sub.currentPeriodStart = now
        sub.currentPeriodEnd = now.plusSeconds(30L * 24 * 3600)

        tenant.plan = sub.plan
        tenant.monthlyQuota = PLAN_QUOTA[sub.plan] ?: 100
        tenantRepository.save(tenant)
        subscriptionRepository.save(sub)

        return SubscriptionStatusResponse.from(tenant, sub)
    }

    @Transactional
    fun cancel(tenantId: Long): SubscriptionStatusResponse {
        val tenant = tenantRepository.findById(tenantId).orElseThrow {
            BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다")
        }

        val sub = subscriptionRepository.findFirstByTenantIdAndStatus(tenantId, SubscriptionStatus.ACTIVE)
            .firstOrNull()
            ?: throw BusinessException(ErrorCode.NOT_FOUND, "활성 구독이 없습니다")

        sub.status = SubscriptionStatus.CANCELLED
        sub.cancelledAt = Instant.now()
        subscriptionRepository.save(sub)

        tenant.plan = Plan.FREE
        tenant.monthlyQuota = PLAN_QUOTA[Plan.FREE]!!
        tenantRepository.save(tenant)

        return SubscriptionStatusResponse.from(tenant, sub)
    }

    @Transactional(readOnly = true)
    fun getStatus(tenantId: Long): SubscriptionStatusResponse {
        val tenant = tenantRepository.findById(tenantId).orElseThrow {
            BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다")
        }
        val sub = subscriptionRepository.findFirstByTenantIdAndStatus(tenantId, SubscriptionStatus.ACTIVE)
            .firstOrNull()
        return SubscriptionStatusResponse.from(tenant, sub)
    }

    private fun fetchPortonePaymentStatus(impUid: String): String {
        val token = getPortoneToken()
        return try {
            val resp = webClient.get()
                .uri("https://api.iamport.kr/payments/{impUid}", impUid)
                .header("Authorization", token)
                .retrieve()
                .bodyToMono<Map<String, Any>>()
                .block() ?: return "error"
            @Suppress("UNCHECKED_CAST")
            val response = resp["response"] as? Map<String, Any> ?: return "error"
            response["status"] as? String ?: "error"
        } catch (e: Exception) {
            "error"
        }
    }

    private fun getPortoneToken(): String {
        return try {
            val resp = webClient.post()
                .uri("https://api.iamport.kr/users/getToken")
                .bodyValue(mapOf("imp_key" to portoneApiKey, "imp_secret" to portoneApiSecret))
                .retrieve()
                .bodyToMono<Map<String, Any>>()
                .block() ?: return ""
            @Suppress("UNCHECKED_CAST")
            val response = resp["response"] as? Map<String, Any> ?: return ""
            response["access_token"] as? String ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}

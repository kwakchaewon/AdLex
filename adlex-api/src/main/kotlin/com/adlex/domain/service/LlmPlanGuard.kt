package com.adlex.domain.service

import com.adlex.api.advice.BusinessException
import com.adlex.api.advice.ErrorCode
import com.adlex.domain.entity.Plan
import com.adlex.domain.repository.TenantRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * LLM Layer 2 기능 접근 플랜 제어.
 * PRO 이상 플랜(PRO, ENTERPRISE)만 LLM 기능을 사용할 수 있습니다.
 */
@Component
class LlmPlanGuard(
    private val tenantRepository: TenantRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * LLM 기능 사용 가능 여부 확인.
     * 테넌트를 찾을 수 없으면 FREE 플랜으로 간주합니다.
     */
    fun canUseLlm(tenantId: Long): Boolean {
        val plan = tenantRepository.findById(tenantId).map { it.plan }.orElse(Plan.FREE)
        return plan.ordinal >= Plan.PRO.ordinal
    }

    /**
     * LLM 기능 사용 가능 여부 강제 검증.
     * PRO 미만 플랜이면 [BusinessException] 발생.
     *
     * @throws BusinessException PRO 미만 플랜인 경우 PLAN_UPGRADE_REQUIRED(402)
     */
    fun requireLlmAccess(tenantId: Long) {
        if (!canUseLlm(tenantId)) {
            val plan = tenantRepository.findById(tenantId).map { it.plan }.orElse(Plan.FREE)
            log.debug("LLM 접근 거부: tenantId=$tenantId, plan=$plan")
            throw BusinessException(
                ErrorCode.PLAN_UPGRADE_REQUIRED,
                "LLM 기능은 PRO 플랜 이상에서만 사용 가능합니다. 현재 플랜: $plan"
            )
        }
    }
}

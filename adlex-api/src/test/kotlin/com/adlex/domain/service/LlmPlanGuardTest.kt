package com.adlex.domain.service

import com.adlex.api.advice.BusinessException
import com.adlex.api.advice.ErrorCode
import com.adlex.domain.entity.Plan
import com.adlex.domain.entity.Tenant
import com.adlex.domain.repository.TenantRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Optional

class LlmPlanGuardTest {

    private val tenantRepository = mockk<TenantRepository>()
    private lateinit var guard: LlmPlanGuard

    @BeforeEach
    fun setUp() {
        guard = LlmPlanGuard(tenantRepository)
    }

    private fun tenant(plan: Plan) =
        Tenant(email = "test@test.com", passwordHash = "hash").also { it.plan = plan }

    @Test
    fun `FREE 플랜은 LLM 사용 불가`() {
        every { tenantRepository.findById(1L) } returns Optional.of(tenant(Plan.FREE))
        assertFalse(guard.canUseLlm(1L))
    }

    @Test
    fun `STARTER 플랜은 LLM 사용 불가`() {
        every { tenantRepository.findById(1L) } returns Optional.of(tenant(Plan.STARTER))
        assertFalse(guard.canUseLlm(1L))
    }

    @Test
    fun `PRO 플랜은 LLM 사용 가능`() {
        every { tenantRepository.findById(1L) } returns Optional.of(tenant(Plan.PRO))
        assertTrue(guard.canUseLlm(1L))
    }

    @Test
    fun `ENTERPRISE 플랜은 LLM 사용 가능`() {
        every { tenantRepository.findById(1L) } returns Optional.of(tenant(Plan.ENTERPRISE))
        assertTrue(guard.canUseLlm(1L))
    }

    @Test
    fun `테넌트 없으면 LLM 사용 불가 (FREE 간주)`() {
        every { tenantRepository.findById(99L) } returns Optional.empty()
        assertFalse(guard.canUseLlm(99L))
    }

    @Test
    fun `requireLlmAccess - FREE 플랜이면 PLAN_UPGRADE_REQUIRED 예외 발생`() {
        every { tenantRepository.findById(1L) } returns Optional.of(tenant(Plan.FREE))

        val ex = assertThrows<BusinessException> { guard.requireLlmAccess(1L) }
        assertEquals(ErrorCode.PLAN_UPGRADE_REQUIRED, ex.errorCode)
        assertTrue(ex.message!!.contains("PRO"))
        assertTrue(ex.message!!.contains("FREE"))
    }

    @Test
    fun `requireLlmAccess - STARTER 플랜이면 예외 발생`() {
        every { tenantRepository.findById(1L) } returns Optional.of(tenant(Plan.STARTER))

        assertThrows<BusinessException> { guard.requireLlmAccess(1L) }
    }

    @Test
    fun `requireLlmAccess - PRO 플랜이면 예외 없음`() {
        every { tenantRepository.findById(1L) } returns Optional.of(tenant(Plan.PRO))
        assertDoesNotThrow { guard.requireLlmAccess(1L) }
    }

    @Test
    fun `requireLlmAccess - ENTERPRISE 플랜이면 예외 없음`() {
        every { tenantRepository.findById(1L) } returns Optional.of(tenant(Plan.ENTERPRISE))
        assertDoesNotThrow { guard.requireLlmAccess(1L) }
    }
}

package com.adlex.domain.service

import com.adlex.api.advice.BusinessException
import com.adlex.api.dto.ConfirmRequest
import com.adlex.api.dto.SubscribeRequest
import com.adlex.domain.entity.Plan
import com.adlex.domain.entity.Subscription
import com.adlex.domain.entity.SubscriptionStatus
import com.adlex.domain.entity.Tenant
import com.adlex.domain.repository.SubscriptionRepository
import com.adlex.domain.repository.TenantRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.util.Optional

class BillingServiceTest {

    private val tenantRepository = mockk<TenantRepository>()
    private val subscriptionRepository = mockk<SubscriptionRepository>()
    private val webClient = mockk<WebClient>()
    private lateinit var service: BillingService

    private val tenant = Tenant(
        email = "test@example.com",
        passwordHash = "hash",
        plan = Plan.FREE,
        monthlyQuota = 100,
        monthlyUsed = 0
    )

    @BeforeEach
    fun setUp() {
        service = BillingService(tenantRepository, subscriptionRepository, webClient)
        every { tenantRepository.findById(1L) } returns Optional.of(tenant)
        every { tenantRepository.save(any()) } returnsArgument 0
        every { subscriptionRepository.findFirstByTenantIdAndStatus(1L, SubscriptionStatus.ACTIVE) } returns emptyList()
        every { subscriptionRepository.save(any()) } answers { firstArg() }
    }

    @Test
    fun `subscribe FREE - cancels active sub and sets FREE plan`() {
        val activeSub = mockk<Subscription>(relaxed = true)
        every { subscriptionRepository.findFirstByTenantIdAndStatus(1L, SubscriptionStatus.ACTIVE) } returns listOf(activeSub)

        val result = service.subscribe(1L, SubscribeRequest(Plan.FREE, ""))

        assertThat(result.plan).isEqualTo(Plan.FREE)
        assertThat(result.monthlyQuota).isEqualTo(100)
        verify { activeSub.status = SubscriptionStatus.CANCELLED }
    }

    @Test
    fun `subscribe paid plan - creates PENDING subscription`() {
        every { subscriptionRepository.findFirstByTenantIdAndStatus(1L, SubscriptionStatus.PENDING) } returns emptyList()

        val result = service.subscribe(1L, SubscribeRequest(Plan.PRO, "customer_uid_abc"))

        assertThat(result.status).isEqualTo(SubscriptionStatus.PENDING)
        verify { subscriptionRepository.save(match { it.plan == Plan.PRO && it.status == SubscriptionStatus.PENDING }) }
    }

    @Test
    fun `confirm - activates subscription when PortOne returns paid`() {
        val sub = Subscription(tenant = tenant, plan = Plan.PRO, status = SubscriptionStatus.PENDING)
        every { subscriptionRepository.findFirstByTenantIdAndStatus(1L, SubscriptionStatus.PENDING) } returns listOf(sub)
        mockPortoneResponse("paid")

        val result = service.confirm(1L, ConfirmRequest("imp_123", "adlex_order_001"))

        assertThat(result.plan).isEqualTo(Plan.PRO)
        assertThat(result.status).isEqualTo(SubscriptionStatus.ACTIVE)
        assertThat(result.monthlyQuota).isEqualTo(10_000)
    }

    @Test
    fun `confirm - throws when PortOne status is not paid`() {
        mockPortoneResponse("failed")

        assertThatThrownBy { service.confirm(1L, ConfirmRequest("imp_fail", "order_fail")) }
            .isInstanceOf(BusinessException::class.java)
            .hasMessageContaining("결제가 완료되지 않았습니다")
    }

    @Test
    fun `cancel - cancels active subscription and restores FREE plan`() {
        val sub = Subscription(tenant = tenant, plan = Plan.PRO, status = SubscriptionStatus.ACTIVE)
        every { subscriptionRepository.findFirstByTenantIdAndStatus(1L, SubscriptionStatus.ACTIVE) } returns listOf(sub)

        val result = service.cancel(1L)

        assertThat(result.plan).isEqualTo(Plan.FREE)
        assertThat(result.status).isEqualTo(SubscriptionStatus.CANCELLED)
        assertThat(result.monthlyQuota).isEqualTo(100)
    }

    @Test
    fun `cancel - throws when no active subscription`() {
        assertThatThrownBy { service.cancel(1L) }
            .isInstanceOf(BusinessException::class.java)
            .hasMessageContaining("활성 구독이 없습니다")
    }

    @Test
    fun `getStatus - returns active subscription`() {
        val sub = Subscription(tenant = tenant, plan = Plan.STARTER, status = SubscriptionStatus.ACTIVE)
        every { subscriptionRepository.findFirstByTenantIdAndStatus(1L, SubscriptionStatus.ACTIVE) } returns listOf(sub)

        val result = service.getStatus(1L)

        assertThat(result.plan).isEqualTo(Plan.FREE) // tenant.plan not updated in mock
        assertThat(result.status).isEqualTo(SubscriptionStatus.ACTIVE)
    }

    @Test
    fun `getStatus - returns FREE with no subscription when no active sub exists`() {
        val result = service.getStatus(1L)

        assertThat(result.plan).isEqualTo(Plan.FREE)
        assertThat(result.status).isNull()
        assertThat(result.subscriptionId).isNull()
    }

    // ── helpers ──

    private fun mockPortoneResponse(status: String) {
        val reqSpec    = mockk<WebClient.RequestHeadersUriSpec<*>>()
        val reqHeaders = mockk<WebClient.RequestHeadersSpec<*>>()
        val respSpec   = mockk<WebClient.ResponseSpec>()
        val tokenReqSpec    = mockk<WebClient.RequestBodyUriSpec>()
        val tokenReqHeaders = mockk<WebClient.RequestHeadersSpec<*>>()
        val tokenRespSpec   = mockk<WebClient.ResponseSpec>()

        // token call
        every { webClient.post() } returns tokenReqSpec
        every { tokenReqSpec.uri(any<String>()) } returns tokenReqHeaders
        every { (tokenReqHeaders as WebClient.RequestBodySpec).bodyValue(any()) } returns tokenReqHeaders
        every { tokenReqHeaders.retrieve() } returns tokenRespSpec
        every { tokenRespSpec.bodyToMono(Map::class.java) } returns
            Mono.just(mapOf("response" to mapOf("access_token" to "test-token")))

        // payment status call
        every { webClient.get() } returns reqSpec
        every { reqSpec.uri(any<String>(), any<String>()) } returns reqHeaders
        every { reqHeaders.header(any(), any()) } returns reqHeaders
        every { reqHeaders.retrieve() } returns respSpec
        every { respSpec.bodyToMono(Map::class.java) } returns
            Mono.just(mapOf("response" to mapOf("status" to status)))
    }
}

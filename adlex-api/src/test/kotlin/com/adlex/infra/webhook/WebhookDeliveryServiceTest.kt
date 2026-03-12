package com.adlex.infra.webhook

import com.adlex.domain.entity.WebhookEndpoint
import com.adlex.domain.repository.WebhookEndpointRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.Instant

class WebhookDeliveryServiceTest {

    private val repository = mockk<WebhookEndpointRepository>()
    private val webClient = mockk<WebClient>()
    private val objectMapper = jacksonObjectMapper()

    private val service = WebhookDeliveryService(repository, webClient, objectMapper)

    private val requestBodySpec = mockk<WebClient.RequestBodySpec>()
    private val requestHeadersSpec = mockk<WebClient.RequestHeadersSpec<*>>()
    private val responseSpec = mockk<WebClient.ResponseSpec>()

    @BeforeEach
    fun setUp() {
        val uriSpec = mockk<WebClient.RequestBodyUriSpec>()
        every { webClient.post() } returns uriSpec
        every { uriSpec.uri(any<String>()) } returns requestBodySpec
        every { requestBodySpec.header(any(), any()) } returns requestBodySpec

        @Suppress("UNCHECKED_CAST")
        every { requestBodySpec.bodyValue(any()) } returns requestHeadersSpec as WebClient.RequestBodySpec
        every { (requestHeadersSpec as WebClient.RequestHeadersSpec<*>).retrieve() } returns responseSpec
    }

    @Test
    fun `활성 웹훅 없으면 전송 안함`() {
        every { repository.findAllByTenantIdAndActiveTrue(1L) } returns emptyList()

        service.deliverAsync(1L, "check.completed", mapOf("test" to true))

        verify(exactly = 0) { webClient.post() }
    }

    @Test
    fun `이벤트 불일치하면 전송 안함`() {
        val endpoint = mockk<WebhookEndpoint>()
        every { endpoint.events } returns "other.event"
        every { repository.findAllByTenantIdAndActiveTrue(1L) } returns listOf(endpoint)

        service.deliverAsync(1L, "check.completed", mapOf("test" to true))

        verify(exactly = 0) { webClient.post() }
    }

    @Test
    fun `정상 전송 성공`() {
        val endpoint = WebhookEndpoint(
            tenantId = 1L,
            url = "https://example.com/webhook",
            secret = "abc123",
            events = "check.completed"
        )
        every { repository.findAllByTenantIdAndActiveTrue(1L) } returns listOf(endpoint)
        every { responseSpec.toBodilessEntity() } returns Mono.just(ResponseEntity.ok().build())

        service.deliverAsync(1L, "check.completed", mapOf("data" to "test"))

        verify { webClient.post() }
    }

    @Test
    fun `HTTP 오류 시 재시도 후 최종 실패 로그`() {
        val endpoint = WebhookEndpoint(
            tenantId = 1L,
            url = "https://fail.example.com/webhook",
            secret = "abc123",
            events = "check.completed"
        )
        every { repository.findAllByTenantIdAndActiveTrue(1L) } returns listOf(endpoint)
        every { responseSpec.toBodilessEntity() } throws
                WebClientResponseException.create(500, "Internal Server Error", mockk(), byteArrayOf(), null)

        // 재시도 딜레이를 0으로 설정
        ReflectionTestUtils.setField(service, "retryDelayOverride", 0L)

        // 예외 없이 완료되어야 함 (최종 실패는 로그만)
        assertDoesNotThrow {
            service.deliverAsync(1L, "check.completed", mapOf("data" to "test"))
        }
    }

    @Test
    fun `테스트 이벤트 전송 성공`() {
        every { responseSpec.toBodilessEntity() } returns Mono.just(ResponseEntity.ok().build())

        val result = service.deliverTest("https://example.com/webhook", "secret123")

        assertTrue(result.success)
        assertEquals(200, result.statusCode)
    }

    @Test
    fun `테스트 이벤트 전송 실패 시 결과 반환`() {
        every { responseSpec.toBodilessEntity() } throws
                WebClientResponseException.create(404, "Not Found", mockk(), byteArrayOf(), null)

        val result = service.deliverTest("https://bad.example.com/webhook", "secret123")

        assertFalse(result.success)
        assertEquals(404, result.statusCode)
    }

    @Test
    fun `HMAC 서명이 동일 입력에 동일 값 반환`() {
        val method = service.javaClass.getDeclaredMethod("sign", String::class.java, String::class.java)
        method.isAccessible = true

        val sig1 = method.invoke(service, "mysecret", "1234567890.{\"data\":\"test\"}")
        val sig2 = method.invoke(service, "mysecret", "1234567890.{\"data\":\"test\"}")

        assertEquals(sig1, sig2)
    }
}

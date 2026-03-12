package com.adlex.domain.service

import com.adlex.api.advice.BusinessException
import com.adlex.api.dto.WebhookCreateRequest
import com.adlex.domain.entity.WebhookEndpoint
import com.adlex.domain.repository.WebhookEndpointRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WebhookEndpointServiceTest {

    private val repository = mockk<WebhookEndpointRepository>()
    private val service = WebhookEndpointService(repository)

    @Test
    fun `웹훅 엔드포인트 생성 — secret 포함 반환`() {
        val slot = slot<WebhookEndpoint>()
        every { repository.save(capture(slot)) } answers {
            slot.captured.also {
                // simulate DB id assignment
            }
        }

        val response = service.create(
            tenantId = 1L,
            request = WebhookCreateRequest(url = "https://example.com/hook", events = listOf("check.completed"))
        )

        assertNotNull(response.secret)
        assertEquals(64, response.secret!!.length) // 32 bytes hex
        assertEquals("https://example.com/hook", response.url)
        assertTrue(response.active)
        assertEquals(listOf("check.completed"), response.events)
    }

    @Test
    fun `목록 조회 — secret null 반환`() {
        val endpoint = WebhookEndpoint(1L, "https://example.com/hook", "secret123", true, "check.completed")
        every { repository.findAllByTenantId(1L) } returns listOf(endpoint)

        val result = service.findAll(1L)

        assertEquals(1, result.size)
        assertNull(result[0].secret)
    }

    @Test
    fun `존재하지 않는 ID 삭제 시 예외`() {
        every { repository.findByIdAndTenantId(99L, 1L) } returns null

        assertThrows(BusinessException::class.java) {
            service.delete(tenantId = 1L, webhookId = 99L)
        }
    }

    @Test
    fun `정상 삭제`() {
        val endpoint = WebhookEndpoint(1L, "https://example.com/hook", "secret123")
        every { repository.findByIdAndTenantId(1L, 1L) } returns endpoint
        every { repository.delete(endpoint) } just runs

        assertDoesNotThrow { service.delete(tenantId = 1L, webhookId = 1L) }
        verify { repository.delete(endpoint) }
    }
}

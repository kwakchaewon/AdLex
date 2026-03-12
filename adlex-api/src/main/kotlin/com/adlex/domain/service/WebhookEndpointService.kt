package com.adlex.domain.service

import com.adlex.api.dto.WebhookCreateRequest
import com.adlex.api.dto.WebhookResponse
import com.adlex.domain.entity.WebhookEndpoint
import com.adlex.domain.repository.WebhookEndpointRepository
import com.adlex.api.advice.BusinessException
import com.adlex.api.advice.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom

@Service
class WebhookEndpointService(
    private val webhookEndpointRepository: WebhookEndpointRepository
) {
    private val secureRandom = SecureRandom()
    private val hexChars = "0123456789abcdef".toCharArray()

    @Transactional
    fun create(tenantId: Long, request: WebhookCreateRequest): WebhookResponse {
        val secret = generateSecret()
        val events = request.events.joinToString(",")
        val endpoint = webhookEndpointRepository.save(
            WebhookEndpoint(
                tenantId = tenantId,
                url = request.url,
                secret = secret,
                events = events
            )
        )
        return toResponse(endpoint, includeSecret = true)
    }

    @Transactional(readOnly = true)
    fun findAll(tenantId: Long): List<WebhookResponse> =
        webhookEndpointRepository.findAllByTenantId(tenantId).map { toResponse(it) }

    @Transactional
    fun delete(tenantId: Long, webhookId: Long) {
        val endpoint = webhookEndpointRepository.findByIdAndTenantId(webhookId, tenantId)
            ?: throw BusinessException(ErrorCode.NOT_FOUND, "웹훅 엔드포인트를 찾을 수 없습니다.")
        webhookEndpointRepository.delete(endpoint)
    }

    @Transactional(readOnly = true)
    fun findById(tenantId: Long, webhookId: Long): WebhookEndpoint =
        webhookEndpointRepository.findByIdAndTenantId(webhookId, tenantId)
            ?: throw BusinessException(ErrorCode.NOT_FOUND, "웹훅 엔드포인트를 찾을 수 없습니다.")

    private fun generateSecret(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun toResponse(endpoint: WebhookEndpoint, includeSecret: Boolean = false) = WebhookResponse(
        id = endpoint.id,
        url = endpoint.url,
        secret = if (includeSecret) endpoint.secret else null,
        active = endpoint.active,
        events = endpoint.events.split(","),
        createdAt = endpoint.createdAt
    )
}

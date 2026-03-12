package com.adlex.infra.webhook

import com.adlex.domain.repository.WebhookEndpointRepository
import com.adlex.domain.service.CheckResultDto
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * 준법 검사 결과를 테넌트의 Webhook 엔드포인트로 비동기 전송.
 *
 * - 이벤트: check.completed
 * - 서명: X-AdLex-Signature: sha256=<HMAC-SHA256(secret, body)>
 * - 재시도: 최대 3회 (1s → 3s → 9s 백오프)
 */
@Service
class WebhookDeliveryService(
    private val webhookEndpointRepository: WebhookEndpointRepository,
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    fun deliverAsync(tenantId: Long, event: String, payload: Any) {
        val endpoints = webhookEndpointRepository.findAllByTenantIdAndActiveTrue(tenantId)
        if (endpoints.isEmpty()) return

        val body = objectMapper.writeValueAsString(payload)
        val timestamp = Instant.now().epochSecond

        endpoints.filter { event in it.events.split(",") }.forEach { endpoint ->
            val signature = sign(endpoint.secret, "$timestamp.$body")
            deliver(endpoint.url, body, timestamp, signature, event)
        }
    }

    /** 테스트 이벤트 동기 발송 (결과 반환) */
    fun deliverTest(url: String, secret: String): WebhookDeliveryResult {
        val payload = objectMapper.writeValueAsString(
            mapOf("event" to "test", "message" to "AdLex webhook test event")
        )
        val timestamp = Instant.now().epochSecond
        val signature = sign(secret, "$timestamp.$payload")
        return deliver(url, payload, timestamp, signature, "test")
    }

    private fun deliver(
        url: String,
        body: String,
        timestamp: Long,
        signature: String,
        event: String
    ): WebhookDeliveryResult {
        val maxRetries = 3
        var delayMs = 1000L
        var lastResult: WebhookDeliveryResult = WebhookDeliveryResult(false, null, "not attempted")

        repeat(maxRetries) { attempt ->
            lastResult = trySend(url, body, timestamp, signature, event)
            if (lastResult.success) return lastResult
            if (attempt < maxRetries - 1) {
                log.warn("웹훅 전송 실패 (시도 ${attempt + 1}/$maxRetries) url=$url: ${lastResult.message}")
                Thread.sleep(delayMs)
                delayMs *= 3
            }
        }
        if (!lastResult.success) {
            log.error("웹훅 전송 최종 실패 url=$url: ${lastResult.message}")
        }
        return lastResult
    }

    private fun trySend(
        url: String,
        body: String,
        timestamp: Long,
        signature: String,
        event: String
    ): WebhookDeliveryResult {
        return try {
            val response = webClient.post()
                .uri(url)
                .header("Content-Type", "application/json")
                .header("X-AdLex-Signature", "sha256=$signature")
                .header("X-AdLex-Event", event)
                .header("X-AdLex-Timestamp", timestamp.toString())
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block()

            val status = response?.statusCode?.value() ?: 0
            WebhookDeliveryResult(success = true, statusCode = status, message = "OK")
        } catch (e: WebClientResponseException) {
            WebhookDeliveryResult(false, e.statusCode.value(), "HTTP ${e.statusCode}")
        } catch (e: Exception) {
            WebhookDeliveryResult(false, null, e.message ?: "unknown error")
        }
    }

    private fun sign(secret: String, data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        return mac.doFinal(data.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}

data class WebhookDeliveryResult(
    val success: Boolean,
    val statusCode: Int?,
    val message: String
)

/** Webhook 페이로드 */
data class CheckCompletedPayload(
    val event: String = "check.completed",
    val tenantId: Long,
    val compliant: Boolean,
    val violationCount: Int,
    val checkedAt: Instant,
    val processingMs: Long
)

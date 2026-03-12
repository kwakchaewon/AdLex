package com.adlex.api.controller

import com.adlex.api.dto.WebhookCreateRequest
import com.adlex.api.dto.WebhookResponse
import com.adlex.api.dto.WebhookTestResponse
import com.adlex.domain.service.WebhookEndpointService
import com.adlex.infra.webhook.WebhookDeliveryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/webhooks")
@Tag(name = "Webhook", description = "Webhook 엔드포인트 관리 API")
class WebhookController(
    private val webhookEndpointService: WebhookEndpointService,
    private val webhookDeliveryService: WebhookDeliveryService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Webhook 엔드포인트 등록")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "등록 완료 (secret 1회 반환)"),
        ApiResponse(responseCode = "400", description = "Validation 실패"),
        ApiResponse(responseCode = "401", description = "인증 실패")
    )
    fun create(@RequestBody @Valid request: WebhookCreateRequest): WebhookResponse =
        webhookEndpointService.create(resolveTenantId(), request)

    @GetMapping
    @Operation(summary = "Webhook 엔드포인트 목록 조회")
    fun list(): List<WebhookResponse> =
        webhookEndpointService.findAll(resolveTenantId())

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Webhook 엔드포인트 삭제")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "삭제 완료"),
        ApiResponse(responseCode = "404", description = "엔드포인트 없음")
    )
    fun delete(@PathVariable id: Long) =
        webhookEndpointService.delete(resolveTenantId(), id)

    @PostMapping("/{id}/test")
    @Operation(summary = "Webhook 테스트 이벤트 전송")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "전송 결과"),
        ApiResponse(responseCode = "404", description = "엔드포인트 없음")
    )
    fun test(@PathVariable id: Long): WebhookTestResponse {
        val endpoint = webhookEndpointService.findById(resolveTenantId(), id)
        val result = webhookDeliveryService.deliverTest(endpoint.url, endpoint.secret)
        return WebhookTestResponse(
            success = result.success,
            statusCode = result.statusCode,
            message = result.message
        )
    }

    private fun resolveTenantId(): Long =
        (SecurityContextHolder.getContext().authentication?.principal as? Long) ?: 1L
}

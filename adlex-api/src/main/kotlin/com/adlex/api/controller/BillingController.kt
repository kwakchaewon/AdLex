package com.adlex.api.controller

import com.adlex.api.dto.ConfirmRequest
import com.adlex.api.dto.SubscribeRequest
import com.adlex.api.dto.SubscriptionStatusResponse
import com.adlex.domain.service.BillingService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Billing", description = "구독/결제 관리 API")
@RestController
@RequestMapping("/api/billing")
class BillingController(private val billingService: BillingService) {

    @Operation(summary = "구독 시작", security = [SecurityRequirement(name = "bearerAuth")])
    @PostMapping("/subscribe")
    fun subscribe(
        @AuthenticationPrincipal tenantId: Long,
        @Valid @RequestBody req: SubscribeRequest
    ): SubscriptionStatusResponse = billingService.subscribe(tenantId, req)

    @Operation(summary = "결제 확인", security = [SecurityRequirement(name = "bearerAuth")])
    @PostMapping("/confirm")
    fun confirm(
        @AuthenticationPrincipal tenantId: Long,
        @Valid @RequestBody req: ConfirmRequest
    ): SubscriptionStatusResponse = billingService.confirm(tenantId, req)

    @Operation(summary = "구독 취소", security = [SecurityRequirement(name = "bearerAuth")])
    @PostMapping("/cancel")
    fun cancel(@AuthenticationPrincipal tenantId: Long): SubscriptionStatusResponse =
        billingService.cancel(tenantId)

    @Operation(summary = "구독 상태 조회", security = [SecurityRequirement(name = "bearerAuth")])
    @GetMapping("/status")
    fun getStatus(@AuthenticationPrincipal tenantId: Long): SubscriptionStatusResponse =
        billingService.getStatus(tenantId)
}

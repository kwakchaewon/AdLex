package com.adlex.api.dto

import com.adlex.domain.entity.Plan
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Schema(description = "구독 시작 요청")
data class SubscribeRequest(
    @field:NotNull @Schema(description = "구독 플랜") val plan: Plan,
    @field:NotBlank @Schema(description = "PortOne 빌링키 (FREE 플랜은 빈 문자열 가능)", example = "customer_uid_abc123")
    val customerUid: String = ""
)

@Schema(description = "결제 확인 요청")
data class ConfirmRequest(
    @field:NotBlank @Schema(description = "PortOne 결제번호", example = "imp_123456789")
    val impUid: String,
    @field:NotBlank @Schema(description = "가맹점 주문번호", example = "adlex_order_20260312_001")
    val merchantUid: String
)

package com.adlex.api.dto

import com.adlex.engine.model.RuleType
import com.adlex.engine.model.Severity
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreateRuleRequest(
    @field:NotBlank @field:Size(max = 50)
    @Schema(description = "규칙 코드 (고유)", example = "SPAM_001")
    val code: String,

    @field:NotBlank @field:Size(max = 100)
    @Schema(description = "규칙명", example = "스팸 문구 금지")
    val name: String,

    @Schema(description = "규칙 설명")
    val description: String? = null,

    @field:NotNull
    @Schema(description = "규칙 타입")
    val type: RuleType,

    @field:NotBlank @field:Size(max = 50)
    @Schema(description = "채널 CSV (ALL, SMS, KAKAO, EMAIL)", example = "SMS,KAKAO")
    val channel: String,

    @field:NotNull
    @Schema(description = "심각도")
    val severity: Severity,

    @Schema(description = "정규식 패턴")
    val pattern: String? = null,

    @Schema(description = "추가 설정 (JSON)")
    val config: String? = null,

    @Schema(description = "법적 근거")
    val legalBasis: String? = null,

    @Schema(description = "활성화 여부", defaultValue = "true")
    val active: Boolean = true
)

data class UpdateRuleRequest(
    @field:Size(max = 100)
    @Schema(description = "규칙명")
    val name: String? = null,

    @Schema(description = "규칙 설명")
    val description: String? = null,

    @Schema(description = "규칙 타입")
    val type: RuleType? = null,

    @field:Size(max = 50)
    @Schema(description = "채널 CSV")
    val channel: String? = null,

    @Schema(description = "심각도")
    val severity: Severity? = null,

    @Schema(description = "정규식 패턴")
    val pattern: String? = null,

    @Schema(description = "추가 설정 (JSON)")
    val config: String? = null,

    @Schema(description = "법적 근거")
    val legalBasis: String? = null,

    @Schema(description = "활성화 여부")
    val active: Boolean? = null
)

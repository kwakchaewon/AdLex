package com.adlex.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class WebhookCreateRequest(
    @field:NotBlank
    @field:Size(max = 2048)
    val url: String,

    /** 등록할 이벤트 목록 (기본: check.completed) */
    val events: List<String> = listOf("check.completed")
)

data class WebhookResponse(
    val id: Long,
    val url: String,
    val secret: String?,   // 생성 시에만 반환, 이후 null
    val active: Boolean,
    val events: List<String>,
    val createdAt: Instant
)

data class WebhookTestResponse(
    val success: Boolean,
    val statusCode: Int?,
    val message: String
)

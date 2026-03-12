package com.adlex.api.dto

import com.adlex.engine.model.Channel
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant

data class CheckRequest(
    @field:NotBlank(message = "message는 필수입니다")
    @field:Size(max = 2000, message = "message는 2000자 이내여야 합니다")
    val message: String,

    @field:NotNull(message = "channel은 필수입니다")
    val channel: Channel,

    @field:Valid
    val sender: SenderInfoDto? = null,

    val scheduledAt: Instant? = null,

    val options: CheckOptionsDto? = null
)

data class SenderInfoDto(
    val name: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null
)

data class CheckOptionsDto(
    val skipRules: List<String>? = null,
    val useLlm: Boolean = false
)

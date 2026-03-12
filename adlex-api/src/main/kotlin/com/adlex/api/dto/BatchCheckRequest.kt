package com.adlex.api.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

data class BatchCheckRequest(
    @field:NotEmpty(message = "messages는 비어있을 수 없습니다")
    @field:Size(max = 100, message = "messages는 최대 100건까지 가능합니다")
    val messages: List<@Valid CheckRequest>
)

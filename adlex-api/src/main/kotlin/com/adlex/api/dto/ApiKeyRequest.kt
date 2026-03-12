package com.adlex.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateApiKeyRequest(
    @field:NotBlank @field:Size(min = 1, max = 50)
    @Schema(description = "API Key 이름", example = "Production Key")
    val name: String
)

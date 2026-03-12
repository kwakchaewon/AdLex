package com.adlex.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank @field:Email
    @Schema(description = "이메일", example = "user@example.com")
    val email: String,

    @field:NotBlank @field:Size(min = 8, max = 100)
    @Schema(description = "비밀번호 (8자 이상)", example = "password123")
    val password: String,

    @field:Size(max = 100)
    @Schema(description = "회사명", example = "AdLex Corp")
    val companyName: String? = null
)

data class LoginRequest(
    @field:NotBlank @field:Email
    @Schema(description = "이메일", example = "user@example.com")
    val email: String,

    @field:NotBlank
    @Schema(description = "비밀번호", example = "password123")
    val password: String
)

data class RefreshTokenRequest(
    @field:NotBlank
    @Schema(description = "리프레시 토큰")
    val refreshToken: String
)

data class UpdateMeRequest(
    @field:Size(max = 100)
    @Schema(description = "회사명", example = "AdLex Corp")
    val companyName: String? = null
)

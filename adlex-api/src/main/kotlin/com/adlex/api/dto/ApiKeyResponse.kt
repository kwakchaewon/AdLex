package com.adlex.api.dto

import com.adlex.domain.entity.ApiKey
import com.adlex.domain.entity.ApiKeyStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class ApiKeyResponse(
    val id: Long,
    val name: String,
    @Schema(description = "키 앞 12자리 (식별용)", example = "al_live_AbCd")
    val keyPrefix: String,
    val status: ApiKeyStatus,
    val lastUsedAt: Instant?,
    val createdAt: Instant
) {
    companion object {
        fun from(apiKey: ApiKey) = ApiKeyResponse(
            id = apiKey.id,
            name = apiKey.name,
            keyPrefix = apiKey.keyPrefix,
            status = apiKey.status,
            lastUsedAt = apiKey.lastUsedAt,
            createdAt = apiKey.createdAt
        )
    }
}

data class CreateApiKeyResponse(
    val id: Long,
    val name: String,
    val keyPrefix: String,
    val status: ApiKeyStatus,
    val lastUsedAt: Instant?,
    val createdAt: Instant,
    @Schema(description = "발급된 API Key (최초 1회만 표시됨)")
    val key: String
) {
    companion object {
        fun from(apiKey: ApiKey, rawKey: String) = CreateApiKeyResponse(
            id = apiKey.id,
            name = apiKey.name,
            keyPrefix = apiKey.keyPrefix,
            status = apiKey.status,
            lastUsedAt = apiKey.lastUsedAt,
            createdAt = apiKey.createdAt,
            key = rawKey
        )
    }
}

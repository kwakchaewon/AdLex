package com.adlex.api.controller

import com.adlex.api.dto.ApiKeyResponse
import com.adlex.api.dto.CreateApiKeyRequest
import com.adlex.api.dto.CreateApiKeyResponse
import com.adlex.domain.service.ApiKeyService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "API Key", description = "API Key 관리")
@RestController
@RequestMapping("/api/keys")
@SecurityRequirement(name = "bearerAuth")
class ApiKeyController(private val apiKeyService: ApiKeyService) {

    @Operation(summary = "API Key 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal tenantId: Long,
        @Valid @RequestBody req: CreateApiKeyRequest
    ): CreateApiKeyResponse {
        val (rawKey, apiKey) = apiKeyService.generateKey(tenantId, req.name)
        return CreateApiKeyResponse.from(apiKey, rawKey)
    }

    @Operation(summary = "내 API Key 목록 조회")
    @GetMapping
    fun list(@AuthenticationPrincipal tenantId: Long): List<ApiKeyResponse> =
        apiKeyService.listKeys(tenantId).map { ApiKeyResponse.from(it) }

    @Operation(summary = "API Key 폐기")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revoke(
        @AuthenticationPrincipal tenantId: Long,
        @PathVariable id: Long
    ) = apiKeyService.revokeKey(tenantId, id)
}

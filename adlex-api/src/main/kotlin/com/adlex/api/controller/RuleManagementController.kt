package com.adlex.api.controller

import com.adlex.api.dto.CreateRuleRequest
import com.adlex.api.dto.RuleResponse
import com.adlex.api.dto.UpdateRuleRequest
import com.adlex.domain.service.RuleService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = "Rule Management", description = "규칙 관리 API (관리자)")
@RestController
@RequestMapping("/api/rules")
class RuleManagementController(private val ruleService: RuleService) {

    @Operation(summary = "전체 규칙 목록 조회", security = [SecurityRequirement(name = "bearerAuth")])
    @GetMapping
    fun getAll(@AuthenticationPrincipal tenantId: Long): List<RuleResponse> =
        ruleService.getAll()

    @Operation(summary = "규칙 단건 조회", security = [SecurityRequirement(name = "bearerAuth")])
    @GetMapping("/{id}")
    fun getById(
        @AuthenticationPrincipal tenantId: Long,
        @PathVariable id: Long
    ): RuleResponse = ruleService.getById(id)

    @Operation(summary = "규칙 생성", security = [SecurityRequirement(name = "bearerAuth")])
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal tenantId: Long,
        @Valid @RequestBody req: CreateRuleRequest
    ): RuleResponse = ruleService.create(req)

    @Operation(summary = "규칙 수정", security = [SecurityRequirement(name = "bearerAuth")])
    @PutMapping("/{id}")
    fun update(
        @AuthenticationPrincipal tenantId: Long,
        @PathVariable id: Long,
        @Valid @RequestBody req: UpdateRuleRequest
    ): RuleResponse = ruleService.update(id, req)

    @Operation(summary = "규칙 비활성화 (소프트 삭제)", security = [SecurityRequirement(name = "bearerAuth")])
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @AuthenticationPrincipal tenantId: Long,
        @PathVariable id: Long
    ) = ruleService.delete(id)
}

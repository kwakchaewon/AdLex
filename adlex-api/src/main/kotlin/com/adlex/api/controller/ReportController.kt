package com.adlex.api.controller

import com.adlex.api.dto.ReportRequest
import com.adlex.api.dto.ReportResponse
import com.adlex.domain.service.ReportService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/report")
@Tag(name = "Report", description = "준법 검사 리포트 API (관련 법령 + 유사 판례 포함)")
class ReportController(
    private val reportService: ReportService
) {

    @PostMapping
    @Operation(
        summary = "준법 검사 리포트 생성",
        description = "메시지 검사 결과에 관련 법령 조문과 유사 위반 판례를 포함한 상세 리포트를 반환합니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "리포트 생성 완료"),
        ApiResponse(responseCode = "400", description = "Validation 실패"),
        ApiResponse(responseCode = "401", description = "인증 실패"),
        ApiResponse(responseCode = "402", description = "플랜 업그레이드 필요 (LLM 분석)")
    )
    fun generate(@RequestBody @Valid request: ReportRequest): ReportResponse =
        reportService.generate(resolveTenantId(), request)

    private fun resolveTenantId(): Long =
        (SecurityContextHolder.getContext().authentication?.principal as? Long) ?: 1L
}

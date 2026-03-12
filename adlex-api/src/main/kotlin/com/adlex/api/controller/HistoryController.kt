package com.adlex.api.controller

import com.adlex.api.dto.HistoryDetailResponse
import com.adlex.api.dto.HistoryItemResponse
import com.adlex.api.dto.PageResponse
import com.adlex.domain.service.HistoryService
import com.adlex.engine.model.Channel
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@Tag(name = "History", description = "검사 히스토리 API")
@RestController
@RequestMapping("/api/history")
@SecurityRequirement(name = "bearerAuth")
class HistoryController(private val historyService: HistoryService) {

    @Operation(summary = "검사 히스토리 목록 조회")
    @GetMapping
    fun list(
        @AuthenticationPrincipal tenantId: Long,
        @Parameter(description = "채널 필터") @RequestParam(required = false) channel: Channel?,
        @Parameter(description = "준수 여부 필터") @RequestParam(required = false) compliant: Boolean?,
        @Parameter(description = "시작일시 (ISO-8601)") @RequestParam(required = false) from: Instant?,
        @Parameter(description = "종료일시 (ISO-8601)") @RequestParam(required = false) to: Instant?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): PageResponse<HistoryItemResponse> =
        historyService.list(tenantId, channel, compliant, from, to, page, size.coerceAtMost(100))

    @Operation(summary = "검사 히스토리 상세 조회")
    @GetMapping("/{id}")
    fun detail(
        @AuthenticationPrincipal tenantId: Long,
        @PathVariable id: Long
    ): HistoryDetailResponse =
        historyService.detail(tenantId, id)
}

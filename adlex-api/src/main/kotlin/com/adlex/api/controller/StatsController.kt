package com.adlex.api.controller

import com.adlex.api.dto.StatsResponse
import com.adlex.domain.service.StatsService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Stats", description = "통계 API")
@RestController
@RequestMapping("/api/stats")
@SecurityRequirement(name = "bearerAuth")
class StatsController(private val statsService: StatsService) {

    @Operation(summary = "검사 통계 조회 (요약 + 일별 트렌드 + 채널별)")
    @GetMapping
    fun getStats(
        @AuthenticationPrincipal tenantId: Long,
        @Parameter(description = "조회 기간 (일수, 최대 90)") @RequestParam(defaultValue = "30") days: Int
    ): StatsResponse =
        statsService.getStats(tenantId, days.coerceIn(1, 90))
}

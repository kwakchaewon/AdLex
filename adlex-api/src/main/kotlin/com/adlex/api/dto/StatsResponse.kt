package com.adlex.api.dto

import com.adlex.engine.model.Channel
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class StatsResponse(
    val summary: StatsSummary,
    val daily: List<DailyStat>,
    val channels: List<ChannelStat>
)

data class StatsSummary(
    val totalChecks: Long,
    val compliantChecks: Long,
    val violationChecks: Long,
    @Schema(description = "준수율 (0.0 ~ 1.0)")
    val complianceRate: Double,
    val days: Int
)

data class DailyStat(
    val date: LocalDate,
    val total: Long,
    val compliant: Long,
    val violations: Long
)

data class ChannelStat(
    val channel: Channel,
    val total: Long,
    val compliant: Long,
    val complianceRate: Double
)

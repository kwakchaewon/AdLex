package com.adlex.api.dto

import com.adlex.domain.entity.CheckLog
import com.adlex.engine.model.Channel
import com.adlex.engine.model.Severity
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class HistoryItemResponse(
    val id: Long,
    val channel: Channel,
    val compliant: Boolean,
    val violationCount: Int,
    val processingMs: Long,
    val checkedAt: Instant
) {
    companion object {
        fun from(log: CheckLog) = HistoryItemResponse(
            id = log.id,
            channel = log.channel,
            compliant = log.compliant,
            violationCount = log.violationCount,
            processingMs = log.processingMs,
            checkedAt = log.createdAt
        )
    }
}

data class HistoryDetailResponse(
    val id: Long,
    val message: String,
    val channel: Channel,
    val compliant: Boolean,
    val violationCount: Int,
    val violations: List<ViolationDto>,
    val processingMs: Long,
    val checkedAt: Instant
)

data class PageResponse<T>(
    val content: List<T>,
    @Schema(description = "현재 페이지 (0부터 시작)")
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

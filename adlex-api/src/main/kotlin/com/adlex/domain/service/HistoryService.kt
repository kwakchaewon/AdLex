package com.adlex.domain.service

import com.adlex.api.advice.BusinessException
import com.adlex.api.advice.ErrorCode
import com.adlex.api.dto.HistoryDetailResponse
import com.adlex.api.dto.HistoryItemResponse
import com.adlex.api.dto.PageResponse
import com.adlex.api.dto.ViolationDto
import com.adlex.domain.repository.CheckLogRepository
import com.adlex.engine.model.Channel
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class HistoryService(private val checkLogRepository: CheckLogRepository) {

    private val mapper = jacksonObjectMapper()
    private val violationsType = object : TypeReference<List<ViolationDto>>() {}

    @Transactional(readOnly = true)
    fun list(
        tenantId: Long,
        channel: Channel?,
        compliant: Boolean?,
        from: Instant?,
        to: Instant?,
        page: Int,
        size: Int
    ): PageResponse<HistoryItemResponse> {
        val result = checkLogRepository.findWithFilters(
            tenantId = tenantId,
            channel = channel,
            compliant = compliant,
            from = from,
            to = to,
            pageable = PageRequest.of(page, size)
        )
        return PageResponse(
            content = result.content.map { HistoryItemResponse.from(it) },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }

    @Transactional(readOnly = true)
    fun detail(tenantId: Long, id: Long): HistoryDetailResponse {
        val log = checkLogRepository.findByIdAndTenantId(id, tenantId)
            ?: throw BusinessException(ErrorCode.NOT_FOUND, "히스토리를 찾을 수 없습니다")
        val violations = runCatching { mapper.readValue(log.violations, violationsType) }.getOrElse { emptyList() }
        return HistoryDetailResponse(
            id = log.id,
            message = log.message,
            channel = log.channel,
            compliant = log.compliant,
            violationCount = log.violationCount,
            violations = violations,
            processingMs = log.processingMs,
            checkedAt = log.createdAt
        )
    }
}

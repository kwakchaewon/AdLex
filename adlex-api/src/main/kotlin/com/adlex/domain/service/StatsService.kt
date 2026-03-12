package com.adlex.domain.service

import com.adlex.api.dto.ChannelStat
import com.adlex.api.dto.DailyStat
import com.adlex.api.dto.StatsResponse
import com.adlex.api.dto.StatsSummary
import com.adlex.domain.repository.CheckLogRepository
import com.adlex.engine.model.Channel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class StatsService(private val checkLogRepository: CheckLogRepository) {

    @Transactional(readOnly = true)
    fun getStats(tenantId: Long, days: Int): StatsResponse {
        val to = Instant.now()
        val from = to.minus(days.toLong(), ChronoUnit.DAYS)

        val total = checkLogRepository.countByTenantIdAndCreatedAtBetween(tenantId, from, to)
        val compliant = checkLogRepository.countByTenantIdAndCompliantTrueAndCreatedAtBetween(tenantId, from, to)
        val violations = total - compliant
        val complianceRate = if (total == 0L) 1.0 else compliant.toDouble() / total

        val summary = StatsSummary(
            totalChecks = total,
            compliantChecks = compliant,
            violationChecks = violations,
            complianceRate = complianceRate,
            days = days
        )

        val daily = checkLogRepository.countByDay(tenantId, from, to).map { row ->
            val rowViolations = row.total - row.compliant
            DailyStat(
                date = row.date.toLocalDate(),
                total = row.total,
                compliant = row.compliant,
                violations = rowViolations
            )
        }

        val channels = checkLogRepository.countByChannel(tenantId, from, to).map { row ->
            val channelTotal = row.total
            val channelCompliant = row.compliant
            ChannelStat(
                channel = Channel.valueOf(row.channel),
                total = channelTotal,
                compliant = channelCompliant,
                complianceRate = if (channelTotal == 0L) 1.0 else channelCompliant.toDouble() / channelTotal
            )
        }

        return StatsResponse(summary = summary, daily = daily, channels = channels)
    }
}

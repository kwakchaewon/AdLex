package com.adlex.domain.service

import com.adlex.domain.repository.ChannelStatProjection
import com.adlex.domain.repository.CheckLogRepository
import com.adlex.domain.repository.DailyStatProjection
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Date
import java.time.Instant
import java.time.LocalDate

class StatsServiceTest {

    private val checkLogRepository: CheckLogRepository = mockk()
    private lateinit var statsService: StatsService

    @BeforeEach
    fun setUp() {
        statsService = StatsService(checkLogRepository)
    }

    private fun channelStat(channel: String, total: Long, compliant: Long) =
        object : ChannelStatProjection {
            override val channel = channel
            override val total = total
            override val compliant = compliant
        }

    private fun dailyStat(date: LocalDate, total: Long, compliant: Long) =
        object : DailyStatProjection {
            override val date = Date.valueOf(date)
            override val total = total
            override val compliant = compliant
        }

    @Test
    fun `getStats - 기본 통계 반환`() {
        every { checkLogRepository.countByTenantIdAndCreatedAtBetween(10L, any(), any()) } returns 100L
        every { checkLogRepository.countByTenantIdAndCompliantTrueAndCreatedAtBetween(10L, any(), any()) } returns 80L
        every { checkLogRepository.countByDay(10L, any(), any()) } returns listOf(
            dailyStat(LocalDate.now(), 10L, 8L)
        )
        every { checkLogRepository.countByChannel(10L, any(), any()) } returns listOf(
            channelStat("SMS", 60L, 50L),
            channelStat("KAKAO", 40L, 30L)
        )

        val result = statsService.getStats(10L, 30)

        assertThat(result.summary.totalChecks).isEqualTo(100L)
        assertThat(result.summary.compliantChecks).isEqualTo(80L)
        assertThat(result.summary.violationChecks).isEqualTo(20L)
        assertThat(result.summary.complianceRate).isEqualTo(0.8)
        assertThat(result.daily).hasSize(1)
        assertThat(result.daily[0].violations).isEqualTo(2L)
        assertThat(result.channels).hasSize(2)
    }

    @Test
    fun `getStats - 검사 이력 없을 시 준수율 1_0`() {
        every { checkLogRepository.countByTenantIdAndCreatedAtBetween(10L, any(), any()) } returns 0L
        every { checkLogRepository.countByTenantIdAndCompliantTrueAndCreatedAtBetween(10L, any(), any()) } returns 0L
        every { checkLogRepository.countByDay(10L, any(), any()) } returns emptyList()
        every { checkLogRepository.countByChannel(10L, any(), any()) } returns emptyList()

        val result = statsService.getStats(10L, 30)

        assertThat(result.summary.complianceRate).isEqualTo(1.0)
        assertThat(result.daily).isEmpty()
        assertThat(result.channels).isEmpty()
    }

    @Test
    fun `getStats - 채널별 준수율 계산`() {
        every { checkLogRepository.countByTenantIdAndCreatedAtBetween(10L, any(), any()) } returns 50L
        every { checkLogRepository.countByTenantIdAndCompliantTrueAndCreatedAtBetween(10L, any(), any()) } returns 25L
        every { checkLogRepository.countByDay(10L, any(), any()) } returns emptyList()
        every { checkLogRepository.countByChannel(10L, any(), any()) } returns listOf(
            channelStat("EMAIL", 50L, 25L)
        )

        val result = statsService.getStats(10L, 7)

        assertThat(result.channels[0].complianceRate).isEqualTo(0.5)
        assertThat(result.summary.days).isEqualTo(7)
    }
}

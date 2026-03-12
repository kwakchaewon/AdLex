package com.adlex.engine.evaluator

import com.adlex.domain.entity.Rule
import com.adlex.engine.model.Channel
import com.adlex.engine.model.EvaluationContext
import com.adlex.engine.model.RuleType
import com.adlex.engine.model.Severity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class TimeRangeEvaluatorTest {

    private val evaluator = TimeRangeEvaluator()
    private val tz = ZoneId.of("Asia/Seoul")

    private val nightSmsRule = Rule(
        code = "NIGHT_SMS", name = "야간발송제한", type = RuleType.TIME_RANGE,
        channel = "SMS", severity = Severity.HIGH,
        config = """{"denyStart":"21:00","denyEnd":"08:00","timezone":"Asia/Seoul"}"""
    )

    private fun kstInstant(hour: Int, minute: Int = 0) =
        ZonedDateTime.of(LocalDateTime.of(2026, 3, 12, hour, minute), tz).toInstant()

    @Test
    fun `supports TIME_RANGE only`() {
        assertTrue(evaluator.supports(RuleType.TIME_RANGE))
        assertFalse(evaluator.supports(RuleType.REGEX))
    }

    @Test
    fun `허용 시간대 (10시) - null 반환`() {
        val ctx = EvaluationContext(
            message = "봄 세일", channel = Channel.SMS,
            scheduledAt = kstInstant(10)
        )
        assertNull(evaluator.evaluate(ctx, nightSmsRule))
    }

    @Test
    fun `야간 제한 (22시) - violation 반환`() {
        val ctx = EvaluationContext(
            message = "봄 세일", channel = Channel.SMS,
            scheduledAt = kstInstant(22)
        )
        val result = evaluator.evaluate(ctx, nightSmsRule)
        assertNotNull(result)
        assertEquals("NIGHT_SMS", result!!.ruleCode)
        assertEquals(Severity.HIGH, result.severity)
    }

    @Test
    fun `새벽 (3시) - violation 반환 (자정 넘어도 제한)`() {
        val ctx = EvaluationContext(
            message = "봄 세일", channel = Channel.SMS,
            scheduledAt = kstInstant(3)
        )
        assertNotNull(evaluator.evaluate(ctx, nightSmsRule))
    }

    @Test
    fun `경계 시간 (21시 정각) - violation 반환`() {
        val ctx = EvaluationContext(
            message = "봄 세일", channel = Channel.SMS,
            scheduledAt = kstInstant(21, 0)
        )
        assertNotNull(evaluator.evaluate(ctx, nightSmsRule))
    }

    @Test
    fun `허용 경계 (20시 59분) - null 반환`() {
        val ctx = EvaluationContext(
            message = "봄 세일", channel = Channel.SMS,
            scheduledAt = kstInstant(20, 59)
        )
        assertNull(evaluator.evaluate(ctx, nightSmsRule))
    }

    @Test
    fun `config null이면 null 반환`() {
        val rule = Rule(
            code = "NO_CONFIG", name = "설정없음", type = RuleType.TIME_RANGE,
            channel = "SMS", severity = Severity.LOW
        )
        val ctx = EvaluationContext(message = "테스트", channel = Channel.SMS)
        assertNull(evaluator.evaluate(ctx, rule))
    }
}

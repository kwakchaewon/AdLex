package com.adlex.engine.evaluator

import com.adlex.domain.entity.Rule
import com.adlex.engine.model.Channel
import com.adlex.engine.model.EvaluationContext
import com.adlex.engine.model.RuleType
import com.adlex.engine.model.Severity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LengthCheckEvaluatorTest {

    private val evaluator = LengthCheckEvaluator()

    @Test
    fun `supports LENGTH_CHECK 타입`() {
        assertTrue(evaluator.supports(RuleType.LENGTH_CHECK))
        assertFalse(evaluator.supports(RuleType.KEYWORD))
    }

    @Test
    fun `SMS 90바이트 이하 — 위반 없음`() {
        val rule = smsLengthRule()
        val ctx = context("안녕하세요 테스트입니다", Channel.SMS)  // 짧은 메시지

        assertNull(evaluator.evaluate(ctx, rule))
    }

    @Test
    fun `SMS 90바이트 초과 — 위반 반환`() {
        val rule = smsLengthRule()
        // 한글 46자 = EUC-KR 92바이트 > 90
        val longMessage = "가".repeat(46)
        val ctx = context(longMessage, Channel.SMS)

        val result = evaluator.evaluate(ctx, rule)

        assertNotNull(result)
        assertEquals("CH_SMS_LEN_001", result!!.ruleCode)
        assertTrue(result.message.contains("초과"))
        assertNotNull(result.suggestion)
    }

    @Test
    fun `KAKAO 1000자 이하 — 위반 없음`() {
        val rule = kakaoLengthRule()
        val ctx = context("가".repeat(1000), Channel.KAKAO)

        assertNull(evaluator.evaluate(ctx, rule))
    }

    @Test
    fun `KAKAO 1001자 초과 — 위반 반환`() {
        val rule = kakaoLengthRule()
        val ctx = context("가".repeat(1001), Channel.KAKAO)

        val result = evaluator.evaluate(ctx, rule)

        assertNotNull(result)
        assertTrue(result!!.message.contains("1001자"))
        assertTrue(result.message.contains("최대 1000자"))
    }

    @Test
    fun `config 없으면 위반 없음`() {
        val rule = Rule(
            code = "TEST", name = "테스트", type = RuleType.LENGTH_CHECK,
            channel = "SMS", severity = Severity.HIGH, config = null
        )
        val ctx = context("가".repeat(100), Channel.SMS)

        assertNull(evaluator.evaluate(ctx, rule))
    }

    @Test
    fun `maxChars 방식 — 정확히 경계값 위반 없음`() {
        val rule = Rule(
            code = "TEST", name = "이메일 길이", type = RuleType.LENGTH_CHECK,
            channel = "EMAIL", severity = Severity.LOW,
            config = """{"maxChars": 50}"""
        )
        val ctx = context("a".repeat(50), Channel.EMAIL)

        assertNull(evaluator.evaluate(ctx, rule))
    }

    // -------------------------------------------------------------------------

    private fun smsLengthRule() = Rule(
        code = "CH_SMS_LEN_001",
        name = "SMS 메시지 길이 제한",
        type = RuleType.LENGTH_CHECK,
        channel = "SMS",
        severity = Severity.HIGH,
        config = """{"maxBytes": 90, "encoding": "EUC-KR"}"""
    )

    private fun kakaoLengthRule() = Rule(
        code = "CH_KAKAO_LEN_001",
        name = "카카오 메시지 길이 제한",
        type = RuleType.LENGTH_CHECK,
        channel = "KAKAO",
        severity = Severity.HIGH,
        config = """{"maxChars": 1000}"""
    )

    private fun context(message: String, channel: Channel) =
        EvaluationContext(message = message, channel = channel)
}

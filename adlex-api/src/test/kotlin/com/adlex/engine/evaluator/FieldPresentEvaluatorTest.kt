package com.adlex.engine.evaluator

import com.adlex.domain.entity.Rule
import com.adlex.engine.model.Channel
import com.adlex.engine.model.EvaluationContext
import com.adlex.engine.model.RuleType
import com.adlex.engine.model.SenderInfo
import com.adlex.engine.model.Severity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FieldPresentEvaluatorTest {

    private val evaluator = FieldPresentEvaluator()

    private fun senderNameRule() = Rule(
        code = "SENDER_ID", name = "발신자표기", type = RuleType.FIELD_PRESENT,
        channel = "SMS,KAKAO", severity = Severity.MEDIUM,
        config = """{"field":"senderName"}"""
    )

    private fun unsubscribeRule() = Rule(
        code = "OPT_OUT_LINK", name = "수신거부링크", type = RuleType.FIELD_PRESENT,
        channel = "EMAIL", severity = Severity.HIGH,
        config = """{"field":"unsubscribeUrl"}"""
    )

    @Test
    fun `supports FIELD_PRESENT only`() {
        assertTrue(evaluator.supports(RuleType.FIELD_PRESENT))
        assertFalse(evaluator.supports(RuleType.REGEX))
    }

    @Test
    fun `senderName 있으면 null`() {
        val ctx = EvaluationContext(
            message = "봄 세일", channel = Channel.SMS,
            sender = SenderInfo(name = "홍길동쇼핑")
        )
        assertNull(evaluator.evaluate(ctx, senderNameRule()))
    }

    @Test
    fun `senderName 없으면 violation`() {
        val ctx = EvaluationContext(message = "봄 세일", channel = Channel.SMS)
        val result = evaluator.evaluate(ctx, senderNameRule())
        assertNotNull(result)
        assertEquals("SENDER_ID", result!!.ruleCode)
    }

    @Test
    fun `senderName 빈 문자열이면 violation`() {
        val ctx = EvaluationContext(
            message = "봄 세일", channel = Channel.SMS,
            sender = SenderInfo(name = "")
        )
        assertNotNull(evaluator.evaluate(ctx, senderNameRule()))
    }

    @Test
    fun `options에서 unsubscribeUrl 있으면 null`() {
        val ctx = EvaluationContext(
            message = "이메일 본문", channel = Channel.EMAIL,
            options = mapOf("unsubscribeUrl" to "https://example.com/unsubscribe")
        )
        assertNull(evaluator.evaluate(ctx, unsubscribeRule()))
    }

    @Test
    fun `options에서 unsubscribeUrl 없으면 violation`() {
        val ctx = EvaluationContext(message = "이메일 본문", channel = Channel.EMAIL)
        assertNotNull(evaluator.evaluate(ctx, unsubscribeRule()))
    }

    @Test
    fun `config null이면 null 반환`() {
        val rule = Rule(
            code = "NO_CONFIG", name = "설정없음", type = RuleType.FIELD_PRESENT,
            channel = "SMS", severity = Severity.LOW
        )
        val ctx = EvaluationContext(message = "테스트", channel = Channel.SMS)
        assertNull(evaluator.evaluate(ctx, rule))
    }
}

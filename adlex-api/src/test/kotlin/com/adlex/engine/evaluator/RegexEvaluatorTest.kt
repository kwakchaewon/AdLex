package com.adlex.engine.evaluator

import com.adlex.domain.entity.Rule
import com.adlex.engine.model.Channel
import com.adlex.engine.model.EvaluationContext
import com.adlex.engine.model.RuleType
import com.adlex.engine.model.Severity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RegexEvaluatorTest {

    private val evaluator = RegexEvaluator()

    private fun ruleOf(
        code: String = "TEST",
        pattern: String,
        config: String = """{"matchMode":"REQUIRE"}""",
        severity: Severity = Severity.HIGH
    ) = Rule(
        code = code, name = "테스트규칙", type = RuleType.REGEX,
        channel = "SMS", severity = severity,
        pattern = pattern, config = config
    )

    private fun contextOf(message: String, options: Map<String, Any> = emptyMap()) =
        EvaluationContext(message = message, channel = Channel.SMS, options = options)

    @Test
    fun `supports REGEX only`() {
        assertTrue(evaluator.supports(RuleType.REGEX))
        assertFalse(evaluator.supports(RuleType.KEYWORD))
    }

    @Test
    fun `REQUIRE - 패턴 매칭되면 null (위반 없음)`() {
        val rule = ruleOf(pattern = """\(광고\)""")
        val ctx = contextOf("(광고) 봄 세일 50% 할인")
        assertNull(evaluator.evaluate(ctx, rule))
    }

    @Test
    fun `REQUIRE - 패턴 미매칭이면 violation 반환`() {
        val rule = ruleOf(pattern = """\(광고\)""")
        val ctx = contextOf("봄 세일 50% 할인")
        val result = evaluator.evaluate(ctx, rule)
        assertNotNull(result)
        assertEquals("TEST", result!!.ruleCode)
        assertEquals(Severity.HIGH, result.severity)
    }

    @Test
    fun `DENY - 패턴 미매칭이면 null (위반 없음)`() {
        val rule = ruleOf(pattern = "확실한 효과", config = """{"matchMode":"DENY"}""")
        val ctx = contextOf("좋은 제품입니다")
        assertNull(evaluator.evaluate(ctx, rule))
    }

    @Test
    fun `DENY - 패턴 매칭되면 violation 반환`() {
        val rule = ruleOf(pattern = "확실한 효과", config = """{"matchMode":"DENY"}""")
        val ctx = contextOf("100% 확실한 효과를 보장합니다")
        val result = evaluator.evaluate(ctx, rule)
        assertNotNull(result)
    }

    @Test
    fun `target 지정 시 options에서 검사`() {
        val rule = ruleOf(
            pattern = """\(광고\)""",
            config = """{"matchMode":"REQUIRE","target":"subject"}"""
        )
        val ctxWithSubject = contextOf("본문입니다", options = mapOf("subject" to "(광고) 이메일"))
        assertNull(evaluator.evaluate(ctxWithSubject, rule))

        val ctxNoSubject = contextOf("본문입니다", options = mapOf("subject" to "이메일 제목"))
        assertNotNull(evaluator.evaluate(ctxNoSubject, rule))
    }

    @Test
    fun `pattern null이면 null 반환`() {
        val rule = Rule(
            code = "NO_PATTERN", name = "패턴없음", type = RuleType.REGEX,
            channel = "SMS", severity = Severity.LOW, pattern = null
        )
        assertNull(evaluator.evaluate(contextOf("메시지"), rule))
    }
}

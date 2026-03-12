package com.adlex.engine.evaluator

import com.adlex.domain.entity.Rule
import com.adlex.engine.model.Channel
import com.adlex.engine.model.EvaluationContext
import com.adlex.engine.model.RuleType
import com.adlex.engine.model.Severity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class KeywordEvaluatorTest {

    private val evaluator = KeywordEvaluator()

    private fun ruleOf(keywords: String, config: String = """{"matchMode":"DENY"}""") =
        Rule(
            code = "KW_TEST", name = "키워드테스트", type = RuleType.KEYWORD,
            channel = "SMS", severity = Severity.MEDIUM,
            pattern = keywords, config = config
        )

    private fun contextOf(message: String) =
        EvaluationContext(message = message, channel = Channel.SMS)

    @Test
    fun `supports KEYWORD only`() {
        assertTrue(evaluator.supports(RuleType.KEYWORD))
        assertFalse(evaluator.supports(RuleType.REGEX))
    }

    @Test
    fun `DENY - 키워드 미포함이면 null`() {
        val rule = ruleOf("100% 효과,부작용 없음")
        assertNull(evaluator.evaluate(contextOf("좋은 제품입니다"), rule))
    }

    @Test
    fun `DENY - 키워드 포함 시 violation 반환`() {
        val rule = ruleOf("100% 효과,부작용 없음")
        val result = evaluator.evaluate(contextOf("100% 효과가 있습니다"), rule)
        assertNotNull(result)
        assertEquals("KW_TEST", result!!.ruleCode)
        assertTrue(result.message.contains("100% 효과"))
    }

    @Test
    fun `DENY - 두 번째 키워드 매칭`() {
        val rule = ruleOf("키워드1,부작용 없음")
        assertNotNull(evaluator.evaluate(contextOf("부작용 없음이 증명되었습니다"), rule))
    }

    @Test
    fun `pattern null이면 null 반환`() {
        val rule = Rule(
            code = "NO_PATTERN", name = "패턴없음", type = RuleType.KEYWORD,
            channel = "SMS", severity = Severity.LOW, pattern = null
        )
        assertNull(evaluator.evaluate(contextOf("메시지"), rule))
    }
}

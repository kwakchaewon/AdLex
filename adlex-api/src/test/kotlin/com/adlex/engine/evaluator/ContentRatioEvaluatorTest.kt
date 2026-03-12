package com.adlex.engine.evaluator

import com.adlex.domain.entity.Rule
import com.adlex.engine.model.Channel
import com.adlex.engine.model.EvaluationContext
import com.adlex.engine.model.RuleType
import com.adlex.engine.model.Severity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ContentRatioEvaluatorTest {

    private val evaluator = ContentRatioEvaluator()

    @Test
    fun `supports CONTENT_RATIO 타입`() {
        assertTrue(evaluator.supports(RuleType.CONTENT_RATIO))
        assertFalse(evaluator.supports(RuleType.REGEX))
    }

    @Test
    fun `과장 표현 3회 이하 — 위반 없음`() {
        val rule = superlativeRule(maxCount = 3)
        val ctx = context("최고의 제품, 최상의 품질입니다.")  // 최고·최상 2회

        assertNull(evaluator.evaluate(ctx, rule))
    }

    @Test
    fun `과장 표현 4회 초과 — 위반 반환`() {
        val rule = superlativeRule(maxCount = 3)
        val ctx = context("최고의 최상 품질! 1등 제품. 최저가 보장!")  // 4회

        val result = evaluator.evaluate(ctx, rule)

        assertNotNull(result)
        assertEquals("CH_ALL_RATIO_001", result!!.ruleCode)
        assertTrue(result.message.contains("4회"))
        assertTrue(result.message.contains("최대 3회"))
        assertNotNull(result.suggestion)
    }

    @Test
    fun `동일 키워드 반복 횟수 합산`() {
        val rule = superlativeRule(maxCount = 2)
        val ctx = context("최고! 최고! 최고!")  // 최고 3회 → 합산 3

        val result = evaluator.evaluate(ctx, rule)

        assertNotNull(result)
        assertTrue(result!!.message.contains("3회"))
    }

    @Test
    fun `절대적 효과 표현 1회 초과 — 위반 반환`() {
        val rule = absoluteRule()
        val ctx = context("완전무료 이벤트! 완전무료로 드립니다.")  // 2회

        val result = evaluator.evaluate(ctx, rule)

        assertNotNull(result)
        assertEquals(Severity.HIGH, result!!.severity)
    }

    @Test
    fun `키워드 없는 정상 메시지 — 위반 없음`() {
        val rule = superlativeRule(maxCount = 3)
        val ctx = context("우리 제품을 소개합니다. 합리적인 가격에 좋은 품질을 제공합니다.")

        assertNull(evaluator.evaluate(ctx, rule))
    }

    @Test
    fun `config 없고 pattern도 없으면 위반 없음`() {
        val rule = Rule(
            code = "TEST", name = "테스트", type = RuleType.CONTENT_RATIO,
            channel = "SMS", severity = Severity.MEDIUM, config = null, pattern = null
        )
        val ctx = context("최고 최상 1등 최저가 테스트")

        assertNull(evaluator.evaluate(ctx, rule))
    }

    @Test
    fun `pattern 필드로 키워드 인식 (config 없이)`() {
        val rule = Rule(
            code = "TEST", name = "테스트", type = RuleType.CONTENT_RATIO,
            channel = "SMS", severity = Severity.MEDIUM,
            pattern = "최고,최상",
            config = """{"maxCount": 1}"""
        )
        val ctx = context("최고의 최상 품질")  // 2회 > maxCount 1

        val result = evaluator.evaluate(ctx, rule)
        assertNotNull(result)
    }

    // -------------------------------------------------------------------------

    private fun superlativeRule(maxCount: Int) = Rule(
        code = "CH_ALL_RATIO_001",
        name = "최상급 과장 표현 과다",
        type = RuleType.CONTENT_RATIO,
        channel = "SMS,KAKAO,EMAIL",
        severity = Severity.MEDIUM,
        pattern = "최고,최상,1등,최저가",
        config = """{"maxCount": $maxCount, "keywords": "최고,최상,1등,최저가"}"""
    )

    private fun absoluteRule() = Rule(
        code = "CH_ALL_RATIO_002",
        name = "절대적 효과 과장 표현",
        type = RuleType.CONTENT_RATIO,
        channel = "SMS,KAKAO,EMAIL",
        severity = Severity.HIGH,
        pattern = "완전무료,100% 효과",
        config = """{"maxCount": 1, "keywords": "완전무료,100% 효과"}"""
    )

    private fun context(message: String) =
        EvaluationContext(message = message, channel = Channel.SMS)
}

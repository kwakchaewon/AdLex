package com.adlex.engine.evaluator

import com.adlex.domain.entity.Rule
import com.adlex.engine.model.EvaluationContext
import com.adlex.engine.model.EvaluationResult
import com.adlex.engine.model.RuleType
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component

@Component
class RegexEvaluator : RuleEvaluator {

    private val mapper = jacksonObjectMapper()

    override fun supports(type: RuleType) = type == RuleType.REGEX

    override fun evaluate(context: EvaluationContext, rule: Rule): EvaluationResult? {
        val pattern = rule.pattern ?: return null
        val config = rule.config?.let { mapper.readValue<Map<String, String>>(it) } ?: emptyMap()
        val matchMode = config["matchMode"] ?: "REQUIRE"
        val target = config["target"]

        // 검사 대상 텍스트: target 지정 시 options에서, 기본은 message
        val text = if (target != null) {
            context.options[target]?.toString() ?: ""
        } else {
            context.message
        }

        val regex = Regex(pattern)
        val matched = regex.containsMatchIn(text)

        val isViolation = when (matchMode) {
            "REQUIRE" -> !matched  // 패턴이 있어야 하는데 없으면 위반
            "DENY" -> matched      // 패턴이 없어야 하는데 있으면 위반
            else -> false
        }

        return if (isViolation) {
            EvaluationResult(
                ruleCode = rule.code,
                severity = rule.severity,
                message = buildViolationMessage(rule, matchMode, target),
                legalBasis = rule.legalBasis,
                suggestion = buildSuggestion(rule, matchMode)
            )
        } else null
    }

    private fun buildViolationMessage(rule: Rule, matchMode: String, target: String?): String =
        when (matchMode) {
            "REQUIRE" -> "${rule.name} 누락: ${target?.let { "$it 필드에 " } ?: ""}필수 표기가 없습니다"
            "DENY" -> "${rule.name} 위반: 금지된 표현이 포함되어 있습니다"
            else -> "${rule.name} 위반"
        }

    private fun buildSuggestion(rule: Rule, matchMode: String): String? =
        if (matchMode == "REQUIRE") "메시지에 ${rule.name}을 추가해 주세요" else null
}

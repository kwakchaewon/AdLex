package com.adlex.engine.evaluator

import com.adlex.domain.entity.Rule
import com.adlex.engine.model.EvaluationContext
import com.adlex.engine.model.EvaluationResult
import com.adlex.engine.model.RuleType
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component

@Component
class KeywordEvaluator : RuleEvaluator {

    private val mapper = jacksonObjectMapper()

    override fun supports(type: RuleType) = type == RuleType.KEYWORD

    override fun evaluate(context: EvaluationContext, rule: Rule): EvaluationResult? {
        val pattern = rule.pattern ?: return null
        val config = rule.config?.let { mapper.readValue<Map<String, String>>(it) } ?: emptyMap()
        val matchMode = config["matchMode"] ?: "DENY"

        val keywords = pattern.split(",").map(String::trim).filter(String::isNotBlank)
        val matchedKeyword = keywords.firstOrNull { context.message.contains(it) }

        val isViolation = when (matchMode) {
            "DENY" -> matchedKeyword != null
            "REQUIRE" -> matchedKeyword == null
            else -> false
        }

        return if (isViolation) {
            EvaluationResult(
                ruleCode = rule.code,
                severity = rule.severity,
                message = if (matchMode == "DENY")
                    "${rule.name} 위반: '${matchedKeyword}' 키워드가 포함되어 있습니다"
                else
                    "${rule.name} 누락: 필수 키워드가 없습니다",
                legalBasis = rule.legalBasis
            )
        } else null
    }
}

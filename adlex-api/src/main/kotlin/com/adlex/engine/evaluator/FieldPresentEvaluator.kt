package com.adlex.engine.evaluator

import com.adlex.domain.entity.Rule
import com.adlex.engine.model.EvaluationContext
import com.adlex.engine.model.EvaluationResult
import com.adlex.engine.model.RuleType
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component

@Component
class FieldPresentEvaluator : RuleEvaluator {

    private val mapper = jacksonObjectMapper()

    override fun supports(type: RuleType) = type == RuleType.FIELD_PRESENT

    override fun evaluate(context: EvaluationContext, rule: Rule): EvaluationResult? {
        val config = rule.config?.let { mapper.readValue<Map<String, String>>(it) } ?: return null
        val field = config["field"] ?: return null

        val value = resolvefield(field, context)
        val isPresent = !value.isNullOrBlank()

        return if (!isPresent) {
            EvaluationResult(
                ruleCode = rule.code,
                severity = rule.severity,
                message = "${rule.name} 누락: '$field' 정보가 없습니다",
                legalBasis = rule.legalBasis,
                suggestion = "'$field' 값을 포함하여 요청해 주세요"
            )
        } else null
    }

    /** sender 필드 또는 options에서 값을 읽어옴 */
    private fun resolvefield(field: String, context: EvaluationContext): String? =
        when (field) {
            "senderName" -> context.sender?.name
            "senderPhone" -> context.sender?.phoneNumber
            "senderEmail" -> context.sender?.email
            else -> context.options[field]?.toString()
        }
}

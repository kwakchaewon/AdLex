package com.adlex.engine.evaluator

import com.adlex.domain.entity.Rule
import com.adlex.engine.model.EvaluationContext
import com.adlex.engine.model.EvaluationResult
import com.adlex.engine.model.RuleType
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component

/**
 * 채널별 메시지 길이 제한 평가기.
 *
 * config JSON 형식:
 *  - 바이트 기반 (SMS): {"maxBytes": 90, "encoding": "EUC-KR"}
 *  - 문자 기반 (KAKAO/EMAIL): {"maxChars": 1000}
 *
 * 채널별 기준:
 *  - SMS     : 90바이트 (EUC-KR 기준, 한글 45자 / 영문 90자)
 *  - KAKAO   : 1,000자 (카카오 알림톡/친구톡 최대 길이)
 *  - EMAIL   : 제목(subject) 200자, 본문 별도 설정 가능
 */
@Component
class LengthCheckEvaluator : RuleEvaluator {

    private val mapper = jacksonObjectMapper()

    override fun supports(type: RuleType) = type == RuleType.LENGTH_CHECK

    override fun evaluate(context: EvaluationContext, rule: Rule): EvaluationResult? {
        val config = rule.config?.let {
            runCatching { mapper.readValue<Map<String, Any>>(it) }.getOrElse { emptyMap() }
        } ?: emptyMap()

        val maxBytes = (config["maxBytes"] as? Number)?.toInt()
        val maxChars = (config["maxChars"] as? Number)?.toInt()
        val encoding = config["encoding"] as? String ?: "EUC-KR"

        // subject 체크 (EMAIL 제목)
        val target = if (config["checkSubject"] == true) {
            context.options["subject"] as? String ?: context.message
        } else {
            context.message
        }

        return when {
            maxBytes != null -> checkBytes(target, maxBytes, encoding, rule)
            maxChars != null -> checkChars(target, maxChars, rule)
            else -> null
        }
    }

    private fun checkBytes(message: String, maxBytes: Int, encoding: String, rule: Rule): EvaluationResult? {
        val actualBytes = runCatching { message.toByteArray(charset(encoding)).size }
            .getOrElse { message.length }

        return if (actualBytes > maxBytes) {
            EvaluationResult(
                ruleCode = rule.code,
                severity = rule.severity,
                message = "${rule.name}: 메시지 길이 초과 (${actualBytes}바이트 / 최대 ${maxBytes}바이트)",
                legalBasis = rule.legalBasis,
                suggestion = "메시지를 ${maxBytes}바이트 이하로 줄여주세요."
            )
        } else null
    }

    private fun checkChars(message: String, maxChars: Int, rule: Rule): EvaluationResult? {
        val actualChars = message.length

        return if (actualChars > maxChars) {
            EvaluationResult(
                ruleCode = rule.code,
                severity = rule.severity,
                message = "${rule.name}: 메시지 길이 초과 (${actualChars}자 / 최대 ${maxChars}자)",
                legalBasis = rule.legalBasis,
                suggestion = "메시지를 ${maxChars}자 이하로 줄여주세요."
            )
        } else null
    }
}

package com.adlex.engine.evaluator

import com.adlex.domain.entity.Rule
import com.adlex.engine.model.EvaluationContext
import com.adlex.engine.model.EvaluationResult
import com.adlex.engine.model.RuleType
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component

/**
 * 광고성 키워드 밀도(Content Ratio) 평가기.
 *
 * 과장·과대 광고 표현이 과도하게 사용된 경우를 감지합니다.
 *
 * config JSON 형식:
 * {
 *   "maxCount": 3,
 *   "keywords": "최고,최상,1등,최저가,완전무료,압도적,혁신적,독보적"
 * }
 *
 * - maxCount  : 허용 최대 키워드 등장 횟수 (초과 시 위반)
 * - keywords  : 감지할 과장 표현 목록 (콤마 구분)
 *
 * 활용 예:
 *  - 최상급 표현(최고·최상·1등) 3개 초과 → MEDIUM 위반
 *  - 완전무료·절대적·독보적 등 과장 표현 → HIGH 위반
 */
@Component
class ContentRatioEvaluator : RuleEvaluator {

    private val mapper = jacksonObjectMapper()

    override fun supports(type: RuleType) = type == RuleType.CONTENT_RATIO

    override fun evaluate(context: EvaluationContext, rule: Rule): EvaluationResult? {
        val config = rule.config?.let {
            runCatching { mapper.readValue<Map<String, Any>>(it) }.getOrElse { emptyMap() }
        } ?: emptyMap()

        val maxCount = (config["maxCount"] as? Number)?.toInt() ?: 3
        val keywordsStr = config["keywords"] as? String
            ?: rule.pattern
            ?: return null

        val keywords = keywordsStr.split(",").map(String::trim).filter(String::isNotBlank)
        if (keywords.isEmpty()) return null

        val matched = keywords.filter { kw -> context.message.contains(kw) }
        val totalCount = matched.sumOf { kw ->
            var count = 0
            var idx = 0
            while (true) {
                idx = context.message.indexOf(kw, idx)
                if (idx == -1) break
                count++
                idx += kw.length
            }
            count
        }

        return if (totalCount > maxCount) {
            EvaluationResult(
                ruleCode = rule.code,
                severity = rule.severity,
                message = "${rule.name}: 과장 표현 과다 사용 (${totalCount}회 / 최대 ${maxCount}회 허용) — '${matched.joinToString(", ")}'",
                legalBasis = rule.legalBasis,
                suggestion = "과장·최상급 표현을 줄이고 구체적인 근거를 제시하세요."
            )
        } else null
    }
}

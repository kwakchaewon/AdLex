package com.adlex.engine.evaluator

import com.adlex.domain.entity.Rule
import com.adlex.engine.model.EvaluationContext
import com.adlex.engine.model.EvaluationResult
import com.adlex.engine.model.RuleType
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
class TimeRangeEvaluator : RuleEvaluator {

    private val mapper = jacksonObjectMapper()
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    override fun supports(type: RuleType) = type == RuleType.TIME_RANGE

    override fun evaluate(context: EvaluationContext, rule: Rule): EvaluationResult? {
        val config = rule.config?.let { mapper.readValue<Map<String, String>>(it) } ?: return null
        val denyStart = config["denyStart"] ?: return null
        val denyEnd = config["denyEnd"] ?: return null
        val timezone = config["timezone"] ?: "Asia/Seoul"

        val zoneId = ZoneId.of(timezone)
        val checkInstant = context.scheduledAt ?: Instant.now()
        val localTime = checkInstant.atZone(zoneId).toLocalTime()

        val start = LocalTime.parse(denyStart, timeFormatter)
        val end = LocalTime.parse(denyEnd, timeFormatter)

        val isDenied = if (start.isAfter(end)) {
            // 자정을 넘는 경우 (예: 21:00 ~ 08:00)
            localTime >= start || localTime <= end
        } else {
            localTime >= start && localTime <= end
        }

        return if (isDenied) {
            EvaluationResult(
                ruleCode = rule.code,
                severity = rule.severity,
                message = "${rule.name} 위반: ${denyStart}~${denyEnd}(${timezone}) 발송 제한 시간대입니다",
                legalBasis = rule.legalBasis,
                suggestion = "${denyEnd} 이후에 발송해 주세요"
            )
        } else null
    }
}

package com.adlex.domain.service

import com.adlex.domain.entity.CheckLog
import com.adlex.domain.repository.CheckLogRepository
import com.adlex.engine.RuleRegistry
import com.adlex.engine.evaluator.RuleEvaluator
import com.adlex.engine.model.EvaluationContext
import com.adlex.engine.model.EvaluationResult
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.stereotype.Service
import java.time.Instant

data class CheckResultDto(
    val compliant: Boolean,
    val violations: List<EvaluationResult>,
    val checkedAt: Instant,
    val processingMs: Long
)

@Service
class ComplianceCheckService(
    private val ruleRegistry: RuleRegistry,
    private val evaluators: List<RuleEvaluator>,
    private val checkLogRepository: CheckLogRepository
) {
    private val mapper = jacksonObjectMapper()

    @Suppress("UNCHECKED_CAST")
    fun check(tenantId: Long, context: EvaluationContext): CheckResultDto {
        val startMs = System.currentTimeMillis()

        // 1. 채널에 해당하는 활성 규칙 로드
        val rules = ruleRegistry.getRulesByChannel(context.channel)

        // 2. skipRules 옵션 처리
        val skipRules = (context.options["skipRules"] as? List<*>)
            ?.mapNotNull { it?.toString() }
            ?: emptyList()

        val applicableRules = if (skipRules.isEmpty()) rules
        else rules.filter { it.code !in skipRules }

        // 3. 각 규칙 평가
        val violations = applicableRules.mapNotNull { rule ->
            evaluators.find { it.supports(rule.type) }?.evaluate(context, rule)
        }

        val processingMs = System.currentTimeMillis() - startMs

        // 4. CheckLog 저장
        checkLogRepository.save(
            CheckLog(
                tenantId = tenantId,
                message = context.message,
                channel = context.channel,
                compliant = violations.isEmpty(),
                violationCount = violations.size,
                violations = mapper.writeValueAsString(violations),
                processingMs = processingMs
            )
        )

        return CheckResultDto(
            compliant = violations.isEmpty(),
            violations = violations,
            checkedAt = Instant.now(),
            processingMs = processingMs
        )
    }
}

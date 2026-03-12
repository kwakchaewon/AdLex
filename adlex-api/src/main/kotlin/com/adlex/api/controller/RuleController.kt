package com.adlex.api.controller

import com.adlex.api.advice.BusinessException
import com.adlex.api.advice.ErrorCode
import com.adlex.api.dto.RuleResponse
import com.adlex.engine.RuleRegistry
import com.adlex.engine.model.Channel
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/rules")
@Tag(name = "Rules", description = "법규 준수 규칙 조회 API")
class RuleController(
    private val ruleRegistry: RuleRegistry
) {

    @GetMapping
    @Operation(summary = "활성 규칙 목록 조회")
    fun getRules(@RequestParam channel: Channel?): List<RuleResponse> {
        val rules = if (channel != null) {
            ruleRegistry.getRulesByChannel(channel)
        } else {
            ruleRegistry.getActiveRules()
        }
        return rules.map { rule ->
            RuleResponse(
                code = rule.code,
                name = rule.name,
                description = rule.description,
                type = rule.type,
                channels = rule.channels(),
                severity = rule.severity,
                legalBasis = rule.legalBasis,
                active = rule.active
            )
        }
    }

    @GetMapping("/{code}")
    @Operation(summary = "규칙 상세 조회")
    fun getRule(@PathVariable code: String): RuleResponse {
        val rule = ruleRegistry.getActiveRules().find { it.code == code }
            ?: throw BusinessException(ErrorCode.NOT_FOUND, "규칙을 찾을 수 없습니다: $code")
        return RuleResponse(
            code = rule.code,
            name = rule.name,
            description = rule.description,
            type = rule.type,
            channels = rule.channels(),
            severity = rule.severity,
            legalBasis = rule.legalBasis,
            active = rule.active
        )
    }
}

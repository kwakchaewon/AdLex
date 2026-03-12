package com.adlex.api.dto

import com.adlex.domain.entity.Rule
import com.adlex.engine.model.Channel
import com.adlex.engine.model.RuleType
import com.adlex.engine.model.Severity
import java.time.Instant

data class RuleResponse(
    val id: Long,
    val code: String,
    val name: String,
    val description: String?,
    val type: RuleType,
    val channel: String,
    val channels: List<Channel>,
    val severity: Severity,
    val pattern: String?,
    val config: String?,
    val legalBasis: String?,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(rule: Rule) = RuleResponse(
            id = rule.id,
            code = rule.code,
            name = rule.name,
            description = rule.description,
            type = rule.type,
            channel = rule.channel,
            channels = rule.channels(),
            severity = rule.severity,
            pattern = rule.pattern,
            config = rule.config,
            legalBasis = rule.legalBasis,
            active = rule.active,
            createdAt = rule.createdAt,
            updatedAt = rule.updatedAt
        )
    }
}

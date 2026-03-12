package com.adlex.api.dto

import com.adlex.engine.model.Channel
import com.adlex.engine.model.RuleType
import com.adlex.engine.model.Severity

data class RuleResponse(
    val code: String,
    val name: String,
    val description: String?,
    val type: RuleType,
    val channels: List<Channel>,
    val severity: Severity,
    val legalBasis: String?,
    val active: Boolean
)

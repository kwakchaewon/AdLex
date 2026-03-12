package com.adlex.engine.model

data class EvaluationResult(
    val ruleCode: String,
    val severity: Severity,
    val message: String,
    val legalBasis: String? = null,
    val suggestion: String? = null
)

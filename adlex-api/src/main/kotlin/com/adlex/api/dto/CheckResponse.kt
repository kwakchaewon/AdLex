package com.adlex.api.dto

import com.adlex.engine.model.Severity
import java.time.Instant

data class CheckResponse(
    val compliant: Boolean,
    val violationCount: Int,
    val violations: List<ViolationDto>,
    val checkedAt: Instant,
    val processingMs: Long,
    val llmAnalysis: LlmAnalysisDto? = null
)

data class LlmAnalysisDto(
    val analysis: String,
    val citedLawCount: Int,
    val citedPrecedentCount: Int
)

data class ViolationDto(
    val ruleCode: String,
    val severity: Severity,
    val message: String,
    val legalBasis: String? = null,
    val suggestion: String? = null
)

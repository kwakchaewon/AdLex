package com.adlex.api.dto

data class BatchCheckResponse(
    val results: List<CheckResponse>,
    val summary: BatchSummary
)

data class BatchSummary(
    val total: Int,
    val compliant: Int,
    val violated: Int,
    val processingMs: Long
)

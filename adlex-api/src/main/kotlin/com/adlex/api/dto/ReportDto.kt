package com.adlex.api.dto

import com.adlex.engine.model.Channel
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant

data class ReportRequest(
    @field:NotBlank
    val message: String,

    @field:NotNull
    val channel: Channel,

    /** LLM Layer 2 보조 분석 포함 여부 (PRO 이상 플랜) */
    val includeLlmAnalysis: Boolean = false
)

data class ReportResponse(
    val message: String,
    val channel: Channel,
    val compliant: Boolean,
    val violationCount: Int,
    val violations: List<ViolationDto>,

    /** RAG 검색으로 찾은 관련 법령 조문 */
    val relatedLaws: List<RelatedLawDto>,

    /** RAG 검색으로 찾은 유사 위반 판례 */
    val similarPrecedents: List<SimilarPrecedentDto>,

    /** LLM Layer 2 보조 분석 (includeLlmAnalysis=true 이고 PRO 이상 플랜인 경우) */
    val llmAnalysis: LlmAnalysisDto?,

    val generatedAt: Instant,
    val processingMs: Long
)

data class RelatedLawDto(
    val lawName: String,
    val articleNo: String?,
    val articleTitle: String?,
    val content: String,
    val sourceUrl: String?
)

data class SimilarPrecedentDto(
    val caseNo: String?,
    val authority: String,
    val title: String,
    val summary: String?
)

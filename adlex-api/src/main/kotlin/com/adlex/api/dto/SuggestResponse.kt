package com.adlex.api.dto

data class SuggestResponse(
    /** 원본 메시지 */
    val original: String,
    /** LLM이 제안한 수정 메시지 */
    val suggested: String,
    /** 수정 이유 요약 */
    val rationale: String,
    /** 참조된 법령 수 */
    val citedLawCount: Int,
    /** 참조된 판례 수 */
    val citedPrecedentCount: Int
)

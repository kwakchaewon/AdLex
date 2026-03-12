package com.adlex.infra.rag

/**
 * RAG 검색 결과 컨텍스트.
 * LLM 프롬프트에 주입되는 법령·판례 정보.
 */
data class RagContext(
    val lawChunks: List<LawChunkResult>,
    val precedents: List<PrecedentResult>
) {
    /** 컨텍스트가 유효한지 (법령 또는 판례 결과가 하나 이상 있는 경우) */
    val hasContent: Boolean get() = lawChunks.isNotEmpty() || precedents.isNotEmpty()

    /** LLM 프롬프트 삽입용 포맷 문자열 */
    fun toPromptText(): String = buildString {
        if (lawChunks.isNotEmpty()) {
            appendLine("[관련 법령]")
            lawChunks.forEachIndexed { i, chunk ->
                appendLine("${i + 1}. ${chunk.lawName} ${chunk.articleNo ?: ""} ${chunk.articleTitle ?: ""}")
                appendLine("   ${chunk.content}")
            }
        }
        if (precedents.isNotEmpty()) {
            appendLine()
            appendLine("[유사 위반 사례]")
            precedents.forEachIndexed { i, p ->
                appendLine("${i + 1}. [${p.authority}] ${p.title}")
                p.summary?.let { appendLine("   $it") }
            }
        }
    }
}

data class LawChunkResult(
    val id: Long,
    val lawName: String,
    val articleNo: String?,
    val articleTitle: String?,
    val content: String,
    val sourceUrl: String?
)

data class PrecedentResult(
    val id: Long,
    val caseNo: String?,
    val authority: String,
    val title: String,
    val summary: String?
)

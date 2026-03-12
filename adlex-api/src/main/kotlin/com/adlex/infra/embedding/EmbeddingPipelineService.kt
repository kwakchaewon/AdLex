package com.adlex.infra.embedding

import com.adlex.domain.repository.LawChunkRepository
import com.adlex.domain.repository.PrecedentRepository
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 임베딩 파이프라인 서비스.
 * embedding이 없는 law_chunks / precedents 를 배치 처리하여 벡터를 채움.
 * - AdminController에서 수동 트리거: POST /api/admin/embeddings/generate
 * - 법령 크롤러(3.9) 완료 후 자동 호출 예정
 */
@Service
class EmbeddingPipelineService(
    private val embeddingService: EmbeddingService,
    private val lawChunkRepository: LawChunkRepository,
    private val precedentRepository: PrecedentRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val batchSize = 20   // OpenAI API 배치 크기

    /** law_chunks 전체 임베딩 생성 (embedding IS NULL 인 항목만 처리) */
    @Transactional
    fun generateLawChunkEmbeddings(): EmbeddingPipelineResult {
        val chunks = lawChunkRepository.findAllByActiveTrue()
            .filter { it.id > 0 }   // 실제 저장된 항목만

        if (chunks.isEmpty()) {
            log.info("임베딩 대상 law_chunks 없음")
            return EmbeddingPipelineResult(processed = 0, skipped = 0)
        }

        log.info("law_chunks 임베딩 시작: 총 ${chunks.size}개")
        var processed = 0

        chunks.chunked(batchSize).forEach { batch ->
            val texts = batch.map { chunk ->
                // 법령명 + 조항 + 내용을 합쳐 임베딩 — 검색 품질 향상
                buildEmbeddingText(
                    lawName = chunk.lawName,
                    articleNo = chunk.articleNo,
                    articleTitle = chunk.articleTitle,
                    content = chunk.content
                )
            }

            runCatching {
                val vectors = embeddingService.embedBatch(texts)
                batch.zip(vectors).forEach { (chunk, vector) ->
                    lawChunkRepository.updateEmbedding(chunk.id, vector)
                    processed++
                }
                log.debug("law_chunks 배치 처리 완료: ${batch.size}개")
            }.onFailure { e ->
                log.error("law_chunks 배치 임베딩 실패: ${e.message}", e)
            }
        }

        log.info("law_chunks 임베딩 완료: $processed / ${chunks.size}개")
        return EmbeddingPipelineResult(processed = processed, skipped = chunks.size - processed)
    }

    /** precedents 전체 임베딩 생성 */
    @Transactional
    fun generatePrecedentEmbeddings(): EmbeddingPipelineResult {
        val precedents = precedentRepository.findAllByActiveTrue()

        if (precedents.isEmpty()) {
            log.info("임베딩 대상 precedents 없음")
            return EmbeddingPipelineResult(processed = 0, skipped = 0)
        }

        log.info("precedents 임베딩 시작: 총 ${precedents.size}개")
        var processed = 0

        precedents.chunked(batchSize).forEach { batch ->
            val texts = batch.map { p ->
                buildEmbeddingText(
                    lawName = p.authority,
                    articleNo = p.caseNo,
                    articleTitle = p.title,
                    content = "${p.summary ?: ""}\n${p.content}"
                )
            }

            runCatching {
                val vectors = embeddingService.embedBatch(texts)
                batch.zip(vectors).forEach { (precedent, vector) ->
                    precedentRepository.updateEmbedding(precedent.id, vector)
                    processed++
                }
            }.onFailure { e ->
                log.error("precedents 배치 임베딩 실패: ${e.message}", e)
            }
        }

        log.info("precedents 임베딩 완료: $processed / ${precedents.size}개")
        return EmbeddingPipelineResult(processed = processed, skipped = precedents.size - processed)
    }

    /** 전체 파이프라인 실행 */
    fun generateAll(): EmbeddingPipelineResult {
        val law = generateLawChunkEmbeddings()
        val prec = generatePrecedentEmbeddings()
        return EmbeddingPipelineResult(
            processed = law.processed + prec.processed,
            skipped = law.skipped + prec.skipped
        )
    }

    private fun buildEmbeddingText(
        lawName: String,
        articleNo: String?,
        articleTitle: String?,
        content: String
    ): String = buildString {
        append(lawName)
        articleNo?.let { append(" $it") }
        articleTitle?.let { append(" $it") }
        append(": ")
        append(content.take(512))   // OpenAI 토큰 한도 고려
    }
}

data class EmbeddingPipelineResult(val processed: Int, val skipped: Int)

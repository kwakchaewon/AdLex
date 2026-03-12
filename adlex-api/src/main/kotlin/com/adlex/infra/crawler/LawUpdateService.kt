package com.adlex.infra.crawler

import com.adlex.domain.entity.LawChunk
import com.adlex.domain.repository.LawChunkRepository
import com.adlex.infra.embedding.EmbeddingPipelineService
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException

/**
 * 법령 변경 감지 후 청크 분할 → DB 저장 → 임베딩 갱신 파이프라인.
 *
 * 흐름:
 *  1. 국가법령정보센터 API /DRF/lawService.do 에서 전문(조문 단위) 조회
 *  2. 기존 청크 비활성화 (active = false)
 *  3. 새 청크 저장
 *  4. 신규 청크 임베딩 생성 (EmbeddingPipelineService)
 */
@Service
class LawUpdateService(
    private val lawChunkRepository: LawChunkRepository,
    private val embeddingPipelineService: EmbeddingPipelineService,
    @Value("\${adlex.crawler.law-api-key:}") private val apiKey: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val webClient = WebClient.builder()
        .baseUrl("https://www.law.go.kr")
        .defaultHeader("Accept", "application/json")
        .build()

    /**
     * 변경 감지된 법령 1개를 업데이트합니다.
     *
     * @param changeResult 크롤러에서 반환한 법령 변경 결과 (mst 필수)
     * @return 업데이트 결과
     */
    @Transactional
    fun updateLaw(changeResult: LawChangeResult): LawUpdateResult {
        if (changeResult.mst == null) {
            log.warn("법령 일련번호 없음 — 업데이트 건너뜀: ${changeResult.lawName}")
            return LawUpdateResult(lawName = changeResult.lawName, success = false, reason = "MST 없음")
        }

        if (apiKey.isBlank()) {
            log.warn("LAW_API_KEY 미설정 — 법령 업데이트 건너뜀: ${changeResult.lawName}")
            return LawUpdateResult(lawName = changeResult.lawName, success = false, reason = "API 키 없음")
        }

        log.info("법령 업데이트 시작: ${changeResult.lawName} (MST=${changeResult.mst})")

        // 1. 국가법령정보센터 전문 조회
        val articles = fetchLawArticles(changeResult.mst, changeResult.lawName)
            ?: return LawUpdateResult(lawName = changeResult.lawName, success = false, reason = "API 조회 실패")

        if (articles.isEmpty()) {
            log.warn("법령 조문 없음: ${changeResult.lawName}")
            return LawUpdateResult(lawName = changeResult.lawName, success = false, reason = "조문 없음")
        }

        // 2. 기존 청크 비활성화
        val existing = lawChunkRepository.findAllByLawNameAndActiveTrue(changeResult.lawName)
        existing.forEach { it.active = false }
        lawChunkRepository.saveAll(existing)
        log.debug("기존 청크 비활성화: ${existing.size}개")

        // 3. 새 청크 저장 (embedding 은 null — 다음 단계에서 채움)
        val effectiveDate = changeResult.latestDate
        val sourceUrl = "https://www.law.go.kr/LSW/lsInfoP.do?lsiSeq=${changeResult.mst}"

        val newChunks = articles.map { article ->
            LawChunk(
                lawName = changeResult.lawName,
                articleNo = article.articleNo,
                articleTitle = article.title,
                content = article.content,
                lawDate = effectiveDate,
                sourceUrl = sourceUrl
            )
        }
        val saved = lawChunkRepository.saveAll(newChunks)
        log.info("새 청크 저장: ${saved.size}개 (법령: ${changeResult.lawName})")

        // 4. 신규 법령 청크 임베딩 생성
        val embResult = runCatching {
            embeddingPipelineService.generateEmbeddingsForLaw(changeResult.lawName)
        }.onFailure { e ->
            log.error("임베딩 생성 실패 [${changeResult.lawName}]: ${e.message}", e)
        }.getOrNull()

        val embeddedCount = embResult?.processed ?: 0
        log.info(
            "법령 업데이트 완료: ${changeResult.lawName} — 청크 ${saved.size}개, 임베딩 ${embeddedCount}개"
        )

        return LawUpdateResult(
            lawName = changeResult.lawName,
            success = true,
            chunkCount = saved.size,
            embeddedCount = embeddedCount,
            reason = "업데이트 완료 (시행일: ${changeResult.latestDate})"
        )
    }

    /**
     * 변경된 법령 목록 전체를 순차 업데이트.
     */
    fun updateChangedLaws(changes: List<LawChangeResult>): List<LawUpdateResult> {
        val targets = changes.filter { it.changed }
        if (targets.isEmpty()) {
            log.info("업데이트 대상 법령 없음")
            return emptyList()
        }

        log.info("법령 업데이트 시작: ${targets.size}개")
        return targets.map { changeResult ->
            runCatching { updateLaw(changeResult) }
                .onFailure { log.error("법령 업데이트 실패 [${changeResult.lawName}]: ${it.message}") }
                .getOrElse {
                    LawUpdateResult(lawName = changeResult.lawName, success = false, reason = it.message ?: "오류")
                }
        }.also { results ->
            val ok = results.count { it.success }
            log.info("법령 업데이트 완료: ${ok}/${results.size}개 성공")
        }
    }

    // -------------------------------------------------------------------------
    // 내부: 국가법령정보센터 법령 전문 조회
    // -------------------------------------------------------------------------

    @Suppress("UNCHECKED_CAST")
    private fun fetchLawArticles(mst: String, lawName: String): List<LawArticle>? {
        return try {
            val response = webClient.get()
                .uri { builder ->
                    builder.path("/DRF/lawService.do")
                        .queryParam("OC", apiKey)
                        .queryParam("target", "law")
                        .queryParam("MST", mst)
                        .queryParam("type", "JSON")
                        .build()
                }
                .retrieve()
                .bodyToMono(Map::class.java)
                .block() ?: return null

            val lawData = response["법령"] as? Map<String, Any> ?: return null
            val articlesMap = lawData["조문"] as? Map<String, Any> ?: return null
            val articleList = articlesMap["조문단위"] as? List<Map<String, Any>> ?: return null

            articleList.mapNotNull { article ->
                val articleNo = article["조문번호"]?.toString() ?: return@mapNotNull null
                val title = article["조문제목"]?.toString()
                val content = buildArticleContent(article)
                if (content.isBlank()) return@mapNotNull null
                LawArticle(articleNo = "제${articleNo}조", title = title, content = content)
            }
        } catch (e: WebClientResponseException) {
            log.warn("법령 전문 API 오류 [$lawName]: ${e.statusCode}")
            null
        } catch (e: Exception) {
            log.warn("법령 전문 API 호출 실패 [$lawName]: ${e.message}")
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildArticleContent(article: Map<String, Any>): String = buildString {
        val mainContent = article["조문내용"]?.toString()
        if (!mainContent.isNullOrBlank()) append(mainContent)

        // 항(paragraph) 목록
        val paragraphs = article["항"] as? List<Map<String, Any>> ?: emptyList()
        paragraphs.forEach { para ->
            val paraNo = para["항번호"]?.toString() ?: ""
            val paraContent = para["항내용"]?.toString() ?: ""
            if (paraContent.isNotBlank()) {
                append("\n${paraNo}항 $paraContent")
            }
        }
    }
}

/** 법령 조문 1개 (청크 단위) */
data class LawArticle(
    val articleNo: String,
    val title: String?,
    val content: String
)

/** 법령 업데이트 파이프라인 결과 */
data class LawUpdateResult(
    val lawName: String,
    val success: Boolean,
    val chunkCount: Int = 0,
    val embeddedCount: Int = 0,
    val reason: String = ""
)

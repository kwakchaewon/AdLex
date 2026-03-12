package com.adlex.infra.crawler

import com.adlex.domain.repository.LawChunkRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 국가법령정보센터 Open API 기반 법령 변경 감지 크롤러.
 *
 * 대상 법령별로 API에서 최신 시행일자를 조회하여
 * DB에 저장된 최신 시행일자와 비교, 변경된 법령 목록을 반환합니다.
 *
 * API 문서: https://www.law.go.kr/LSW/openApiInfo.do
 */
@Service
class LawCrawlerService(
    private val lawChunkRepository: LawChunkRepository,
    @Value("\${adlex.crawler.law-api-key:}") private val apiKey: String,
    @Value("\${adlex.crawler.target-laws}") private val targetLaws: List<String>
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    private val webClient = WebClient.builder()
        .baseUrl("https://www.law.go.kr")
        .defaultHeader("Accept", "application/json")
        .build()

    /**
     * 모든 대상 법령의 변경 여부를 검사합니다.
     *
     * @return 변경이 감지된 법령의 크롤링 결과 목록
     */
    fun detectChanges(): List<LawChangeResult> {
        if (apiKey.isBlank()) {
            log.warn("LAW_API_KEY가 설정되지 않아 법령 크롤링을 건너뜁니다.")
            return emptyList()
        }

        log.info("법령 변경 감지 시작: 대상 법령 ${targetLaws.size}개")

        return targetLaws.mapNotNull { lawName ->
            runCatching { checkLawChange(lawName) }
                .onFailure { log.warn("법령 변경 감지 실패 [$lawName]: ${it.message}") }
                .getOrNull()
        }.also {
            log.info("법령 변경 감지 완료: 변경 감지 ${it.filter { r -> r.changed }.size}개")
        }
    }

    /**
     * 개별 법령 변경 여부 확인.
     *
     * @param lawName 법령명 (예: "표시·광고의 공정화에 관한 법률")
     * @return 법령 변경 결과 (변경 없으면 changed=false)
     */
    fun checkLawChange(lawName: String): LawChangeResult {
        val apiResult = fetchLatestLawInfo(lawName)
            ?: return LawChangeResult(lawName = lawName, changed = false, reason = "API에서 법령 정보를 찾을 수 없음")

        val storedDate = lawChunkRepository.findAllByLawNameAndActiveTrue(lawName)
            .mapNotNull { it.lawDate }
            .maxOrNull()

        val changed = storedDate == null || apiResult.effectiveDate.isAfter(storedDate)

        return LawChangeResult(
            lawName = lawName,
            changed = changed,
            latestDate = apiResult.effectiveDate,
            storedDate = storedDate,
            mst = apiResult.mst,
            reason = when {
                storedDate == null -> "DB에 법령 데이터 없음 (신규)"
                changed -> "API 시행일자(${apiResult.effectiveDate}) > DB 시행일자($storedDate)"
                else -> "변경 없음"
            }
        ).also {
            if (changed) {
                log.info("법령 변경 감지: $lawName — ${it.reason}")
            } else {
                log.debug("법령 변경 없음: $lawName (시행일: ${storedDate})")
            }
        }
    }

    /**
     * 국가법령정보센터 API에서 법령 최신 정보 조회.
     * 법령명으로 검색 후 첫 번째 결과(가장 최신)를 반환합니다.
     */
    private fun fetchLatestLawInfo(lawName: String): LawApiResult? {
        return try {
            @Suppress("UNCHECKED_CAST")
            val response = webClient.get()
                .uri { builder ->
                    builder.path("/DRF/lawSearch.do")
                        .queryParam("OC", apiKey)
                        .queryParam("target", "law")
                        .queryParam("type", "JSON")
                        .queryParam("query", lawName)
                        .queryParam("display", "1")
                        .build()
                }
                .retrieve()
                .bodyToMono(Map::class.java)
                .block() ?: return null

            val lawSearch = response["LawSearch"] as? Map<String, Any> ?: return null
            val laws = lawSearch["law"] as? List<Map<String, Any>> ?: return null
            val law = laws.firstOrNull() ?: return null

            val mst = law["법령일련번호"]?.toString() ?: return null
            val effectiveDateStr = law["시행일자"]?.toString() ?: return null

            val effectiveDate = runCatching {
                LocalDate.parse(effectiveDateStr, dateFormatter)
            }.getOrNull() ?: return null

            LawApiResult(mst = mst, effectiveDate = effectiveDate)
        } catch (e: WebClientResponseException) {
            log.warn("국가법령정보센터 API 오류 [$lawName]: ${e.statusCode}")
            null
        } catch (e: Exception) {
            log.warn("국가법령정보센터 API 호출 실패 [$lawName]: ${e.message}")
            null
        }
    }
}

/** 국가법령정보센터 API 조회 결과 */
data class LawApiResult(
    val mst: String,
    val effectiveDate: LocalDate
)

/** 법령 변경 감지 결과 */
data class LawChangeResult(
    val lawName: String,
    val changed: Boolean,
    val latestDate: LocalDate? = null,
    val storedDate: LocalDate? = null,
    val mst: String? = null,
    val reason: String = ""
)

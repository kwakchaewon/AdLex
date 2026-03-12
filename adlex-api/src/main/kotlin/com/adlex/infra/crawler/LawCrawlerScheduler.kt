package com.adlex.infra.crawler

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 법령 변경 감지 스케줄러.
 * adlex.crawler.enabled=true 일 때만 활성화됩니다.
 *
 * 기본 실행 시간: 매일 새벽 2시 (adlex.crawler.cron 으로 변경 가능)
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = ["adlex.crawler.enabled"], havingValue = "true", matchIfMissing = false)
class LawCrawlerScheduler(
    private val lawCrawlerService: LawCrawlerService,
    private val lawUpdateService: LawUpdateService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 법령 변경 감지 + 청크·임베딩 자동 업데이트 스케줄.
     * cron 표현식은 application.yml의 adlex.crawler.cron 참조.
     */
    @Scheduled(cron = "\${adlex.crawler.cron:0 0 2 * * *}")
    fun runDailyCrawl() {
        log.info("법령 변경 감지 스케줄 시작")
        try {
            // 1단계: 변경 감지
            val results = lawCrawlerService.detectChanges()
            val changed = results.filter { it.changed }
            val unchanged = results.filter { !it.changed }

            log.info(
                "법령 변경 감지 완료: 총 ${results.size}개 중 변경 ${changed.size}개, 미변경 ${unchanged.size}개"
            )

            // 2단계: 변경된 법령 청크·임베딩 업데이트
            if (changed.isNotEmpty()) {
                log.info("변경 법령 업데이트 시작: ${changed.map { it.lawName }}")
                val updateResults = lawUpdateService.updateChangedLaws(changed)
                val ok = updateResults.count { it.success }
                log.info("법령 업데이트 완료: ${ok}/${updateResults.size}개 성공")
            }
        } catch (e: Exception) {
            log.error("법령 변경 감지 스케줄 실행 중 오류 발생", e)
        }
    }
}

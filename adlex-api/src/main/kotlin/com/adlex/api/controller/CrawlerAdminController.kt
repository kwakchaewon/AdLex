package com.adlex.api.controller

import com.adlex.infra.crawler.LawChangeResult
import com.adlex.infra.crawler.LawCrawlerService
import com.adlex.infra.crawler.LawUpdateResult
import com.adlex.infra.crawler.LawUpdateService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal/crawler")
@Tag(name = "Crawler Admin", description = "법령 크롤러 관리 API (내부용)")
class CrawlerAdminController(
    private val lawCrawlerService: LawCrawlerService,
    private val lawUpdateService: LawUpdateService
) {

    @PostMapping("/law/detect")
    @Operation(summary = "전체 법령 변경 감지 실행 (수동 트리거)")
    fun detectAllChanges(): List<LawChangeResult> =
        lawCrawlerService.detectChanges()

    @GetMapping("/law/check")
    @Operation(summary = "특정 법령 변경 여부 확인")
    fun checkLaw(@RequestParam lawName: String): LawChangeResult =
        lawCrawlerService.checkLawChange(lawName)

    @PostMapping("/law/update")
    @Operation(summary = "전체 법령 변경 감지 + 청크·임베딩 업데이트 (수동 트리거)")
    fun detectAndUpdate(): List<LawUpdateResult> {
        val changes = lawCrawlerService.detectChanges()
        return lawUpdateService.updateChangedLaws(changes)
    }

    @PostMapping("/law/update/single")
    @Operation(summary = "특정 법령 청크·임베딩 강제 업데이트")
    fun updateSingleLaw(@RequestParam lawName: String): LawUpdateResult {
        val change = lawCrawlerService.checkLawChange(lawName)
        return lawUpdateService.updateLaw(change.copy(changed = true))
    }
}

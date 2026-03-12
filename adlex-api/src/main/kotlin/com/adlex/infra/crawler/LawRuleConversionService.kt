package com.adlex.infra.crawler

import com.adlex.domain.entity.LawChunk
import com.adlex.domain.entity.Rule
import com.adlex.domain.repository.LawChunkRepository
import com.adlex.domain.repository.RuleRepository
import com.adlex.engine.model.RuleType
import com.adlex.engine.model.Severity
import com.adlex.infra.llm.ClaudeApiClient
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 법령 조문 → 규칙 DB 자동 변환 파이프라인 (LLM 보조).
 *
 * 흐름:
 *  1. 법령명으로 활성 LawChunk 목록 조회
 *  2. 각 조문을 Claude API에 전달하여 규칙 후보(JSON) 추출
 *  3. 규칙 코드로 upsert (신규 생성 시 active=false → 담당자 검토 후 활성화)
 *
 * 자동 생성된 규칙은 active=false 로 저장되며, 운영자가 검토 후 수동 활성화해야 합니다.
 */
@Service
class LawRuleConversionService(
    private val claudeApiClient: ClaudeApiClient,
    private val ruleRepository: RuleRepository,
    private val lawChunkRepository: LawChunkRepository,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 특정 법령의 활성 청크를 LLM으로 분석하여 규칙 DB에 자동 반영합니다.
     *
     * @param lawName 법령명
     * @return 변환 결과
     */
    @Transactional
    fun convertLawToRules(lawName: String): LawRuleConversionResult {
        val chunks = lawChunkRepository.findAllByLawNameAndActiveTrue(lawName)
        if (chunks.isEmpty()) {
            log.info("변환 대상 청크 없음: $lawName")
            return LawRuleConversionResult(lawName = lawName, processed = 0)
        }

        log.info("법령 → 규칙 변환 시작: $lawName (${chunks.size}개 조문)")
        var created = 0
        var updated = 0
        var failed = 0

        chunks.forEach { chunk ->
            runCatching {
                val candidates = extractRulesFromChunk(chunk)
                candidates.forEach { candidate ->
                    when (upsertRule(candidate)) {
                        UpsertAction.CREATED -> created++
                        UpsertAction.UPDATED -> updated++
                        UpsertAction.SKIPPED -> { /* no-op */ }
                    }
                }
            }.onFailure { e ->
                log.warn("청크 규칙 변환 실패 [${chunk.articleNo}]: ${e.message}")
                failed++
            }
        }

        log.info("법령 → 규칙 변환 완료: $lawName — 생성 ${created}개, 수정 ${updated}개, 실패 ${failed}개")
        return LawRuleConversionResult(
            lawName = lawName,
            processed = chunks.size,
            created = created,
            updated = updated,
            failed = failed
        )
    }

    private fun extractRulesFromChunk(chunk: LawChunk): List<RuleCandidate> {
        val prompt = buildPrompt(chunk)
        val response = claudeApiClient.analyze(
            userMessage = prompt,
            systemPrompt = RULE_EXTRACTION_SYSTEM_PROMPT,
            maxTokens = 2048
        )
        return parseRulesFromLlmResponse(response.content)
    }

    private fun buildPrompt(chunk: LawChunk): String = buildString {
        append("법령: ${chunk.lawName}")
        chunk.articleNo?.let { append("\n조항: $it") }
        chunk.articleTitle?.let { append(" $it") }
        append("\n\n조문 내용:\n${chunk.content}")
    }

    private fun parseRulesFromLlmResponse(response: String): List<RuleCandidate> {
        return try {
            // LLM 응답에서 JSON 배열 구간 추출
            val jsonStart = response.indexOf('[')
            val jsonEnd = response.lastIndexOf(']')
            if (jsonStart == -1 || jsonEnd == -1 || jsonEnd <= jsonStart) return emptyList()

            val json = response.substring(jsonStart, jsonEnd + 1)

            @Suppress("UNCHECKED_CAST")
            val list = objectMapper.readValue<List<Map<String, Any?>>>(json)

            list.mapNotNull { map ->
                runCatching {
                    RuleCandidate(
                        code = map["code"] as? String ?: return@mapNotNull null,
                        name = map["name"] as? String ?: return@mapNotNull null,
                        description = map["description"] as? String,
                        type = map["type"] as? String ?: "LLM_JUDGE",
                        channel = map["channel"] as? String ?: "SMS,KAKAO,EMAIL",
                        severity = map["severity"] as? String ?: "MEDIUM",
                        pattern = map["pattern"] as? String,
                        legalBasis = map["legalBasis"] as? String
                    )
                }.getOrNull()
            }
        } catch (e: Exception) {
            log.warn("규칙 JSON 파싱 실패: ${e.message}")
            emptyList()
        }
    }

    private fun upsertRule(candidate: RuleCandidate): UpsertAction {
        val type = runCatching { RuleType.valueOf(candidate.type) }.getOrElse { RuleType.LLM_JUDGE }
        val severity = runCatching { Severity.valueOf(candidate.severity) }.getOrElse { Severity.MEDIUM }

        val existing = ruleRepository.findByCode(candidate.code)
        return if (existing != null) {
            // 기존 규칙 업데이트 (active 상태는 유지)
            existing.name = candidate.name
            candidate.description?.let { existing.description = it }
            existing.type = type
            existing.severity = severity
            candidate.pattern?.let { existing.pattern = it }
            candidate.legalBasis?.let { existing.legalBasis = it }
            ruleRepository.save(existing)
            log.debug("규칙 업데이트: ${candidate.code}")
            UpsertAction.UPDATED
        } else {
            // 신규 규칙 — active=false (담당자 검토 후 활성화)
            val rule = Rule(
                code = candidate.code,
                name = candidate.name,
                description = candidate.description,
                type = type,
                channel = candidate.channel,
                severity = severity,
                pattern = candidate.pattern,
                legalBasis = candidate.legalBasis,
                active = false
            )
            ruleRepository.save(rule)
            log.info("신규 규칙 생성 (검토 필요): ${candidate.code} — ${candidate.name}")
            UpsertAction.CREATED
        }
    }

    private enum class UpsertAction { CREATED, UPDATED, SKIPPED }

    companion object {
        val RULE_EXTRACTION_SYSTEM_PROMPT = """당신은 대한민국 마케팅·광고 법규 전문가입니다.
주어진 법령 조문에서 광고·마케팅 메시지 자동 준법 검사에 활용할 수 있는 구체적인 규칙을 추출하세요.

반드시 아래 형식의 JSON 배열만 출력하세요. 추출할 규칙이 없으면 빈 배열 []을 반환하세요.

[
  {
    "code": "규칙코드 (예: ADV_001, COSM_005)",
    "name": "규칙명 (한국어, 30자 이내)",
    "description": "검사 내용 설명 (한국어)",
    "type": "KEYWORD | REGEX | LLM_JUDGE",
    "channel": "SMS | KAKAO | EMAIL | SMS,KAKAO,EMAIL",
    "severity": "HIGH | MEDIUM | LOW",
    "pattern": "KEYWORD/REGEX 타입이면 패턴 (KEYWORD: 콤마 구분 금지어, REGEX: 정규식), LLM_JUDGE면 null",
    "legalBasis": "법령명 조항 (예: 표시·광고의 공정화에 관한 법률 제3조)"
  }
]

코드 접두사: 표시광고법→ADV_, 화장품법→COSM_, 식품표시광고법→FOOD_,
건강기능식품법→HLTH_, 전자상거래법→ECOM_, 의료기기법→MED_, 의료법→HOSP_,
방문판매법→VISIT_, 기타→GEN_

규칙 생성 기준:
1. 금지 표현이 명확한 경우 → KEYWORD 또는 REGEX 타입
2. 맥락 판단이 필요한 과장·오인 표현 → LLM_JUDGE 타입
3. 자동 생성 규칙은 담당자 검토 후 활성화 예정임을 고려하여 충분히 세분화하세요."""
    }
}

/** LLM이 추출한 규칙 후보 */
data class RuleCandidate(
    val code: String,
    val name: String,
    val description: String?,
    val type: String,
    val channel: String,
    val severity: String,
    val pattern: String?,
    val legalBasis: String?
)

/** 법령 → 규칙 변환 결과 */
data class LawRuleConversionResult(
    val lawName: String,
    val processed: Int,
    val created: Int = 0,
    val updated: Int = 0,
    val failed: Int = 0
)

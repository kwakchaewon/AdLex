package com.adlex.infra.crawler

import com.adlex.domain.entity.LawChunk
import com.adlex.domain.entity.Rule
import com.adlex.domain.repository.LawChunkRepository
import com.adlex.domain.repository.RuleRepository
import com.adlex.engine.model.RuleType
import com.adlex.engine.model.Severity
import com.adlex.infra.llm.ClaudeApiClient
import com.adlex.infra.llm.LlmAnalysisResponse
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class LawRuleConversionServiceTest {

    private val claudeApiClient = mockk<ClaudeApiClient>()
    private val ruleRepository = mockk<RuleRepository>()
    private val lawChunkRepository = mockk<LawChunkRepository>()
    private val objectMapper = ObjectMapper().registerKotlinModule()

    private lateinit var service: LawRuleConversionService

    @BeforeEach
    fun setUp() {
        service = LawRuleConversionService(
            claudeApiClient = claudeApiClient,
            ruleRepository = ruleRepository,
            lawChunkRepository = lawChunkRepository,
            objectMapper = objectMapper
        )
    }

    @Test
    fun `청크 없으면 빈 결과 반환`() {
        every { lawChunkRepository.findAllByLawNameAndActiveTrue("화장품법") } returns emptyList()

        val result = service.convertLawToRules("화장품법")

        assertEquals("화장품법", result.lawName)
        assertEquals(0, result.processed)
        assertEquals(0, result.created)
    }

    @Test
    fun `LLM 응답에서 규칙 파싱 후 신규 생성`() {
        val chunk = chunk("화장품법", "제13조", "부당한 표시·광고 행위 금지")
        every { lawChunkRepository.findAllByLawNameAndActiveTrue("화장품법") } returns listOf(chunk)

        val llmJson = """
            [
              {
                "code": "COSM_001",
                "name": "화장품 효능 과장 금지",
                "description": "의약품 효능을 암시하는 표현 금지",
                "type": "KEYWORD",
                "channel": "SMS,KAKAO,EMAIL",
                "severity": "HIGH",
                "pattern": "질병 치료,피부병 완치",
                "legalBasis": "화장품법 제13조"
              }
            ]
        """.trimIndent()

        every {
            claudeApiClient.analyze(any(), any(), any(), any())
        } returns LlmAnalysisResponse(content = llmJson, inputTokens = 100, outputTokens = 50)

        every { ruleRepository.findByCode("COSM_001") } returns null

        val savedSlot = slot<Rule>()
        every { ruleRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val result = service.convertLawToRules("화장품법")

        assertEquals(1, result.processed)
        assertEquals(1, result.created)
        assertEquals(0, result.updated)

        val saved = savedSlot.captured
        assertEquals("COSM_001", saved.code)
        assertEquals("화장품 효능 과장 금지", saved.name)
        assertEquals(RuleType.KEYWORD, saved.type)
        assertEquals(Severity.HIGH, saved.severity)
        assertFalse(saved.active) // 자동 생성 → active=false
    }

    @Test
    fun `기존 규칙이면 업데이트`() {
        val chunk = chunk("화장품법", "제13조", "부당한 표시·광고 행위 금지")
        every { lawChunkRepository.findAllByLawNameAndActiveTrue("화장품법") } returns listOf(chunk)

        val llmJson = """
            [{"code":"COSM_001","name":"업데이트된 규칙명","description":"설명","type":"LLM_JUDGE","channel":"SMS","severity":"MEDIUM","pattern":null,"legalBasis":"화장품법 제13조"}]
        """.trimIndent()

        every {
            claudeApiClient.analyze(any(), any(), any(), any())
        } returns LlmAnalysisResponse(content = llmJson, inputTokens = 100, outputTokens = 50)

        val existingRule = Rule(
            code = "COSM_001", name = "기존 규칙명", type = RuleType.KEYWORD,
            channel = "SMS", severity = Severity.HIGH, active = true
        )
        every { ruleRepository.findByCode("COSM_001") } returns existingRule
        every { ruleRepository.save(any()) } answers { firstArg() }

        val result = service.convertLawToRules("화장품법")

        assertEquals(0, result.created)
        assertEquals(1, result.updated)
        assertEquals("업데이트된 규칙명", existingRule.name)
        assertEquals(RuleType.LLM_JUDGE, existingRule.type)
        assertTrue(existingRule.active) // 기존 active 상태 유지
    }

    @Test
    fun `LLM 응답에 JSON 없으면 규칙 미생성`() {
        val chunk = chunk("화장품법", "제1조", "목적")
        every { lawChunkRepository.findAllByLawNameAndActiveTrue("화장품법") } returns listOf(chunk)

        every {
            claudeApiClient.analyze(any(), any(), any(), any())
        } returns LlmAnalysisResponse(
            content = "이 조문에서는 추출할 규칙이 없습니다.",
            inputTokens = 50, outputTokens = 20
        )

        val result = service.convertLawToRules("화장품법")

        assertEquals(1, result.processed)
        assertEquals(0, result.created)
        verify(exactly = 0) { ruleRepository.save(any()) }
    }

    @Test
    fun `LLM 응답이 빈 배열이면 규칙 미생성`() {
        val chunk = chunk("화장품법", "제2조", "정의")
        every { lawChunkRepository.findAllByLawNameAndActiveTrue("화장품법") } returns listOf(chunk)

        every {
            claudeApiClient.analyze(any(), any(), any(), any())
        } returns LlmAnalysisResponse(content = "[]", inputTokens = 50, outputTokens = 5)

        val result = service.convertLawToRules("화장품법")

        assertEquals(0, result.created)
        verify(exactly = 0) { ruleRepository.save(any()) }
    }

    @Test
    fun `개별 청크 LLM 오류 시 해당 청크만 건너뜀`() {
        val chunk1 = chunk("화장품법", "제13조", "금지 조항")
        val chunk2 = chunk("화장품법", "제14조", "또 다른 조항")
        every { lawChunkRepository.findAllByLawNameAndActiveTrue("화장품법") } returns listOf(chunk1, chunk2)

        // 첫 번째 청크는 오류, 두 번째는 성공
        every {
            claudeApiClient.analyze(match { it.contains("제13조") }, any(), any(), any())
        } throws RuntimeException("LLM 오류")

        every {
            claudeApiClient.analyze(match { it.contains("제14조") }, any(), any(), any())
        } returns LlmAnalysisResponse(
            content = """[{"code":"COSM_014","name":"규칙","description":"설명","type":"LLM_JUDGE","channel":"SMS","severity":"LOW","pattern":null,"legalBasis":"화장품법 제14조"}]""",
            inputTokens = 50, outputTokens = 30
        )

        every { ruleRepository.findByCode("COSM_014") } returns null
        every { ruleRepository.save(any()) } answers { firstArg() }

        val result = service.convertLawToRules("화장품법")

        assertEquals(2, result.processed)
        assertEquals(1, result.created)
        assertEquals(1, result.failed)
    }

    // -------------------------------------------------------------------------

    private fun chunk(lawName: String, articleNo: String, content: String) = LawChunk(
        lawName = lawName,
        articleNo = articleNo,
        content = content,
        lawDate = LocalDate.of(2024, 1, 1)
    )
}

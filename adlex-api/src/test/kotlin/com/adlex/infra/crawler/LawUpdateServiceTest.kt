package com.adlex.infra.crawler

import com.adlex.domain.entity.LawChunk
import com.adlex.domain.repository.LawChunkRepository
import com.adlex.infra.embedding.EmbeddingPipelineResult
import com.adlex.infra.embedding.EmbeddingPipelineService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.LocalDate

class LawUpdateServiceTest {

    private val lawChunkRepository = mockk<LawChunkRepository>()
    private val embeddingPipelineService = mockk<EmbeddingPipelineService>()
    private lateinit var service: LawUpdateService

    @BeforeEach
    fun setUp() {
        service = LawUpdateService(
            lawChunkRepository = lawChunkRepository,
            embeddingPipelineService = embeddingPipelineService,
            apiKey = "test-api-key"
        )
    }

    @Test
    fun `MST 없으면 업데이트 건너뜀`() {
        val changeResult = LawChangeResult(
            lawName = "표시·광고의 공정화에 관한 법률",
            changed = true,
            mst = null
        )

        val result = service.updateLaw(changeResult)

        assertFalse(result.success)
        assertTrue(result.reason.contains("MST"))
    }

    @Test
    fun `API 키 없으면 업데이트 건너뜀`() {
        val noKeyService = LawUpdateService(
            lawChunkRepository = lawChunkRepository,
            embeddingPipelineService = embeddingPipelineService,
            apiKey = ""
        )
        val changeResult = LawChangeResult(
            lawName = "화장품법",
            changed = true,
            mst = "123456"
        )

        val result = noKeyService.updateLaw(changeResult)

        assertFalse(result.success)
        assertTrue(result.reason.contains("API 키"))
    }

    @Test
    fun `API 응답 정상 → 기존 청크 비활성화 후 새 청크 저장 및 임베딩 생성`() {
        val mockWebClient = buildMockWebClient(listOf(
            mapOf("조문번호" to "1", "조문제목" to "목적", "조문내용" to "이 법은..."),
            mapOf("조문번호" to "2", "조문제목" to "정의", "조문내용" to "이 법에서 사용하는 용어...")
        ))
        ReflectionTestUtils.setField(service, "webClient", mockWebClient)

        val existingChunk = mockk<LawChunk>(relaxed = true)
        every { lawChunkRepository.findAllByLawNameAndActiveTrue("표시·광고의 공정화에 관한 법률") } returns listOf(existingChunk)
        every { lawChunkRepository.saveAll(any<List<LawChunk>>()) } answers { firstArg() }
        every { embeddingPipelineService.generateEmbeddingsForLaw("표시·광고의 공정화에 관한 법률") } returns
                EmbeddingPipelineResult(processed = 2, skipped = 0)

        val changeResult = LawChangeResult(
            lawName = "표시·광고의 공정화에 관한 법률",
            changed = true,
            latestDate = LocalDate.of(2025, 1, 1),
            mst = "123456"
        )

        val result = service.updateLaw(changeResult)

        assertTrue(result.success)
        assertEquals("표시·광고의 공정화에 관한 법률", result.lawName)
        assertEquals(2, result.chunkCount)
        assertEquals(2, result.embeddedCount)

        // 기존 청크 비활성화 확인
        verify { existingChunk.active = false }
        // 임베딩 생성 호출 확인
        verify { embeddingPipelineService.generateEmbeddingsForLaw("표시·광고의 공정화에 관한 법률") }
    }

    @Test
    fun `API 응답에 조문 없으면 실패 반환`() {
        val mockWebClient = buildMockWebClient(emptyList())
        ReflectionTestUtils.setField(service, "webClient", mockWebClient)

        val changeResult = LawChangeResult(
            lawName = "화장품법",
            changed = true,
            latestDate = LocalDate.of(2025, 1, 1),
            mst = "999999"
        )

        val result = service.updateLaw(changeResult)

        assertFalse(result.success)
        assertTrue(result.reason.contains("조문 없음"))
    }

    @Test
    fun `API 오류 → 실패 반환 (예외 전파 안 함)`() {
        val failingWebClient = mockk<WebClient>()
        val uriSpec = mockk<WebClient.RequestHeadersUriSpec<*>>()
        val headersSpec = mockk<WebClient.RequestHeadersSpec<*>>()
        val responseSpec = mockk<WebClient.ResponseSpec>()

        every { failingWebClient.get() } returns uriSpec
        every { uriSpec.uri(any<java.util.function.Function<*, *>>()) } returns headersSpec
        every { headersSpec.retrieve() } returns responseSpec
        every { responseSpec.bodyToMono(Map::class.java) } returns Mono.error(RuntimeException("네트워크 오류"))

        ReflectionTestUtils.setField(service, "webClient", failingWebClient)

        val changeResult = LawChangeResult(
            lawName = "화장품법",
            changed = true,
            mst = "999999"
        )

        val result = service.updateLaw(changeResult)

        assertFalse(result.success)
        assertEquals("API 조회 실패", result.reason)
    }

    @Test
    fun `updateChangedLaws — changed=false 법령은 제외`() {
        val changes = listOf(
            LawChangeResult(lawName = "화장품법", changed = false),
            LawChangeResult(lawName = "표시광고법", changed = false)
        )

        val results = service.updateChangedLaws(changes)

        assertTrue(results.isEmpty())
    }

    // -------------------------------------------------------------------------

    private fun buildMockWebClient(articles: List<Map<String, Any>>): WebClient {
        val mockWebClient = mockk<WebClient>()
        val uriSpec = mockk<WebClient.RequestHeadersUriSpec<*>>()
        val headersSpec = mockk<WebClient.RequestHeadersSpec<*>>()
        val responseSpec = mockk<WebClient.ResponseSpec>()

        every { mockWebClient.get() } returns uriSpec
        every { uriSpec.uri(any<java.util.function.Function<*, *>>()) } returns headersSpec
        every { headersSpec.retrieve() } returns responseSpec
        every { responseSpec.bodyToMono(Map::class.java) } returns Mono.just(
            mapOf(
                "법령" to mapOf(
                    "조문" to mapOf(
                        "조문단위" to articles
                    )
                )
            )
        )
        return mockWebClient
    }
}

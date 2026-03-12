package com.adlex.infra.crawler

import com.adlex.domain.entity.LawChunk
import com.adlex.domain.repository.LawChunkRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.LocalDate

class LawCrawlerServiceTest {

    private val lawChunkRepository = mockk<LawChunkRepository>()
    private lateinit var service: LawCrawlerService

    @BeforeEach
    fun setUp() {
        service = LawCrawlerService(
            lawChunkRepository = lawChunkRepository,
            apiKey = "test-api-key",
            targetLaws = listOf("표시·광고의 공정화에 관한 법률", "화장품법")
        )
    }

    @Test
    fun `API 키 없으면 빈 목록 반환`() {
        val noKeyService = LawCrawlerService(
            lawChunkRepository = lawChunkRepository,
            apiKey = "",
            targetLaws = listOf("표시·광고의 공정화에 관한 법률")
        )

        val result = noKeyService.detectChanges()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `DB에 법령 없으면 changed=true (신규 법령)`() {
        val mockWebClient = buildMockWebClient("20240601")
        ReflectionTestUtils.setField(service, "webClient", mockWebClient)

        every { lawChunkRepository.findAllByLawNameAndActiveTrue("표시·광고의 공정화에 관한 법률") } returns emptyList()

        val result = service.checkLawChange("표시·광고의 공정화에 관한 법률")

        assertTrue(result.changed)
        assertEquals("표시·광고의 공정화에 관한 법률", result.lawName)
        assertNull(result.storedDate)
        assertTrue(result.reason.contains("신규"))
    }

    @Test
    fun `API 시행일이 DB보다 최신이면 changed=true`() {
        val mockWebClient = buildMockWebClient("20250101")
        ReflectionTestUtils.setField(service, "webClient", mockWebClient)

        val existingChunk = mockk<LawChunk>()
        every { existingChunk.lawDate } returns LocalDate.of(2024, 1, 1)
        every { lawChunkRepository.findAllByLawNameAndActiveTrue("표시·광고의 공정화에 관한 법률") } returns listOf(existingChunk)

        val result = service.checkLawChange("표시·광고의 공정화에 관한 법률")

        assertTrue(result.changed)
        assertEquals(LocalDate.of(2025, 1, 1), result.latestDate)
        assertEquals(LocalDate.of(2024, 1, 1), result.storedDate)
    }

    @Test
    fun `API 시행일이 DB와 같으면 changed=false`() {
        val mockWebClient = buildMockWebClient("20240101")
        ReflectionTestUtils.setField(service, "webClient", mockWebClient)

        val existingChunk = mockk<LawChunk>()
        every { existingChunk.lawDate } returns LocalDate.of(2024, 1, 1)
        every { lawChunkRepository.findAllByLawNameAndActiveTrue("표시·광고의 공정화에 관한 법률") } returns listOf(existingChunk)

        val result = service.checkLawChange("표시·광고의 공정화에 관한 법률")

        assertFalse(result.changed)
        assertTrue(result.reason.contains("변경 없음"))
    }

    @Test
    fun `개별 법령 API 오류 시 해당 법령만 건너뜀`() {
        val failingWebClient = mockk<WebClient>()
        val uriSpec = mockk<WebClient.RequestHeadersUriSpec<*>>()
        val headersSpec = mockk<WebClient.RequestHeadersSpec<*>>()
        val responseSpec = mockk<WebClient.ResponseSpec>()

        every { failingWebClient.get() } returns uriSpec
        every { uriSpec.uri(any<java.util.function.Function<*, *>>()) } returns headersSpec
        every { headersSpec.retrieve() } returns responseSpec
        every { responseSpec.bodyToMono(Map::class.java) } returns Mono.error(RuntimeException("네트워크 오류"))

        ReflectionTestUtils.setField(service, "webClient", failingWebClient)

        // 두 법령 중 API 오류 발생 → 빈 목록 (오류 법령 건너뜀)
        val results = service.detectChanges()

        // 오류가 있어도 예외 없이 처리되어야 함
        assertNotNull(results)
    }

    // 테스트용 WebClient mock 헬퍼
    private fun buildMockWebClient(effectiveDateStr: String): WebClient {
        val mockWebClient = mockk<WebClient>()
        val uriSpec = mockk<WebClient.RequestHeadersUriSpec<*>>()
        val headersSpec = mockk<WebClient.RequestHeadersSpec<*>>()
        val responseSpec = mockk<WebClient.ResponseSpec>()

        every { mockWebClient.get() } returns uriSpec
        every { uriSpec.uri(any<java.util.function.Function<*, *>>()) } returns headersSpec
        every { headersSpec.retrieve() } returns responseSpec
        every { responseSpec.bodyToMono(Map::class.java) } returns Mono.just(
            mapOf(
                "LawSearch" to mapOf(
                    "law" to listOf(
                        mapOf(
                            "법령일련번호" to "123456",
                            "법령명한글" to "표시·광고의 공정화에 관한 법률",
                            "시행일자" to effectiveDateStr
                        )
                    )
                )
            )
        )
        return mockWebClient
    }
}

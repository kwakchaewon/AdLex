package com.adlex.infra.llm

import com.adlex.infra.rag.LawChunkResult
import com.adlex.infra.rag.PrecedentResult
import com.adlex.infra.rag.RagContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

class ClaudeApiClientTest {

    private lateinit var client: ClaudeApiClient

    @BeforeEach
    fun setUp() {
        client = ClaudeApiClient(
            apiKey = "test-api-key",
            model = "claude-opus-4-6",
            defaultMaxTokens = 1024
        )
    }

    @Test
    fun `API 키 없으면 기본 응답 반환`() {
        val noKeyClient = ClaudeApiClient(
            apiKey = "",
            model = "claude-opus-4-6",
            defaultMaxTokens = 1024
        )

        val result = noKeyClient.analyze("테스트 광고 메시지")

        assertTrue(result.content.contains("API 키"))
        assertEquals(0, result.inputTokens)
        assertEquals(0, result.outputTokens)
    }

    @Test
    fun `RAG 컨텍스트 없으면 기본 시스템 프롬프트만 사용`() {
        val mockWebClient = mockk<WebClient>()
        val requestBodyUriSpec = mockk<WebClient.RequestBodyUriSpec>()
        val requestBodySpec = mockk<WebClient.RequestBodySpec>()
        val responseSpec = mockk<WebClient.ResponseSpec>()

        val capturedBody = slot<Map<String, Any>>()

        every { mockWebClient.post() } returns requestBodyUriSpec
        every { requestBodyUriSpec.uri("/v1/messages") } returns requestBodySpec
        every { requestBodySpec.bodyValue(capture(capturedBody)) } returns requestBodySpec
        every { requestBodySpec.retrieve() } returns responseSpec
        every { responseSpec.bodyToMono(Map::class.java) } returns Mono.just(
            mapOf(
                "content" to listOf(mapOf("type" to "text", "text" to "법규 위반 없음")),
                "usage" to mapOf("input_tokens" to 100, "output_tokens" to 50)
            )
        )

        ReflectionTestUtils.setField(client, "webClient", mockWebClient)

        val result = client.analyze("지금 당장 구매하세요! 최고의 가격!")

        assertEquals("법규 위반 없음", result.content)
        assertEquals(100, result.inputTokens)
        assertEquals(50, result.outputTokens)

        val systemPrompt = capturedBody.captured["system"] as String
        assertTrue(systemPrompt.contains("마케팅·광고 법규 전문가"))
    }

    @Test
    fun `RAG 컨텍스트 있으면 시스템 프롬프트에 법령 정보 주입`() {
        val mockWebClient = mockk<WebClient>()
        val requestBodyUriSpec = mockk<WebClient.RequestBodyUriSpec>()
        val requestBodySpec = mockk<WebClient.RequestBodySpec>()
        val responseSpec = mockk<WebClient.ResponseSpec>()

        val capturedBody = slot<Map<String, Any>>()

        every { mockWebClient.post() } returns requestBodyUriSpec
        every { requestBodyUriSpec.uri("/v1/messages") } returns requestBodySpec
        every { requestBodySpec.bodyValue(capture(capturedBody)) } returns requestBodySpec
        every { requestBodySpec.retrieve() } returns responseSpec
        every { responseSpec.bodyToMono(Map::class.java) } returns Mono.just(
            mapOf(
                "content" to listOf(mapOf("type" to "text", "text" to "관련 법령 기준 위반 가능성 있음")),
                "usage" to mapOf("input_tokens" to 200, "output_tokens" to 80)
            )
        )

        ReflectionTestUtils.setField(client, "webClient", mockWebClient)

        val ragContext = RagContext(
            lawChunks = listOf(
                LawChunkResult(1L, "표시·광고의 공정화에 관한 법률", "제3조", "허위·과장 광고 금지", "허위 또는 과장된 표현은 금지됩니다.", null)
            ),
            precedents = emptyList()
        )

        val result = client.analyze("최고 품질 보장!", ragContext = ragContext)

        assertEquals("관련 법령 기준 위반 가능성 있음", result.content)

        val systemPrompt = capturedBody.captured["system"] as String
        assertTrue(systemPrompt.contains("[관련 법령]"))
        assertTrue(systemPrompt.contains("표시·광고의 공정화에 관한 법률"))
    }

    @Test
    fun `WebClientResponseException 발생 시 LlmException으로 래핑`() {
        val mockWebClient = mockk<WebClient>()
        val requestBodyUriSpec = mockk<WebClient.RequestBodyUriSpec>()
        val requestBodySpec = mockk<WebClient.RequestBodySpec>()
        val responseSpec = mockk<WebClient.ResponseSpec>()

        every { mockWebClient.post() } returns requestBodyUriSpec
        every { requestBodyUriSpec.uri("/v1/messages") } returns requestBodySpec
        every { requestBodySpec.bodyValue(any()) } returns requestBodySpec
        every { requestBodySpec.retrieve() } returns responseSpec
        every { responseSpec.bodyToMono(Map::class.java) } returns Mono.error(
            WebClientResponseException.create(401, "Unauthorized", null, null, null)
        )

        ReflectionTestUtils.setField(client, "webClient", mockWebClient)

        val exception = assertThrows<LlmException> {
            client.analyze("테스트 메시지")
        }
        assertTrue(exception.message?.contains("401") == true)
    }

    @Test
    fun `커스텀 시스템 프롬프트 사용`() {
        val mockWebClient = mockk<WebClient>()
        val requestBodyUriSpec = mockk<WebClient.RequestBodyUriSpec>()
        val requestBodySpec = mockk<WebClient.RequestBodySpec>()
        val responseSpec = mockk<WebClient.ResponseSpec>()

        val capturedBody = slot<Map<String, Any>>()

        every { mockWebClient.post() } returns requestBodyUriSpec
        every { requestBodyUriSpec.uri("/v1/messages") } returns requestBodySpec
        every { requestBodySpec.bodyValue(capture(capturedBody)) } returns requestBodySpec
        every { requestBodySpec.retrieve() } returns responseSpec
        every { responseSpec.bodyToMono(Map::class.java) } returns Mono.just(
            mapOf(
                "content" to listOf(mapOf("type" to "text", "text" to "결과")),
                "usage" to mapOf("input_tokens" to 10, "output_tokens" to 5)
            )
        )

        ReflectionTestUtils.setField(client, "webClient", mockWebClient)

        val customSystem = "커스텀 시스템 프롬프트"
        client.analyze("메시지", systemPrompt = customSystem)

        assertEquals(customSystem, capturedBody.captured["system"])
    }
}

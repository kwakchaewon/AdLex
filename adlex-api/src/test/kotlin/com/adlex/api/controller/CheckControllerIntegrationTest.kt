package com.adlex.api.controller

import com.adlex.domain.entity.ApiKey
import com.adlex.domain.entity.ApiKeyStatus
import com.adlex.domain.repository.ApiKeyRepository
import com.adlex.domain.service.ApiKeyService
import com.adlex.domain.service.CheckResultDto
import com.adlex.domain.service.ComplianceCheckService
import com.adlex.engine.model.EvaluationResult
import com.adlex.engine.model.Severity
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CheckControllerIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var apiKeyRepository: ApiKeyRepository
    @Autowired lateinit var apiKeyService: ApiKeyService

    @MockBean lateinit var redisTemplate: StringRedisTemplate
    @MockBean lateinit var complianceCheckService: ComplianceCheckService

    private val TEST_RAW_KEY = "al_live_ABCDEFGHIJKLMNOPQRSTUVWXYZabcd"
    private val TEST_TENANT_ID = 1L

    @Suppress("UNCHECKED_CAST")
    @BeforeEach
    fun setUp() {
        apiKeyRepository.deleteAll()
        apiKeyRepository.save(
            ApiKey(
                tenantId = TEST_TENANT_ID,
                name = "test-key",
                keyHash = apiKeyService.sha256Hex(TEST_RAW_KEY),
                keyPrefix = TEST_RAW_KEY.take(12),
                status = ApiKeyStatus.ACTIVE
            )
        )

        // Redis mock: cache miss → DB 조회, rate limit 1/10
        val ops = mock(ValueOperations::class.java) as ValueOperations<String, String>
        given(redisTemplate.opsForValue()).willReturn(ops)
        given(ops.get(anyString())).willReturn(null)
        given(ops.increment(anyString())).willReturn(1L)
        given(redisTemplate.expire(anyString(), any())).willReturn(true)
    }

    private fun compliantResult() = CheckResultDto(
        compliant = true, violations = emptyList(),
        checkedAt = Instant.now(), processingMs = 5
    )

    private fun violationResult(vararg codes: Pair<String, Severity>) = CheckResultDto(
        compliant = false,
        violations = codes.map { (code, sev) ->
            EvaluationResult(ruleCode = code, severity = sev, message = "$code 위반")
        },
        checkedAt = Instant.now(),
        processingMs = 5
    )

    @Test
    fun `시나리오1 - 정상 SMS, 200 compliant=true`() {
        whenever(complianceCheckService.check(anyLong(), org.mockito.kotlin.any())).thenReturn(compliantResult())

        mockMvc.post("/v1/check") {
            header("X-API-Key", TEST_RAW_KEY)
            contentType = MediaType.APPLICATION_JSON
            content = """{"message":"(광고) 봄 세일! 080-1234-5678 수신거부","channel":"SMS"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.compliant") { value(true) }
            jsonPath("$.violationCount") { value(0) }
        }
    }

    @Test
    fun `시나리오2 - 광고 미표시 SMS, compliant=false violations contains AD_LABEL`() {
        whenever(complianceCheckService.check(anyLong(), org.mockito.kotlin.any())).thenReturn(violationResult("AD_LABEL" to Severity.HIGH))

        mockMvc.post("/v1/check") {
            header("X-API-Key", TEST_RAW_KEY)
            contentType = MediaType.APPLICATION_JSON
            content = """{"message":"봄 세일 50% 할인","channel":"SMS"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.compliant") { value(false) }
            jsonPath("$.violations[0].ruleCode") { value("AD_LABEL") }
        }
    }

    @Test
    fun `시나리오3 - 080 미표시 SMS, violations contains OPT_OUT_080`() {
        whenever(complianceCheckService.check(anyLong(), org.mockito.kotlin.any())).thenReturn(violationResult("OPT_OUT_080" to Severity.HIGH))

        mockMvc.post("/v1/check") {
            header("X-API-Key", TEST_RAW_KEY)
            contentType = MediaType.APPLICATION_JSON
            content = """{"message":"(광고) 봄 세일","channel":"SMS"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.compliant") { value(false) }
            jsonPath("$.violations[0].ruleCode") { value("OPT_OUT_080") }
        }
    }

    @Test
    fun `시나리오4 - 야간 발송 SMS, violations contains NIGHT_SMS`() {
        whenever(complianceCheckService.check(anyLong(), org.mockito.kotlin.any())).thenReturn(violationResult("NIGHT_SMS" to Severity.HIGH))

        mockMvc.post("/v1/check") {
            header("X-API-Key", TEST_RAW_KEY)
            contentType = MediaType.APPLICATION_JSON
            content = """{"message":"(광고) 봄 세일 080-1234-5678","channel":"SMS","scheduledAt":"2026-03-12T13:00:00Z"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.compliant") { value(false) }
            jsonPath("$.violations[0].ruleCode") { value("NIGHT_SMS") }
        }
    }

    @Test
    fun `시나리오5 - 복합 위반 3건, violationCount=3`() {
        whenever(complianceCheckService.check(anyLong(), org.mockito.kotlin.any())).thenReturn(
            violationResult("AD_LABEL" to Severity.HIGH, "OPT_OUT_080" to Severity.HIGH, "NIGHT_SMS" to Severity.HIGH)
        )

        mockMvc.post("/v1/check") {
            header("X-API-Key", TEST_RAW_KEY)
            contentType = MediaType.APPLICATION_JSON
            content = """{"message":"봄 세일","channel":"SMS","scheduledAt":"2026-03-12T13:00:00Z"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.compliant") { value(false) }
            jsonPath("$.violationCount") { value(3) }
        }
    }

    @Test
    fun `시나리오6 - 정상 이메일, compliant=true`() {
        whenever(complianceCheckService.check(anyLong(), org.mockito.kotlin.any())).thenReturn(compliantResult())

        mockMvc.post("/v1/check") {
            header("X-API-Key", TEST_RAW_KEY)
            contentType = MediaType.APPLICATION_JSON
            content = """{"message":"이메일 본문","channel":"EMAIL","options":{"subject":"(광고) 봄 세일","unsubscribeUrl":"https://example.com/unsub"}}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.compliant") { value(true) }
        }
    }

    @Test
    fun `시나리오7 - X-API-Key 없이 호출, 401 반환`() {
        mockMvc.post("/v1/check") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"message":"테스트","channel":"SMS"}"""
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `시나리오8 - Batch 3건, results size=3 summary 검증`() {
        whenever(complianceCheckService.check(anyLong(), org.mockito.kotlin.any()))
            .thenReturn(compliantResult())
            .thenReturn(violationResult("AD_LABEL" to Severity.HIGH))
            .thenReturn(compliantResult())

        mockMvc.post("/v1/check/batch") {
            header("X-API-Key", TEST_RAW_KEY)
            contentType = MediaType.APPLICATION_JSON
            content = """{"messages":[
                {"message":"(광고) 정상","channel":"SMS"},
                {"message":"미표시","channel":"SMS"},
                {"message":"(광고) 정상2","channel":"SMS"}
            ]}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.results.length()") { value(3) }
            jsonPath("$.summary.total") { value(3) }
            jsonPath("$.summary.compliant") { value(2) }
            jsonPath("$.summary.violated") { value(1) }
        }
    }
}

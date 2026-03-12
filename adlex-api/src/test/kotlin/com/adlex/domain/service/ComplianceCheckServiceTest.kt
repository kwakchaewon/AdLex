package com.adlex.domain.service

import com.adlex.domain.entity.CheckLog
import com.adlex.domain.entity.Rule
import com.adlex.domain.repository.CheckLogRepository
import com.adlex.engine.RuleRegistry
import com.adlex.engine.evaluator.RuleEvaluator
import com.adlex.engine.model.Channel
import com.adlex.engine.model.EvaluationContext
import com.adlex.engine.model.EvaluationResult
import com.adlex.engine.model.RuleType
import com.adlex.engine.model.Severity
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ComplianceCheckServiceTest {

    private val ruleRegistry: RuleRegistry = mockk()
    private val checkLogRepository: CheckLogRepository = mockk()
    private val mockEvaluator: RuleEvaluator = mockk()

    private lateinit var service: ComplianceCheckService

    private val sampleRule = Rule(
        code = "AD_LABEL", name = "(광고) 표기", type = RuleType.REGEX,
        channel = "SMS", severity = Severity.HIGH,
        pattern = """\(광고\)""", config = """{"matchMode":"REQUIRE"}"""
    )

    @BeforeEach
    fun setUp() {
        service = ComplianceCheckService(ruleRegistry, listOf(mockEvaluator), checkLogRepository)
        every { checkLogRepository.save(any<CheckLog>()) } returnsArgument 0
    }

    @Test
    fun `위반 없는 메시지 - compliant true 반환`() {
        every { ruleRegistry.getRulesByChannel(Channel.SMS) } returns listOf(sampleRule)
        every { mockEvaluator.supports(RuleType.REGEX) } returns true
        every { mockEvaluator.evaluate(any(), sampleRule) } returns null

        val ctx = EvaluationContext(message = "(광고) 정상 메시지", channel = Channel.SMS)
        val result = service.check(tenantId = 1L, context = ctx)

        assertTrue(result.compliant)
        assertTrue(result.violations.isEmpty())
        verify { checkLogRepository.save(any<CheckLog>()) }
    }

    @Test
    fun `위반 1건 - compliant false, violations size 1`() {
        val violation = EvaluationResult(
            ruleCode = "AD_LABEL", severity = Severity.HIGH,
            message = "(광고) 표기 누락", legalBasis = "정보통신망법 제50조"
        )
        every { ruleRegistry.getRulesByChannel(Channel.SMS) } returns listOf(sampleRule)
        every { mockEvaluator.supports(RuleType.REGEX) } returns true
        every { mockEvaluator.evaluate(any(), sampleRule) } returns violation

        val ctx = EvaluationContext(message = "광고 표기 없음", channel = Channel.SMS)
        val result = service.check(tenantId = 1L, context = ctx)

        assertFalse(result.compliant)
        assertEquals(1, result.violations.size)
        assertEquals("AD_LABEL", result.violations[0].ruleCode)
    }

    @Test
    fun `skipRules 적용 - 해당 규칙 평가 스킵`() {
        every { ruleRegistry.getRulesByChannel(Channel.SMS) } returns listOf(sampleRule)

        val ctx = EvaluationContext(
            message = "메시지", channel = Channel.SMS,
            options = mapOf("skipRules" to listOf("AD_LABEL"))
        )
        val result = service.check(tenantId = 1L, context = ctx)

        assertTrue(result.compliant)
        verify(exactly = 0) { mockEvaluator.evaluate(any(), any()) }
    }

    @Test
    fun `evaluator가 없으면 null (위반 없음으로 처리)`() {
        val unknownEvaluator: RuleEvaluator = mockk()
        every { unknownEvaluator.supports(any()) } returns false
        val svc = ComplianceCheckService(ruleRegistry, listOf(unknownEvaluator), checkLogRepository)

        every { ruleRegistry.getRulesByChannel(Channel.SMS) } returns listOf(sampleRule)

        val ctx = EvaluationContext(message = "메시지", channel = Channel.SMS)
        val result = svc.check(tenantId = 1L, context = ctx)

        assertTrue(result.compliant)
    }

    @Test
    fun `CheckLog 저장 시 올바른 데이터`() {
        every { ruleRegistry.getRulesByChannel(Channel.SMS) } returns emptyList()

        val ctx = EvaluationContext(message = "테스트 메시지", channel = Channel.SMS)
        service.check(tenantId = 42L, context = ctx)

        verify {
            checkLogRepository.save(match { log ->
                log.tenantId == 42L &&
                log.message == "테스트 메시지" &&
                log.channel == Channel.SMS &&
                log.compliant
            })
        }
    }
}

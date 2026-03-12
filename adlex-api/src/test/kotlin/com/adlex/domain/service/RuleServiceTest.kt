package com.adlex.domain.service

import com.adlex.api.advice.BusinessException
import com.adlex.api.dto.CreateRuleRequest
import com.adlex.api.dto.UpdateRuleRequest
import com.adlex.domain.entity.Rule
import com.adlex.domain.repository.RuleRepository
import com.adlex.engine.RuleRegistry
import com.adlex.engine.model.RuleType
import com.adlex.engine.model.Severity
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional

class RuleServiceTest {

    private val ruleRepository = mockk<RuleRepository>()
    private val ruleRegistry = mockk<RuleRegistry>()
    private lateinit var service: RuleService

    private fun makeRule(id: Long = 1L, code: String = "SPAM_001", active: Boolean = true) = Rule(
        code = code,
        name = "스팸 문구 금지",
        description = "광고성 스팸 표현",
        type = RuleType.KEYWORD,
        channel = "SMS,KAKAO",
        severity = Severity.HIGH,
        pattern = null,
        config = null,
        legalBasis = "정보통신망법 제50조",
        active = active
    ).also {
        val idField = it.javaClass.superclass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(it, id)
    }

    @BeforeEach
    fun setUp() {
        service = RuleService(ruleRepository, ruleRegistry)
    }

    @Test
    fun `getAll - 전체 규칙 목록 반환`() {
        val rules = listOf(makeRule(1L), makeRule(2L, "SPAM_002"))
        every { ruleRepository.findAll() } returns rules

        val result = service.getAll()

        assertThat(result).hasSize(2)
        assertThat(result[0].code).isEqualTo("SPAM_001")
    }

    @Test
    fun `getById - 존재하는 규칙 반환`() {
        val rule = makeRule(1L)
        every { ruleRepository.findById(1L) } returns Optional.of(rule)

        val result = service.getById(1L)

        assertThat(result.code).isEqualTo("SPAM_001")
        assertThat(result.severity).isEqualTo(Severity.HIGH)
    }

    @Test
    fun `getById - 없는 ID이면 BusinessException`() {
        every { ruleRepository.findById(99L) } returns Optional.empty()

        assertThatThrownBy { service.getById(99L) }
            .isInstanceOf(BusinessException::class.java)
            .hasMessageContaining("99")
    }

    @Test
    fun `create - 새 규칙 생성 및 캐시 무효화`() {
        val req = CreateRuleRequest(
            code = "NEW_001",
            name = "새 규칙",
            type = RuleType.REGEX,
            channel = "SMS",
            severity = Severity.MEDIUM
        )
        val saved = makeRule(10L, "NEW_001")
        every { ruleRepository.existsByCode("NEW_001") } returns false
        every { ruleRepository.save(any()) } returns saved
        justRun { ruleRegistry.invalidateCache() }

        val result = service.create(req)

        assertThat(result.code).isEqualTo("NEW_001")
        verify { ruleRegistry.invalidateCache() }
    }

    @Test
    fun `create - 중복 코드이면 ValidationError`() {
        val req = CreateRuleRequest(
            code = "SPAM_001",
            name = "중복",
            type = RuleType.KEYWORD,
            channel = "SMS",
            severity = Severity.LOW
        )
        every { ruleRepository.existsByCode("SPAM_001") } returns true

        assertThatThrownBy { service.create(req) }
            .isInstanceOf(BusinessException::class.java)
            .hasMessageContaining("SPAM_001")
    }

    @Test
    fun `update - 필드 수정 및 캐시 무효화`() {
        val rule = makeRule(1L)
        every { ruleRepository.findById(1L) } returns Optional.of(rule)
        every { ruleRepository.save(any()) } returns rule
        justRun { ruleRegistry.invalidateCache() }

        val result = service.update(1L, UpdateRuleRequest(name = "수정된 이름", active = false))

        assertThat(result.name).isEqualTo("수정된 이름")
        verify { ruleRegistry.invalidateCache() }
    }

    @Test
    fun `delete - 소프트 삭제(active=false) 및 캐시 무효화`() {
        val rule = makeRule(1L)
        every { ruleRepository.findById(1L) } returns Optional.of(rule)
        every { ruleRepository.save(any()) } returns rule
        justRun { ruleRegistry.invalidateCache() }

        service.delete(1L)

        assertThat(rule.active).isFalse()
        verify { ruleRegistry.invalidateCache() }
    }

    @Test
    fun `delete - 없는 ID이면 BusinessException`() {
        every { ruleRepository.findById(99L) } returns Optional.empty()

        assertThatThrownBy { service.delete(99L) }
            .isInstanceOf(BusinessException::class.java)
    }
}

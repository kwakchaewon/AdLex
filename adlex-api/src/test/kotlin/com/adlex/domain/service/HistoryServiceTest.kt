package com.adlex.domain.service

import com.adlex.api.advice.BusinessException
import com.adlex.api.advice.ErrorCode
import com.adlex.domain.entity.CheckLog
import com.adlex.domain.repository.CheckLogRepository
import com.adlex.engine.model.Channel
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class HistoryServiceTest {

    private val checkLogRepository: CheckLogRepository = mockk()
    private lateinit var historyService: HistoryService

    @BeforeEach
    fun setUp() {
        historyService = HistoryService(checkLogRepository)
    }

    private fun checkLog(id: Long = 1L, tenantId: Long = 10L, compliant: Boolean = true): CheckLog {
        val log = CheckLog(
            tenantId = tenantId,
            message = "테스트 광고 메시지입니다",
            channel = Channel.SMS,
            compliant = compliant,
            violationCount = 0,
            violations = "[]",
            processingMs = 42L
        )
        val idField = log.javaClass.superclass.getDeclaredField("id").also { it.isAccessible = true }
        idField.set(log, id)
        return log
    }

    @Test
    fun `list - 페이지 결과 반환`() {
        val logs = listOf(checkLog(1L), checkLog(2L))
        every {
            checkLogRepository.findWithFilters(10L, null, null, null, null, PageRequest.of(0, 20))
        } returns PageImpl(logs)

        val result = historyService.list(10L, null, null, null, null, 0, 20)

        assertThat(result.content).hasSize(2)
        assertThat(result.totalElements).isEqualTo(2L)
        assertThat(result.page).isEqualTo(0)
    }

    @Test
    fun `list - size 100 초과 시 100으로 제한`() {
        every {
            checkLogRepository.findWithFilters(10L, null, null, null, null, PageRequest.of(0, 100))
        } returns PageImpl(emptyList())

        historyService.list(10L, null, null, null, null, 0, 999)
        // size=100으로 호출됐는지 verify (mockk 호출 매칭으로 확인)
    }

    @Test
    fun `detail - 성공`() {
        val log = checkLog(1L, tenantId = 10L, compliant = false).also {
            val field = it.javaClass.getDeclaredField("violations").also { f -> f.isAccessible = true }
            // violations은 val이라 리플렉션 필요 없음, 생성 시 전달
        }
        val logWithViolation = CheckLog(
            tenantId = 10L,
            message = "광고 메시지",
            channel = Channel.SMS,
            compliant = false,
            violationCount = 1,
            violations = """[{"ruleCode":"AD_LABEL","severity":"HIGH","message":"광고 표시 누락","legalBasis":null,"suggestion":null}]""",
            processingMs = 30L
        )
        every { checkLogRepository.findByIdAndTenantId(1L, 10L) } returns logWithViolation

        val result = historyService.detail(10L, 1L)

        assertThat(result.compliant).isFalse()
        assertThat(result.violations).hasSize(1)
        assertThat(result.violations[0].ruleCode).isEqualTo("AD_LABEL")
    }

    @Test
    fun `detail - 없는 id 시 NOT_FOUND`() {
        every { checkLogRepository.findByIdAndTenantId(99L, 10L) } returns null

        assertThatThrownBy { historyService.detail(10L, 99L) }
            .isInstanceOf(BusinessException::class.java)
            .satisfies { ex -> assertThat((ex as BusinessException).errorCode).isEqualTo(ErrorCode.NOT_FOUND) }
    }

    @Test
    fun `detail - 다른 테넌트 접근 시 NOT_FOUND`() {
        every { checkLogRepository.findByIdAndTenantId(1L, 99L) } returns null

        assertThatThrownBy { historyService.detail(99L, 1L) }
            .isInstanceOf(BusinessException::class.java)
            .satisfies { ex -> assertThat((ex as BusinessException).errorCode).isEqualTo(ErrorCode.NOT_FOUND) }
    }
}

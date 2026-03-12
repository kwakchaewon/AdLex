package com.adlex.domain.service

import com.adlex.api.advice.BusinessException
import com.adlex.api.advice.ErrorCode
import com.adlex.domain.entity.ApiKey
import com.adlex.domain.entity.ApiKeyStatus
import com.adlex.domain.repository.ApiKeyRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional

class ApiKeyServiceTest {

    private val apiKeyRepository: ApiKeyRepository = mockk()
    private lateinit var apiKeyService: ApiKeyService

    @BeforeEach
    fun setUp() {
        apiKeyService = ApiKeyService(apiKeyRepository)
    }

    private fun apiKey(id: Long = 1L, tenantId: Long = 10L, status: ApiKeyStatus = ApiKeyStatus.ACTIVE): ApiKey {
        val key = ApiKey(tenantId = tenantId, name = "Test Key", keyHash = "hash", keyPrefix = "al_live_AbCd")
        val idField = key.javaClass.superclass.getDeclaredField("id").also { it.isAccessible = true }
        idField.set(key, id)
        key.status = status
        return key
    }

    @Test
    fun `generateKey - rawKey가 al_live_ 로 시작하고 저장됨`() {
        every { apiKeyRepository.save(any()) } answers { firstArg() }

        val (rawKey, saved) = apiKeyService.generateKey(10L, "My Key")

        assertThat(rawKey).startsWith("al_live_")
        assertThat(saved.tenantId).isEqualTo(10L)
        assertThat(saved.name).isEqualTo("My Key")
    }

    @Test
    fun `listKeys - 테넌트 소유 키 목록 반환`() {
        val keys = listOf(apiKey(1L, 10L), apiKey(2L, 10L))
        every { apiKeyRepository.findAllByTenantId(10L) } returns keys

        val result = apiKeyService.listKeys(10L)

        assertThat(result).hasSize(2)
    }

    @Test
    fun `revokeKey - 성공 시 REVOKED 상태로 변경`() {
        val key = apiKey(1L, 10L, ApiKeyStatus.ACTIVE)
        every { apiKeyRepository.findById(1L) } returns Optional.of(key)
        every { apiKeyRepository.save(any()) } answers { firstArg() }

        apiKeyService.revokeKey(10L, 1L)

        assertThat(key.status).isEqualTo(ApiKeyStatus.REVOKED)
        verify { apiKeyRepository.save(key) }
    }

    @Test
    fun `revokeKey - 존재하지 않는 키 시 NOT_FOUND`() {
        every { apiKeyRepository.findById(99L) } returns Optional.empty()

        assertThatThrownBy { apiKeyService.revokeKey(10L, 99L) }
            .isInstanceOf(BusinessException::class.java)
            .satisfies { ex -> assertThat((ex as BusinessException).errorCode).isEqualTo(ErrorCode.NOT_FOUND) }
    }

    @Test
    fun `revokeKey - 다른 테넌트 키 접근 시 FORBIDDEN`() {
        val key = apiKey(1L, tenantId = 99L)
        every { apiKeyRepository.findById(1L) } returns Optional.of(key)

        assertThatThrownBy { apiKeyService.revokeKey(10L, 1L) }
            .isInstanceOf(BusinessException::class.java)
            .satisfies { ex -> assertThat((ex as BusinessException).errorCode).isEqualTo(ErrorCode.FORBIDDEN) }
    }

    @Test
    fun `validateKey - ACTIVE 키 반환`() {
        val key = apiKey()
        every { apiKeyRepository.findByKeyHash(any()) } returns key

        val result = apiKeyService.validateKey("al_live_somerawkey")

        assertThat(result).isNotNull
    }

    @Test
    fun `validateKey - REVOKED 키 null 반환`() {
        val key = apiKey(status = ApiKeyStatus.REVOKED)
        every { apiKeyRepository.findByKeyHash(any()) } returns key

        val result = apiKeyService.validateKey("al_live_somerawkey")

        assertThat(result).isNull()
    }
}

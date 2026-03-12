package com.adlex.domain.service

import com.adlex.api.advice.BusinessException
import com.adlex.api.advice.ErrorCode
import com.adlex.api.dto.LoginRequest
import com.adlex.api.dto.RefreshTokenRequest
import com.adlex.api.dto.RegisterRequest
import com.adlex.api.dto.UpdateMeRequest
import com.adlex.domain.entity.Plan
import com.adlex.domain.entity.Tenant
import com.adlex.domain.repository.TenantRepository
import com.adlex.infra.security.JwtProvider
import io.jsonwebtoken.Claims
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.Optional

class UserServiceTest {

    private val tenantRepository: TenantRepository = mockk()
    private val jwtProvider: JwtProvider = mockk()
    private val passwordEncoder = BCryptPasswordEncoder()
    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userService = UserService(tenantRepository, jwtProvider, passwordEncoder)
    }

    private fun tenant(id: Long = 1L, email: String = "test@example.com") = Tenant(
        email = email,
        passwordHash = passwordEncoder.encode("password123"),
        companyName = "Test Corp"
    ).also {
        val field = it.javaClass.superclass.getDeclaredField("id")
        field.isAccessible = true
        field.set(it, id)
    }

    @Test
    fun `register - 성공`() {
        val req = RegisterRequest("new@example.com", "password123", "New Corp")
        every { tenantRepository.findByEmail(req.email) } returns null
        every { tenantRepository.save(any()) } answers { firstArg() }

        val result = userService.register(req)

        assertThat(result.email).isEqualTo(req.email)
        assertThat(result.companyName).isEqualTo(req.companyName)
    }

    @Test
    fun `register - 이메일 중복 시 DUPLICATE_EMAIL`() {
        val req = RegisterRequest("dup@example.com", "password123")
        every { tenantRepository.findByEmail(req.email) } returns tenant(email = req.email)

        assertThatThrownBy { userService.register(req) }
            .isInstanceOf(BusinessException::class.java)
            .satisfies({ ex ->
                assertThat((ex as BusinessException).errorCode).isEqualTo(ErrorCode.DUPLICATE_EMAIL)
            })
    }

    @Test
    fun `login - 성공`() {
        val t = tenant()
        every { tenantRepository.findByEmail(t.email) } returns t
        every { jwtProvider.generateAccessToken(any(), any(), any()) } returns "access-token"
        every { jwtProvider.generateRefreshToken(any()) } returns "refresh-token"

        val result = userService.login(LoginRequest(t.email, "password123"))

        assertThat(result.accessToken).isEqualTo("access-token")
        assertThat(result.tokenType).isEqualTo("Bearer")
    }

    @Test
    fun `login - 이메일 없음 시 UNAUTHORIZED`() {
        every { tenantRepository.findByEmail(any()) } returns null

        assertThatThrownBy { userService.login(LoginRequest("no@example.com", "pw")) }
            .isInstanceOf(BusinessException::class.java)
            .satisfies({ ex ->
                assertThat((ex as BusinessException).errorCode).isEqualTo(ErrorCode.UNAUTHORIZED)
            })
    }

    @Test
    fun `login - 비밀번호 불일치 시 UNAUTHORIZED`() {
        val t = tenant()
        every { tenantRepository.findByEmail(t.email) } returns t

        assertThatThrownBy { userService.login(LoginRequest(t.email, "wrongpassword")) }
            .isInstanceOf(BusinessException::class.java)
            .satisfies({ ex ->
                assertThat((ex as BusinessException).errorCode).isEqualTo(ErrorCode.UNAUTHORIZED)
            })
    }

    @Test
    fun `refresh - 유효하지 않은 토큰 시 UNAUTHORIZED`() {
        every { jwtProvider.validateToken(any()) } returns null

        assertThatThrownBy { userService.refresh(RefreshTokenRequest("bad-token")) }
            .isInstanceOf(BusinessException::class.java)
            .satisfies({ ex ->
                assertThat((ex as BusinessException).errorCode).isEqualTo(ErrorCode.UNAUTHORIZED)
            })
    }

    @Test
    fun `getMe - 성공`() {
        val t = tenant()
        every { tenantRepository.findById(t.id) } returns Optional.of(t)

        val result = userService.getMe(t.id)

        assertThat(result.email).isEqualTo(t.email)
        assertThat(result.plan).isEqualTo(Plan.FREE)
    }

    @Test
    fun `updateMe - 회사명 수정 성공`() {
        val t = tenant()
        every { tenantRepository.findById(t.id) } returns Optional.of(t)
        every { tenantRepository.save(any()) } answers { firstArg() }

        val result = userService.updateMe(t.id, UpdateMeRequest("Updated Corp"))

        assertThat(result.companyName).isEqualTo("Updated Corp")
    }

    @Test
    fun `withdraw - 성공`() {
        val t = tenant()
        every { tenantRepository.findById(t.id) } returns Optional.of(t)
        every { tenantRepository.delete(t) } returns Unit

        userService.withdraw(t.id)

        verify { tenantRepository.delete(t) }
    }
}

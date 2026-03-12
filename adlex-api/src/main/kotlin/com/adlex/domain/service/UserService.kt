package com.adlex.domain.service

import com.adlex.api.advice.BusinessException
import com.adlex.api.advice.ErrorCode
import com.adlex.api.dto.AuthResponse
import com.adlex.api.dto.LoginRequest
import com.adlex.api.dto.MeResponse
import com.adlex.api.dto.RefreshTokenRequest
import com.adlex.api.dto.RegisterRequest
import com.adlex.api.dto.UpdateMeRequest
import com.adlex.domain.entity.Tenant
import com.adlex.domain.repository.TenantRepository
import com.adlex.infra.security.JwtProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val tenantRepository: TenantRepository,
    private val jwtProvider: JwtProvider,
    private val passwordEncoder: PasswordEncoder
) {

    @Transactional
    fun register(req: RegisterRequest): MeResponse {
        if (tenantRepository.findByEmail(req.email) != null)
            throw BusinessException(ErrorCode.DUPLICATE_EMAIL, "이미 사용 중인 이메일입니다")
        val tenant = tenantRepository.save(
            Tenant(
                email = req.email,
                passwordHash = passwordEncoder.encode(req.password),
                companyName = req.companyName
            )
        )
        return MeResponse.from(tenant)
    }

    @Transactional(readOnly = true)
    fun login(req: LoginRequest): AuthResponse {
        val tenant = tenantRepository.findByEmail(req.email)
            ?: throw BusinessException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다")
        if (!passwordEncoder.matches(req.password, tenant.passwordHash))
            throw BusinessException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다")
        return AuthResponse(
            accessToken = jwtProvider.generateAccessToken(tenant.id, tenant.email, tenant.plan),
            refreshToken = jwtProvider.generateRefreshToken(tenant.id)
        )
    }

    fun refresh(req: RefreshTokenRequest): AuthResponse {
        val claims = jwtProvider.validateToken(req.refreshToken)
            ?: throw BusinessException(ErrorCode.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다")
        val tenant = tenantRepository.findById(claims.subject.toLong()).orElseThrow {
            BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다")
        }
        return AuthResponse(
            accessToken = jwtProvider.generateAccessToken(tenant.id, tenant.email, tenant.plan),
            refreshToken = jwtProvider.generateRefreshToken(tenant.id)
        )
    }

    @Transactional(readOnly = true)
    fun getMe(tenantId: Long): MeResponse =
        MeResponse.from(findTenant(tenantId))

    @Transactional
    fun updateMe(tenantId: Long, req: UpdateMeRequest): MeResponse {
        val tenant = findTenant(tenantId)
        tenant.companyName = req.companyName
        return MeResponse.from(tenantRepository.save(tenant))
    }

    @Transactional
    fun withdraw(tenantId: Long) =
        tenantRepository.delete(findTenant(tenantId))

    private fun findTenant(tenantId: Long): Tenant =
        tenantRepository.findById(tenantId).orElseThrow {
            BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다")
        }
}

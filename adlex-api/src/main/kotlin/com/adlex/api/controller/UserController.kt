package com.adlex.api.controller

import com.adlex.api.dto.AuthResponse
import com.adlex.api.dto.LoginRequest
import com.adlex.api.dto.MeResponse
import com.adlex.api.dto.RefreshTokenRequest
import com.adlex.api.dto.RegisterRequest
import com.adlex.api.dto.UpdateMeRequest
import com.adlex.domain.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "User", description = "회원 인증 및 관리 API")
@RestController
class UserController(private val userService: UserService) {

    @Operation(summary = "회원가입")
    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody req: RegisterRequest): MeResponse =
        userService.register(req)

    @Operation(summary = "로그인")
    @PostMapping("/api/auth/login")
    fun login(@Valid @RequestBody req: LoginRequest): AuthResponse =
        userService.login(req)

    @Operation(summary = "액세스 토큰 갱신")
    @PostMapping("/api/auth/refresh")
    fun refresh(@Valid @RequestBody req: RefreshTokenRequest): AuthResponse =
        userService.refresh(req)

    @Operation(summary = "내 정보 조회", security = [SecurityRequirement(name = "bearerAuth")])
    @GetMapping("/api/users/me")
    fun getMe(@AuthenticationPrincipal tenantId: Long): MeResponse =
        userService.getMe(tenantId)

    @Operation(summary = "내 정보 수정", security = [SecurityRequirement(name = "bearerAuth")])
    @PutMapping("/api/users/me")
    fun updateMe(
        @AuthenticationPrincipal tenantId: Long,
        @Valid @RequestBody req: UpdateMeRequest
    ): MeResponse = userService.updateMe(tenantId, req)

    @Operation(summary = "회원 탈퇴", security = [SecurityRequirement(name = "bearerAuth")])
    @DeleteMapping("/api/users/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun withdraw(@AuthenticationPrincipal tenantId: Long) =
        userService.withdraw(tenantId)
}

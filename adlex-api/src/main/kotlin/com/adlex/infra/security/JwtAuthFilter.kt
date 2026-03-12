package com.adlex.infra.security

import com.adlex.api.advice.ErrorCode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthFilter(private val jwtProvider: JwtProvider) : OncePerRequestFilter() {

    private val mapper: ObjectMapper = jacksonObjectMapper()

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        !request.requestURI.startsWith("/api/users/")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val header = request.getHeader("Authorization")
        if (header.isNullOrBlank() || !header.startsWith("Bearer ")) {
            writeError(response, "Authorization Bearer 토큰이 필요합니다")
            return
        }
        val claims = jwtProvider.validateToken(header.removePrefix("Bearer "))
        if (claims == null) {
            writeError(response, "유효하지 않은 토큰입니다")
            return
        }
        val auth = UsernamePasswordAuthenticationToken(
            claims.subject.toLong(), null, listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
        SecurityContextHolder.getContext().authentication = auth
        filterChain.doFilter(request, response)
    }

    private fun writeError(response: HttpServletResponse, message: String) {
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        response.writer.write(
            mapper.writeValueAsString(
                mapOf("error" to mapOf("code" to ErrorCode.UNAUTHORIZED.name, "message" to message))
            )
        )
    }
}

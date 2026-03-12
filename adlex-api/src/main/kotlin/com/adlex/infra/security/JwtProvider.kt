package com.adlex.infra.security

import com.adlex.domain.entity.Plan
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*

@Component
class JwtProvider(@Value("\${adlex.jwt.secret}") private val secret: String) {

    private val key by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    }

    fun generateAccessToken(tenantId: Long, email: String, plan: Plan): String =
        Jwts.builder()
            .subject(tenantId.toString())
            .claim("email", email)
            .claim("plan", plan.name)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 30 * 60 * 1000L)) // 30분
            .signWith(key)
            .compact()

    fun generateRefreshToken(tenantId: Long): String =
        Jwts.builder()
            .subject(tenantId.toString())
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L)) // 7일
            .signWith(key)
            .compact()

    fun validateToken(token: String): Claims? = runCatching {
        Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }.getOrNull()
}

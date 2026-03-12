package com.adlex.infra.security

import com.adlex.api.advice.ErrorCode
import com.adlex.domain.entity.ApiKeyStatus
import com.adlex.domain.entity.Plan
import com.adlex.domain.repository.ApiKeyRepository
import com.adlex.domain.service.ApiKeyService
import com.adlex.infra.cache.RateLimiter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration

class ApiKeyAuthFilter(
    private val apiKeyRepository: ApiKeyRepository,
    private val apiKeyService: ApiKeyService,
    private val rateLimiter: RateLimiter,
    private val redisTemplate: StringRedisTemplate
) : OncePerRequestFilter() {

    private val mapper: ObjectMapper = jacksonObjectMapper()
    private val CACHE_TTL = Duration.ofMinutes(5)

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        !request.requestURI.startsWith("/v1/")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val rawKey = request.getHeader("X-API-Key")
        if (rawKey.isNullOrBlank()) {
            writeError(response, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "X-API-Key 헤더가 필요합니다")
            return
        }

        // SHA-256 해시 계산
        val keyHash = apiKeyService.sha256Hex(rawKey)

        // Redis 캐시 조회
        val cached = redisTemplate.opsForValue().get("apikey:$keyHash")
        val (tenantId, plan) = if (cached != null) {
            val data = mapper.readValue<Map<String, String>>(cached)
            Pair(data["tenantId"]!!.toLong(), Plan.valueOf(data["plan"]!!))
        } else {
            // DB 조회
            val apiKey = apiKeyRepository.findByKeyHash(keyHash)
            if (apiKey == null || apiKey.status == ApiKeyStatus.REVOKED) {
                writeError(response, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "유효하지 않은 API Key입니다")
                return
            }
            // Redis 캐시 저장
            val tenant = apiKey.tenantId
            val tenantPlan = Plan.FREE // Task 3.2 이후 Tenant.plan 조회로 교체
            redisTemplate.opsForValue().set(
                "apikey:$keyHash",
                mapper.writeValueAsString(mapOf("tenantId" to tenant.toString(), "plan" to tenantPlan.name)),
                CACHE_TTL
            )
            Pair(tenant, tenantPlan)
        }

        // Rate Limit 체크
        val rateLimitResult = rateLimiter.isAllowed(tenantId, plan)
        response.setHeader("X-RateLimit-Limit", rateLimitResult.limit.toString())
        response.setHeader("X-RateLimit-Remaining", rateLimitResult.remaining.toString())
        response.setHeader("X-RateLimit-Reset", rateLimitResult.resetAt.toString())

        if (!rateLimitResult.allowed) {
            val retryAfter = rateLimitResult.resetAt - System.currentTimeMillis() / 1000
            response.setHeader("Retry-After", retryAfter.toString())
            writeError(response, HttpStatus.TOO_MANY_REQUESTS, ErrorCode.RATE_LIMIT_EXCEEDED, "Rate limit을 초과했습니다. ${retryAfter}초 후 재시도하세요")
            return
        }

        // SecurityContext 설정
        val auth = UsernamePasswordAuthenticationToken(
            tenantId,
            null,
            listOf(SimpleGrantedAuthority("ROLE_API_USER"))
        ).also { it.details = mapOf("plan" to plan, "tenantId" to tenantId) }
        SecurityContextHolder.getContext().authentication = auth

        filterChain.doFilter(request, response)
    }

    private fun writeError(
        response: HttpServletResponse,
        status: HttpStatus,
        errorCode: ErrorCode,
        message: String
    ) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        response.writer.write(
            mapper.writeValueAsString(
                mapOf("error" to mapOf("code" to errorCode.name, "message" to message))
            )
        )
    }
}

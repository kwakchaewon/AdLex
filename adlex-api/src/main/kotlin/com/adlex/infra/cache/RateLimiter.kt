package com.adlex.infra.cache

import com.adlex.domain.entity.Plan
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

data class RateLimitResult(
    val allowed: Boolean,
    val limit: Int,
    val remaining: Int,
    val resetAt: Long  // epoch seconds
)

@Component
class RateLimiter(private val redisTemplate: StringRedisTemplate) {

    companion object {
        private val PLAN_LIMITS = mapOf(
            Plan.FREE to 10,
            Plan.STARTER to 60,
            Plan.PRO to 300,
            Plan.ENTERPRISE to 1000
        )
    }

    /**
     * Fixed Window Rate Limit (1분 단위).
     * Redis key: "ratelimit:{tenantId}:{epochMinute}"
     */
    fun isAllowed(tenantId: Long, plan: Plan): RateLimitResult {
        val limit = PLAN_LIMITS[plan] ?: 10
        val now = Instant.now()
        val epochMinute = now.epochSecond / 60
        val resetAt = (epochMinute + 1) * 60

        val key = "ratelimit:$tenantId:$epochMinute"
        val ops = redisTemplate.opsForValue()

        val count = ops.increment(key) ?: 1L
        if (count == 1L) {
            redisTemplate.expire(key, Duration.ofSeconds(60))
        }

        val remaining = maxOf(0, limit - count.toInt())
        return RateLimitResult(
            allowed = count <= limit,
            limit = limit,
            remaining = remaining,
            resetAt = resetAt
        )
    }

    /**
     * 월간 Quota 체크 및 증가.
     * Redis key: "quota:{tenantId}:{yyyy-MM}"
     */
    fun incrementAndCheckQuota(tenantId: Long, monthlyQuota: Int): Boolean {
        val yearMonth = java.time.YearMonth.now().toString()  // "2026-03"
        val key = "quota:$tenantId:$yearMonth"
        val ops = redisTemplate.opsForValue()

        val count = ops.increment(key) ?: 1L
        if (count == 1L) {
            // 월말까지 TTL 설정 (넉넉하게 35일)
            redisTemplate.expire(key, Duration.ofDays(35))
        }

        return count <= monthlyQuota
    }
}

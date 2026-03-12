package com.adlex.engine

import com.adlex.domain.entity.Rule
import com.adlex.domain.repository.RuleRepository
import com.adlex.engine.model.Channel
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class RuleRegistry(
    private val ruleRepository: RuleRepository,
    private val redisTemplate: StringRedisTemplate
) {
    private val mapper = jacksonObjectMapper()

    companion object {
        private const val CACHE_KEY = "rules:active"
        private val TTL = Duration.ofMinutes(10)
    }

    fun getActiveRules(): List<Rule> {
        val cached = redisTemplate.opsForValue().get(CACHE_KEY)
        if (cached != null) {
            val ids = mapper.readValue<List<Long>>(cached)
            return ruleRepository.findAllById(ids)
        }
        val rules = ruleRepository.findAllByActiveTrue()
        redisTemplate.opsForValue().set(
            CACHE_KEY,
            mapper.writeValueAsString(rules.map { it.id }),
            TTL
        )
        return rules
    }

    fun getRulesByChannel(channel: Channel): List<Rule> =
        getActiveRules().filter { rule ->
            rule.channels().any { it == channel }
        }

    fun invalidateCache() {
        redisTemplate.delete(CACHE_KEY)
    }
}

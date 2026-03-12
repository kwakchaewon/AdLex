package com.adlex.infra.security

import com.adlex.domain.repository.ApiKeyRepository
import com.adlex.domain.service.ApiKeyService
import com.adlex.infra.cache.RateLimiter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val apiKeyRepository: ApiKeyRepository,
    private val apiKeyService: ApiKeyService,
    private val rateLimiter: RateLimiter,
    private val redisTemplate: StringRedisTemplate
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/swagger-ui/**",
                        "/api-docs/**",
                        "/v3/api-docs/**",
                        "/actuator/health/**"
                    ).permitAll()
                    // /api/auth/** 공개 경로는 Task 3.2(회원 API)에서 추가
                    // /v1/** 은 ApiKeyAuthFilter에서 자체 인증 처리 (permitAll로 security 체인 통과)
                    .requestMatchers("/v1/**").permitAll()
                    .anyRequest().authenticated()
            }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .addFilterBefore(apiKeyAuthFilter(), UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun apiKeyAuthFilter(): ApiKeyAuthFilter =
        ApiKeyAuthFilter(apiKeyRepository, apiKeyService, rateLimiter, redisTemplate)

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}

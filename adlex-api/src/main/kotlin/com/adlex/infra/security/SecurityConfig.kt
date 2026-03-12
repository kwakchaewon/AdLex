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
    private val redisTemplate: StringRedisTemplate,
    private val jwtProvider: JwtProvider
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
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/v1/**").permitAll()
                    .anyRequest().authenticated()
            }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(apiKeyAuthFilter(), UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun apiKeyAuthFilter(): ApiKeyAuthFilter =
        ApiKeyAuthFilter(apiKeyRepository, apiKeyService, rateLimiter, redisTemplate)

    @Bean
    fun jwtAuthFilter(): JwtAuthFilter =
        JwtAuthFilter(jwtProvider)

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}

package com.adlex.domain.service

import com.adlex.domain.entity.ApiKey
import com.adlex.domain.entity.ApiKeyStatus
import com.adlex.domain.repository.ApiKeyRepository
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom

@Service
class ApiKeyService(
    private val apiKeyRepository: ApiKeyRepository
) {
    companion object {
        private val BASE62 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        private val random = SecureRandom()
    }

    /**
     * API Key 생성.
     * @return Pair(rawKey, savedApiKey) — rawKey는 이때만 반환, 이후 조회 불가
     */
    fun generateKey(tenantId: Long, name: String): Pair<String, ApiKey> {
        val rawKey = "al_live_" + buildString(32) {
            repeat(32) { append(BASE62[random.nextInt(BASE62.length)]) }
        }
        val keyHash = sha256Hex(rawKey)
        val keyPrefix = rawKey.take(12)

        val apiKey = apiKeyRepository.save(
            ApiKey(
                tenantId = tenantId,
                name = name,
                keyHash = keyHash,
                keyPrefix = keyPrefix
            )
        )
        return Pair(rawKey, apiKey)
    }

    fun validateKey(rawKey: String): ApiKey? {
        val hash = sha256Hex(rawKey)
        val apiKey = apiKeyRepository.findByKeyHash(hash) ?: return null
        return if (apiKey.status == ApiKeyStatus.ACTIVE) apiKey else null
    }

    fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

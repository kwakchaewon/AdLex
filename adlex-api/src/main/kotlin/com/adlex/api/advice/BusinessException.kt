package com.adlex.api.advice

enum class ErrorCode(val status: Int) {
    VALIDATION_ERROR(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    DUPLICATE_EMAIL(409),
    RATE_LIMIT_EXCEEDED(429),
    QUOTA_EXCEEDED(402),
    INTERNAL_ERROR(500)
}

class BusinessException(
    val errorCode: ErrorCode,
    override val message: String
) : RuntimeException(message)

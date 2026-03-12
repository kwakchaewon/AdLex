package com.adlex.api.advice

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val fieldErrors = ex.bindingResult.allErrors.associate { error ->
            val field = (error as? FieldError)?.field ?: "unknown"
            field to (error.defaultMessage ?: "유효하지 않은 값")
        }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    ErrorDetail(
                        code = ErrorCode.VALIDATION_ERROR.name,
                        message = "입력값이 올바르지 않습니다",
                        details = fieldErrors
                    )
                )
            )
    }

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(ex: BusinessException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(ex.errorCode.status)
            .body(
                ErrorResponse(
                    ErrorDetail(
                        code = ex.errorCode.name,
                        message = ex.message
                    )
                )
            )

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorResponse(
                    ErrorDetail(
                        code = ErrorCode.INTERNAL_ERROR.name,
                        message = "서버 오류가 발생했습니다"
                    )
                )
            )
}

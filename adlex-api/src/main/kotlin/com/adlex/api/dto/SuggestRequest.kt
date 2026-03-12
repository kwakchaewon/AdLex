package com.adlex.api.dto

import com.adlex.engine.model.Channel
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class SuggestRequest(
    @field:NotBlank(message = "message는 필수입니다")
    @field:Size(max = 2000, message = "message는 2000자 이내여야 합니다")
    val message: String,

    @field:NotNull(message = "channel은 필수입니다")
    val channel: Channel,

    /** 수정 방향 힌트 (선택). 예: "의약품 관련 표현 제거", "가격 표현 구체화" */
    @field:Size(max = 200, message = "hint는 200자 이내여야 합니다")
    val hint: String? = null
)

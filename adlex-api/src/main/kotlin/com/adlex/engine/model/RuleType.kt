package com.adlex.engine.model

enum class RuleType {
    REGEX,
    KEYWORD,
    TIME_RANGE,
    FIELD_PRESENT,
    LLM_JUDGE,
    LENGTH_CHECK,      // 채널별 메시지 길이 제한
    CONTENT_RATIO      // 광고성 키워드 밀도 제한
}

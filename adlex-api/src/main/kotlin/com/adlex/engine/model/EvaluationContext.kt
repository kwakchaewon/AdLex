package com.adlex.engine.model

import java.time.Instant

data class EvaluationContext(
    val message: String,
    val channel: Channel,
    val sender: SenderInfo? = null,
    val scheduledAt: Instant? = null,
    /** 추가 옵션: subject, unsubscribeUrl, skipRules 등 */
    val options: Map<String, Any> = emptyMap()
)

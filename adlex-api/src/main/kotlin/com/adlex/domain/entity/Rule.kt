package com.adlex.domain.entity

import com.adlex.engine.model.Channel
import com.adlex.engine.model.RuleType
import com.adlex.engine.model.Severity
import jakarta.persistence.*

@Entity
@Table(name = "rules")
class Rule(

    @Column(nullable = false, unique = true, length = 50)
    val code: String,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var type: RuleType,

    // CSV 문자열로 저장: "SMS", "SMS,KAKAO" 등
    @Column(nullable = false, length = 50)
    var channel: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var severity: Severity,

    @Column(columnDefinition = "TEXT")
    var pattern: String? = null,

    @Column(columnDefinition = "jsonb")
    var config: String? = null,

    @Column(name = "legal_basis", columnDefinition = "TEXT")
    var legalBasis: String? = null,

    @Column(nullable = false)
    var active: Boolean = true

) : BaseEntity() {

    /** channel CSV 문자열 → Channel enum 목록 변환 */
    fun channels(): List<Channel> =
        channel.split(",").mapNotNull { runCatching { Channel.valueOf(it.trim()) }.getOrNull() }
}

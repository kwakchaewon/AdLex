package com.adlex.domain.entity

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "precedents")
class Precedent(

    @Column(name = "case_no", length = 100)
    val caseNo: String? = null,

    @Column(nullable = false, length = 100)
    val authority: String,

    @Column(nullable = false, length = 500)
    val title: String,

    @Column(columnDefinition = "TEXT")
    val summary: String? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    // embedding 컬럼은 JPA로 직접 관리하지 않음.
    // PrecedentRepository의 native query로 읽기/쓰기 처리.

    @Column(name = "decided_at")
    val decidedAt: LocalDate? = null,

    @Column(nullable = false)
    var active: Boolean = true

) : BaseEntity()

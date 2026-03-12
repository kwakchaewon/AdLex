package com.adlex.domain.entity

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "law_chunks")
class LawChunk(

    @Column(name = "law_name", nullable = false, length = 200)
    val lawName: String,

    @Column(name = "article_no", length = 50)
    val articleNo: String? = null,

    @Column(name = "article_title", length = 200)
    val articleTitle: String? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    // embedding 컬럼은 JPA로 직접 관리하지 않음.
    // LawChunkRepository의 native query로 읽기/쓰기 처리.

    @Column(name = "law_date")
    val lawDate: LocalDate? = null,

    @Column(name = "source_url", columnDefinition = "TEXT")
    val sourceUrl: String? = null,

    @Column(nullable = false)
    var active: Boolean = true

) : BaseEntity()

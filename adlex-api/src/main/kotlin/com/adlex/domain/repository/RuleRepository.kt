package com.adlex.domain.repository

import com.adlex.domain.entity.Rule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RuleRepository : JpaRepository<Rule, Long> {
    fun findAllByActiveTrue(): List<Rule>
}

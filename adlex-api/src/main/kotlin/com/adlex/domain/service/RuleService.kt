package com.adlex.domain.service

import com.adlex.api.advice.BusinessException
import com.adlex.api.advice.ErrorCode
import com.adlex.api.dto.CreateRuleRequest
import com.adlex.api.dto.RuleResponse
import com.adlex.api.dto.UpdateRuleRequest
import com.adlex.domain.entity.Rule
import com.adlex.domain.repository.RuleRepository
import com.adlex.engine.RuleRegistry
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RuleService(
    private val ruleRepository: RuleRepository,
    private val ruleRegistry: RuleRegistry
) {

    fun getAll(): List<RuleResponse> =
        ruleRepository.findAll().map { RuleResponse.from(it) }

    fun getById(id: Long): RuleResponse {
        val rule = ruleRepository.findById(id).orElseThrow {
            BusinessException(ErrorCode.NOT_FOUND, "규칙을 찾을 수 없습니다: $id")
        }
        return RuleResponse.from(rule)
    }

    @Transactional
    fun create(req: CreateRuleRequest): RuleResponse {
        if (ruleRepository.existsByCode(req.code)) {
            throw BusinessException(ErrorCode.VALIDATION_ERROR, "이미 존재하는 규칙 코드입니다: ${req.code}")
        }
        val rule = Rule(
            code = req.code,
            name = req.name,
            description = req.description,
            type = req.type,
            channel = req.channel,
            severity = req.severity,
            pattern = req.pattern,
            config = req.config,
            legalBasis = req.legalBasis,
            active = req.active
        )
        val saved = ruleRepository.save(rule)
        ruleRegistry.invalidateCache()
        return RuleResponse.from(saved)
    }

    @Transactional
    fun update(id: Long, req: UpdateRuleRequest): RuleResponse {
        val rule = ruleRepository.findById(id).orElseThrow {
            BusinessException(ErrorCode.NOT_FOUND, "규칙을 찾을 수 없습니다: $id")
        }
        req.name?.let { rule.name = it }
        req.description?.let { rule.description = it }
        req.type?.let { rule.type = it }
        req.channel?.let { rule.channel = it }
        req.severity?.let { rule.severity = it }
        req.pattern?.let { rule.pattern = it }
        req.config?.let { rule.config = it }
        req.legalBasis?.let { rule.legalBasis = it }
        req.active?.let { rule.active = it }
        val saved = ruleRepository.save(rule)
        ruleRegistry.invalidateCache()
        return RuleResponse.from(saved)
    }

    @Transactional
    fun delete(id: Long) {
        val rule = ruleRepository.findById(id).orElseThrow {
            BusinessException(ErrorCode.NOT_FOUND, "규칙을 찾을 수 없습니다: $id")
        }
        rule.active = false
        ruleRepository.save(rule)
        ruleRegistry.invalidateCache()
    }
}

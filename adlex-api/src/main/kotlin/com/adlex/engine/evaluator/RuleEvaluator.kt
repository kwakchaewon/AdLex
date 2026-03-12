package com.adlex.engine.evaluator

import com.adlex.domain.entity.Rule
import com.adlex.engine.model.EvaluationContext
import com.adlex.engine.model.EvaluationResult
import com.adlex.engine.model.RuleType

interface RuleEvaluator {
    fun supports(type: RuleType): Boolean
    /** null = 위반 없음, non-null = 위반 발견 */
    fun evaluate(context: EvaluationContext, rule: Rule): EvaluationResult?
}

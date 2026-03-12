"""AdLex SDK 데이터 모델."""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from typing import List, Optional


@dataclass
class SenderInfo:
    """발신자 정보."""
    name: Optional[str] = None
    phone_number: Optional[str] = None
    email: Optional[str] = None


@dataclass
class CheckOptions:
    """검사 옵션."""
    skip_rules: Optional[List[str]] = None
    use_llm: bool = False


@dataclass
class Violation:
    """법규 위반 항목."""
    rule_code: str
    severity: str           # HIGH | MEDIUM | LOW
    message: str
    legal_basis: Optional[str] = None
    suggestion: Optional[str] = None


@dataclass
class LlmAnalysis:
    """LLM 보조 분석 결과 (PRO 이상 플랜)."""
    analysis: str
    cited_law_count: int
    cited_precedent_count: int


@dataclass
class CheckResult:
    """단건 준법 검사 결과."""
    compliant: bool
    violation_count: int
    violations: List[Violation]
    checked_at: datetime
    processing_ms: int
    llm_analysis: Optional[LlmAnalysis] = None


@dataclass
class BatchSummary:
    """배치 검사 집계."""
    total: int
    compliant: int
    violated: int
    processing_ms: int


@dataclass
class BatchCheckResult:
    """배치 준법 검사 결과."""
    results: List[CheckResult]
    summary: BatchSummary


@dataclass
class RelatedLaw:
    """관련 법령 조문."""
    law_name: str
    content: str
    article_no: Optional[str] = None
    article_title: Optional[str] = None
    source_url: Optional[str] = None


@dataclass
class SimilarPrecedent:
    """유사 위반 판례."""
    authority: str
    title: str
    case_no: Optional[str] = None
    summary: Optional[str] = None


@dataclass
class ReportResult:
    """준법 검사 리포트 (관련 법령 + 유사 판례 포함)."""
    message: str
    channel: str
    compliant: bool
    violation_count: int
    violations: List[Violation]
    related_laws: List[RelatedLaw]
    similar_precedents: List[SimilarPrecedent]
    generated_at: datetime
    processing_ms: int
    llm_analysis: Optional[LlmAnalysis] = None

"""AdLex API 클라이언트 (동기 + 비동기)."""

from __future__ import annotations

from datetime import datetime, timezone
from typing import Any, Dict, List, Optional

import httpx

from .exceptions import (
    AdlexError, AuthenticationError, NotFoundError,
    PlanUpgradeRequiredError, QuotaExceededError,
    RateLimitError, ServerError, ValidationError,
)
from .models import (
    BatchCheckResult, BatchSummary, CheckOptions, CheckResult,
    LlmAnalysis, RelatedLaw, ReportResult, SenderInfo,
    SimilarPrecedent, Violation,
)

DEFAULT_BASE_URL = "https://api.adlex.io"
DEFAULT_TIMEOUT = 30.0


def _parse_datetime(value: Optional[str]) -> Optional[datetime]:
    if not value:
        return None
    # ISO 8601 with timezone
    try:
        return datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError:
        return datetime.now(tz=timezone.utc)


def _parse_violation(data: Dict[str, Any]) -> Violation:
    return Violation(
        rule_code=data["ruleCode"],
        severity=data["severity"],
        message=data["message"],
        legal_basis=data.get("legalBasis"),
        suggestion=data.get("suggestion"),
    )


def _parse_check_result(data: Dict[str, Any]) -> CheckResult:
    llm_data = data.get("llmAnalysis")
    return CheckResult(
        compliant=data["compliant"],
        violation_count=data["violationCount"],
        violations=[_parse_violation(v) for v in data.get("violations", [])],
        checked_at=_parse_datetime(data.get("checkedAt")) or datetime.now(tz=timezone.utc),
        processing_ms=data.get("processingMs", 0),
        llm_analysis=LlmAnalysis(
            analysis=llm_data["analysis"],
            cited_law_count=llm_data["citedLawCount"],
            cited_precedent_count=llm_data["citedPrecedentCount"],
        ) if llm_data else None,
    )


def _parse_report_result(data: Dict[str, Any]) -> ReportResult:
    llm_data = data.get("llmAnalysis")
    return ReportResult(
        message=data["message"],
        channel=data["channel"],
        compliant=data["compliant"],
        violation_count=data["violationCount"],
        violations=[_parse_violation(v) for v in data.get("violations", [])],
        related_laws=[
            RelatedLaw(
                law_name=l["lawName"],
                content=l["content"],
                article_no=l.get("articleNo"),
                article_title=l.get("articleTitle"),
                source_url=l.get("sourceUrl"),
            )
            for l in data.get("relatedLaws", [])
        ],
        similar_precedents=[
            SimilarPrecedent(
                authority=p["authority"],
                title=p["title"],
                case_no=p.get("caseNo"),
                summary=p.get("summary"),
            )
            for p in data.get("similarPrecedents", [])
        ],
        generated_at=_parse_datetime(data.get("generatedAt")) or datetime.now(tz=timezone.utc),
        processing_ms=data.get("processingMs", 0),
        llm_analysis=LlmAnalysis(
            analysis=llm_data["analysis"],
            cited_law_count=llm_data["citedLawCount"],
            cited_precedent_count=llm_data["citedPrecedentCount"],
        ) if llm_data else None,
    )


def _raise_for_status(response: httpx.Response) -> None:
    if response.is_success:
        return
    status = response.status_code
    try:
        body = response.json()
        msg = body.get("message", response.text)
    except Exception:
        msg = response.text

    if status == 400:
        raise ValidationError(msg, status)
    elif status == 401:
        raise AuthenticationError(msg, status)
    elif status == 402:
        if "플랜" in msg or "plan" in msg.lower():
            raise PlanUpgradeRequiredError(msg, status)
        raise QuotaExceededError(msg, status)
    elif status == 404:
        raise NotFoundError(msg, status)
    elif status == 429:
        raise RateLimitError(msg, status)
    elif status >= 500:
        raise ServerError(msg, status)
    else:
        raise AdlexError(msg, status)


def _build_check_body(
    message: str,
    channel: str,
    sender: Optional[SenderInfo],
    options: Optional[CheckOptions],
) -> Dict[str, Any]:
    body: Dict[str, Any] = {"message": message, "channel": channel}
    if sender:
        body["sender"] = {k: v for k, v in {
            "name": sender.name,
            "phoneNumber": sender.phone_number,
            "email": sender.email,
        }.items() if v is not None}
    if options:
        body["options"] = {k: v for k, v in {
            "skipRules": options.skip_rules,
            "useLlm": options.use_llm,
        }.items() if v is not None}
    return body


class AdlexClient:
    """AdLex 동기 클라이언트.

    Example::

        client = AdlexClient(api_key="ak-xxxx")
        result = client.check("지금 구매하면 100% 할인!", channel="SMS")
        if not result.compliant:
            for v in result.violations:
                print(f"[{v.severity}] {v.rule_code}: {v.message}")
    """

    def __init__(
        self,
        api_key: str,
        base_url: str = DEFAULT_BASE_URL,
        timeout: float = DEFAULT_TIMEOUT,
    ) -> None:
        self._http = httpx.Client(
            base_url=base_url,
            headers={
                "X-API-Key": api_key,
                "Content-Type": "application/json",
                "Accept": "application/json",
            },
            timeout=timeout,
        )

    def check(
        self,
        message: str,
        channel: str,
        sender: Optional[SenderInfo] = None,
        options: Optional[CheckOptions] = None,
    ) -> CheckResult:
        """단건 메시지 준법 검사."""
        body = _build_check_body(message, channel, sender, options)
        response = self._http.post("/v1/check", json=body)
        _raise_for_status(response)
        return _parse_check_result(response.json())

    def batch_check(
        self,
        messages: List[str],
        channel: str,
        sender: Optional[SenderInfo] = None,
        options: Optional[CheckOptions] = None,
    ) -> BatchCheckResult:
        """최대 100건 메시지 일괄 준법 검사."""
        if not messages:
            raise ValueError("messages는 1개 이상이어야 합니다.")
        if len(messages) > 100:
            raise ValueError("messages는 최대 100건까지 지원합니다.")

        body = {
            "messages": [
                _build_check_body(m, channel, sender, options) for m in messages
            ]
        }
        response = self._http.post("/v1/check/batch", json=body)
        _raise_for_status(response)

        data = response.json()
        summary_data = data.get("summary", {})
        return BatchCheckResult(
            results=[_parse_check_result(r) for r in data.get("results", [])],
            summary=BatchSummary(
                total=summary_data.get("total", len(messages)),
                compliant=summary_data.get("compliant", 0),
                violated=summary_data.get("violated", 0),
                processing_ms=summary_data.get("processingMs", 0),
            ),
        )

    def report(
        self,
        message: str,
        channel: str,
        include_llm_analysis: bool = False,
    ) -> ReportResult:
        """준법 검사 리포트 생성 (관련 법령 + 유사 판례 포함)."""
        body = {
            "message": message,
            "channel": channel,
            "includeLlmAnalysis": include_llm_analysis,
        }
        response = self._http.post("/v1/report", json=body)
        _raise_for_status(response)
        return _parse_report_result(response.json())

    def close(self) -> None:
        self._http.close()

    def __enter__(self) -> "AdlexClient":
        return self

    def __exit__(self, *args: Any) -> None:
        self.close()


class AsyncAdlexClient:
    """AdLex 비동기 클라이언트 (asyncio).

    Example::

        async with AsyncAdlexClient(api_key="ak-xxxx") as client:
            result = await client.check("광고 메시지", channel="SMS")
    """

    def __init__(
        self,
        api_key: str,
        base_url: str = DEFAULT_BASE_URL,
        timeout: float = DEFAULT_TIMEOUT,
    ) -> None:
        self._http = httpx.AsyncClient(
            base_url=base_url,
            headers={
                "X-API-Key": api_key,
                "Content-Type": "application/json",
                "Accept": "application/json",
            },
            timeout=timeout,
        )

    async def check(
        self,
        message: str,
        channel: str,
        sender: Optional[SenderInfo] = None,
        options: Optional[CheckOptions] = None,
    ) -> CheckResult:
        body = _build_check_body(message, channel, sender, options)
        response = await self._http.post("/v1/check", json=body)
        _raise_for_status(response)
        return _parse_check_result(response.json())

    async def batch_check(
        self,
        messages: List[str],
        channel: str,
        sender: Optional[SenderInfo] = None,
        options: Optional[CheckOptions] = None,
    ) -> BatchCheckResult:
        if not messages:
            raise ValueError("messages는 1개 이상이어야 합니다.")
        if len(messages) > 100:
            raise ValueError("messages는 최대 100건까지 지원합니다.")

        body = {
            "messages": [
                _build_check_body(m, channel, sender, options) for m in messages
            ]
        }
        response = await self._http.post("/v1/check/batch", json=body)
        _raise_for_status(response)

        data = response.json()
        summary_data = data.get("summary", {})
        return BatchCheckResult(
            results=[_parse_check_result(r) for r in data.get("results", [])],
            summary=BatchSummary(
                total=summary_data.get("total", len(messages)),
                compliant=summary_data.get("compliant", 0),
                violated=summary_data.get("violated", 0),
                processing_ms=summary_data.get("processingMs", 0),
            ),
        )

    async def report(
        self,
        message: str,
        channel: str,
        include_llm_analysis: bool = False,
    ) -> ReportResult:
        body = {
            "message": message,
            "channel": channel,
            "includeLlmAnalysis": include_llm_analysis,
        }
        response = await self._http.post("/v1/report", json=body)
        _raise_for_status(response)
        return _parse_report_result(response.json())

    async def aclose(self) -> None:
        await self._http.aclose()

    async def __aenter__(self) -> "AsyncAdlexClient":
        return self

    async def __aexit__(self, *args: Any) -> None:
        await self.aclose()

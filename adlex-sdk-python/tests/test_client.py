"""AdlexClient 단위 테스트 (respx로 HTTP 모킹)."""

from __future__ import annotations

import pytest
import respx
import httpx
from datetime import datetime, timezone

from adlex import (
    AdlexClient,
    AsyncAdlexClient,
    AuthenticationError,
    RateLimitError,
    ValidationError,
)

BASE_URL = "https://api.adlex.io"

CHECK_RESPONSE = {
    "compliant": False,
    "violationCount": 1,
    "violations": [
        {
            "ruleCode": "ADV_001",
            "severity": "HIGH",
            "message": "금지 표현이 포함되어 있습니다",
            "legalBasis": "표시광고법 제3조",
            "suggestion": "표현을 수정하세요",
        }
    ],
    "checkedAt": "2024-01-01T00:00:00Z",
    "processingMs": 42,
}

BATCH_RESPONSE = {
    "results": [CHECK_RESPONSE, {**CHECK_RESPONSE, "compliant": True, "violationCount": 0, "violations": []}],
    "summary": {"total": 2, "compliant": 1, "violated": 1, "processingMs": 80},
}

REPORT_RESPONSE = {
    "message": "테스트 메시지",
    "channel": "SMS",
    "compliant": False,
    "violationCount": 1,
    "violations": [CHECK_RESPONSE["violations"][0]],
    "relatedLaws": [
        {
            "lawName": "표시광고법",
            "articleNo": "제3조",
            "articleTitle": "부당표시 금지",
            "content": "사업자는 부당한 표시를 해서는 안 된다.",
            "sourceUrl": None,
        }
    ],
    "similarPrecedents": [
        {
            "caseNo": "2021나123",
            "authority": "공정거래위원회",
            "title": "허위광고 사례",
            "summary": "100% 효과를 보장한다고 광고하여 시정명령",
        }
    ],
    "llmAnalysis": None,
    "generatedAt": "2024-01-01T00:00:00Z",
    "processingMs": 150,
}


# ─── 동기 클라이언트 ────────────────────────────────────────────────

class TestAdlexClient:

    @respx.mock
    def test_check_violation(self):
        respx.post(f"{BASE_URL}/v1/check").mock(return_value=httpx.Response(200, json=CHECK_RESPONSE))

        with AdlexClient(api_key="ak-test", base_url=BASE_URL) as client:
            result = client.check("100% 효과 보장!", channel="SMS")

        assert result.compliant is False
        assert result.violation_count == 1
        assert result.violations[0].rule_code == "ADV_001"
        assert result.violations[0].severity == "HIGH"
        assert result.violations[0].legal_basis == "표시광고법 제3조"

    @respx.mock
    def test_check_compliant(self):
        response = {**CHECK_RESPONSE, "compliant": True, "violationCount": 0, "violations": []}
        respx.post(f"{BASE_URL}/v1/check").mock(return_value=httpx.Response(200, json=response))

        with AdlexClient(api_key="ak-test", base_url=BASE_URL) as client:
            result = client.check("정직한 광고입니다", channel="SMS")

        assert result.compliant is True
        assert result.violations == []

    @respx.mock
    def test_batch_check(self):
        respx.post(f"{BASE_URL}/v1/check/batch").mock(return_value=httpx.Response(200, json=BATCH_RESPONSE))

        with AdlexClient(api_key="ak-test", base_url=BASE_URL) as client:
            result = client.batch_check(["메시지1", "메시지2"], channel="SMS")

        assert len(result.results) == 2
        assert result.summary.total == 2
        assert result.summary.compliant == 1
        assert result.summary.violated == 1

    def test_batch_check_empty_raises(self):
        with AdlexClient(api_key="ak-test", base_url=BASE_URL) as client:
            with pytest.raises(ValueError, match="1개 이상"):
                client.batch_check([], channel="SMS")

    def test_batch_check_over_100_raises(self):
        with AdlexClient(api_key="ak-test", base_url=BASE_URL) as client:
            with pytest.raises(ValueError, match="100건"):
                client.batch_check(["msg"] * 101, channel="SMS")

    @respx.mock
    def test_report(self):
        respx.post(f"{BASE_URL}/v1/report").mock(return_value=httpx.Response(200, json=REPORT_RESPONSE))

        with AdlexClient(api_key="ak-test", base_url=BASE_URL) as client:
            report = client.report("테스트 메시지", channel="SMS")

        assert report.compliant is False
        assert len(report.related_laws) == 1
        assert report.related_laws[0].law_name == "표시광고법"
        assert len(report.similar_precedents) == 1
        assert report.similar_precedents[0].authority == "공정거래위원회"
        assert report.llm_analysis is None

    @respx.mock
    def test_auth_error(self):
        respx.post(f"{BASE_URL}/v1/check").mock(
            return_value=httpx.Response(401, json={"message": "인증 실패"})
        )

        with AdlexClient(api_key="invalid", base_url=BASE_URL) as client:
            with pytest.raises(AuthenticationError) as exc:
                client.check("메시지", channel="SMS")
        assert exc.value.status_code == 401

    @respx.mock
    def test_rate_limit_error(self):
        respx.post(f"{BASE_URL}/v1/check").mock(
            return_value=httpx.Response(429, json={"message": "Rate limit exceeded"})
        )

        with AdlexClient(api_key="ak-test", base_url=BASE_URL) as client:
            with pytest.raises(RateLimitError) as exc:
                client.check("메시지", channel="SMS")
        assert exc.value.status_code == 429

    @respx.mock
    def test_validation_error(self):
        respx.post(f"{BASE_URL}/v1/check").mock(
            return_value=httpx.Response(400, json={"message": "message는 필수입니다"})
        )

        with AdlexClient(api_key="ak-test", base_url=BASE_URL) as client:
            with pytest.raises(ValidationError):
                client.check("", channel="SMS")


# ─── 비동기 클라이언트 ────────────────────────────────────────────────

class TestAsyncAdlexClient:

    @pytest.mark.asyncio
    @respx.mock
    async def test_async_check(self):
        respx.post(f"{BASE_URL}/v1/check").mock(return_value=httpx.Response(200, json=CHECK_RESPONSE))

        async with AsyncAdlexClient(api_key="ak-test", base_url=BASE_URL) as client:
            result = await client.check("100% 효과!", channel="SMS")

        assert result.compliant is False

    @pytest.mark.asyncio
    @respx.mock
    async def test_async_batch_check(self):
        respx.post(f"{BASE_URL}/v1/check/batch").mock(return_value=httpx.Response(200, json=BATCH_RESPONSE))

        async with AsyncAdlexClient(api_key="ak-test", base_url=BASE_URL) as client:
            result = await client.batch_check(["msg1", "msg2"], channel="SMS")

        assert result.summary.total == 2

    @pytest.mark.asyncio
    @respx.mock
    async def test_async_report(self):
        respx.post(f"{BASE_URL}/v1/report").mock(return_value=httpx.Response(200, json=REPORT_RESPONSE))

        async with AsyncAdlexClient(api_key="ak-test", base_url=BASE_URL) as client:
            report = await client.report("테스트 메시지", channel="SMS")

        assert len(report.related_laws) == 1

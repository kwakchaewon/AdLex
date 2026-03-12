"""AdLex SDK 예외 클래스."""

from __future__ import annotations


class AdlexError(Exception):
    """AdLex API 호출 관련 기본 예외."""

    def __init__(self, message: str, status_code: int | None = None) -> None:
        super().__init__(message)
        self.status_code = status_code


class AuthenticationError(AdlexError):
    """API 키 인증 실패 (401)."""


class RateLimitError(AdlexError):
    """요청 한도 초과 (429)."""


class QuotaExceededError(AdlexError):
    """월 사용량 초과 (402)."""


class PlanUpgradeRequiredError(AdlexError):
    """현재 플랜에서 지원하지 않는 기능 (402)."""


class NotFoundError(AdlexError):
    """리소스를 찾을 수 없음 (404)."""


class ValidationError(AdlexError):
    """요청 파라미터 검증 실패 (400)."""


class ServerError(AdlexError):
    """AdLex 서버 내부 오류 (5xx)."""

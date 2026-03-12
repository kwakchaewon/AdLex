"""AdLex Python SDK."""

from .client import AdlexClient, AsyncAdlexClient
from .exceptions import (
    AdlexError,
    AuthenticationError,
    NotFoundError,
    PlanUpgradeRequiredError,
    QuotaExceededError,
    RateLimitError,
    ServerError,
    ValidationError,
)
from .models import (
    BatchCheckResult,
    BatchSummary,
    CheckOptions,
    CheckResult,
    LlmAnalysis,
    RelatedLaw,
    ReportResult,
    SenderInfo,
    SimilarPrecedent,
    Violation,
)

__all__ = [
    "AdlexClient",
    "AsyncAdlexClient",
    # exceptions
    "AdlexError",
    "AuthenticationError",
    "NotFoundError",
    "PlanUpgradeRequiredError",
    "QuotaExceededError",
    "RateLimitError",
    "ServerError",
    "ValidationError",
    # models
    "BatchCheckResult",
    "BatchSummary",
    "CheckOptions",
    "CheckResult",
    "LlmAnalysis",
    "RelatedLaw",
    "ReportResult",
    "SenderInfo",
    "SimilarPrecedent",
    "Violation",
]

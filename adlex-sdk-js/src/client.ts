import {
  AdlexClientOptions,
  BatchCheckResult,
  Channel,
  CheckOptions,
  CheckResult,
  LlmAnalysis,
  RelatedLaw,
  ReportResult,
  SenderInfo,
  SimilarPrecedent,
  Violation,
} from "./types";
import {
  AdlexError,
  AuthenticationError,
  NotFoundError,
  PlanUpgradeRequiredError,
  QuotaExceededError,
  RateLimitError,
  ServerError,
  ValidationError,
} from "./errors";

const DEFAULT_BASE_URL = "https://api.adlex.io";
const DEFAULT_TIMEOUT_MS = 30_000;

// ─── 파싱 헬퍼 ──────────────────────────────────────────────────────

function parseViolation(data: Record<string, unknown>): Violation {
  return {
    ruleCode: data.ruleCode as string,
    severity: data.severity as Violation["severity"],
    message: data.message as string,
    legalBasis: data.legalBasis as string | undefined,
    suggestion: data.suggestion as string | undefined,
  };
}

function parseLlmAnalysis(data: Record<string, unknown>): LlmAnalysis {
  return {
    analysis: data.analysis as string,
    citedLawCount: data.citedLawCount as number,
    citedPrecedentCount: data.citedPrecedentCount as number,
  };
}

function parseCheckResult(data: Record<string, unknown>): CheckResult {
  return {
    compliant: data.compliant as boolean,
    violationCount: data.violationCount as number,
    violations: ((data.violations as unknown[]) ?? []).map((v) =>
      parseViolation(v as Record<string, unknown>)
    ),
    checkedAt: new Date(data.checkedAt as string),
    processingMs: data.processingMs as number,
    llmAnalysis: data.llmAnalysis
      ? parseLlmAnalysis(data.llmAnalysis as Record<string, unknown>)
      : undefined,
  };
}

function parseReportResult(data: Record<string, unknown>): ReportResult {
  return {
    message: data.message as string,
    channel: data.channel as Channel,
    compliant: data.compliant as boolean,
    violationCount: data.violationCount as number,
    violations: ((data.violations as unknown[]) ?? []).map((v) =>
      parseViolation(v as Record<string, unknown>)
    ),
    relatedLaws: ((data.relatedLaws as unknown[]) ?? []).map(
      (l): RelatedLaw => {
        const law = l as Record<string, unknown>;
        return {
          lawName: law.lawName as string,
          content: law.content as string,
          articleNo: law.articleNo as string | undefined,
          articleTitle: law.articleTitle as string | undefined,
          sourceUrl: law.sourceUrl as string | undefined,
        };
      }
    ),
    similarPrecedents: ((data.similarPrecedents as unknown[]) ?? []).map(
      (p): SimilarPrecedent => {
        const prec = p as Record<string, unknown>;
        return {
          authority: prec.authority as string,
          title: prec.title as string,
          caseNo: prec.caseNo as string | undefined,
          summary: prec.summary as string | undefined,
        };
      }
    ),
    generatedAt: new Date(data.generatedAt as string),
    processingMs: data.processingMs as number,
    llmAnalysis: data.llmAnalysis
      ? parseLlmAnalysis(data.llmAnalysis as Record<string, unknown>)
      : undefined,
  };
}

// ─── HTTP 오류 처리 ──────────────────────────────────────────────────

async function throwForStatus(response: Response): Promise<void> {
  if (response.ok) return;

  const status = response.status;
  let message: string;
  try {
    const body = (await response.json()) as Record<string, unknown>;
    message = (body.message as string) ?? response.statusText;
  } catch {
    message = response.statusText;
  }

  if (status === 400) throw new ValidationError(message);
  if (status === 401) throw new AuthenticationError(message);
  if (status === 402) {
    if (message.includes("플랜") || message.toLowerCase().includes("plan")) {
      throw new PlanUpgradeRequiredError(message);
    }
    throw new QuotaExceededError(message);
  }
  if (status === 404) throw new NotFoundError(message);
  if (status === 429) throw new RateLimitError(message);
  if (status >= 500) throw new ServerError(message, status);
  throw new AdlexError(message, status);
}

// ─── 요청 바디 빌더 ──────────────────────────────────────────────────

function buildCheckBody(
  message: string,
  channel: Channel,
  sender?: SenderInfo,
  options?: CheckOptions
): Record<string, unknown> {
  const body: Record<string, unknown> = { message, channel };

  if (sender) {
    const s: Record<string, string> = {};
    if (sender.name) s.name = sender.name;
    if (sender.phoneNumber) s.phoneNumber = sender.phoneNumber;
    if (sender.email) s.email = sender.email;
    if (Object.keys(s).length > 0) body.sender = s;
  }

  if (options) {
    const o: Record<string, unknown> = {};
    if (options.skipRules?.length) o.skipRules = options.skipRules;
    if (options.useLlm) o.useLlm = true;
    if (Object.keys(o).length > 0) body.options = o;
  }

  return body;
}

// ─── 클라이언트 ──────────────────────────────────────────────────────

/**
 * AdLex API 클라이언트.
 *
 * @example
 * ```ts
 * const client = new AdlexClient({ apiKey: "ak-xxxx" });
 *
 * const result = await client.check("지금 구매하면 100% 할인!", { channel: "SMS" });
 * if (!result.compliant) {
 *   result.violations.forEach(v => console.log(`[${v.severity}] ${v.ruleCode}: ${v.message}`));
 * }
 * ```
 */
export class AdlexClient {
  private readonly baseUrl: string;
  private readonly headers: Record<string, string>;
  private readonly timeoutMs: number;

  constructor(options: AdlexClientOptions) {
    this.baseUrl = (options.baseUrl ?? DEFAULT_BASE_URL).replace(/\/$/, "");
    this.timeoutMs = options.timeout ?? DEFAULT_TIMEOUT_MS;
    this.headers = {
      "X-API-Key": options.apiKey,
      "Content-Type": "application/json",
      Accept: "application/json",
    };
  }

  /** 단건 메시지 준법 검사 */
  async check(
    message: string,
    opts: {
      channel: Channel;
      sender?: SenderInfo;
      options?: CheckOptions;
    }
  ): Promise<CheckResult> {
    const body = buildCheckBody(message, opts.channel, opts.sender, opts.options);
    const response = await this.fetch("/v1/check", body);
    return parseCheckResult((await response.json()) as Record<string, unknown>);
  }

  /** 최대 100건 메시지 일괄 준법 검사 */
  async batchCheck(
    messages: string[],
    opts: {
      channel: Channel;
      sender?: SenderInfo;
      options?: CheckOptions;
    }
  ): Promise<BatchCheckResult> {
    if (messages.length === 0) throw new Error("messages는 1개 이상이어야 합니다.");
    if (messages.length > 100) throw new Error("messages는 최대 100건까지 지원합니다.");

    const body = {
      messages: messages.map((m) =>
        buildCheckBody(m, opts.channel, opts.sender, opts.options)
      ),
    };
    const response = await this.fetch("/v1/check/batch", body);
    const data = (await response.json()) as Record<string, unknown>;
    const summary = (data.summary ?? {}) as Record<string, unknown>;

    return {
      results: ((data.results as unknown[]) ?? []).map((r) =>
        parseCheckResult(r as Record<string, unknown>)
      ),
      summary: {
        total: (summary.total as number) ?? messages.length,
        compliant: (summary.compliant as number) ?? 0,
        violated: (summary.violated as number) ?? 0,
        processingMs: (summary.processingMs as number) ?? 0,
      },
    };
  }

  /** 준법 검사 리포트 생성 (관련 법령 + 유사 판례 포함) */
  async report(
    message: string,
    opts: {
      channel: Channel;
      includeLlmAnalysis?: boolean;
    }
  ): Promise<ReportResult> {
    const body = {
      message,
      channel: opts.channel,
      includeLlmAnalysis: opts.includeLlmAnalysis ?? false,
    };
    const response = await this.fetch("/v1/report", body);
    return parseReportResult((await response.json()) as Record<string, unknown>);
  }

  private async fetch(path: string, body: unknown): Promise<Response> {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), this.timeoutMs);

    try {
      const response = await globalThis.fetch(`${this.baseUrl}${path}`, {
        method: "POST",
        headers: this.headers,
        body: JSON.stringify(body),
        signal: controller.signal,
      });
      await throwForStatus(response.clone());
      return response;
    } catch (err) {
      if (err instanceof AdlexError) throw err;
      if ((err as Error).name === "AbortError") {
        throw new AdlexError(`Request timeout after ${this.timeoutMs}ms`);
      }
      throw new AdlexError(`Network error: ${(err as Error).message}`);
    } finally {
      clearTimeout(timer);
    }
  }
}

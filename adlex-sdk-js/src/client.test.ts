import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { AdlexClient } from "./client";
import {
  AuthenticationError,
  RateLimitError,
  ValidationError,
  ServerError,
} from "./errors";

const BASE_URL = "https://api.adlex.io";

const CHECK_RESPONSE = {
  compliant: false,
  violationCount: 1,
  violations: [
    {
      ruleCode: "ADV_001",
      severity: "HIGH",
      message: "금지 표현이 포함되어 있습니다",
      legalBasis: "표시광고법 제3조",
      suggestion: "표현을 수정하세요",
    },
  ],
  checkedAt: "2024-01-01T00:00:00Z",
  processingMs: 42,
};

const COMPLIANT_RESPONSE = {
  compliant: true,
  violationCount: 0,
  violations: [],
  checkedAt: "2024-01-01T00:00:00Z",
  processingMs: 10,
};

const BATCH_RESPONSE = {
  results: [CHECK_RESPONSE, COMPLIANT_RESPONSE],
  summary: { total: 2, compliant: 1, violated: 1, processingMs: 80 },
};

const REPORT_RESPONSE = {
  message: "테스트 메시지",
  channel: "SMS",
  compliant: false,
  violationCount: 1,
  violations: [CHECK_RESPONSE.violations[0]],
  relatedLaws: [
    {
      lawName: "표시광고법",
      articleNo: "제3조",
      articleTitle: "부당표시 금지",
      content: "사업자는 부당한 표시를 해서는 안 된다.",
    },
  ],
  similarPrecedents: [
    {
      caseNo: "2021나123",
      authority: "공정거래위원회",
      title: "허위광고 사례",
      summary: "100% 효과 보장 광고에 시정명령",
    },
  ],
  llmAnalysis: null,
  generatedAt: "2024-01-01T00:00:00Z",
  processingMs: 150,
};

function mockFetch(status: number, body: unknown) {
  return vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
    new Response(JSON.stringify(body), {
      status,
      headers: { "Content-Type": "application/json" },
    })
  );
}

describe("AdlexClient", () => {
  const client = new AdlexClient({ apiKey: "ak-test", baseUrl: BASE_URL });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  // ─── check ──────────────────────────────────────────────────────

  it("check — 위반 메시지 파싱", async () => {
    mockFetch(200, CHECK_RESPONSE);

    const result = await client.check("100% 효과 보장!", { channel: "SMS" });

    expect(result.compliant).toBe(false);
    expect(result.violationCount).toBe(1);
    expect(result.violations[0].ruleCode).toBe("ADV_001");
    expect(result.violations[0].severity).toBe("HIGH");
    expect(result.violations[0].legalBasis).toBe("표시광고법 제3조");
    expect(result.checkedAt).toBeInstanceOf(Date);
  });

  it("check — 준법 메시지", async () => {
    mockFetch(200, COMPLIANT_RESPONSE);

    const result = await client.check("정직한 광고입니다", { channel: "SMS" });

    expect(result.compliant).toBe(true);
    expect(result.violations).toHaveLength(0);
  });

  it("check — 요청 바디에 API 키 헤더 포함", async () => {
    const spy = mockFetch(200, COMPLIANT_RESPONSE);

    await client.check("메시지", { channel: "SMS" });

    const [, init] = spy.mock.calls[0];
    const headers = init?.headers as Record<string, string>;
    expect(headers["X-API-Key"]).toBe("ak-test");
  });

  // ─── batchCheck ──────────────────────────────────────────────────

  it("batchCheck — 결과 파싱", async () => {
    mockFetch(200, BATCH_RESPONSE);

    const result = await client.batchCheck(["메시지1", "메시지2"], { channel: "SMS" });

    expect(result.results).toHaveLength(2);
    expect(result.summary.total).toBe(2);
    expect(result.summary.compliant).toBe(1);
    expect(result.summary.violated).toBe(1);
  });

  it("batchCheck — 빈 배열 예외", async () => {
    await expect(client.batchCheck([], { channel: "SMS" })).rejects.toThrow(
      "1개 이상"
    );
  });

  it("batchCheck — 101건 예외", async () => {
    await expect(
      client.batchCheck(new Array(101).fill("msg"), { channel: "SMS" })
    ).rejects.toThrow("100건");
  });

  // ─── report ──────────────────────────────────────────────────────

  it("report — 법령·판례 포함 파싱", async () => {
    mockFetch(200, REPORT_RESPONSE);

    const report = await client.report("테스트 메시지", { channel: "SMS" });

    expect(report.relatedLaws).toHaveLength(1);
    expect(report.relatedLaws[0].lawName).toBe("표시광고법");
    expect(report.similarPrecedents).toHaveLength(1);
    expect(report.similarPrecedents[0].authority).toBe("공정거래위원회");
    expect(report.llmAnalysis).toBeUndefined();
    expect(report.generatedAt).toBeInstanceOf(Date);
  });

  // ─── 에러 처리 ──────────────────────────────────────────────────

  it("401 → AuthenticationError", async () => {
    mockFetch(401, { message: "인증 실패" });

    await expect(client.check("메시지", { channel: "SMS" })).rejects.toThrow(
      AuthenticationError
    );
  });

  it("429 → RateLimitError", async () => {
    mockFetch(429, { message: "Rate limit exceeded" });

    await expect(client.check("메시지", { channel: "SMS" })).rejects.toThrow(
      RateLimitError
    );
  });

  it("400 → ValidationError", async () => {
    mockFetch(400, { message: "message는 필수입니다" });

    await expect(client.check("", { channel: "SMS" })).rejects.toThrow(
      ValidationError
    );
  });

  it("500 → ServerError", async () => {
    mockFetch(500, { message: "Internal Server Error" });

    await expect(client.check("메시지", { channel: "SMS" })).rejects.toThrow(
      ServerError
    );
  });
});

/** AdLex SDK 타입 정의 */

export type Channel = "SMS" | "KAKAO" | "EMAIL";
export type Severity = "HIGH" | "MEDIUM" | "LOW";

export interface SenderInfo {
  name?: string;
  phoneNumber?: string;
  email?: string;
}

export interface CheckOptions {
  skipRules?: string[];
  useLlm?: boolean;
}

export interface Violation {
  ruleCode: string;
  severity: Severity;
  message: string;
  legalBasis?: string;
  suggestion?: string;
}

export interface LlmAnalysis {
  analysis: string;
  citedLawCount: number;
  citedPrecedentCount: number;
}

export interface CheckResult {
  compliant: boolean;
  violationCount: number;
  violations: Violation[];
  checkedAt: Date;
  processingMs: number;
  llmAnalysis?: LlmAnalysis;
}

export interface BatchSummary {
  total: number;
  compliant: number;
  violated: number;
  processingMs: number;
}

export interface BatchCheckResult {
  results: CheckResult[];
  summary: BatchSummary;
}

export interface RelatedLaw {
  lawName: string;
  content: string;
  articleNo?: string;
  articleTitle?: string;
  sourceUrl?: string;
}

export interface SimilarPrecedent {
  authority: string;
  title: string;
  caseNo?: string;
  summary?: string;
}

export interface ReportResult {
  message: string;
  channel: Channel;
  compliant: boolean;
  violationCount: number;
  violations: Violation[];
  relatedLaws: RelatedLaw[];
  similarPrecedents: SimilarPrecedent[];
  generatedAt: Date;
  processingMs: number;
  llmAnalysis?: LlmAnalysis;
}

/** 클라이언트 생성 옵션 */
export interface AdlexClientOptions {
  apiKey: string;
  baseUrl?: string;
  timeout?: number;
}

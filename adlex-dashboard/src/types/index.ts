export interface ApiResponse<T> {
  data: T
  message?: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export type Channel = 'SMS' | 'KAKAO' | 'EMAIL'
export type Severity = 'HIGH' | 'MEDIUM' | 'LOW'

export interface CheckViolation {
  ruleCode: string
  severity: Severity
  message: string
  legalBasis: string
  suggestion?: string
}

export interface CheckResult {
  requestId: string
  compliant: boolean
  violations: CheckViolation[]
  checkedAt: string
}

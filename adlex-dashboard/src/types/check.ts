export type Channel = 'SMS' | 'KAKAO' | 'EMAIL'
export type Severity = 'HIGH' | 'MEDIUM' | 'LOW'

export interface SenderInfo {
  name?: string
  phoneNumber?: string
  email?: string
}

export interface CheckRequest {
  message: string
  channel: Channel
  sender?: SenderInfo
  scheduledAt?: string
}

export interface ViolationDto {
  ruleCode: string
  severity: Severity
  message: string
  legalBasis?: string
  suggestion?: string
}

export interface CheckResponse {
  compliant: boolean
  violationCount: number
  violations: ViolationDto[]
  checkedAt: string
  processingMs: number
}

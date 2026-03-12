import axios from 'axios'

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
})

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

apiClient.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('access_token')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  },
)

export type Channel = 'SMS' | 'KAKAO' | 'EMAIL'
export type Severity = 'HIGH' | 'MEDIUM' | 'LOW'

export interface ViolationDto {
  ruleCode: string
  severity: Severity
  message: string
  legalBasis: string | null
  suggestion: string | null
}

export interface HistoryItemResponse {
  id: number
  channel: Channel
  compliant: boolean
  violationCount: number
  processingMs: number
  checkedAt: string
}

export interface HistoryDetailResponse {
  id: number
  message: string
  channel: Channel
  compliant: boolean
  violationCount: number
  violations: ViolationDto[]
  processingMs: number
  checkedAt: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface HistoryFilter {
  channel?: Channel
  compliant?: boolean
  from?: string
  to?: string
  page?: number
  size?: number
}

export function fetchHistory(filter: HistoryFilter = {}): Promise<PageResponse<HistoryItemResponse>> {
  const params = new URLSearchParams()
  if (filter.channel)              params.set('channel', filter.channel)
  if (filter.compliant !== undefined) params.set('compliant', String(filter.compliant))
  if (filter.from)                 params.set('from', filter.from)
  if (filter.to)                   params.set('to', filter.to)
  params.set('page', String(filter.page ?? 0))
  params.set('size', String(filter.size ?? 20))
  return apiClient.get<PageResponse<HistoryItemResponse>>(`/history?${params}`).then((r) => r.data)
}

export function fetchHistoryDetail(id: number): Promise<HistoryDetailResponse> {
  return apiClient.get<HistoryDetailResponse>(`/history/${id}`).then((r) => r.data)
}

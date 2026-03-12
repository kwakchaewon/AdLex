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

export type RuleType = 'REGEX' | 'KEYWORD' | 'TIME_RANGE' | 'FIELD_PRESENT' | 'LLM_JUDGE'
export type Severity = 'HIGH' | 'MEDIUM' | 'LOW'
export type Channel = 'SMS' | 'KAKAO' | 'EMAIL'

export interface RuleResponse {
  id: number
  code: string
  name: string
  description?: string
  type: RuleType
  channel: string
  channels: Channel[]
  severity: Severity
  pattern?: string
  config?: string
  legalBasis?: string
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateRuleRequest {
  code: string
  name: string
  description?: string
  type: RuleType
  channel: string
  severity: Severity
  pattern?: string
  config?: string
  legalBasis?: string
  active?: boolean
}

export interface UpdateRuleRequest {
  name?: string
  description?: string
  type?: RuleType
  channel?: string
  severity?: Severity
  pattern?: string
  config?: string
  legalBasis?: string
  active?: boolean
}

export const fetchRules = (): Promise<RuleResponse[]> =>
  apiClient.get<RuleResponse[]>('/rules').then((r) => r.data)

export const createRule = (req: CreateRuleRequest): Promise<RuleResponse> =>
  apiClient.post<RuleResponse>('/rules', req).then((r) => r.data)

export const updateRule = (id: number, req: UpdateRuleRequest): Promise<RuleResponse> =>
  apiClient.put<RuleResponse>(`/rules/${id}`, req).then((r) => r.data)

export const deleteRule = (id: number): Promise<void> =>
  apiClient.delete(`/rules/${id}`).then(() => undefined)

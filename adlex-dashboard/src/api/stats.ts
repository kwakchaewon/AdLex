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

export interface StatsSummary {
  totalChecks: number
  compliantChecks: number
  violationChecks: number
  complianceRate: number
  days: number
}

export interface DailyStat {
  date: string
  total: number
  compliant: number
  violations: number
}

export interface ChannelStat {
  channel: string
  total: number
  compliant: number
  complianceRate: number
}

export interface StatsResponse {
  summary: StatsSummary
  daily: DailyStat[]
  channels: ChannelStat[]
}

export function fetchStats(days: number): Promise<StatsResponse> {
  return apiClient.get<StatsResponse>(`/stats?days=${days}`).then((r) => r.data)
}

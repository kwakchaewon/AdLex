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

export interface MeResponse {
  id: number
  email: string
  companyName: string | null
  plan: string
  monthlyQuota: number
  monthlyUsed: number
  createdAt: string
}

export const getMe = (): Promise<MeResponse> =>
  apiClient.get<MeResponse>('/users/me').then((r) => r.data)

export const updateMe = (companyName: string | null): Promise<MeResponse> =>
  apiClient.put<MeResponse>('/users/me', { companyName }).then((r) => r.data)

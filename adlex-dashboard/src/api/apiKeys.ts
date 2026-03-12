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

export interface ApiKeyResponse {
  id: number
  name: string
  keyPrefix: string
  status: 'ACTIVE' | 'REVOKED'
  lastUsedAt: string | null
  createdAt: string
}

export interface CreateApiKeyResponse extends ApiKeyResponse {
  key: string
}

export const fetchKeys = (): Promise<ApiKeyResponse[]> =>
  apiClient.get<ApiKeyResponse[]>('/keys').then((r) => r.data)

export const createKey = (name: string): Promise<CreateApiKeyResponse> =>
  apiClient.post<CreateApiKeyResponse>('/keys', { name }).then((r) => r.data)

export const revokeKey = (id: number): Promise<void> =>
  apiClient.delete(`/keys/${id}`).then(() => undefined)

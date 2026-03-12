import axios from 'axios'

export type Plan = 'FREE' | 'STARTER' | 'PRO' | 'ENTERPRISE'
export type SubscriptionStatus = 'PENDING' | 'ACTIVE' | 'CANCELLED' | 'EXPIRED'

export interface SubscriptionStatusResponse {
  subscriptionId: number | null
  plan: Plan
  status: SubscriptionStatus | null
  currentPeriodEnd: string | null
  monthlyQuota: number
  monthlyUsed: number
}

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 15000,
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

export const fetchBillingStatus = (): Promise<SubscriptionStatusResponse> =>
  apiClient.get<SubscriptionStatusResponse>('/billing/status').then((r) => r.data)

export const subscribePlan = (plan: Plan, customerUid: string): Promise<SubscriptionStatusResponse> =>
  apiClient.post<SubscriptionStatusResponse>('/billing/subscribe', { plan, customerUid }).then((r) => r.data)

export const confirmPayment = (impUid: string, merchantUid: string): Promise<SubscriptionStatusResponse> =>
  apiClient.post<SubscriptionStatusResponse>('/billing/confirm', { impUid, merchantUid }).then((r) => r.data)

export const cancelSubscription = (): Promise<SubscriptionStatusResponse> =>
  apiClient.post<SubscriptionStatusResponse>('/billing/cancel').then((r) => r.data)

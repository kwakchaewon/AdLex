import { ref } from 'vue'
import {
  fetchBillingStatus,
  subscribePlan,
  confirmPayment,
  cancelSubscription,
  type Plan,
  type SubscriptionStatusResponse,
} from '@/api/billing'

declare global {
  interface Window {
    IMP?: {
      init: (merchantId: string) => void
      request_pay: (params: Record<string, unknown>, callback: (rsp: { success: boolean; imp_uid?: string; merchant_uid?: string; error_msg?: string }) => void) => void
    }
  }
}

const PORTONE_MERCHANT_ID = import.meta.env.VITE_PORTONE_MERCHANT_ID ?? 'imp00000000'

export function usePayment() {
  const status = ref<SubscriptionStatusResponse | null>(null)
  const loading = ref(false)
  const error   = ref('')

  async function loadStatus() {
    loading.value = true
    error.value   = ''
    try {
      status.value = await fetchBillingStatus()
    } catch {
      error.value = '구독 상태를 불러오지 못했습니다.'
    } finally {
      loading.value = false
    }
  }

  async function startPayment(plan: Plan): Promise<boolean> {
    if (plan === 'FREE') {
      loading.value = true
      error.value   = ''
      try {
        status.value = await subscribePlan('FREE', '')
        return true
      } catch {
        error.value = '무료 플랜 전환에 실패했습니다.'
        return false
      } finally {
        loading.value = false
      }
    }

    const merchantUid = `adlex_order_${Date.now()}`
    loading.value = true
    error.value   = ''

    // Subscribe (PENDING)
    try {
      await subscribePlan(plan, '')
    } catch {
      error.value = '구독 요청에 실패했습니다.'
      loading.value = false
      return false
    }

    // PortOne 결제창 호출
    return new Promise((resolve) => {
      const imp = window.IMP
      if (!imp) {
        error.value = 'PortOne SDK가 로드되지 않았습니다. 페이지를 새로고침해주세요.'
        loading.value = false
        resolve(false)
        return
      }

      imp.init(PORTONE_MERCHANT_ID)
      imp.request_pay(
        {
          pg:           'html5_inicis',
          pay_method:   'card',
          merchant_uid: merchantUid,
          name:         `AdLex ${plan} 플랜`,
          amount:       PLAN_PRICES[plan],
          buyer_email:  '',
          buyer_name:   'AdLex User',
        },
        async (rsp) => {
          if (!rsp.success) {
            error.value   = rsp.error_msg ?? '결제가 취소되었습니다.'
            loading.value = false
            resolve(false)
            return
          }
          try {
            status.value  = await confirmPayment(rsp.imp_uid!, merchantUid)
            loading.value = false
            resolve(true)
          } catch {
            error.value   = '결제 확인에 실패했습니다.'
            loading.value = false
            resolve(false)
          }
        },
      )
    })
  }

  async function cancel(): Promise<boolean> {
    loading.value = true
    error.value   = ''
    try {
      status.value = await cancelSubscription()
      return true
    } catch {
      error.value = '구독 취소에 실패했습니다.'
      return false
    } finally {
      loading.value = false
    }
  }

  return { status, loading, error, loadStatus, startPayment, cancel }
}

export const PLAN_INFO: Record<Plan, { name: string; price: number; quota: number | string; color: string; popular?: boolean }> = {
  FREE:       { name: '무료',        price: 0,       quota: 100,      color: '#6b7280' },
  STARTER:    { name: '스타터',      price: 29000,   quota: 1000,     color: '#3b82f6' },
  PRO:        { name: '프로',        price: 99000,   quota: 10000,    color: '#8b5cf6', popular: true },
  ENTERPRISE: { name: '엔터프라이즈', price: 299000, quota: '무제한', color: '#f59e0b' },
}

const PLAN_PRICES: Record<Plan, number> = {
  FREE: 0, STARTER: 29000, PRO: 99000, ENTERPRISE: 299000,
}

<template>
  <div class="settings">

    <!-- Tab bar -->
    <div class="tab-bar">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        class="tab-btn"
        :class="{ active: activeTab === tab.id }"
        @click="activeTab = tab.id"
        type="button"
      >{{ tab.label }}</button>
      <div class="tab-indicator" :style="indicatorStyle" />
    </div>

    <!-- 플랜 & 결제 tab -->
    <div v-if="activeTab === 'billing'" class="tab-content fade-in">

      <!-- Error banner -->
      <div v-if="error" class="error-banner">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
        {{ error }}
        <button class="error-close" @click="error = ''" type="button">×</button>
      </div>

      <!-- Current subscription card -->
      <div class="card status-card">
        <div class="card-header">
          <h2 class="card-title">현재 구독</h2>
          <div v-if="status" class="status-badges">
            <span class="plan-badge" :style="{ background: planInfo[status.plan].color + '18', color: planInfo[status.plan].color, borderColor: planInfo[status.plan].color + '40' }">
              {{ planInfo[status.plan].name }}
            </span>
            <span v-if="status.status" class="sub-status-badge" :class="`sub-${status.status.toLowerCase()}`">
              {{ statusLabel(status.status) }}
            </span>
          </div>
        </div>

        <div v-if="loading && !status" class="skeleton-wrap">
          <div class="skel w40" /><div class="skel w60" /><div class="skel h8 w100" />
        </div>

        <div v-if="status" class="status-body">
          <div class="status-row">
            <div class="status-meta">
              <span class="meta-label">월 검사 한도</span>
              <span class="meta-value mono">{{ status.monthlyUsed.toLocaleString() }} / {{ status.monthlyQuota === 2147483647 ? '무제한' : status.monthlyQuota.toLocaleString() }}</span>
            </div>
            <div v-if="status.currentPeriodEnd" class="status-meta">
              <span class="meta-label">{{ status.status === 'ACTIVE' ? '다음 갱신일' : '만료일' }}</span>
              <span class="meta-value">{{ fmtDate(status.currentPeriodEnd) }}</span>
            </div>
          </div>

          <!-- Usage progress bar -->
          <div class="usage-bar-wrap">
            <div class="usage-bar-bg">
              <div
                class="usage-bar-fill"
                :class="usageClass"
                :style="{ width: Math.min(usagePct, 100) + '%' }"
              />
            </div>
            <span class="usage-pct mono" :class="usageClass">{{ usagePct }}%</span>
          </div>

          <!-- Cancel confirm -->
          <div v-if="showCancelConfirm" class="cancel-confirm">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#f59e0b" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
            <span>구독을 취소하면 <strong>즉시 무료 플랜으로 전환</strong>됩니다. 계속하시겠습니까?</span>
            <div class="confirm-actions">
              <button class="btn-cancel-confirm" @click="doCancelSubscription" :disabled="loading" type="button">
                <span v-if="loading" class="spinner-sm" />
                {{ loading ? '처리 중...' : '취소 확인' }}
              </button>
              <button class="btn-cancel-abort" @click="showCancelConfirm = false" type="button">되돌아가기</button>
            </div>
          </div>

          <button
            v-if="status.status === 'ACTIVE' && !showCancelConfirm"
            class="btn-cancel-sub"
            @click="showCancelConfirm = true"
            type="button"
          >구독 취소</button>
        </div>
      </div>

      <!-- Plan cards -->
      <div class="plans-grid">
        <div
          v-for="plan in planKeys"
          :key="plan"
          class="plan-card"
          :class="{
            'plan-current': status?.plan === plan,
            'plan-pro': planInfo[plan].popular,
          }"
        >
          <div v-if="planInfo[plan].popular" class="popular-badge">추천</div>
          <div v-if="status?.plan === plan" class="current-badge">현재 플랜</div>

          <div class="plan-color-bar" :style="{ background: planInfo[plan].color }" />

          <div class="plan-header">
            <span class="plan-name" :style="{ color: planInfo[plan].color }">{{ planInfo[plan].name }}</span>
            <div class="plan-price">
              <span class="price-num">{{ planInfo[plan].price === 0 ? '무료' : '₩' + planInfo[plan].price.toLocaleString() }}</span>
              <span v-if="planInfo[plan].price > 0" class="price-period">/월</span>
            </div>
          </div>

          <div class="plan-quota">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
            월 <strong>{{ typeof planInfo[plan].quota === 'number' ? planInfo[plan].quota.toLocaleString() : planInfo[plan].quota }}</strong>건 검사
          </div>

          <button
            class="plan-btn"
            :class="{ 'plan-btn-current': status?.plan === plan, 'plan-btn-pro': planInfo[plan].popular && status?.plan !== plan }"
            :disabled="status?.plan === plan || loading"
            @click="handlePlanSelect(plan)"
            type="button"
          >
            <span v-if="loading && selectedPlan === plan" class="spinner-sm" />
            <template v-else>
              {{ status?.plan === plan ? '현재 플랜' : plan === 'FREE' ? '무료로 전환' : '결제하기' }}
            </template>
          </button>
        </div>
      </div>

    </div>

    <!-- 계정 tab -->
    <div v-else-if="activeTab === 'account'" class="tab-content fade-in">

      <!-- Profile success -->
      <div v-if="profileSuccess" class="success-banner">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
        프로필이 저장되었습니다.
        <button class="error-close" @click="profileSuccess = false" type="button">×</button>
      </div>
      <div v-if="profileError" class="error-banner">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
        {{ profileError }}
        <button class="error-close" @click="profileError = ''" type="button">×</button>
      </div>

      <!-- Profile card -->
      <div class="card">
        <div class="card-header">
          <h2 class="card-title">프로필</h2>
        </div>
        <div v-if="profileLoading && !profile" class="skeleton-wrap">
          <div class="skel w60" /><div class="skel w40" />
        </div>
        <form v-else @submit.prevent="saveProfile" class="profile-form">
          <div class="form-row">
            <label class="form-label">이메일</label>
            <input class="field-input" :value="profile?.email" disabled />
            <span class="form-hint">이메일은 변경할 수 없습니다</span>
          </div>
          <div class="form-row">
            <label class="form-label">회사명</label>
            <input v-model="companyNameInput" class="field-input" placeholder="회사명 (선택사항)" maxlength="100" />
          </div>
          <div class="form-row">
            <label class="form-label">가입일</label>
            <input class="field-input" :value="profile ? fmtDate(profile.createdAt) : ''" disabled />
          </div>
          <div class="form-actions">
            <button class="btn-save" :disabled="profileLoading" type="submit">
              <span v-if="profileLoading" class="spinner-sm" />
              {{ profileLoading ? '저장 중...' : '저장' }}
            </button>
          </div>
        </form>
      </div>

      <!-- Password card -->
      <div class="card">
        <div class="card-header">
          <h2 class="card-title">비밀번호 변경</h2>
        </div>
        <div class="coming-inline">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
          <span>비밀번호 변경 기능은 곧 제공될 예정입니다.</span>
        </div>
      </div>

    </div>

    <!-- Coming soon tabs (보안) -->
    <div v-else class="tab-content fade-in coming-soon">
      <div class="coming-icon">
        <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
      </div>
      <p class="coming-title">준비 중</p>
      <p class="coming-desc">이 기능은 곧 제공될 예정입니다.</p>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { usePayment, PLAN_INFO } from '@/composables/usePayment'
import type { Plan, SubscriptionStatus } from '@/api/billing'
import { getMe, updateMe, type MeResponse } from '@/api/user'

const { status, loading, error, loadStatus, startPayment, cancel } = usePayment()

// ── 계정 탭 state ──
const profile        = ref<MeResponse | null>(null)
const companyNameInput = ref('')
const profileLoading = ref(false)
const profileError   = ref('')
const profileSuccess = ref(false)

async function loadProfile() {
  profileLoading.value = true
  try {
    profile.value = await getMe()
    companyNameInput.value = profile.value.companyName ?? ''
  } catch {
    profileError.value = '프로필을 불러오지 못했습니다.'
  } finally {
    profileLoading.value = false
  }
}

async function saveProfile() {
  profileLoading.value = true
  profileError.value   = ''
  profileSuccess.value = false
  try {
    profile.value = await updateMe(companyNameInput.value.trim() || null)
    profileSuccess.value = true
  } catch {
    profileError.value = '저장에 실패했습니다. 다시 시도해주세요.'
  } finally {
    profileLoading.value = false
  }
}

const activeTab     = ref('billing')
const showCancelConfirm = ref(false)
const selectedPlan  = ref<Plan | null>(null)

const tabs = [
  { id: 'billing',  label: '플랜 & 결제' },
  { id: 'account',  label: '계정' },
  { id: 'security', label: '보안' },
]

const planInfo = PLAN_INFO
const planKeys = Object.keys(PLAN_INFO) as Plan[]

const tabIndex = computed(() => tabs.findIndex((t) => t.id === activeTab.value))
const indicatorStyle = computed(() => ({
  transform: `translateX(${tabIndex.value * 100}%)`,
  width: `${100 / tabs.length}%`,
}))

const usagePct = computed(() => {
  if (!status.value) return 0
  const { monthlyUsed, monthlyQuota } = status.value
  if (monthlyQuota >= 2147483647) return 0
  return Math.round((monthlyUsed / monthlyQuota) * 100)
})

const usageClass = computed(() => {
  if (usagePct.value >= 100) return 'over'
  if (usagePct.value >= 80)  return 'warn'
  return ''
})

function statusLabel(s: SubscriptionStatus): string {
  return { ACTIVE: '활성', PENDING: '처리 중', CANCELLED: '취소됨', EXPIRED: '만료됨' }[s] ?? s
}

function fmtDate(iso: string) {
  return new Date(iso).toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' })
}

async function handlePlanSelect(plan: Plan) {
  selectedPlan.value = plan
  const ok = await startPayment(plan)
  if (ok) selectedPlan.value = null
}

async function doCancelSubscription() {
  const ok = await cancel()
  if (ok) showCancelConfirm.value = false
}

onMounted(() => { loadStatus(); loadProfile() })
</script>

<style scoped>
.settings {
  font-family: 'DM Sans', sans-serif;
  padding: 1.5rem 2rem;
  background: #f8f9fc;
  min-height: calc(100vh - 60px);
  max-width: 900px;
}

/* ── Tab bar ── */
.tab-bar {
  position: relative;
  display: flex;
  background: #fff;
  border: 1px solid #e9ecf3;
  border-radius: 10px;
  padding: 4px;
  margin-bottom: 1.5rem;
  overflow: hidden;
  width: fit-content;
}
.tab-btn {
  position: relative;
  z-index: 1;
  padding: .5rem 1.25rem;
  background: none;
  border: none;
  font-family: 'DM Sans', sans-serif;
  font-size: .85rem;
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  border-radius: 7px;
  transition: color .2s;
  white-space: nowrap;
}
.tab-btn.active { color: #0d1530; font-weight: 600; }
.tab-indicator {
  position: absolute;
  top: 4px; bottom: 4px; left: 4px;
  background: #f1f5f9;
  border-radius: 7px;
  transition: transform .25s cubic-bezier(.4,0,.2,1);
  pointer-events: none;
}

/* ── Tab content ── */
.tab-content { display: flex; flex-direction: column; gap: 1.25rem; }
.fade-in { animation: fadeUp .25s ease both; }
@keyframes fadeUp { from { opacity: 0; transform: translateY(6px); } to { opacity: 1; transform: translateY(0); } }

/* ── Error banner ── */
.error-banner {
  display: flex; align-items: center; gap: .625rem;
  padding: .75rem 1rem; background: #fef2f2; border: 1px solid #fecaca;
  border-radius: 9px; font-size: .825rem; color: #dc2626;
}
.error-close { margin-left: auto; background: none; border: none; cursor: pointer; font-size: 1.1rem; color: #dc2626; line-height: 1; padding: 0 2px; }

/* ── Cards ── */
.card {
  background: #fff;
  border: 1px solid #e9ecf3;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0,0,0,.04);
  padding: 1.5rem;
}
.card-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1rem; }
.card-title { font-family: 'Syne', sans-serif; font-size: 1rem; font-weight: 600; color: #0d1530; margin: 0; }
.status-badges { display: flex; align-items: center; gap: .5rem; }
.plan-badge {
  font-family: 'DM Mono', monospace; font-size: .72rem; font-weight: 500;
  border: 1.5px solid; border-radius: 100px; padding: 2px 10px; letter-spacing: .04em;
}
.sub-status-badge {
  font-family: 'DM Mono', monospace; font-size: .7rem; font-weight: 500;
  border-radius: 100px; padding: 2px 8px;
}
.sub-active    { background: #f0fdf4; color: #16a34a; }
.sub-pending   { background: #fffbeb; color: #d97706; }
.sub-cancelled, .sub-expired { background: #fef2f2; color: #dc2626; }

/* ── Skeleton ── */
.skeleton-wrap { display: flex; flex-direction: column; gap: .625rem; animation: pulse 1.5s ease infinite; }
@keyframes pulse { 0%,100% { opacity: 1 } 50% { opacity: .4 } }
.skel { background: #e9ecf3; border-radius: 6px; height: 14px; }
.skel.h8 { height: 8px; }
.skel.w40 { width: 40%; }
.skel.w60 { width: 60%; }
.skel.w100 { width: 100%; }

/* ── Status body ── */
.status-body { display: flex; flex-direction: column; gap: 1rem; }
.status-row { display: flex; gap: 2rem; flex-wrap: wrap; }
.status-meta { display: flex; flex-direction: column; gap: .2rem; }
.meta-label { font-size: .72rem; font-weight: 600; color: #9ca3af; text-transform: uppercase; letter-spacing: .06em; }
.meta-value { font-size: .875rem; font-weight: 500; color: #0d1530; }
.meta-value.mono { font-family: 'DM Mono', monospace; }

.usage-bar-wrap { display: flex; align-items: center; gap: .75rem; }
.usage-bar-bg { flex: 1; height: 6px; background: #e9ecf3; border-radius: 100px; overflow: hidden; }
.usage-bar-fill { height: 100%; background: #3b82f6; border-radius: 100px; transition: width .4s ease; }
.usage-bar-fill.warn { background: #f59e0b; }
.usage-bar-fill.over { background: #ef4444; }
.usage-pct { font-family: 'DM Mono', monospace; font-size: .72rem; color: #6b7280; width: 36px; text-align: right; }
.usage-pct.warn { color: #d97706; }
.usage-pct.over { color: #ef4444; }

.cancel-confirm {
  display: flex; align-items: flex-start; gap: .75rem; flex-wrap: wrap;
  padding: .875rem 1rem; background: #fffbeb; border: 1px solid #fde68a; border-radius: 9px;
  font-size: .825rem; color: #92400e;
}
.confirm-actions { display: flex; gap: .5rem; margin-left: auto; flex-shrink: 0; }
.btn-cancel-confirm {
  display: flex; align-items: center; gap: .4rem;
  padding: .35rem .875rem; background: #ef4444; color: #fff; border: none; border-radius: 7px;
  font-family: 'DM Sans', sans-serif; font-size: .8rem; font-weight: 600; cursor: pointer;
  transition: background .15s;
}
.btn-cancel-confirm:hover:not(:disabled) { background: #dc2626; }
.btn-cancel-confirm:disabled { opacity: .6; cursor: not-allowed; }
.btn-cancel-abort {
  padding: .35rem .875rem; background: none; border: 1.5px solid #e9ecf3; border-radius: 7px;
  font-family: 'DM Sans', sans-serif; font-size: .8rem; font-weight: 500; color: #6b7280; cursor: pointer;
  transition: border-color .15s, color .15s;
}
.btn-cancel-abort:hover { border-color: #9ca3af; color: #374151; }

.btn-cancel-sub {
  align-self: flex-start; padding: .35rem .875rem; background: none;
  border: 1.5px solid #fca5a5; color: #dc2626; border-radius: 7px;
  font-family: 'DM Sans', sans-serif; font-size: .8rem; font-weight: 500; cursor: pointer;
  transition: background .15s, border-color .15s;
}
.btn-cancel-sub:hover { background: #fef2f2; border-color: #ef4444; }

/* ── Plans grid ── */
.plans-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
}

.plan-card {
  position: relative;
  background: #fff;
  border: 2px solid #e9ecf3;
  border-radius: 12px;
  padding: 1.25rem 1.25rem 1rem;
  display: flex;
  flex-direction: column;
  gap: .75rem;
  transition: border-color .2s, box-shadow .2s, transform .15s;
  overflow: hidden;
}
.plan-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,.07); }
.plan-card.plan-current { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,.1); }
.plan-card.plan-pro     { border-color: #8b5cf6; box-shadow: 0 0 0 3px rgba(139,92,246,.08); }

.plan-color-bar { position: absolute; top: 0; left: 0; right: 0; height: 3px; border-radius: 2px 2px 0 0; }

.popular-badge {
  position: absolute; top: .625rem; right: .625rem;
  font-family: 'DM Mono', monospace; font-size: .65rem; font-weight: 500; letter-spacing: .06em;
  background: #ede9fe; color: #7c3aed; border-radius: 100px; padding: 2px 8px;
}
.current-badge {
  position: absolute; top: .625rem; right: .625rem;
  font-family: 'DM Mono', monospace; font-size: .65rem; font-weight: 500; letter-spacing: .06em;
  background: #eff6ff; color: #1d4ed8; border-radius: 100px; padding: 2px 8px;
}

.plan-header { display: flex; flex-direction: column; gap: .2rem; margin-top: .25rem; }
.plan-name { font-family: 'Syne', sans-serif; font-size: .85rem; font-weight: 700; letter-spacing: .02em; }
.plan-price { display: flex; align-items: baseline; gap: .25rem; }
.price-num { font-family: 'DM Sans', sans-serif; font-size: 1.5rem; font-weight: 700; color: #0d1530; font-variant-numeric: tabular-nums; }
.price-period { font-size: .72rem; color: #9ca3af; }

.plan-quota {
  display: flex; align-items: center; gap: .35rem;
  font-size: .78rem; color: #6b7280;
}
.plan-quota svg { color: #22c55e; flex-shrink: 0; }
.plan-quota strong { color: #374151; }

.plan-btn {
  margin-top: auto;
  display: flex; align-items: center; justify-content: center; gap: .4rem;
  width: 100%; padding: .5rem;
  background: #f1f5f9; color: #374151;
  border: none; border-radius: 8px;
  font-family: 'DM Sans', sans-serif; font-size: .8rem; font-weight: 600;
  cursor: pointer; transition: background .15s, color .15s;
}
.plan-btn:hover:not(:disabled) { background: #e2e8f0; }
.plan-btn.plan-btn-current { background: #eff6ff; color: #1d4ed8; cursor: default; }
.plan-btn.plan-btn-pro { background: #8b5cf6; color: #fff; }
.plan-btn.plan-btn-pro:hover:not(:disabled) { background: #7c3aed; }
.plan-btn:disabled { opacity: .6; cursor: not-allowed; }

/* ── Spinners ── */
.spinner-sm {
  width: 13px; height: 13px;
  border: 2px solid rgba(255,255,255,.35); border-top-color: #fff;
  border-radius: 50%; animation: spin .7s linear infinite; flex-shrink: 0;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ── Coming soon ── */
.coming-soon {
  align-items: center; justify-content: center;
  min-height: 280px; gap: .75rem;
  background: #fff; border: 1px solid #e9ecf3; border-radius: 12px;
  color: #9ca3af; text-align: center;
}
.coming-icon { opacity: .3; }
.coming-title { font-family: 'Syne', sans-serif; font-size: .95rem; font-weight: 600; color: #9ca3af; margin: 0; }
.coming-desc  { font-size: .8rem; color: #d1d5db; margin: 0; }

/* ── Profile form ── */
.profile-form { display: flex; flex-direction: column; gap: .875rem; }
.form-row { display: flex; flex-direction: column; gap: .35rem; }
.form-label { font-size: .775rem; font-weight: 600; color: #374151; }
.form-hint  { font-size: .72rem; color: #9ca3af; }
.form-actions { display: flex; justify-content: flex-end; padding-top: .25rem; }

.btn-save {
  display: flex; align-items: center; gap: .4rem;
  padding: .5rem 1.25rem; background: #3b82f6; color: #fff;
  border: none; border-radius: 8px;
  font-family: 'DM Sans', sans-serif; font-size: .85rem; font-weight: 600;
  cursor: pointer; transition: background .15s, box-shadow .15s;
}
.btn-save:hover:not(:disabled) { background: #2563eb; box-shadow: 0 4px 12px rgba(59,130,246,.3); }
.btn-save:disabled { opacity: .6; cursor: not-allowed; }

.success-banner {
  display: flex; align-items: center; gap: .625rem;
  padding: .75rem 1rem; background: #f0fdf4; border: 1px solid #bbf7d0;
  border-radius: 9px; font-size: .825rem; color: #16a34a;
}

.coming-inline {
  display: flex; align-items: center; gap: .5rem;
  font-size: .825rem; color: #9ca3af; padding: .25rem 0;
}

.mono { font-family: 'DM Mono', monospace; }

@media (max-width: 900px) {
  .plans-grid { grid-template-columns: repeat(2, 1fr); }
  .settings   { padding: 1rem; }
}
@media (max-width: 560px) {
  .plans-grid { grid-template-columns: 1fr; }
}
</style>

<template>
  <div class="auth-root">
    <div class="brand-panel">
      <div class="grid-overlay" aria-hidden="true" />
      <div class="brand-content">
        <div class="logo-block">
          <span class="logo-mark">A</span>
          <span class="logo-text">ADLEX</span>
        </div>
        <p class="brand-tagline">광고 법규 준수,<br />자동화하세요.</p>
        <ul class="feature-list">
          <li v-for="f in features" :key="f.label" class="feature-item">
            <span class="feature-icon" aria-hidden="true">{{ f.icon }}</span>
            <span>{{ f.label }}</span>
          </li>
        </ul>
        <div class="brand-badge">정보통신망법 · 표시광고법 준수</div>
      </div>
      <div class="geo-decoration" aria-hidden="true">
        <svg viewBox="0 0 400 400" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="200" cy="200" r="160" stroke="rgba(59,130,246,0.12)" stroke-width="1"/>
          <circle cx="200" cy="200" r="120" stroke="rgba(59,130,246,0.08)" stroke-width="1"/>
          <circle cx="200" cy="200" r="80" stroke="rgba(59,130,246,0.06)" stroke-width="1"/>
          <path d="M200 40 L360 280 L40 280 Z" stroke="rgba(59,130,246,0.1)" stroke-width="1" fill="none"/>
          <path d="M200 100 L320 300 L80 300 Z" stroke="rgba(59,130,246,0.07)" stroke-width="1" fill="none"/>
          <line x1="40" y1="200" x2="360" y2="200" stroke="rgba(59,130,246,0.06)" stroke-width="1"/>
          <line x1="200" y1="40" x2="200" y2="360" stroke="rgba(59,130,246,0.06)" stroke-width="1"/>
        </svg>
      </div>
    </div>

    <div class="form-panel">
      <div class="form-card">
        <div class="mobile-logo">
          <span class="logo-mark-sm">A</span>
          <span class="logo-text-sm">ADLEX</span>
        </div>

        <div class="tab-row" role="tablist">
          <button role="tab" :aria-selected="activeTab === 'login'"
            :class="['tab-btn', { active: activeTab === 'login' }]"
            @click="switchTab('login')">로그인</button>
          <button role="tab" :aria-selected="activeTab === 'register'"
            :class="['tab-btn', { active: activeTab === 'register' }]"
            @click="switchTab('register')">회원가입</button>
          <div class="tab-indicator" :style="{ transform: `translateX(${activeTab === 'login' ? '0%' : '100%'})` }" />
        </div>

        <Transition name="err-fade">
          <div v-if="error" class="error-box" role="alert">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
              <path d="M8 1a7 7 0 100 14A7 7 0 008 1zm-.75 3.75a.75.75 0 011.5 0v3.5a.75.75 0 01-1.5 0v-3.5zm.75 7a.875.875 0 110-1.75.875.875 0 010 1.75z"/>
            </svg>
            {{ error }}
          </div>
        </Transition>

        <Transition name="slide-fade" mode="out-in">
          <form v-if="activeTab === 'login'" key="login" class="auth-form" @submit.prevent="handleLogin" novalidate>
            <div class="field-group">
              <label class="field-label" for="login-email">이메일</label>
              <input id="login-email" v-model="loginForm.email" type="email"
                class="field-input" placeholder="you@company.com" autocomplete="email" required />
            </div>
            <div class="field-group">
              <label class="field-label" for="login-password">비밀번호</label>
              <div class="input-wrapper">
                <input id="login-password" v-model="loginForm.password"
                  :type="showPassword ? 'text' : 'password'"
                  class="field-input" placeholder="••••••••" autocomplete="current-password" required />
                <button type="button" class="toggle-pw" @click="showPassword = !showPassword"
                  :aria-label="showPassword ? '비밀번호 숨기기' : '비밀번호 보기'">
                  <svg v-if="!showPassword" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
                  </svg>
                  <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94"/>
                    <path d="M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19"/>
                    <line x1="1" y1="1" x2="23" y2="23"/>
                  </svg>
                </button>
              </div>
            </div>
            <button type="submit" class="submit-btn" :disabled="loading">
              <span v-if="!loading">로그인</span>
              <span v-else class="spinner" aria-label="로그인 중" />
            </button>
          </form>

          <form v-else key="register" class="auth-form" @submit.prevent="handleRegister" novalidate>
            <div class="field-group">
              <label class="field-label" for="reg-email">이메일</label>
              <input id="reg-email" v-model="registerForm.email" type="email"
                class="field-input" placeholder="you@company.com" autocomplete="email" required />
            </div>
            <div class="field-group">
              <label class="field-label" for="reg-company">
                회사명 <span class="optional">(선택)</span>
              </label>
              <input id="reg-company" v-model="registerForm.companyName" type="text"
                class="field-input" placeholder="(주)애드렉스" autocomplete="organization" />
            </div>
            <div class="field-group">
              <label class="field-label" for="reg-password">
                비밀번호 <span class="optional">(8자 이상)</span>
              </label>
              <div class="input-wrapper">
                <input id="reg-password" v-model="registerForm.password"
                  :type="showRegPassword ? 'text' : 'password'"
                  class="field-input" placeholder="••••••••" autocomplete="new-password" minlength="8" required />
                <button type="button" class="toggle-pw" @click="showRegPassword = !showRegPassword"
                  :aria-label="showRegPassword ? '비밀번호 숨기기' : '비밀번호 보기'">
                  <svg v-if="!showRegPassword" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
                  </svg>
                  <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94"/>
                    <path d="M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19"/>
                    <line x1="1" y1="1" x2="23" y2="23"/>
                  </svg>
                </button>
              </div>
            </div>
            <!-- Terms Agreement -->
            <div class="terms-agree-wrap">
              <label class="agree-row">
                <input type="checkbox" v-model="termsAgreed" class="agree-check" />
                <span class="agree-text">
                  <button type="button" class="terms-open-btn" @click="openModal('terms')">[이용약관]</button>
                  에 동의합니다 <span class="req-mark">(필수)</span>
                </span>
              </label>
              <label class="agree-row">
                <input type="checkbox" v-model="privacyAgreed" class="agree-check" />
                <span class="agree-text">
                  <button type="button" class="terms-open-btn" @click="openModal('privacy')">[개인정보처리방침]</button>
                  에 동의합니다 <span class="req-mark">(필수)</span>
                </span>
              </label>
            </div>
            <button type="submit" class="submit-btn" :disabled="loading || !termsAgreed || !privacyAgreed">
              <span v-if="!loading">시작하기 — 무료</span>
              <span v-else class="spinner" aria-label="가입 중" />
            </button>
          </form>
        </Transition>
      </div>
    </div>

    <!-- Terms Modal -->
    <Transition name="modal-fade">
      <div v-if="modalOpen" class="modal-backdrop" @click.self="modalOpen = false">
        <div class="modal-box">
          <div class="modal-header">
            <h3 class="modal-title">{{ modalType === 'terms' ? '이용약관' : '개인정보처리방침' }}</h3>
            <button class="modal-close" @click="modalOpen = false">✕</button>
          </div>
          <div class="modal-body">
            <template v-if="modalType === 'terms'">
              <h4>제1조 (목적)</h4>
              <p>본 약관은 (주)이든티앤에스(이하 "회사")가 제공하는 AdLex 서비스(이하 "서비스")의 이용조건 및 절차, 회사와 이용자 간의 권리·의무 및 책임사항을 규정함을 목적으로 합니다.</p>
              <h4>제2조 (서비스 내용)</h4>
              <p>회사는 마케팅·광고 메시지의 법규 준수 자동 검사 API 서비스를 제공합니다. 서비스는 정보통신망법, 표시광고법 등 관련 법령에 따른 규칙 엔진을 기반으로 동작합니다.</p>
              <h4>제3조 (이용 계약의 성립)</h4>
              <p>이용 계약은 이용자가 본 약관에 동의하고 회원가입 신청을 완료한 후 회사가 이를 승낙함으로써 성립합니다.</p>
              <h4>제4조 (서비스 이용 요금)</h4>
              <p>무료 플랜은 월 100건 무료로 제공됩니다. 유료 플랜은 STARTER(₩29,000/월), PRO(₩99,000/월), ENTERPRISE(₩299,000/월)로 구성되며, 요금은 사전 고지 후 변경될 수 있습니다.</p>
              <h4>제5조 (서비스 중단)</h4>
              <p>회사는 시스템 점검, 장애, 천재지변 등의 사유로 서비스 제공을 일시적으로 중단할 수 있습니다. 이 경우 사전 또는 사후 공지합니다.</p>
              <h4>제6조 (금지 행위)</h4>
              <p>이용자는 서비스를 이용하여 타인의 권리를 침해하거나 법령을 위반하는 행위, API Key를 제3자에게 무단으로 제공하는 행위, 비정상적인 방법으로 서비스를 이용하는 행위를 해서는 안 됩니다.</p>
              <h4>제7조 (면책)</h4>
              <p>본 서비스는 법적 자문을 대체하지 않습니다. 서비스의 검사 결과는 참고용으로만 활용하시기 바라며, 최종 법적 판단은 전문가에게 문의하시기 바랍니다.</p>
              <h4>제8조 (준거법 및 관할)</h4>
              <p>본 약관은 대한민국 법령에 따라 해석되며, 분쟁 발생 시 서울중앙지방법원을 합의관할법원으로 합니다.</p>
              <p class="terms-date">시행일: 2026년 1월 1일</p>
            </template>
            <template v-else>
              <h4>1. 수집하는 개인정보 항목</h4>
              <p>회사는 서비스 제공을 위해 이메일 주소, 회사명(선택), 서비스 이용 기록, 접속 로그를 수집합니다.</p>
              <h4>2. 개인정보 수집 및 이용 목적</h4>
              <p>회원 관리(본인 확인, 서비스 제공), 서비스 개선 및 통계 분석, 요금 청구 및 결제 처리에 활용됩니다.</p>
              <h4>3. 개인정보 보유 및 이용 기간</h4>
              <p>서비스 이용 기간 동안 보유하며, 회원 탈퇴 시 즉시 파기합니다. 단, 관련 법령에 따라 일정 기간 보존이 필요한 정보는 해당 기간 동안 보존합니다.</p>
              <h4>4. 개인정보의 제3자 제공</h4>
              <p>회사는 이용자의 사전 동의 없이 개인정보를 제3자에게 제공하지 않습니다. 단, 법령에 의한 요구가 있는 경우는 예외로 합니다.</p>
              <h4>5. 개인정보 보호책임자</h4>
              <p>개인정보 관련 문의: privacy@adlex.io</p>
              <h4>6. 쿠키 및 자동 수집 정보</h4>
              <p>서비스는 로그인 상태 유지를 위해 localStorage에 액세스 토큰을 저장합니다. 브라우저 설정을 통해 삭제할 수 있습니다.</p>
              <p class="terms-date">시행일: 2026년 1월 1일</p>
            </template>
          </div>
          <div class="modal-footer">
            <button class="modal-agree-btn" @click="agreeFromModal">동의하고 닫기</button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import axios from 'axios'

const router = useRouter()
const authStore = useAuthStore()

const activeTab = ref<'login' | 'register'>('login')
const loading = ref(false)
const error = ref('')
const showPassword = ref(false)
const showRegPassword = ref(false)
const termsAgreed = ref(false)
const privacyAgreed = ref(false)
const modalOpen = ref(false)
const modalType = ref<'terms' | 'privacy'>('terms')

function openModal(type: 'terms' | 'privacy') {
  modalType.value = type
  modalOpen.value = true
}

function agreeFromModal() {
  if (modalType.value === 'terms') termsAgreed.value = true
  else privacyAgreed.value = true
  modalOpen.value = false
}

const loginForm = reactive({ email: '', password: '' })
const registerForm = reactive({ email: '', password: '', companyName: '' })

const features = [
  { icon: '⚖', label: '정보통신망법 · 표시광고법 자동 검사' },
  { icon: '⚡', label: '100ms 이내 실시간 판단' },
  { icon: '📋', label: '법조문 근거 기반 위반 리포트' },
]

function switchTab(tab: 'login' | 'register') {
  error.value = ''
  activeTab.value = tab
  termsAgreed.value = false
  privacyAgreed.value = false
}

async function handleLogin() {
  if (!loginForm.email || !loginForm.password) return
  error.value = ''
  loading.value = true
  try {
    const { data } = await axios.post('/api/auth/login', {
      email: loginForm.email,
      password: loginForm.password,
    })
    authStore.setToken(data.accessToken)
    localStorage.setItem('refresh_token', data.refreshToken)
    router.push('/dashboard')
  } catch (e: any) {
    error.value = e.response?.data?.error?.message ?? '로그인에 실패했습니다.'
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!registerForm.email || !registerForm.password) return
  if (registerForm.password.length < 8) {
    error.value = '비밀번호는 8자 이상이어야 합니다.'
    return
  }
  error.value = ''
  loading.value = true
  try {
    await axios.post('/api/auth/register', {
      email: registerForm.email,
      password: registerForm.password,
      companyName: registerForm.companyName || undefined,
    })
    const { data } = await axios.post('/api/auth/login', {
      email: registerForm.email,
      password: registerForm.password,
    })
    authStore.setToken(data.accessToken)
    localStorage.setItem('refresh_token', data.refreshToken)
    router.push('/dashboard')
  } catch (e: any) {
    error.value = e.response?.data?.error?.message ?? '회원가입에 실패했습니다.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>

.auth-root {
  display: flex;
  min-height: 100vh;
  background: #070d1f;
  font-family: 'DM Sans', sans-serif;
}

.brand-panel {
  position: relative;
  flex: 0 0 46%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: linear-gradient(135deg, #070d1f 0%, #0d1530 60%, #0a1828 100%);
  border-right: 1px solid rgba(59,130,246,0.12);
}

.grid-overlay {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(59,130,246,0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(59,130,246,0.04) 1px, transparent 1px);
  background-size: 40px 40px;
}

.brand-content {
  position: relative;
  z-index: 1;
  max-width: 380px;
  padding: 2.5rem;
  animation: brand-reveal 0.9s cubic-bezier(0.16,1,0.3,1) forwards;
}

@keyframes brand-reveal {
  from { opacity: 0; transform: translateY(24px); }
  to   { opacity: 1; transform: translateY(0); }
}

.logo-block { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 2.5rem; }

.logo-mark {
  display: inline-flex; align-items: center; justify-content: center;
  width: 40px; height: 40px; background: #3b82f6; color: #fff;
  font-family: 'Bebas Neue', sans-serif; font-size: 1.5rem;
  border-radius: 8px; letter-spacing: 0;
}

.logo-text {
  font-family: 'Bebas Neue', sans-serif; font-size: 2rem;
  color: #e8f0ff; letter-spacing: 0.1em;
}

.brand-tagline {
  font-family: 'Syne', sans-serif; font-size: 2.25rem; font-weight: 700;
  color: #f0f6ff; line-height: 1.2; margin: 0 0 2.5rem; letter-spacing: -0.02em;
}

.feature-list { list-style: none; padding: 0; margin: 0 0 2.5rem; display: flex; flex-direction: column; gap: 1rem; }

.feature-item {
  display: flex; align-items: flex-start; gap: 0.75rem;
  color: rgba(200,220,255,0.75); font-size: 0.9rem; line-height: 1.5;
}

.feature-icon { font-size: 1rem; line-height: 1.5; flex-shrink: 0; }

.brand-badge {
  display: inline-block; padding: 0.375rem 0.875rem;
  border: 1px solid rgba(59,130,246,0.3); border-radius: 100px;
  color: rgba(147,197,253,0.8); font-size: 0.75rem;
  letter-spacing: 0.03em; background: rgba(59,130,246,0.06);
}

.geo-decoration {
  position: absolute; right: -80px; bottom: -80px;
  width: 400px; height: 400px; opacity: 0.6; pointer-events: none;
}

.form-panel {
  flex: 1; display: flex; align-items: center; justify-content: center;
  padding: 2rem 1.5rem; background: #f4f6fb;
}

.form-card {
  width: 100%; max-width: 400px; background: #fff; border-radius: 20px;
  padding: 2.5rem 2.5rem 2rem;
  box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05), 0 20px 60px -12px rgba(7,13,31,0.15);
  animation: card-in 0.6s cubic-bezier(0.16,1,0.3,1) forwards;
}

@keyframes card-in {
  from { opacity: 0; transform: translateY(20px) scale(0.98); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}

.mobile-logo { display: none; align-items: center; gap: 0.5rem; margin-bottom: 1.75rem; }

.logo-mark-sm {
  display: inline-flex; align-items: center; justify-content: center;
  width: 32px; height: 32px; background: #3b82f6; color: #fff;
  font-family: 'Bebas Neue', sans-serif; font-size: 1.2rem; border-radius: 7px;
}

.logo-text-sm {
  font-family: 'Bebas Neue', sans-serif; font-size: 1.5rem;
  color: #0d1530; letter-spacing: 0.1em;
}

.tab-row {
  position: relative; display: flex; background: #f0f3f8;
  border-radius: 10px; padding: 3px; margin-bottom: 1.75rem;
}

.tab-btn {
  position: relative; z-index: 1; flex: 1; padding: 0.5rem 0;
  background: none; border: none; border-radius: 8px;
  font-family: 'DM Sans', sans-serif; font-size: 0.9rem; font-weight: 500;
  color: #6b7280; cursor: pointer; transition: color 0.25s;
}

.tab-btn.active { color: #0d1530; font-weight: 600; }

.tab-indicator {
  position: absolute; inset: 3px; width: calc(50% - 3px);
  background: #fff; border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.1);
  transition: transform 0.3s cubic-bezier(0.34,1.56,0.64,1);
  pointer-events: none;
}

.error-box {
  display: flex; align-items: center; gap: 0.5rem; margin-bottom: 1rem;
  padding: 0.75rem 1rem; background: #fef2f2; border: 1px solid #fecaca;
  border-radius: 10px; color: #dc2626; font-size: 0.875rem; line-height: 1.4;
}

.err-fade-enter-active, .err-fade-leave-active { transition: all 0.25s; }
.err-fade-enter-from, .err-fade-leave-to { opacity: 0; transform: translateY(-6px); }

.auth-form { display: flex; flex-direction: column; gap: 1.25rem; }
.field-group { display: flex; flex-direction: column; gap: 0.375rem; }

.field-label {
  font-size: 0.8rem; font-weight: 600; color: #374151;
  letter-spacing: 0.02em; text-transform: uppercase;
}

.optional { font-weight: 400; color: #9ca3af; text-transform: none; letter-spacing: 0; }

.input-wrapper { position: relative; }

.field-input {
  width: 100%; padding: 0.7rem 1rem; background: #f9fafb;
  border: 1.5px solid #e5e7eb; border-radius: 10px;
  font-family: 'DM Sans', sans-serif; font-size: 0.95rem; color: #111827;
  outline: none; transition: border-color 0.2s, box-shadow 0.2s, background 0.2s;
  box-sizing: border-box;
}

.input-wrapper .field-input { padding-right: 2.75rem; }

.field-input:focus {
  border-color: #3b82f6; background: #fff;
  box-shadow: 0 0 0 3px rgba(59,130,246,0.12);
}

.field-input::placeholder { color: #c4cad3; }

.toggle-pw {
  position: absolute; right: 0.75rem; top: 50%; transform: translateY(-50%);
  background: none; border: none; padding: 0; color: #9ca3af;
  cursor: pointer; display: flex; align-items: center; transition: color 0.2s;
}
.toggle-pw:hover { color: #6b7280; }

.submit-btn {
  display: flex; align-items: center; justify-content: center;
  margin-top: 0.25rem; padding: 0.8rem 1.5rem;
  background: #0d1530; color: #fff; border: none; border-radius: 10px;
  font-family: 'DM Sans', sans-serif; font-size: 0.95rem; font-weight: 600;
  cursor: pointer; transition: background 0.2s, transform 0.15s, box-shadow 0.2s;
  min-height: 48px; letter-spacing: 0.01em;
}

.submit-btn:hover:not(:disabled) {
  background: #1a2b4a;
  box-shadow: 0 4px 16px rgba(13,21,48,0.3);
  transform: translateY(-1px);
}

.submit-btn:active:not(:disabled) { transform: translateY(0); }
.submit-btn:disabled { opacity: 0.65; cursor: not-allowed; }

.spinner {
  display: inline-block; width: 18px; height: 18px;
  border: 2.5px solid rgba(255,255,255,0.3); border-top-color: #fff;
  border-radius: 50%; animation: spin 0.7s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.slide-fade-enter-active, .slide-fade-leave-active { transition: opacity 0.2s, transform 0.2s; }
.slide-fade-enter-from { opacity: 0; transform: translateX(12px); }
.slide-fade-leave-to   { opacity: 0; transform: translateX(-12px); }

/* Terms */
.terms-agree-wrap { display: flex; flex-direction: column; gap: 0.5rem; }
.agree-row { display: flex; align-items: flex-start; gap: 0.5rem; cursor: pointer; }
.agree-check { width: 15px; height: 15px; margin-top: 2px; flex-shrink: 0; accent-color: #3b82f6; cursor: pointer; }
.agree-text { font-size: 0.82rem; color: #4b5563; line-height: 1.4; }
.req-mark { color: #ef4444; font-size: 0.78rem; }
.terms-open-btn {
  background: none; border: none; padding: 0;
  color: #3b82f6; font-size: 0.82rem; font-weight: 600;
  cursor: pointer; text-decoration: underline; text-underline-offset: 2px;
  font-family: 'DM Sans', sans-serif;
}
.terms-open-btn:hover { color: #2563eb; }

/* Modal */
.modal-backdrop {
  position: fixed; inset: 0; background: rgba(7,13,31,0.6);
  backdrop-filter: blur(4px); z-index: 200;
  display: flex; align-items: center; justify-content: center;
  padding: 1rem;
}
.modal-box {
  background: #fff; border-radius: 16px; width: 100%; max-width: 520px;
  max-height: 80vh; display: flex; flex-direction: column;
  box-shadow: 0 24px 64px rgba(0,0,0,0.3);
}
.modal-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 1.25rem 1.5rem; border-bottom: 1px solid #f1f5f9; flex-shrink: 0;
}
.modal-title {
  font-family: 'Syne', sans-serif; font-size: 1.05rem; font-weight: 700;
  color: #0f172a; margin: 0;
}
.modal-close {
  background: none; border: none; font-size: 1rem; color: #94a3b8;
  cursor: pointer; padding: 0.25rem; line-height: 1;
  transition: color 0.15s;
}
.modal-close:hover { color: #1e293b; }
.modal-body {
  padding: 1.25rem 1.5rem; overflow-y: auto; flex: 1;
  font-size: 0.83rem; color: #374151; line-height: 1.7;
}
.modal-body h4 {
  font-family: 'DM Sans', sans-serif; font-size: 0.85rem; font-weight: 700;
  color: #0f172a; margin: 1rem 0 0.25rem;
}
.modal-body h4:first-child { margin-top: 0; }
.modal-body p { margin: 0 0 0.25rem; }
.terms-date { color: #94a3b8; font-size: 0.78rem; margin-top: 1rem !important; }
.modal-footer {
  padding: 1rem 1.5rem; border-top: 1px solid #f1f5f9; flex-shrink: 0;
}
.modal-agree-btn {
  width: 100%; padding: 0.65rem;
  background: #0d1530; color: #fff; border: none; border-radius: 9px;
  font-family: 'DM Sans', sans-serif; font-size: 0.9rem; font-weight: 600;
  cursor: pointer; transition: background 0.15s;
}
.modal-agree-btn:hover { background: #1a2b4a; }
.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 0.2s; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }

@media (max-width: 768px) {
  .brand-panel { display: none; }
  .form-panel  { background: #070d1f; }
  .form-card   { box-shadow: 0 8px 40px rgba(0,0,0,0.4); }
  .mobile-logo { display: flex; }
}
</style>

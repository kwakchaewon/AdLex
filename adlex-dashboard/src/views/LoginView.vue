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
            <button type="submit" class="submit-btn" :disabled="loading">
              <span v-if="!loading">시작하기 — 무료</span>
              <span v-else class="spinner" aria-label="가입 중" />
            </button>
          </form>
        </Transition>

        <p class="terms-note">
          계속 진행 시 <a href="#" class="terms-link">이용약관</a> 및
          <a href="#" class="terms-link">개인정보처리방침</a>에 동의합니다.
        </p>
      </div>
    </div>
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
@import url('https://fonts.googleapis.com/css2?family=Bebas+Neue&family=Syne:wght@600;700&family=DM+Sans:wght@400;500;600&display=swap');

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

.terms-note { margin-top: 1.25rem; text-align: center; font-size: 0.78rem; color: #9ca3af; line-height: 1.5; }
.terms-link { color: #6b7280; text-decoration: underline; text-underline-offset: 2px; transition: color 0.2s; }
.terms-link:hover { color: #374151; }

@media (max-width: 768px) {
  .brand-panel { display: none; }
  .form-panel  { background: #070d1f; }
  .form-card   { box-shadow: 0 8px 40px rgba(0,0,0,0.4); }
  .mobile-logo { display: flex; }
}
</style>

<template>
  <header class="app-header">
    <!-- Left: hamburger (mobile) + page title -->
    <div class="header-left">
      <button class="hamburger" @click="$emit('toggle-sidebar')" aria-label="메뉴 열기">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <line x1="3" y1="6" x2="21" y2="6"/>
          <line x1="3" y1="12" x2="21" y2="12"/>
          <line x1="3" y1="18" x2="21" y2="18"/>
        </svg>
      </button>
      <h1 class="page-title">{{ pageTitle }}</h1>
    </div>

    <!-- Right: actions -->
    <div class="header-right">
      <div class="user-chip" v-if="authStore.token">
        <span class="user-dot" aria-hidden="true" />
        <span class="user-label">활성</span>
      </div>
      <button class="logout-btn" @click="handleLogout" aria-label="로그아웃">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/>
          <polyline points="16 17 21 12 16 7"/>
          <line x1="21" y1="12" x2="9" y2="12"/>
        </svg>
        <span class="logout-label">로그아웃</span>
      </button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

defineEmits<{ 'toggle-sidebar': [] }>()

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const pageTitles: Record<string, string> = {
  '/dashboard':  '대시보드',
  '/api-keys':   'API Key 관리',
  '/history':    '검사 히스토리',
  '/playground': 'Playground',
  '/settings':   '설정',
}

const pageTitle = computed(() => pageTitles[route.path] ?? 'AdLex')

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  padding: 0 2rem;
  background: #ffffff;
  border-bottom: 1px solid #e9ecf3;
  flex-shrink: 0;
  position: sticky;
  top: 0;
  z-index: 30;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 0.875rem;
}

.hamburger {
  display: none;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: none;
  border: none;
  border-radius: 8px;
  color: #374151;
  cursor: pointer;
  transition: background 0.15s;
}

.hamburger:hover { background: #f3f4f6; }

.page-title {
  font-family: 'Syne', sans-serif;
  font-size: 1rem;
  font-weight: 600;
  color: #0d1530;
  margin: 0;
  letter-spacing: -0.01em;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.3rem 0.75rem;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 100px;
}

.user-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 5px rgba(34,197,94,0.6);
  animation: pulse-dot 2.5s ease-in-out infinite;
}

@keyframes pulse-dot {
  0%, 100% { box-shadow: 0 0 5px rgba(34,197,94,0.6); }
  50%       { box-shadow: 0 0 10px rgba(34,197,94,0.9); }
}

.user-label {
  font-family: 'DM Sans', sans-serif;
  font-size: 0.75rem;
  font-weight: 600;
  color: #16a34a;
}

.logout-btn {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.4rem 0.875rem;
  background: none;
  border: 1.5px solid #e5e7eb;
  border-radius: 8px;
  color: #6b7280;
  font-family: 'DM Sans', sans-serif;
  font-size: 0.825rem;
  font-weight: 500;
  cursor: pointer;
  transition: border-color 0.18s, color 0.18s, background 0.18s;
}

.logout-btn:hover {
  border-color: #ef4444;
  color: #ef4444;
  background: #fef2f2;
}

@media (max-width: 768px) {
  .hamburger   { display: flex; }
  .app-header  { padding: 0 1rem; }
  .logout-label { display: none; }
  .logout-btn  { padding: 0.4rem 0.5rem; }
  .user-label  { display: none; }
}
</style>

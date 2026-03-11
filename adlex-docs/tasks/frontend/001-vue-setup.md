# Task 3.1: Vue.js 셋업
 
## INPUT
- (없음)
 
## OUTPUT 경로
- adlex-dashboard/package.json
- adlex-dashboard/vite.config.ts
- adlex-dashboard/src/main.ts
- adlex-dashboard/src/App.vue
- adlex-dashboard/src/api/client.ts
- adlex-dashboard/src/router/index.ts
- adlex-dashboard/src/stores/auth.ts
- adlex-dashboard/src/views/LoginView.vue
- adlex-dashboard/src/views/DashboardView.vue
- adlex-dashboard/src/components/layout/AppLayout.vue
- adlex-dashboard/src/components/layout/AppSidebar.vue
- adlex-dashboard/src/components/layout/AppHeader.vue
 
## 상세 스펙
Vue 3 + Vite + TS. PrimeVue 4(Aura) + TailwindCSS. "AdLex" 로고.
Vite proxy: /api,/v1 → localhost:8080
 
## 완료 조건
cd adlex-dashboard; npm run dev → localhost:5173
 
## 커밋
chore(dashboard): initialize AdLex Vue.js dashboard
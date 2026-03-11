# Task 3.6: 로그인 / 회원가입 화면

## INPUT
- adlex-dashboard/src/api/client.ts
- adlex-dashboard/src/stores/auth.ts

## OUTPUT 경로
- adlex-dashboard/src/views/LoginView.vue
- adlex-dashboard/src/views/RegisterView.vue

## 상세 스펙
LoginView: PrimeVue InputText + Password + Button → POST /api/auth/login. JWT 저장.
RegisterView: email, password, confirm, company → POST /api/auth/register.
성공 시 API Key 표시 (복사 버튼 + '다시 볼 수 없음' 경고).
`<script setup lang="ts">` + Composition API.

## 완료 조건
npm run dev → /login, /register 정상 렌더링

## 커밋
feat(dashboard): implement login and register views

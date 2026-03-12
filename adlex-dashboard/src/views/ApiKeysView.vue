<template>
  <div class="keys-page">
    <!-- Header -->
    <div class="page-topbar">
      <div>
        <p class="page-eyebrow">인증</p>
        <h2 class="page-heading">API Key 관리</h2>
      </div>
      <button class="create-btn" @click="openCreate">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        새 Key 생성
      </button>
    </div>

    <!-- Info banner -->
    <div class="info-banner">
      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
      API Key는 <code>X-API-Key</code> 헤더로 전달됩니다. Key는 생성 시 한 번만 표시됩니다.
    </div>

    <!-- Table card -->
    <div class="table-card">
      <!-- Loading -->
      <template v-if="loading">
        <div v-for="i in 3" :key="i" class="skeleton-row" :style="{ animationDelay: `${i * 80}ms` }" />
      </template>

      <!-- Empty -->
      <div v-else-if="!keys.length" class="empty-state">
        <div class="empty-icon-wrap">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="1.5" stroke-linecap="round">
            <circle cx="7.5" cy="15.5" r="4.5"/><path d="M10.9 12.1L20 3"/><path d="M18 5l2 2"/><path d="M15 8l2 2"/>
          </svg>
        </div>
        <p class="empty-title">API Key가 없습니다</p>
        <p class="empty-sub">첫 번째 Key를 생성하여 API를 사용해보세요.</p>
        <button class="empty-cta" @click="openCreate">Key 생성하기</button>
      </div>

      <!-- Table -->
      <table v-else class="keys-table">
        <thead>
          <tr>
            <th>이름</th>
            <th>Key Prefix</th>
            <th>상태</th>
            <th>마지막 사용</th>
            <th>생성일</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(key, i) in keys"
            :key="key.id"
            :class="['key-row', { revoked: key.status === 'REVOKED' }]"
            :style="{ animationDelay: `${i * 40}ms` }"
          >
            <td class="cell-name">
              <span class="key-name">{{ key.name }}</span>
            </td>
            <td class="cell-prefix">
              <code class="key-prefix">{{ key.keyPrefix }}••••••••••••••••••••</code>
            </td>
            <td class="cell-status">
              <span :class="['status-badge', key.status === 'ACTIVE' ? 'active' : 'revoked']">
                <span v-if="key.status === 'ACTIVE'" class="status-dot" aria-hidden="true" />
                {{ key.status === 'ACTIVE' ? '활성' : '폐기됨' }}
              </span>
            </td>
            <td class="cell-date">{{ key.lastUsedAt ? relativeTime(key.lastUsedAt) : '—' }}</td>
            <td class="cell-date">{{ formatDate(key.createdAt) }}</td>
            <td class="cell-action">
              <template v-if="key.status === 'ACTIVE'">
                <template v-if="confirmRevokeId === key.id">
                  <span class="confirm-text">정말 폐기할까요?</span>
                  <button class="action-btn danger" @click="doRevoke(key.id)">폐기</button>
                  <button class="action-btn ghost" @click="confirmRevokeId = null">취소</button>
                </template>
                <button v-else class="action-btn revoke" @click="confirmRevokeId = key.id">
                  폐기
                </button>
              </template>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- ── Create Modal ── -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="showCreate" class="modal-overlay" @click.self="closeCreate">
          <div class="modal" role="dialog" aria-modal="true" aria-label="API Key 생성">
            <div class="modal-header">
              <h3 class="modal-title">새 API Key 생성</h3>
              <button class="modal-close" @click="closeCreate" aria-label="닫기">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <div class="modal-body">
              <label class="field-label" for="key-name">Key 이름</label>
              <input
                id="key-name"
                v-model="newKeyName"
                class="field-input"
                placeholder="Production Server"
                maxlength="50"
                @keydown.enter="submitCreate"
                ref="nameInputRef"
              />
              <p class="field-hint">Key의 용도를 쉽게 알 수 있는 이름을 입력하세요.</p>
            </div>
            <div class="modal-footer">
              <button class="btn-ghost" @click="closeCreate">취소</button>
              <button class="btn-primary" @click="submitCreate" :disabled="!newKeyName.trim() || creating">
                <span v-if="!creating">생성</span>
                <span v-else class="spinner" />
              </button>
            </div>
          </div>
        </div>
      </Transition>

      <!-- ── Key Reveal Modal ── -->
      <Transition name="modal-fade">
        <div v-if="revealKey" class="modal-overlay">
          <div class="modal reveal-modal" role="dialog" aria-modal="true" aria-label="API Key 확인">
            <div class="reveal-warning">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#d97706" stroke-width="2" stroke-linecap="round" aria-hidden="true">
                <path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>
              </svg>
              <p>이 Key는 <strong>지금만</strong> 표시됩니다. 반드시 안전한 곳에 저장하세요.</p>
            </div>
            <p class="reveal-name">{{ revealKey.name }}</p>
            <div class="key-display-wrap">
              <code class="key-display">{{ revealKey.key }}</code>
              <button class="copy-btn" @click="copyKey" :class="{ copied: copied }" :aria-label="copied ? '복사됨' : '복사'">
                <svg v-if="!copied" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/></svg>
                <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><polyline points="20 6 9 17 4 12"/></svg>
                {{ copied ? '복사됨' : '복사' }}
              </button>
            </div>
            <button class="btn-primary full-width" @click="revealKey = null; copied = false">
              확인했습니다
            </button>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import {
  fetchKeys, createKey, revokeKey,
  type ApiKeyResponse, type CreateApiKeyResponse,
} from '@/api/apiKeys'

const keys = ref<ApiKeyResponse[]>([])
const loading = ref(false)
const showCreate = ref(false)
const newKeyName = ref('')
const creating = ref(false)
const revealKey = ref<CreateApiKeyResponse | null>(null)
const copied = ref(false)
const confirmRevokeId = ref<number | null>(null)
const nameInputRef = ref<HTMLInputElement | null>(null)

async function load() {
  loading.value = true
  try { keys.value = await fetchKeys() }
  finally { loading.value = false }
}

function openCreate() {
  newKeyName.value = ''
  showCreate.value = true
  nextTick(() => nameInputRef.value?.focus())
}

function closeCreate() {
  showCreate.value = false
  newKeyName.value = ''
}

async function submitCreate() {
  if (!newKeyName.value.trim() || creating.value) return
  creating.value = true
  try {
    const result = await createKey(newKeyName.value.trim())
    keys.value.unshift(result)
    closeCreate()
    revealKey.value = result
  } finally {
    creating.value = false
  }
}

async function doRevoke(id: number) {
  await revokeKey(id)
  const key = keys.value.find((k) => k.id === id)
  if (key) key.status = 'REVOKED'
  confirmRevokeId.value = null
}

async function copyKey() {
  if (!revealKey.value) return
  await navigator.clipboard.writeText(revealKey.value.key)
  copied.value = true
  setTimeout(() => { copied.value = false }, 2500)
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

function relativeTime(iso: string) {
  const diff = Date.now() - new Date(iso).getTime()
  const m = Math.floor(diff / 60000)
  if (m < 1)   return '방금 전'
  if (m < 60)  return `${m}분 전`
  const h = Math.floor(m / 60)
  if (h < 24)  return `${h}시간 전`
  const d = Math.floor(h / 24)
  if (d < 30)  return `${d}일 전`
  return formatDate(iso)
}

onMounted(load)
</script>

<style scoped>

.keys-page { font-family: 'DM Sans', sans-serif; max-width: 1000px; }

/* ── Top bar ── */
.page-topbar {
  display: flex; align-items: flex-end; justify-content: space-between;
  margin-bottom: 1.25rem; flex-wrap: wrap; gap: 1rem;
}

.page-eyebrow {
  font-size: 0.75rem; font-weight: 600; letter-spacing: 0.08em;
  text-transform: uppercase; color: #94a3b8; margin: 0 0 0.2rem;
}

.page-heading {
  font-family: 'Syne', sans-serif; font-size: 1.5rem; font-weight: 700;
  color: #0d1530; margin: 0; letter-spacing: -0.02em;
}

.create-btn {
  display: flex; align-items: center; gap: 0.5rem;
  padding: 0.6rem 1.125rem;
  background: #0d1530; color: #fff; border: none; border-radius: 10px;
  font-family: 'DM Sans', sans-serif; font-size: 0.875rem; font-weight: 600;
  cursor: pointer; transition: background 0.18s, transform 0.15s, box-shadow 0.18s;
}

.create-btn:hover {
  background: #1a2b4a;
  box-shadow: 0 4px 14px rgba(13,21,48,0.25);
  transform: translateY(-1px);
}

/* ── Info banner ── */
.info-banner {
  display: flex; align-items: center; gap: 0.5rem;
  padding: 0.7rem 1rem; background: #eff6ff; border: 1px solid #bfdbfe;
  border-radius: 10px; font-size: 0.825rem; color: #1d4ed8;
  margin-bottom: 1.25rem;
}

.info-banner code {
  font-family: 'DM Mono', monospace; font-size: 0.8rem;
  background: #dbeafe; padding: 0.1rem 0.35rem; border-radius: 4px;
}

/* ── Table card ── */
.table-card {
  background: #fff; border-radius: 16px; border: 1px solid #eef0f6;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04); overflow: hidden;
}

/* Skeleton */
.skeleton-row {
  height: 60px; margin: 0;
  background: linear-gradient(90deg, #f4f6fb 25%, #e9ecf3 50%, #f4f6fb 75%);
  background-size: 200% 100%; animation: shimmer 1.4s infinite;
  border-bottom: 1px solid #eef0f6;
}

@keyframes shimmer {
  from { background-position: 200% 0; }
  to   { background-position: -200% 0; }
}

/* Empty */
.empty-state {
  text-align: center; padding: 3.5rem 1.5rem;
}

.empty-icon-wrap {
  display: inline-flex; align-items: center; justify-content: center;
  width: 60px; height: 60px; background: #f4f6fb;
  border-radius: 14px; margin-bottom: 1rem;
}

.empty-title {
  font-family: 'Syne', sans-serif; font-size: 1rem; font-weight: 600;
  color: #374151; margin: 0 0 0.4rem;
}

.empty-sub { font-size: 0.85rem; color: #94a3b8; margin: 0 0 1.25rem; }

.empty-cta {
  padding: 0.55rem 1.25rem; background: #0d1530; color: #fff;
  border: none; border-radius: 8px; font-family: 'DM Sans', sans-serif;
  font-size: 0.875rem; font-weight: 500; cursor: pointer; transition: background 0.18s;
}
.empty-cta:hover { background: #1a2b4a; }

/* ── Table ── */
.keys-table { width: 100%; border-collapse: collapse; }

.keys-table th {
  padding: 0.75rem 1.25rem; text-align: left;
  font-size: 0.72rem; font-weight: 700; letter-spacing: 0.06em;
  text-transform: uppercase; color: #94a3b8;
  background: #fafbfd; border-bottom: 1px solid #eef0f6;
}

.key-row {
  border-bottom: 1px solid #f3f4f7;
  animation: row-fade 0.35s cubic-bezier(0.16,1,0.3,1) both;
  transition: background 0.15s;
}

.key-row:hover { background: #fafbfd; }
.key-row.revoked { opacity: 0.55; }
.key-row:last-child { border-bottom: none; }

@keyframes row-fade {
  from { opacity: 0; transform: translateX(-6px); }
  to   { opacity: 1; transform: translateX(0); }
}

.keys-table td { padding: 1rem 1.25rem; vertical-align: middle; }

.key-name { font-weight: 600; color: #1e293b; font-size: 0.9rem; }

.key-prefix {
  font-family: 'DM Mono', monospace; font-size: 0.8rem; color: #475569;
  background: #f1f5f9; padding: 0.2rem 0.5rem; border-radius: 5px;
  letter-spacing: 0.02em;
}

.cell-date { font-size: 0.825rem; color: #94a3b8; white-space: nowrap; }

/* Status badge */
.status-badge {
  display: inline-flex; align-items: center; gap: 0.35rem;
  padding: 0.25rem 0.625rem; border-radius: 100px;
  font-size: 0.75rem; font-weight: 600;
}

.status-badge.active {
  background: #f0fdf4; color: #16a34a; border: 1px solid #bbf7d0;
}

.status-badge.revoked {
  background: #f8fafc; color: #94a3b8; border: 1px solid #e2e8f0;
}

.status-dot {
  width: 6px; height: 6px; border-radius: 50%; background: #22c55e;
  animation: pulse-dot 2.5s ease-in-out infinite;
}

@keyframes pulse-dot {
  0%,100% { box-shadow: 0 0 4px rgba(34,197,94,0.5); }
  50%      { box-shadow: 0 0 8px rgba(34,197,94,0.9); }
}

/* Actions */
.cell-action {
  display: flex; align-items: center; gap: 0.5rem;
  white-space: nowrap;
}

.confirm-text { font-size: 0.8rem; color: #ef4444; font-weight: 500; }

.action-btn {
  padding: 0.3rem 0.7rem; border-radius: 6px; border: 1.5px solid;
  font-family: 'DM Sans', sans-serif; font-size: 0.8rem; font-weight: 500;
  cursor: pointer; transition: all 0.15s;
}

.action-btn.revoke {
  border-color: #e5e7eb; background: none; color: #6b7280;
}
.action-btn.revoke:hover { border-color: #ef4444; color: #ef4444; background: #fef2f2; }

.action-btn.danger { border-color: #ef4444; background: #ef4444; color: #fff; }
.action-btn.danger:hover { background: #dc2626; border-color: #dc2626; }

.action-btn.ghost { border-color: #e5e7eb; background: none; color: #6b7280; }
.action-btn.ghost:hover { background: #f3f4f6; }

/* ── Modal ── */
.modal-overlay {
  position: fixed; inset: 0; background: rgba(7,13,31,0.55);
  display: flex; align-items: center; justify-content: center;
  z-index: 100; padding: 1rem; backdrop-filter: blur(4px);
}

.modal {
  background: #fff; border-radius: 20px; width: 100%; max-width: 460px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.25);
  animation: modal-in 0.3s cubic-bezier(0.16,1,0.3,1);
}

@keyframes modal-in {
  from { opacity: 0; transform: scale(0.94) translateY(12px); }
  to   { opacity: 1; transform: scale(1) translateY(0); }
}

.modal-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 1.5rem 1.5rem 0;
}

.modal-title {
  font-family: 'Syne', sans-serif; font-size: 1.1rem; font-weight: 600;
  color: #0d1530; margin: 0;
}

.modal-close {
  display: flex; align-items: center; justify-content: center;
  width: 32px; height: 32px; background: #f3f4f6; border: none;
  border-radius: 8px; color: #6b7280; cursor: pointer; transition: background 0.15s;
}
.modal-close:hover { background: #e5e7eb; }

.modal-body { padding: 1.25rem 1.5rem; }

.field-label {
  display: block; font-size: 0.8rem; font-weight: 600; color: #374151;
  letter-spacing: 0.02em; text-transform: uppercase; margin-bottom: 0.4rem;
}

.field-input {
  width: 100%; padding: 0.7rem 1rem; background: #f9fafb;
  border: 1.5px solid #e5e7eb; border-radius: 10px;
  font-family: 'DM Sans', sans-serif; font-size: 0.95rem; color: #111827;
  outline: none; transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
}

.field-input:focus {
  border-color: #3b82f6; background: #fff;
  box-shadow: 0 0 0 3px rgba(59,130,246,0.12);
}

.field-hint { font-size: 0.78rem; color: #94a3b8; margin: 0.375rem 0 0; }

.modal-footer {
  display: flex; justify-content: flex-end; gap: 0.625rem;
  padding: 0 1.5rem 1.5rem;
}

.btn-ghost {
  padding: 0.6rem 1.125rem; background: none; border: 1.5px solid #e5e7eb;
  border-radius: 9px; font-family: 'DM Sans', sans-serif; font-size: 0.875rem;
  font-weight: 500; color: #6b7280; cursor: pointer; transition: all 0.15s;
}
.btn-ghost:hover { background: #f3f4f6; }

.btn-primary {
  padding: 0.6rem 1.375rem; background: #0d1530; color: #fff;
  border: none; border-radius: 9px; font-family: 'DM Sans', sans-serif;
  font-size: 0.875rem; font-weight: 600; cursor: pointer;
  transition: background 0.18s, box-shadow 0.18s; display: flex;
  align-items: center; justify-content: center; min-width: 70px;
}
.btn-primary:hover:not(:disabled) { background: #1a2b4a; box-shadow: 0 4px 12px rgba(13,21,48,0.2); }
.btn-primary:disabled { opacity: 0.55; cursor: not-allowed; }
.btn-primary.full-width { width: 100%; padding: 0.8rem; font-size: 0.95rem; }

/* ── Reveal modal ── */
.reveal-modal { padding: 1.5rem; }

.reveal-warning {
  display: flex; align-items: flex-start; gap: 0.625rem;
  background: #fffbeb; border: 1px solid #fde68a;
  border-radius: 10px; padding: 0.875rem 1rem;
  font-size: 0.85rem; color: #92400e; line-height: 1.5;
  margin-bottom: 1.25rem;
}

.reveal-warning strong { font-weight: 700; }
.reveal-warning svg { flex-shrink: 0; margin-top: 1px; }

.reveal-name {
  font-family: 'Syne', sans-serif; font-size: 1rem; font-weight: 600;
  color: #0d1530; margin: 0 0 0.75rem;
}

.key-display-wrap {
  position: relative; margin-bottom: 1.25rem;
  background: #0d1530; border-radius: 10px; overflow: hidden;
}

.key-display {
  display: block; font-family: 'DM Mono', monospace; font-size: 0.8rem;
  color: #93c5fd; padding: 1rem 4rem 1rem 1rem;
  word-break: break-all; line-height: 1.6;
}

.copy-btn {
  position: absolute; right: 0.75rem; top: 50%; transform: translateY(-50%);
  display: flex; align-items: center; gap: 0.35rem;
  padding: 0.4rem 0.75rem; background: rgba(255,255,255,0.08);
  border: 1px solid rgba(255,255,255,0.15); border-radius: 7px;
  color: #cbd5e1; font-family: 'DM Sans', sans-serif; font-size: 0.78rem;
  font-weight: 500; cursor: pointer; transition: all 0.18s; white-space: nowrap;
}

.copy-btn:hover { background: rgba(255,255,255,0.15); color: #fff; }
.copy-btn.copied { color: #4ade80; border-color: rgba(74,222,128,0.4); background: rgba(74,222,128,0.08); }

/* ── Transition ── */
.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 0.2s; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }

.spinner {
  width: 16px; height: 16px;
  border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff;
  border-radius: 50%; animation: spin 0.7s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }
</style>

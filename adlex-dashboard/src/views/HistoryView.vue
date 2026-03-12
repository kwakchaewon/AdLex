<template>
  <div class="history-page">
    <!-- Header -->
    <div class="page-topbar">
      <div>
        <p class="page-eyebrow">검사 이력</p>
        <h2 class="page-heading">히스토리</h2>
      </div>
      <p class="total-count" v-if="page">총 <strong>{{ page.totalElements.toLocaleString('ko-KR') }}</strong>건</p>
    </div>

    <!-- Filter bar -->
    <div class="filter-bar">
      <div class="filter-group">
        <label class="filter-label">채널</label>
        <div class="seg-control">
          <button v-for="opt in channelOpts" :key="opt.value ?? 'all'"
            :class="['seg-btn', { active: filter.channel === opt.value }]"
            @click="setFilter('channel', opt.value)">{{ opt.label }}</button>
        </div>
      </div>

      <div class="filter-group">
        <label class="filter-label">준수 여부</label>
        <div class="seg-control">
          <button v-for="opt in compliantOpts" :key="String(opt.value ?? 'all')"
            :class="['seg-btn', { active: filter.compliant === opt.value }]"
            @click="setFilter('compliant', opt.value)">{{ opt.label }}</button>
        </div>
      </div>

      <div class="filter-group date-group">
        <label class="filter-label">기간</label>
        <div class="date-inputs">
          <input type="date" class="date-input" v-model="filter.from" @change="debouncedLoad" />
          <span class="date-sep">—</span>
          <input type="date" class="date-input" v-model="filter.to" @change="debouncedLoad" />
        </div>
      </div>

      <button v-if="hasFilters" class="clear-btn" @click="clearFilters">
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        필터 초기화
      </button>
    </div>

    <!-- Table card -->
    <div class="table-card">
      <!-- Loading -->
      <template v-if="loading">
        <div v-for="i in 8" :key="i" class="skeleton-row" :style="{ animationDelay: `${i * 60}ms` }" />
      </template>

      <!-- Empty -->
      <div v-else-if="!items.length" class="empty-state">
        <div class="empty-icon">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="1.5" stroke-linecap="round">
            <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2"/>
            <rect x="9" y="3" width="6" height="4" rx="1.5"/><path d="M9 12h6"/><path d="M9 16h4"/>
          </svg>
        </div>
        <p class="empty-title">검사 이력이 없습니다</p>
        <p class="empty-sub">{{ hasFilters ? '필터 조건을 변경해보세요.' : 'API를 통해 메시지를 검사하면 이곳에 기록됩니다.' }}</p>
      </div>

      <!-- Table -->
      <table v-else class="history-table">
        <thead>
          <tr>
            <th>채널</th>
            <th>결과</th>
            <th>위반 건수</th>
            <th>처리 시간</th>
            <th>검사 일시</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, i) in items" :key="item.id"
            class="history-row"
            :style="{ animationDelay: `${i * 30}ms` }"
            @click="openDetail(item.id)"
            :aria-selected="selectedId === item.id"
          >
            <td><span :class="['channel-badge', item.channel.toLowerCase()]">{{ item.channel }}</span></td>
            <td>
              <span :class="['result-badge', item.compliant ? 'pass' : 'fail']">
                <svg v-if="item.compliant" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" aria-hidden="true"><polyline points="20 6 9 17 4 12"/></svg>
                <svg v-else width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" aria-hidden="true"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                {{ item.compliant ? '준수' : '위반' }}
              </span>
            </td>
            <td class="cell-mono">
              <span v-if="!item.compliant" class="violation-count">{{ item.violationCount }}건</span>
              <span v-else class="muted">—</span>
            </td>
            <td class="cell-mono">{{ item.processingMs }}<span class="unit">ms</span></td>
            <td class="cell-date">{{ formatDateTime(item.checkedAt) }}</td>
            <td class="cell-chevron">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polyline points="9 18 15 12 9 6"/></svg>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <div v-if="page && page.totalPages > 1" class="pagination">
        <button class="pg-btn" :disabled="page.page === 0" @click="goPage(page.page - 1)">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polyline points="15 18 9 12 15 6"/></svg>
          이전
        </button>
        <span class="pg-info">{{ page.page + 1 }} / {{ page.totalPages }}</span>
        <button class="pg-btn" :disabled="page.page >= page.totalPages - 1" @click="goPage(page.page + 1)">
          다음
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polyline points="9 18 15 12 9 6"/></svg>
        </button>
      </div>
    </div>

    <!-- ── Detail Drawer ── -->
    <Teleport to="body">
      <Transition name="backdrop-fade">
        <div v-if="detail || detailLoading" class="drawer-backdrop" @click="closeDetail" aria-hidden="true" />
      </Transition>
      <Transition name="drawer-slide">
        <aside v-if="detail || detailLoading" class="detail-drawer" role="complementary" aria-label="검사 상세">
          <div class="drawer-header">
            <h3 class="drawer-title">검사 상세</h3>
            <button class="drawer-close" @click="closeDetail" aria-label="닫기">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>

          <div v-if="detailLoading" class="drawer-loading">
            <div class="drawer-skeleton" v-for="i in 4" :key="i" />
          </div>

          <div v-else-if="detail" class="drawer-body">
            <!-- Meta row -->
            <div class="detail-meta">
              <span :class="['channel-badge', detail.channel.toLowerCase()]">{{ detail.channel }}</span>
              <span :class="['result-badge', detail.compliant ? 'pass' : 'fail']">
                {{ detail.compliant ? '준수' : '위반' }}
              </span>
              <span class="meta-time">{{ formatDateTime(detail.checkedAt) }}</span>
              <span class="meta-ms">{{ detail.processingMs }}ms</span>
            </div>

            <!-- Message -->
            <div class="drawer-section">
              <p class="section-label">원문 메시지</p>
              <div class="message-box">
                <pre class="message-text">{{ detail.message }}</pre>
              </div>
            </div>

            <!-- Result -->
            <div class="drawer-section">
              <p class="section-label">검사 결과</p>

              <div v-if="detail.compliant" class="pass-banner">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#16a34a" stroke-width="2.5" stroke-linecap="round" aria-hidden="true"><polyline points="20 6 9 17 4 12"/></svg>
                <div>
                  <p class="pass-title">법규 준수</p>
                  <p class="pass-sub">모든 규칙을 통과했습니다.</p>
                </div>
              </div>

              <div v-else class="violation-list">
                <div v-for="v in detail.violations" :key="v.ruleCode" class="violation-item">
                  <div class="viol-header">
                    <code class="viol-code">{{ v.ruleCode }}</code>
                    <span :class="['severity-badge', v.severity.toLowerCase()]">{{ v.severity }}</span>
                  </div>
                  <p class="viol-message">{{ v.message }}</p>
                  <div v-if="v.legalBasis" class="viol-basis">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
                    {{ v.legalBasis }}
                  </div>
                  <div v-if="v.suggestion" class="viol-suggestion">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><circle cx="12" cy="12" r="10"/><path d="M12 8v4"/><path d="M12 16h.01"/></svg>
                    {{ v.suggestion }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </aside>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  fetchHistory, fetchHistoryDetail,
  type HistoryItemResponse, type HistoryDetailResponse,
  type Channel, type PageResponse, type HistoryFilter,
} from '@/api/history'

const items = ref<HistoryItemResponse[]>([])
const page = ref<PageResponse<HistoryItemResponse> | null>(null)
const loading = ref(false)
const detail = ref<HistoryDetailResponse | null>(null)
const detailLoading = ref(false)
const selectedId = ref<number | null>(null)

const filter = ref<HistoryFilter & { from?: string; to?: string }>({
  channel: undefined, compliant: undefined, from: undefined, to: undefined, page: 0,
})

const channelOpts = [
  { label: '전체', value: undefined },
  { label: 'SMS',   value: 'SMS'   as Channel },
  { label: 'KAKAO', value: 'KAKAO' as Channel },
  { label: 'EMAIL', value: 'EMAIL' as Channel },
]

const compliantOpts = [
  { label: '전체', value: undefined },
  { label: '준수', value: true  },
  { label: '위반', value: false },
]

const hasFilters = computed(() =>
  filter.value.channel !== undefined ||
  filter.value.compliant !== undefined ||
  !!filter.value.from || !!filter.value.to
)

let debounceTimer: ReturnType<typeof setTimeout>
function debouncedLoad() {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => load(), 300)
}

function setFilter(key: keyof typeof filter.value, value: unknown) {
  (filter.value as Record<string, unknown>)[key as string] = value
  filter.value.page = 0
  debouncedLoad()
}

function clearFilters() {
  filter.value = { channel: undefined, compliant: undefined, from: undefined, to: undefined, page: 0 }
  load()
}

async function load() {
  loading.value = true
  try {
    const result = await fetchHistory(filter.value)
    page.value = result
    items.value = result.content
  } finally {
    loading.value = false
  }
}

function goPage(p: number) {
  filter.value.page = p
  load()
}

async function openDetail(id: number) {
  selectedId.value = id
  detail.value = null
  detailLoading.value = true
  try { detail.value = await fetchHistoryDetail(id) }
  finally { detailLoading.value = false }
}

function closeDetail() {
  detail.value = null
  detailLoading.value = false
  selectedId.value = null
}

function formatDateTime(iso: string) {
  return new Date(iso).toLocaleString('ko-KR', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit',
  })
}

onMounted(load)
</script>

<style scoped>

.history-page { font-family: 'DM Sans', sans-serif; max-width: 1000px; }

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

.total-count { font-size: 0.875rem; color: #6b7280; margin: 0; }
.total-count strong { color: #0d1530; font-weight: 700; }

/* ── Filter bar ── */
.filter-bar {
  display: flex; align-items: flex-end; flex-wrap: wrap; gap: 1rem;
  padding: 1rem 1.25rem; background: #fff; border-radius: 14px;
  border: 1px solid #eef0f6; margin-bottom: 1.25rem;
  box-shadow: 0 1px 3px rgba(0,0,0,0.03);
}

.filter-group { display: flex; flex-direction: column; gap: 0.375rem; }

.filter-label {
  font-size: 0.72rem; font-weight: 700; letter-spacing: 0.06em;
  text-transform: uppercase; color: #94a3b8;
}

.seg-control {
  display: flex; background: #f0f3f8; border-radius: 8px; padding: 2px; gap: 1px;
}

.seg-btn {
  padding: 0.3rem 0.75rem; background: none; border: none; border-radius: 6px;
  font-family: 'DM Sans', sans-serif; font-size: 0.8rem; font-weight: 500;
  color: #6b7280; cursor: pointer; transition: all 0.18s;
}

.seg-btn.active { background: #fff; color: #0d1530; font-weight: 600; box-shadow: 0 1px 3px rgba(0,0,0,0.09); }

.date-group { flex: 1; min-width: 200px; }

.date-inputs { display: flex; align-items: center; gap: 0.5rem; }

.date-input {
  padding: 0.35rem 0.625rem; background: #f9fafb; border: 1.5px solid #e5e7eb;
  border-radius: 8px; font-family: 'DM Sans', sans-serif; font-size: 0.8rem;
  color: #374151; outline: none; transition: border-color 0.18s;
}
.date-input:focus { border-color: #3b82f6; }

.date-sep { color: #94a3b8; font-size: 0.8rem; }

.clear-btn {
  display: flex; align-items: center; gap: 0.35rem; align-self: flex-end;
  padding: 0.38rem 0.75rem; background: none; border: 1.5px solid #fca5a5;
  border-radius: 7px; font-family: 'DM Sans', sans-serif; font-size: 0.78rem;
  font-weight: 500; color: #ef4444; cursor: pointer; transition: all 0.15s;
}
.clear-btn:hover { background: #fef2f2; }

/* ── Table card ── */
.table-card {
  background: #fff; border-radius: 16px; border: 1px solid #eef0f6;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04); overflow: hidden;
}

.skeleton-row {
  height: 56px; border-bottom: 1px solid #f3f4f7;
  background: linear-gradient(90deg, #f4f6fb 25%, #e9ecf3 50%, #f4f6fb 75%);
  background-size: 200% 100%; animation: shimmer 1.4s infinite;
}

@keyframes shimmer {
  from { background-position: 200% 0; }
  to   { background-position: -200% 0; }
}

.empty-state { text-align: center; padding: 3.5rem 1.5rem; }

.empty-icon {
  display: inline-flex; align-items: center; justify-content: center;
  width: 58px; height: 58px; background: #f4f6fb; border-radius: 14px; margin-bottom: 1rem;
}

.empty-title {
  font-family: 'Syne', sans-serif; font-size: 1rem; font-weight: 600;
  color: #374151; margin: 0 0 0.4rem;
}
.empty-sub { font-size: 0.85rem; color: #94a3b8; margin: 0; }

/* ── Table ── */
.history-table { width: 100%; border-collapse: collapse; }

.history-table th {
  padding: 0.7rem 1.25rem; text-align: left;
  font-size: 0.7rem; font-weight: 700; letter-spacing: 0.06em; text-transform: uppercase;
  color: #94a3b8; background: #fafbfd; border-bottom: 1px solid #eef0f6;
}

.history-row {
  border-bottom: 1px solid #f3f4f7; cursor: pointer;
  transition: background 0.13s;
  animation: row-in 0.3s cubic-bezier(0.16,1,0.3,1) both;
}

.history-row:hover { background: #f8fafd; }
.history-row[aria-selected="true"] { background: #eff6ff; }
.history-row:last-child { border-bottom: none; }

@keyframes row-in {
  from { opacity: 0; transform: translateY(6px); }
  to   { opacity: 1; transform: translateY(0); }
}

.history-table td { padding: 0.875rem 1.25rem; vertical-align: middle; }

/* Badges */
.channel-badge {
  display: inline-block; padding: 0.2rem 0.55rem;
  border-radius: 6px; font-family: 'DM Mono', monospace;
  font-size: 0.72rem; font-weight: 500; letter-spacing: 0.04em;
}
.channel-badge.sms   { background: #eff6ff; color: #1d4ed8; }
.channel-badge.kakao { background: #fefce8; color: #854d0e; }
.channel-badge.email { background: #f0fdf4; color: #15803d; }

.result-badge {
  display: inline-flex; align-items: center; gap: 0.3rem;
  padding: 0.22rem 0.6rem; border-radius: 100px;
  font-size: 0.78rem; font-weight: 600;
}
.result-badge.pass { background: #f0fdf4; color: #16a34a; border: 1px solid #bbf7d0; }
.result-badge.fail { background: #fef2f2; color: #dc2626; border: 1px solid #fecaca; }

.cell-mono { font-family: 'DM Mono', monospace; font-size: 0.83rem; color: #475569; }
.violation-count { color: #ef4444; font-weight: 500; }
.muted { color: #cbd5e1; }
.unit { font-size: 0.7rem; color: #94a3b8; margin-left: 1px; }
.cell-date { font-size: 0.82rem; color: #94a3b8; white-space: nowrap; }
.cell-chevron { color: #d1d5db; text-align: right; width: 32px; }
.history-row:hover .cell-chevron { color: #94a3b8; }

/* ── Pagination ── */
.pagination {
  display: flex; align-items: center; justify-content: center; gap: 1rem;
  padding: 1rem; border-top: 1px solid #f3f4f7;
}

.pg-btn {
  display: flex; align-items: center; gap: 0.35rem;
  padding: 0.4rem 0.875rem; background: none; border: 1.5px solid #e5e7eb;
  border-radius: 8px; font-family: 'DM Sans', sans-serif;
  font-size: 0.825rem; font-weight: 500; color: #374151;
  cursor: pointer; transition: all 0.15s;
}
.pg-btn:hover:not(:disabled) { border-color: #3b82f6; color: #3b82f6; }
.pg-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.pg-info { font-size: 0.825rem; color: #6b7280; min-width: 60px; text-align: center; }

/* ── Drawer ── */
.drawer-backdrop {
  position: fixed; inset: 0; background: rgba(7,13,31,0.4);
  z-index: 60; backdrop-filter: blur(2px);
}

.detail-drawer {
  position: fixed; top: 0; right: 0; bottom: 0;
  width: min(480px, 100vw); background: #fff;
  z-index: 70; display: flex; flex-direction: column;
  box-shadow: -4px 0 40px rgba(0,0,0,0.14);
}

.drawer-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 1.25rem 1.5rem; border-bottom: 1px solid #eef0f6; flex-shrink: 0;
}

.drawer-title {
  font-family: 'Syne', sans-serif; font-size: 1rem; font-weight: 600;
  color: #0d1530; margin: 0;
}

.drawer-close {
  display: flex; align-items: center; justify-content: center;
  width: 32px; height: 32px; background: #f3f4f6; border: none;
  border-radius: 8px; color: #6b7280; cursor: pointer; transition: background 0.15s;
}
.drawer-close:hover { background: #e5e7eb; }

.drawer-loading { padding: 1.5rem; display: flex; flex-direction: column; gap: 0.875rem; }

.drawer-skeleton {
  height: 52px; border-radius: 10px;
  background: linear-gradient(90deg, #f4f6fb 25%, #e9ecf3 50%, #f4f6fb 75%);
  background-size: 200% 100%; animation: shimmer 1.4s infinite;
}

.drawer-body {
  flex: 1; overflow-y: auto; padding: 1.25rem 1.5rem;
  display: flex; flex-direction: column; gap: 1.25rem;
}

.detail-meta { display: flex; align-items: center; flex-wrap: wrap; gap: 0.5rem; }
.meta-time { font-size: 0.8rem; color: #94a3b8; margin-left: auto; }
.meta-ms { font-family: 'DM Mono', monospace; font-size: 0.78rem; color: #94a3b8; }

.drawer-section { display: flex; flex-direction: column; gap: 0.625rem; }

.section-label {
  font-size: 0.72rem; font-weight: 700; letter-spacing: 0.06em;
  text-transform: uppercase; color: #94a3b8;
}

.message-box {
  background: #0d1530; border-radius: 10px; padding: 1rem;
  max-height: 180px; overflow-y: auto;
}

.message-text {
  font-family: 'DM Mono', monospace; font-size: 0.82rem; color: #93c5fd;
  line-height: 1.65; margin: 0; white-space: pre-wrap; word-break: break-word;
}

/* Pass banner */
.pass-banner {
  display: flex; align-items: flex-start; gap: 0.75rem;
  background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 12px;
  padding: 1rem;
}

.pass-title { font-weight: 600; color: #15803d; font-size: 0.9rem; margin: 0 0 0.2rem; }
.pass-sub   { font-size: 0.8rem; color: #16a34a; margin: 0; }

/* Violations */
.violation-list { display: flex; flex-direction: column; gap: 0.75rem; }

.violation-item {
  background: #fafbfd; border: 1px solid #eef0f6; border-radius: 12px;
  padding: 0.875rem; display: flex; flex-direction: column; gap: 0.5rem;
  border-left: 3px solid #ef4444;
}

.viol-header { display: flex; align-items: center; gap: 0.5rem; }

.viol-code {
  font-family: 'DM Mono', monospace; font-size: 0.78rem; font-weight: 500;
  background: #f1f5f9; color: #475569; padding: 0.18rem 0.45rem; border-radius: 5px;
}

.severity-badge {
  padding: 0.18rem 0.5rem; border-radius: 5px;
  font-size: 0.7rem; font-weight: 700; letter-spacing: 0.05em;
}
.severity-badge.high   { background: #fef2f2; color: #ef4444; }
.severity-badge.medium { background: #fffbeb; color: #d97706; }
.severity-badge.low    { background: #eff6ff; color: #3b82f6; }

.viol-message { font-size: 0.85rem; color: #374151; line-height: 1.5; margin: 0; }

.viol-basis, .viol-suggestion {
  display: flex; align-items: flex-start; gap: 0.4rem;
  font-size: 0.78rem; line-height: 1.5;
}

.viol-basis { color: #6b7280; }
.viol-suggestion { color: #3b82f6; }

/* ── Transitions ── */
.backdrop-fade-enter-active, .backdrop-fade-leave-active { transition: opacity 0.25s; }
.backdrop-fade-enter-from, .backdrop-fade-leave-to { opacity: 0; }

.drawer-slide-enter-active, .drawer-slide-leave-active {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.drawer-slide-enter-from, .drawer-slide-leave-to { transform: translateX(100%); }
</style>

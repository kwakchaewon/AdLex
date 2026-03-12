<template>
  <div class="rule-manage">
    <!-- Error Banner -->
    <Transition name="banner">
      <div v-if="error" class="error-banner">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
        {{ error }}
        <button class="banner-close" @click="error = null">✕</button>
      </div>
    </Transition>

    <!-- Page Header -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">규칙 관리</h1>
        <span class="rule-count">{{ rules.length }}개</span>
      </div>
      <button class="btn-new" @click="openCreate">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        새 규칙
      </button>
    </div>

    <!-- Filter Bar -->
    <div class="filter-bar">
      <div class="search-wrap">
        <svg class="search-icon" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        <input
          v-model="searchQuery"
          class="search-input"
          type="text"
          placeholder="코드 또는 규칙명 검색…"
        />
      </div>
      <div class="filter-chips">
        <button
          v-for="s in severityOptions"
          :key="s.value"
          :class="['chip', { active: severityFilter === s.value }]"
          :style="severityFilter === s.value && s.value !== 'ALL' ? { background: s.color + '18', color: s.color, borderColor: s.color + '40' } : {}"
          @click="severityFilter = s.value"
        >{{ s.label }}</button>
      </div>
      <div class="filter-chips">
        <button
          v-for="t in typeOptions"
          :key="t"
          :class="['chip', { active: typeFilter === t }]"
          @click="typeFilter = t"
        >{{ t === 'ALL' ? '전체 타입' : t }}</button>
      </div>
      <label class="active-toggle">
        <input type="checkbox" v-model="showInactive" />
        <span class="toggle-label">비활성 포함</span>
      </label>
    </div>

    <!-- Table Card -->
    <div class="table-card">
      <!-- Skeleton -->
      <template v-if="loading">
        <div v-for="i in 3" :key="i" class="skeleton-row">
          <div class="skel skel-code"></div>
          <div class="skel skel-name"></div>
          <div class="skel skel-type"></div>
          <div class="skel skel-badge"></div>
          <div class="skel skel-badge"></div>
        </div>
      </template>

      <!-- Empty -->
      <div v-else-if="filteredRules.length === 0" class="empty-state">
        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="1.5"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="12" y1="18" x2="12.01" y2="18"/><line x1="12" y1="12" x2="12" y2="15"/></svg>
        <p class="empty-title">규칙이 없습니다</p>
        <p class="empty-sub">새 규칙 버튼을 눌러 첫 규칙을 등록하세요.</p>
      </div>

      <!-- Table -->
      <table v-else class="rule-table">
        <thead>
          <tr>
            <th>코드</th>
            <th>규칙명</th>
            <th>타입</th>
            <th>채널</th>
            <th>심각도</th>
            <th>활성</th>
            <th class="th-action">액션</th>
          </tr>
        </thead>
        <tbody>
          <template v-for="rule in filteredRules" :key="rule.id">
            <tr :class="['rule-row', { inactive: !rule.active }]">
              <td>
                <span class="code-badge">{{ rule.code }}</span>
              </td>
              <td>
                <span class="rule-name">{{ rule.name }}</span>
                <span v-if="rule.description" class="rule-desc">{{ rule.description }}</span>
              </td>
              <td>
                <span class="type-tag">{{ rule.type }}</span>
              </td>
              <td>
                <div class="channel-list">
                  <span
                    v-for="ch in parseChannels(rule.channel)"
                    :key="ch"
                    :class="['ch-badge', `ch-${ch.toLowerCase()}`]"
                  >{{ ch }}</span>
                </div>
              </td>
              <td>
                <span :class="['sev-badge', `sev-${rule.severity.toLowerCase()}`]">{{ rule.severity }}</span>
              </td>
              <td>
                <button
                  :class="['active-dot', { on: rule.active }]"
                  @click="toggleActive(rule)"
                  :title="rule.active ? '비활성화' : '활성화'"
                ></button>
              </td>
              <td class="td-action">
                <template v-if="deletingId === rule.id">
                  <div class="inline-confirm">
                    <span class="confirm-text">비활성화할까요?</span>
                    <button class="btn-confirm-ok" @click="confirmDelete(rule.id)">확인</button>
                    <button class="btn-confirm-cancel" @click="deletingId = null">취소</button>
                  </div>
                </template>
                <template v-else>
                  <button class="action-btn" title="수정" @click="openEdit(rule)">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                  </button>
                  <button class="action-btn action-del" title="삭제" @click="deletingId = rule.id">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4h6v2"/></svg>
                  </button>
                </template>
              </td>
            </tr>
          </template>
        </tbody>
      </table>
    </div>

    <!-- Side Panel Overlay -->
    <Transition name="overlay">
      <div v-if="panelOpen" class="panel-overlay" @click.self="closePanel" />
    </Transition>

    <!-- Side Panel -->
    <Transition name="panel">
      <aside v-if="panelOpen" class="side-panel">
        <div class="panel-header">
          <h2 class="panel-title">{{ panelMode === 'create' ? '새 규칙 등록' : '규칙 수정' }}</h2>
          <button class="panel-close" @click="closePanel">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>

        <div class="panel-body">
          <div v-if="panelMode === 'create'" class="form-field">
            <label class="form-label">코드 <span class="req">*</span></label>
            <input v-model="form.code" class="form-input mono" placeholder="SPAM_001" />
            <p class="field-hint">고유한 규칙 식별자 (영문+숫자+언더스코어)</p>
          </div>

          <div class="form-field">
            <label class="form-label">규칙명 <span class="req">*</span></label>
            <input v-model="form.name" class="form-input" placeholder="스팸 문구 금지" />
          </div>

          <div class="form-field">
            <label class="form-label">설명</label>
            <textarea v-model="form.description" class="form-textarea" rows="2" placeholder="규칙에 대한 상세 설명"></textarea>
          </div>

          <div class="form-row-2">
            <div class="form-field">
              <label class="form-label">타입 <span class="req">*</span></label>
              <select v-model="form.type" class="form-select">
                <option value="REGEX">REGEX</option>
                <option value="KEYWORD">KEYWORD</option>
                <option value="TIME_RANGE">TIME_RANGE</option>
                <option value="FIELD_PRESENT">FIELD_PRESENT</option>
                <option value="LLM_JUDGE">LLM_JUDGE</option>
              </select>
            </div>
            <div class="form-field">
              <label class="form-label">심각도 <span class="req">*</span></label>
              <select v-model="form.severity" class="form-select">
                <option value="HIGH">HIGH</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="LOW">LOW</option>
              </select>
            </div>
          </div>

          <div class="form-field">
            <label class="form-label">채널 <span class="req">*</span></label>
            <div class="channel-checks">
              <label v-for="ch in ['SMS', 'KAKAO', 'EMAIL']" :key="ch" class="ch-check">
                <input
                  type="checkbox"
                  :value="ch"
                  :checked="selectedChannels.includes(ch)"
                  @change="toggleChannel(ch)"
                />
                <span :class="['ch-label', `ch-${ch.toLowerCase()}`]">{{ ch }}</span>
              </label>
            </div>
          </div>

          <div class="form-field">
            <label class="form-label">패턴 (정규식)</label>
            <input v-model="form.pattern" class="form-input mono" placeholder="(무료|공짜|할인).{0,5}(클릭|지금)" />
          </div>

          <div class="form-field">
            <label class="form-label">법적 근거</label>
            <input v-model="form.legalBasis" class="form-input" placeholder="정보통신망법 제50조" />
          </div>

          <div class="form-field form-field-inline">
            <label class="form-label">활성화</label>
            <button
              :class="['toggle-btn', { on: form.active }]"
              @click="form.active = !form.active"
            >
              <span class="toggle-knob"></span>
            </button>
          </div>
        </div>

        <div class="panel-footer">
          <button class="btn-cancel" @click="closePanel">취소</button>
          <button class="btn-save" :disabled="saving" @click="saveRule">
            <svg v-if="saving" class="spin" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/></svg>
            {{ saving ? '저장 중…' : '저장' }}
          </button>
        </div>
      </aside>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import {
  fetchRules,
  createRule,
  updateRule,
  deleteRule,
  type RuleResponse,
  type RuleType,
  type Severity,
} from '@/api/admin'

// ── State ──────────────────────────────────────────────────────────
const rules = ref<RuleResponse[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)

const searchQuery = ref('')
const severityFilter = ref('ALL')
const typeFilter = ref('ALL')
const showInactive = ref(false)

const panelOpen = ref(false)
const panelMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const deletingId = ref<number | null>(null)

const selectedChannels = ref<string[]>(['SMS'])

const form = reactive({
  code: '',
  name: '',
  description: '',
  type: 'KEYWORD' as RuleType,
  severity: 'HIGH' as Severity,
  pattern: '',
  legalBasis: '',
  active: true,
})

// ── Options ────────────────────────────────────────────────────────
const severityOptions = [
  { value: 'ALL', label: '전체', color: '#64748b' },
  { value: 'HIGH', label: 'HIGH', color: '#ef4444' },
  { value: 'MEDIUM', label: 'MEDIUM', color: '#f59e0b' },
  { value: 'LOW', label: 'LOW', color: '#3b82f6' },
]
const typeOptions = ['ALL', 'REGEX', 'KEYWORD', 'TIME_RANGE', 'FIELD_PRESENT', 'LLM_JUDGE']

// ── Computed ───────────────────────────────────────────────────────
const filteredRules = computed(() => {
  const q = searchQuery.value.toLowerCase()
  return rules.value.filter((r) => {
    if (!showInactive.value && !r.active) return false
    if (severityFilter.value !== 'ALL' && r.severity !== severityFilter.value) return false
    if (typeFilter.value !== 'ALL' && r.type !== typeFilter.value) return false
    if (q && !r.code.toLowerCase().includes(q) && !r.name.toLowerCase().includes(q)) return false
    return true
  })
})

// ── Helpers ────────────────────────────────────────────────────────
function parseChannels(channel: string): string[] {
  return channel.split(',').map((c) => c.trim()).filter(Boolean)
}

function toggleChannel(ch: string) {
  const idx = selectedChannels.value.indexOf(ch)
  if (idx >= 0) selectedChannels.value.splice(idx, 1)
  else selectedChannels.value.push(ch)
}

// ── API ────────────────────────────────────────────────────────────
async function loadRules() {
  loading.value = true
  error.value = null
  try {
    rules.value = await fetchRules()
  } catch {
    error.value = '규칙 목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  panelMode.value = 'create'
  editingId.value = null
  Object.assign(form, { code: '', name: '', description: '', type: 'KEYWORD', severity: 'HIGH', pattern: '', legalBasis: '', active: true })
  selectedChannels.value = ['SMS']
  panelOpen.value = true
}

function openEdit(rule: RuleResponse) {
  panelMode.value = 'edit'
  editingId.value = rule.id
  Object.assign(form, {
    code: rule.code,
    name: rule.name,
    description: rule.description ?? '',
    type: rule.type,
    severity: rule.severity,
    pattern: rule.pattern ?? '',
    legalBasis: rule.legalBasis ?? '',
    active: rule.active,
  })
  selectedChannels.value = parseChannels(rule.channel)
  panelOpen.value = true
}

function closePanel() {
  panelOpen.value = false
}

async function saveRule() {
  if (!form.name.trim()) { error.value = '규칙명을 입력하세요.'; return }
  if (panelMode.value === 'create' && !form.code.trim()) { error.value = '코드를 입력하세요.'; return }
  if (selectedChannels.value.length === 0) { error.value = '채널을 하나 이상 선택하세요.'; return }

  saving.value = true
  error.value = null
  const channelStr = selectedChannels.value.join(',')
  try {
    if (panelMode.value === 'create') {
      const created = await createRule({
        code: form.code,
        name: form.name,
        description: form.description || undefined,
        type: form.type,
        channel: channelStr,
        severity: form.severity,
        pattern: form.pattern || undefined,
        legalBasis: form.legalBasis || undefined,
        active: form.active,
      })
      rules.value.push(created)
    } else {
      const updated = await updateRule(editingId.value!, {
        name: form.name,
        description: form.description || undefined,
        type: form.type,
        channel: channelStr,
        severity: form.severity,
        pattern: form.pattern || undefined,
        legalBasis: form.legalBasis || undefined,
        active: form.active,
      })
      const idx = rules.value.findIndex((r) => r.id === editingId.value)
      if (idx >= 0) rules.value[idx] = updated
    }
    closePanel()
  } catch {
    error.value = '저장에 실패했습니다. 다시 시도하세요.'
  } finally {
    saving.value = false
  }
}

async function toggleActive(rule: RuleResponse) {
  try {
    const updated = await updateRule(rule.id, { active: !rule.active })
    const idx = rules.value.findIndex((r) => r.id === rule.id)
    if (idx >= 0) rules.value[idx] = updated
  } catch {
    error.value = '상태 변경에 실패했습니다.'
  }
}

async function confirmDelete(id: number) {
  try {
    await deleteRule(id)
    rules.value = rules.value.filter((r) => r.id !== id)
  } catch {
    error.value = '삭제에 실패했습니다.'
  } finally {
    deletingId.value = null
  }
}

onMounted(loadRules)
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600&family=DM+Mono:wght@400;500&family=Syne:wght@600;700&display=swap');

.rule-manage {
  padding: 2rem 2.5rem;
  min-height: 100%;
  background: #f8fafc;
  font-family: 'DM Sans', sans-serif;
  position: relative;
}

/* ── Error Banner ── */
.error-banner {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #dc2626;
  padding: 0.75rem 1rem;
  border-radius: 10px;
  font-size: 0.875rem;
  margin-bottom: 1.25rem;
}
.banner-close {
  margin-left: auto;
  background: none;
  border: none;
  color: #dc2626;
  cursor: pointer;
  font-size: 0.875rem;
  opacity: 0.7;
  padding: 0 0.25rem;
}
.banner-close:hover { opacity: 1; }

.banner-enter-active, .banner-leave-active { transition: all 0.25s ease; }
.banner-enter-from, .banner-leave-to { opacity: 0; transform: translateY(-8px); }

/* ── Page Header ── */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
}
.header-left { display: flex; align-items: center; gap: 0.75rem; }
.page-title {
  font-family: 'Syne', sans-serif;
  font-size: 1.65rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
  letter-spacing: -0.02em;
}
.rule-count {
  background: #e0f2fe;
  color: #0369a1;
  font-size: 0.8rem;
  font-weight: 600;
  padding: 0.25rem 0.625rem;
  border-radius: 999px;
  font-family: 'DM Mono', monospace;
}
.btn-new {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: #3b82f6;
  color: #fff;
  border: none;
  padding: 0.625rem 1.125rem;
  border-radius: 10px;
  font-family: 'DM Sans', sans-serif;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, transform 0.1s;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.35);
}
.btn-new:hover { background: #2563eb; transform: translateY(-1px); }
.btn-new:active { transform: translateY(0); }

/* ── Filter Bar ── */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1.25rem;
  flex-wrap: wrap;
}
.search-wrap {
  position: relative;
  flex: 0 0 220px;
}
.search-icon {
  position: absolute;
  left: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
  pointer-events: none;
}
.search-input {
  width: 100%;
  padding: 0.5rem 0.75rem 0.5rem 2.25rem;
  border: 1.5px solid #e2e8f0;
  border-radius: 9px;
  font-family: 'DM Sans', sans-serif;
  font-size: 0.875rem;
  color: #1e293b;
  background: #fff;
  outline: none;
  transition: border-color 0.15s;
  box-sizing: border-box;
}
.search-input:focus { border-color: #3b82f6; }
.search-input::placeholder { color: #b0bec5; }

.filter-chips { display: flex; gap: 0.375rem; }
.chip {
  padding: 0.4rem 0.75rem;
  border-radius: 8px;
  border: 1.5px solid #e2e8f0;
  background: #fff;
  color: #64748b;
  font-size: 0.78rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
  font-family: 'DM Sans', sans-serif;
}
.chip:hover { border-color: #cbd5e1; background: #f8fafc; }
.chip.active { background: #eff6ff; color: #3b82f6; border-color: #bfdbfe; }

.active-toggle {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  margin-left: auto;
}
.active-toggle input { cursor: pointer; accent-color: #3b82f6; }
.toggle-label { font-size: 0.8rem; color: #64748b; font-weight: 500; user-select: none; }

/* ── Table Card ── */
.table-card {
  background: #fff;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
}

/* Skeleton */
.skeleton-row {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  padding: 1.1rem 1.5rem;
  border-bottom: 1px solid #f1f5f9;
}
.skel {
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
  background-size: 200% 100%;
  border-radius: 6px;
  height: 18px;
  animation: shimmer 1.4s infinite;
}
.skel-code { width: 90px; }
.skel-name { flex: 1; }
.skel-type { width: 80px; }
.skel-badge { width: 60px; }

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* Empty */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 4rem 2rem;
  color: #94a3b8;
}
.empty-title { font-size: 1rem; font-weight: 600; color: #64748b; margin: 0; }
.empty-sub { font-size: 0.875rem; margin: 0; }

/* Table */
.rule-table {
  width: 100%;
  border-collapse: collapse;
}
.rule-table thead th {
  background: #f8fafc;
  padding: 0.75rem 1.25rem;
  text-align: left;
  font-size: 0.72rem;
  font-weight: 600;
  color: #64748b;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  border-bottom: 1px solid #e2e8f0;
}
.th-action { text-align: right; }

.rule-row td {
  padding: 0.875rem 1.25rem;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
  font-size: 0.875rem;
  color: #1e293b;
}
.rule-row:last-child td { border-bottom: none; }
.rule-row:hover td { background: #f8faff; }
.rule-row.inactive { opacity: 0.55; }

.code-badge {
  font-family: 'DM Mono', monospace;
  font-size: 0.78rem;
  background: #eff6ff;
  color: #2563eb;
  padding: 0.25rem 0.5rem;
  border-radius: 6px;
  font-weight: 500;
  white-space: nowrap;
}

.rule-name { display: block; font-weight: 500; color: #0f172a; }
.rule-desc { display: block; font-size: 0.78rem; color: #94a3b8; margin-top: 2px; }

.type-tag {
  font-family: 'DM Mono', monospace;
  font-size: 0.72rem;
  background: #f1f5f9;
  color: #475569;
  padding: 0.2rem 0.5rem;
  border-radius: 5px;
}

.channel-list { display: flex; gap: 0.25rem; flex-wrap: wrap; }
.ch-badge {
  font-size: 0.68rem;
  font-weight: 600;
  padding: 0.18rem 0.45rem;
  border-radius: 5px;
  letter-spacing: 0.04em;
}
.ch-sms { background: #f1f5f9; color: #475569; }
.ch-kakao { background: #fefce8; color: #a16207; }
.ch-email { background: #f0f9ff; color: #0284c7; }

.sev-badge {
  font-size: 0.72rem;
  font-weight: 700;
  padding: 0.25rem 0.55rem;
  border-radius: 6px;
  letter-spacing: 0.04em;
}
.sev-high { background: #fef2f2; color: #ef4444; }
.sev-medium { background: #fffbeb; color: #f59e0b; }
.sev-low { background: #eff6ff; color: #3b82f6; }

.active-dot {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 2px solid #e2e8f0;
  background: #e2e8f0;
  cursor: pointer;
  transition: all 0.2s;
  display: block;
}
.active-dot.on {
  background: #22c55e;
  border-color: #22c55e;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.15);
}
.active-dot:hover { opacity: 0.8; }

.td-action { text-align: right; }
.action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 7px;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  transition: all 0.15s;
  margin-left: 2px;
}
.action-btn:hover { background: #f1f5f9; color: #1e293b; }
.action-del:hover { background: #fef2f2; color: #ef4444; }

.inline-confirm {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  justify-content: flex-end;
}
.confirm-text { font-size: 0.78rem; color: #64748b; }
.btn-confirm-ok {
  padding: 0.3rem 0.7rem;
  border-radius: 6px;
  border: none;
  background: #ef4444;
  color: #fff;
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
}
.btn-confirm-ok:hover { background: #dc2626; }
.btn-confirm-cancel {
  padding: 0.3rem 0.7rem;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background: #fff;
  color: #64748b;
  font-size: 0.78rem;
  font-weight: 500;
  cursor: pointer;
}
.btn-confirm-cancel:hover { background: #f8fafc; }

/* ── Side Panel ── */
.panel-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(2px);
  z-index: 100;
}
.overlay-enter-active, .overlay-leave-active { transition: opacity 0.25s ease; }
.overlay-enter-from, .overlay-leave-to { opacity: 0; }

.side-panel {
  position: fixed;
  top: 0;
  right: 0;
  width: 480px;
  height: 100vh;
  background: #fff;
  z-index: 101;
  display: flex;
  flex-direction: column;
  box-shadow: -4px 0 32px rgba(0,0,0,0.12);
}
.panel-enter-active, .panel-leave-active { transition: transform 0.28s cubic-bezier(0.4, 0, 0.2, 1); }
.panel-enter-from, .panel-leave-to { transform: translateX(100%); }

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #f1f5f9;
  flex-shrink: 0;
}
.panel-title {
  font-family: 'Syne', sans-serif;
  font-size: 1.15rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
  letter-spacing: -0.01em;
}
.panel-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  transition: all 0.15s;
}
.panel-close:hover { background: #f1f5f9; color: #1e293b; }

.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
}

.form-field { display: flex; flex-direction: column; gap: 0.375rem; }
.form-field-inline { flex-direction: row; align-items: center; justify-content: space-between; }
.form-row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
.form-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: #374151;
  letter-spacing: 0.01em;
}
.req { color: #ef4444; margin-left: 2px; }
.field-hint { font-size: 0.74rem; color: #94a3b8; margin: 0; }

.form-input, .form-textarea, .form-select {
  padding: 0.55rem 0.75rem;
  border: 1.5px solid #e2e8f0;
  border-radius: 8px;
  font-family: 'DM Sans', sans-serif;
  font-size: 0.875rem;
  color: #1e293b;
  background: #fff;
  outline: none;
  transition: border-color 0.15s;
  width: 100%;
  box-sizing: border-box;
}
.form-input:focus, .form-textarea:focus, .form-select:focus { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1); }
.mono { font-family: 'DM Mono', monospace; font-size: 0.82rem; }
.form-textarea { resize: vertical; min-height: 64px; }
.form-select { cursor: pointer; }

.channel-checks { display: flex; gap: 0.625rem; }
.ch-check { display: flex; align-items: center; gap: 0.375rem; cursor: pointer; }
.ch-check input { cursor: pointer; accent-color: #3b82f6; width: 14px; height: 14px; }
.ch-label {
  font-size: 0.78rem;
  font-weight: 600;
  padding: 0.25rem 0.6rem;
  border-radius: 6px;
  cursor: pointer;
  letter-spacing: 0.04em;
}

/* Toggle Button */
.toggle-btn {
  width: 42px;
  height: 24px;
  border-radius: 999px;
  border: none;
  background: #e2e8f0;
  cursor: pointer;
  position: relative;
  transition: background 0.2s;
  flex-shrink: 0;
}
.toggle-btn.on { background: #22c55e; }
.toggle-knob {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0,0,0,0.2);
  transition: transform 0.2s;
}
.toggle-btn.on .toggle-knob { transform: translateX(18px); }

.panel-footer {
  display: flex;
  gap: 0.625rem;
  padding: 1.25rem 1.5rem;
  border-top: 1px solid #f1f5f9;
  flex-shrink: 0;
  justify-content: flex-end;
}
.btn-cancel {
  padding: 0.625rem 1.25rem;
  border-radius: 9px;
  border: 1.5px solid #e2e8f0;
  background: #fff;
  color: #64748b;
  font-family: 'DM Sans', sans-serif;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-cancel:hover { background: #f8fafc; border-color: #cbd5e1; }
.btn-save {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.5rem;
  border-radius: 9px;
  border: none;
  background: #3b82f6;
  color: #fff;
  font-family: 'DM Sans', sans-serif;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.35);
}
.btn-save:hover:not(:disabled) { background: #2563eb; }
.btn-save:disabled { opacity: 0.65; cursor: not-allowed; }

.spin {
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .rule-manage { padding: 1.25rem; }
  .side-panel { width: 100%; }
  .filter-bar { flex-direction: column; align-items: flex-start; }
  .search-wrap { flex: none; width: 100%; }
}
</style>

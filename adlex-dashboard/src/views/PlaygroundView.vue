<template>
  <div class="playground">
    <div class="pg-grid">
      <section class="panel input-panel">
        <div class="panel-header">
          <span class="panel-label">INPUT</span>
          <span class="panel-tag">POST /v1/check</span>
        </div>
        <div class="field-group">
          <label class="field-label">API Key</label>
          <div class="key-input-wrap">
            <input :type="showKey ? 'text' : 'password'" v-model="apiKey" class="field-input mono" placeholder="adlex_sk_..." autocomplete="off" spellcheck="false" />
            <button class="key-toggle" @click="showKey = !showKey" type="button">
              <svg v-if="showKey" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94"/><path d="M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
              <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
            </button>
          </div>
        </div>
        <div class="field-group">
          <label class="field-label">채널</label>
          <div class="seg-group">
            <button v-for="ch in channels" :key="ch" class="seg-btn" :class="{ active: form.channel === ch }" @click="form.channel = ch" type="button">{{ ch }}</button>
          </div>
        </div>
        <div class="field-group">
          <div class="field-label-row">
            <label class="field-label">메시지 원문</label>
            <span class="char-count" :class="{ warn: form.message.length > 1800, over: form.message.length > 2000 }">{{ form.message.length }}<span class="char-max">/2000</span></span>
          </div>
          <textarea v-model="form.message" class="msg-textarea mono" placeholder="검사할 광고 메시지를 입력하세요..." maxlength="2000" spellcheck="false" />
        </div>
        <div class="field-group">
          <button class="collapse-toggle" @click="showSender = !showSender" type="button">
            <svg class="collapse-icon" :class="{ open: showSender }" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg>
            <span class="field-label">발신자 정보</span>
            <span class="field-hint">선택사항</span>
          </button>
          <div class="sender-fields" :class="{ visible: showSender }">
            <div class="sender-grid">
              <input v-model="sender.name" class="field-input" placeholder="발신자명" />
              <input v-model="sender.phoneNumber" class="field-input" placeholder="연락처 (010-0000-0000)" />
              <input v-model="sender.email" class="field-input" placeholder="이메일" type="email" />
            </div>
          </div>
        </div>
        <div class="field-group">
          <label class="field-label">발송 예정 시간 <span class="field-hint">선택사항</span></label>
          <input v-model="scheduledAt" class="field-input" type="datetime-local" />
        </div>
        <button class="run-btn" @click="runCheck" :disabled="loading || !canSubmit" type="button">
          <span v-if="loading" class="spinner" />
          <svg v-else width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polygon points="5 3 19 12 5 21 5 3"/></svg>
          {{ loading ? '검사 중...' : '검사 실행' }}
        </button>
        <p v-if="!canSubmit && !loading" class="submit-hint">API Key와 메시지를 입력하세요</p>
      </section>

      <section class="panel result-panel">
        <div class="panel-header">
          <span class="panel-label">RESULT</span>
          <span v-if="result" class="panel-tag mono">{{ result.processingMs }}ms</span>
        </div>
        <div v-if="!loading && !result && !error" class="result-idle">
          <div class="idle-icon">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/></svg>
          </div>
          <p class="idle-title">검사 대기 중</p>
          <p class="idle-desc">왼쪽 패널에서 메시지를 입력하고<br/>검사 실행 버튼을 누르세요.</p>
        </div>
        <div v-if="loading" class="result-skeleton">
          <div class="skel-badge" /><div class="skel-line w80" /><div class="skel-line w60" /><div class="skel-card" /><div class="skel-card" />
        </div>
        <div v-if="error && !loading" class="result-error">
          <div class="err-icon"><svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg></div>
          <p class="err-title">요청 실패</p>
          <p class="err-msg">{{ error }}</p>
        </div>
        <div v-if="result && !loading" class="result-body fade-in">
          <div class="status-row">
            <div class="status-badge" :class="result.compliant ? 'compliant' : 'violation'">
              <svg v-if="result.compliant" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
              <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              <span>{{ result.compliant ? '준수' : '위반' }}</span>
            </div>
            <div v-if="!result.compliant" class="violation-count-badge">{{ result.violationCount }}건</div>
          </div>
          <p class="status-summary" :class="result.compliant ? 'text-green' : 'text-red'">
            {{ result.compliant ? '모든 규칙을 통과했습니다.' : result.violationCount + '개의 위반 항목이 발견되었습니다.' }}
          </p>
          <div class="result-meta">
            <span class="meta-item"><svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>{{ result.processingMs }}ms</span>
            <span class="meta-sep">·</span>
            <span class="meta-item mono">{{ fmtDate(result.checkedAt) }}</span>
          </div>
          <div v-if="result.violations.length" class="violations-list">
            <div v-for="(v, i) in result.violations" :key="i" class="v-card" :class="'sev-' + v.severity.toLowerCase()" :style="{ animationDelay: i * 60 + 'ms' }">
              <div class="v-card-top">
                <span class="v-rule-code mono">{{ v.ruleCode }}</span>
                <span class="v-severity" :class="'sev-label-' + v.severity.toLowerCase()">{{ v.severity }}</span>
              </div>
              <p class="v-message">{{ v.message }}</p>
              <p v-if="v.legalBasis" class="v-legal">
                <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
                {{ v.legalBasis }}
              </p>
              <p v-if="v.suggestion" class="v-suggestion"><span class="sug-label">개선 제안</span>{{ v.suggestion }}</p>
            </div>
          </div>
          <div v-if="result.compliant" class="compliant-visual">
            <svg width="64" height="64" viewBox="0 0 24 24" fill="none">
              <circle cx="12" cy="12" r="11" stroke="#bbf7d0" stroke-width="1.5"/>
              <polyline points="7 12 10.5 15.5 17 9" stroke="#22c55e" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { runCheck as apiRunCheck } from '@/api/check'
import type { Channel, CheckResponse, SenderInfo } from '@/types/check'

const LS_KEY = 'pg_api_key'
const channels: Channel[] = ['SMS', 'KAKAO', 'EMAIL']

const apiKey      = ref('')
const showKey     = ref(false)
const showSender  = ref(false)
const scheduledAt = ref('')
const loading     = ref(false)
const result      = ref<CheckResponse | null>(null)
const error       = ref('')

const form   = ref<{ message: string; channel: Channel }>({ message: '', channel: 'SMS' })
const sender = ref<SenderInfo>({ name: '', phoneNumber: '', email: '' })

const canSubmit = computed(() => apiKey.value.trim() !== '' && form.value.message.trim() !== '')

onMounted(() => {
  const saved = localStorage.getItem(LS_KEY)
  if (saved) apiKey.value = saved
})

async function runCheck() {
  if (!canSubmit.value) return
  loading.value = true
  result.value  = null
  error.value   = ''
  localStorage.setItem(LS_KEY, apiKey.value.trim())

  const sp: SenderInfo = {}
  if (sender.value.name?.trim())        sp.name = sender.value.name.trim()
  if (sender.value.phoneNumber?.trim()) sp.phoneNumber = sender.value.phoneNumber.trim()
  if (sender.value.email?.trim())       sp.email = sender.value.email.trim()

  try {
    result.value = await apiRunCheck(apiKey.value.trim(), {
      message:     form.value.message.trim(),
      channel:     form.value.channel,
      sender:      Object.keys(sp).length ? sp : undefined,
      scheduledAt: scheduledAt.value ? new Date(scheduledAt.value).toISOString() : undefined,
    })
  } catch (e: unknown) {
    const err = e as { response?: { data?: { error?: { message?: string } }; status?: number } }
    error.value =
      err.response?.data?.error?.message ??
      (err.response?.status === 401 ? 'API Key가 유효하지 않습니다.' :
       err.response?.status === 429 ? 'Rate limit을 초과했습니다.' :
       '요청에 실패했습니다. API Key를 확인해주세요.')
  } finally {
    loading.value = false
  }
}

function fmtDate(iso: string) {
  return new Date(iso).toLocaleString('ko-KR', {
    month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit',
  })
}
</script>

<style scoped>

.playground { font-family: 'DM Sans', sans-serif; padding: 1.5rem 2rem; background: #f8f9fc; min-height: calc(100vh - 60px); }
.pg-grid { display: grid; grid-template-columns: 420px 1fr; gap: 1.25rem; max-width: 1140px; margin: 0 auto; align-items: start; }
.panel { background: #ffffff; border: 1px solid #e9ecf3; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,.05); padding: 1.5rem; display: flex; flex-direction: column; gap: 1.125rem; }
.panel-header { display: flex; align-items: center; justify-content: space-between; padding-bottom: 1rem; border-bottom: 1px solid #e9ecf3; }
.panel-label { font-family: 'DM Mono', monospace; font-size: 0.7rem; font-weight: 500; letter-spacing: .1em; color: #6b7280; }
.panel-tag { font-family: 'DM Mono', monospace; font-size: 0.7rem; color: #3b82f6; background: #eff6ff; border: 1px solid #bfdbfe; border-radius: 6px; padding: 2px 8px; }
.field-group { display: flex; flex-direction: column; gap: .45rem; }
.field-label { font-size: .775rem; font-weight: 600; color: #374151; display: flex; align-items: center; gap: .4rem; }
.field-label-row { display: flex; align-items: center; justify-content: space-between; }
.field-hint { font-size: .72rem; font-weight: 400; color: #6b7280; }
.field-input { width: 100%; padding: .55rem .75rem; border: 1.5px solid #e9ecf3; border-radius: 8px; font-family: 'DM Sans', sans-serif; font-size: .85rem; color: #0d1530; background: #fff; transition: border-color .15s, box-shadow .15s; box-sizing: border-box; outline: none; }
.field-input:focus { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,.12); }
.field-input.mono { font-family: 'DM Mono', monospace; font-size: .8rem; }
.key-input-wrap { position: relative; }
.key-input-wrap .field-input { padding-right: 2.5rem; }
.key-toggle { position: absolute; right: .625rem; top: 50%; transform: translateY(-50%); background: none; border: none; cursor: pointer; color: #6b7280; display: flex; align-items: center; padding: 4px; border-radius: 4px; transition: color .15s; }
.key-toggle:hover { color: #0d1530; }
.seg-group { display: flex; border: 1.5px solid #e9ecf3; border-radius: 8px; overflow: hidden; }
.seg-btn { flex: 1; padding: .5rem; background: none; border: none; font-family: 'DM Mono', monospace; font-size: .78rem; font-weight: 500; color: #6b7280; cursor: pointer; transition: background .15s, color .15s; letter-spacing: .04em; }
.seg-btn + .seg-btn { border-left: 1.5px solid #e9ecf3; }
.seg-btn.active { background: #eff6ff; color: #3b82f6; }
.seg-btn:hover:not(.active) { background: #f9fafb; color: #0d1530; }
.msg-textarea { resize: vertical; min-height: 200px; padding: .75rem; border: 1.5px solid #e9ecf3; border-radius: 8px; font-family: 'DM Mono', monospace; font-size: .8rem; line-height: 1.6; color: #0d1530; background: #fafafa; outline: none; transition: border-color .15s, box-shadow .15s; box-sizing: border-box; width: 100%; }
.msg-textarea:focus { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,.12); background: #fff; }
.char-count { font-family: 'DM Mono', monospace; font-size: .72rem; color: #6b7280; }
.char-count.warn { color: #f59e0b; }
.char-count.over { color: #ef4444; }
.char-max { opacity: .5; }
.collapse-toggle { display: flex; align-items: center; gap: .45rem; background: none; border: none; cursor: pointer; padding: 0; }
.collapse-icon { color: #6b7280; transition: transform .2s; flex-shrink: 0; }
.collapse-icon.open { transform: rotate(0deg); }
.collapse-icon:not(.open) { transform: rotate(-90deg); }
.sender-fields { max-height: 0; overflow: hidden; transition: max-height .25s ease; }
.sender-fields.visible { max-height: 200px; }
.sender-grid { display: flex; flex-direction: column; gap: .5rem; padding-top: .5rem; }
.run-btn { display: flex; align-items: center; justify-content: center; gap: .5rem; width: 100%; padding: .75rem; background: #3b82f6; color: #fff; border: none; border-radius: 9px; font-family: 'DM Sans', sans-serif; font-size: .9rem; font-weight: 600; cursor: pointer; transition: background .15s, transform .1s, box-shadow .15s; letter-spacing: -.01em; margin-top: .25rem; }
.run-btn:hover:not(:disabled) { background: #2563eb; box-shadow: 0 4px 12px rgba(59,130,246,.3); }
.run-btn:active:not(:disabled) { transform: scale(.98); }
.run-btn:disabled { opacity: .55; cursor: not-allowed; }
.submit-hint { text-align: center; font-size: .75rem; color: #6b7280; margin: -.5rem 0 0; }
.spinner { width: 15px; height: 15px; border: 2.5px solid rgba(255,255,255,.35); border-top-color: #fff; border-radius: 50%; animation: spin .7s linear infinite; flex-shrink: 0; }
@keyframes spin { to { transform: rotate(360deg); } }
.result-idle { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 340px; gap: 1rem; color: #6b7280; text-align: center; }
.idle-icon  { opacity: .35; }
.idle-title { font-family: 'Syne', sans-serif; font-size: 1rem; font-weight: 600; color: #9ca3af; margin: 0; }
.idle-desc  { font-size: .825rem; color: #d1d5db; margin: 0; line-height: 1.6; }
.result-skeleton { display: flex; flex-direction: column; gap: .875rem; padding: .5rem 0; animation: pulse 1.5s ease-in-out infinite; }
@keyframes pulse { 0%,100% { opacity: 1 } 50% { opacity: .45 } }
.skel-badge { width: 90px; height: 36px; background: #e9ecf3; border-radius: 9px; }
.skel-line  { height: 14px; background: #e9ecf3; border-radius: 6px; }
.skel-line.w80 { width: 80%; }
.skel-line.w60 { width: 60%; }
.skel-card  { height: 90px; background: #e9ecf3; border-radius: 10px; }
.result-error { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 240px; gap: .75rem; text-align: center; }
.err-icon  { color: #ef4444; opacity: .7; }
.err-title { font-family: 'Syne', sans-serif; font-size: 1rem; font-weight: 600; color: #ef4444; margin: 0; }
.err-msg   { font-size: .825rem; color: #6b7280; margin: 0; }
.fade-in { animation: fadeUp .3s ease both; }
@keyframes fadeUp { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
.result-body { display: flex; flex-direction: column; gap: 1rem; }
.status-row  { display: flex; align-items: center; gap: .75rem; }
.status-badge { display: inline-flex; align-items: center; gap: .45rem; padding: .45rem 1rem; border-radius: 100px; font-family: 'Syne', sans-serif; font-size: .95rem; font-weight: 700; letter-spacing: .02em; }
.status-badge.compliant { background: #f0fdf4; color: #15803d; border: 1.5px solid #bbf7d0; }
.status-badge.violation { background: #fef2f2; color: #b91c1c; border: 1.5px solid #fecaca; }
.violation-count-badge { font-family: 'DM Mono', monospace; font-size: .78rem; font-weight: 500; background: #fef2f2; color: #ef4444; border: 1.5px solid #fecaca; border-radius: 100px; padding: .2rem .65rem; }
.status-summary { font-size: .875rem; font-weight: 500; margin: -.25rem 0 0; }
.text-green { color: #16a34a; }
.text-red   { color: #dc2626; }
.result-meta { display: flex; align-items: center; gap: .5rem; font-size: .75rem; color: #6b7280; }
.meta-item { display: flex; align-items: center; gap: .3rem; }
.meta-sep  { opacity: .4; }
.violations-list { display: flex; flex-direction: column; gap: .625rem; margin-top: .25rem; }
.v-card { position: relative; padding: .875rem 1rem .875rem 1.25rem; border-radius: 10px; border: 1px solid #e9ecf3; background: #fff; overflow: hidden; animation: fadeUp .3s ease both; }
.v-card::before { content: ''; position: absolute; left: 0; top: 0; bottom: 0; width: 4px; border-radius: 2px 0 0 2px; }
.v-card.sev-high            { background: #fff8f8; }
.v-card.sev-high::before    { background: #ef4444; }
.v-card.sev-medium          { background: #fffbf0; }
.v-card.sev-medium::before  { background: #f59e0b; }
.v-card.sev-low::before     { background: #3b82f6; }
.v-card-top { display: flex; align-items: center; gap: .625rem; margin-bottom: .45rem; }
.v-rule-code { font-family: 'DM Mono', monospace; font-size: .7rem; font-weight: 500; background: #f1f5f9; color: #475569; border: 1px solid #e2e8f0; border-radius: 5px; padding: 2px 7px; }
.v-severity { font-family: 'DM Mono', monospace; font-size: .68rem; font-weight: 500; letter-spacing: .06em; border-radius: 4px; padding: 2px 6px; }
.sev-label-high   { background: #fee2e2; color: #b91c1c; }
.sev-label-medium { background: #fef3c7; color: #92400e; }
.sev-label-low    { background: #dbeafe; color: #1e40af; }
.v-message { font-size: .845rem; font-weight: 600; color: #0d1530; margin: 0 0 .375rem; line-height: 1.5; }
.v-legal { display: flex; align-items: flex-start; gap: .35rem; font-size: .775rem; color: #6b7280; font-style: italic; margin: 0 0 .35rem; line-height: 1.5; }
.v-legal svg { flex-shrink: 0; margin-top: 2px; }
.v-suggestion { font-size: .78rem; color: #1d4ed8; margin: 0; line-height: 1.55; }
.sug-label { font-family: 'DM Mono', monospace; font-size: .68rem; font-weight: 600; color: #3b82f6; background: #eff6ff; border-radius: 4px; padding: 1px 6px; margin-right: .4rem; }
.compliant-visual { display: flex; justify-content: center; padding: 1.5rem 0 .5rem; animation: scaleIn .4s cubic-bezier(.175,.885,.32,1.275) both; animation-delay: .1s; }
@keyframes scaleIn { from { opacity: 0; transform: scale(.6); } to { opacity: 1; transform: scale(1); } }
.mono { font-family: 'DM Mono', monospace; }
@media (max-width: 900px) {
  .pg-grid    { grid-template-columns: 1fr; }
  .playground { padding: 1rem; }
}
</style>

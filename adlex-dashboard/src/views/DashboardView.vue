<template>
  <div class="dashboard">
    <!-- Top bar: title + period selector -->
    <div class="dash-topbar">
      <div>
        <p class="dash-eyebrow">개요</p>
        <h2 class="dash-heading">검사 현황</h2>
      </div>
      <div class="period-tabs" role="group" aria-label="기간 선택">
        <button
          v-for="d in [7, 30, 90]"
          :key="d"
          :class="['period-btn', { active: selectedDays === d }]"
          @click="changePeriod(d)"
        >{{ d }}일</button>
      </div>
    </div>

    <!-- Loading skeleton -->
    <template v-if="loading">
      <div class="skeleton-grid">
        <div v-for="i in 4" :key="i" class="skeleton-card" />
      </div>
      <div class="skeleton-chart" />
    </template>

    <!-- Error -->
    <div v-else-if="error" class="error-state">
      <p>{{ error }}</p>
      <button class="retry-btn" @click="load">다시 시도</button>
    </div>

    <template v-else-if="stats">
      <!-- Stat cards -->
      <div class="stat-grid">
        <div class="stat-card" :style="{ '--accent': '#3b82f6' }">
          <div class="stat-icon-wrap" style="background: rgba(59,130,246,0.1)">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" stroke-width="2" stroke-linecap="round"><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></svg>
          </div>
          <p class="stat-label">총 검사 건수</p>
          <p class="stat-value">{{ fmt(stats.summary.totalChecks) }}</p>
          <p class="stat-sub">최근 {{ stats.summary.days }}일</p>
        </div>

        <div class="stat-card" :style="{ '--accent': complianceColor }">
          <div class="stat-icon-wrap" :style="{ background: `${complianceColor}18` }">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" :stroke="complianceColor" stroke-width="2" stroke-linecap="round"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
          </div>
          <p class="stat-label">준수율</p>
          <p class="stat-value">{{ compliancePct }}%</p>
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: compliancePct + '%', background: complianceColor }" />
          </div>
        </div>

        <div class="stat-card" :style="{ '--accent': '#ef4444' }">
          <div class="stat-icon-wrap" style="background: rgba(239,68,68,0.1)">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#ef4444" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
          </div>
          <p class="stat-label">위반 건수</p>
          <p class="stat-value">{{ fmt(stats.summary.violationChecks) }}</p>
          <p class="stat-sub">즉시 검토 필요</p>
        </div>

        <div class="stat-card" :style="{ '--accent': '#8b5cf6' }">
          <div class="stat-icon-wrap" style="background: rgba(139,92,246,0.1)">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#8b5cf6" stroke-width="2" stroke-linecap="round"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>
          </div>
          <p class="stat-label">채널 수</p>
          <p class="stat-value">{{ stats.channels.length }}</p>
          <p class="stat-sub">{{ channelNames }}</p>
        </div>
      </div>

      <!-- Daily trend chart -->
      <div class="chart-card" v-if="chartData.length">
        <div class="chart-header">
          <h3 class="chart-title">일별 검사 추이</h3>
          <div class="chart-legend">
            <span class="legend-dot" style="background:#3b82f6" />준수
            <span class="legend-dot" style="background:#ef4444; margin-left:0.75rem" />위반
          </div>
        </div>
        <div class="bar-chart" role="img" aria-label="일별 검사 추이 차트">
          <div
            v-for="(day, i) in chartData"
            :key="day.date"
            class="bar-col"
            :style="{ animationDelay: `${i * 20}ms` }"
          >
            <div class="bar-stack">
              <div class="bar-segment violations"
                :style="{ height: barPct(day.violations, maxTotal) + '%' }"
                :title="`위반 ${day.violations}건`" />
              <div class="bar-segment compliant"
                :style="{ height: barPct(day.compliant, maxTotal) + '%' }"
                :title="`준수 ${day.compliant}건`" />
            </div>
            <span class="bar-label">{{ formatDate(day.date) }}</span>
          </div>
        </div>
      </div>

      <!-- Channel breakdown -->
      <div class="channel-card" v-if="stats.channels.length">
        <h3 class="chart-title" style="margin-bottom: 1.25rem">채널별 준수율</h3>
        <div class="channel-list">
          <div v-for="ch in stats.channels" :key="ch.channel" class="channel-row">
            <div class="channel-info">
              <span class="channel-badge">{{ ch.channel }}</span>
              <span class="channel-count">{{ fmt(ch.total) }}건</span>
            </div>
            <div class="channel-bar-wrap">
              <div class="channel-bar-bg">
                <div class="channel-bar-fill"
                  :style="{ width: (ch.complianceRate * 100).toFixed(1) + '%', background: rateColor(ch.complianceRate) }" />
              </div>
              <span class="channel-pct" :style="{ color: rateColor(ch.complianceRate) }">
                {{ (ch.complianceRate * 100).toFixed(1) }}%
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty state -->
      <div v-if="stats.summary.totalChecks === 0" class="empty-state">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="1.5" stroke-linecap="round"><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></svg>
        <p class="empty-title">아직 검사 데이터가 없습니다</p>
        <p class="empty-sub">API Key를 발급받고 첫 번째 메시지를 검사해보세요.</p>
        <RouterLink to="/playground" class="empty-cta">Playground에서 시작 →</RouterLink>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { fetchStats, type StatsResponse, type ChannelStat } from '@/api/stats'

const selectedDays = ref(30)
const stats = ref<StatsResponse | null>(null)
const loading = ref(false)
const error = ref('')

const compliancePct = computed(() =>
  stats.value ? Math.round(stats.value.summary.complianceRate * 100) : 0
)

const complianceColor = computed(() => {
  const p = compliancePct.value
  if (p >= 90) return '#22c55e'
  if (p >= 70) return '#f59e0b'
  return '#ef4444'
})

const channelNames = computed(() =>
  stats.value?.channels.map((c) => c.channel).join(' · ') ?? ''
)

const chartData = computed(() => {
  if (!stats.value?.daily.length) return []
  return [...stats.value.daily].reverse().slice(-30)
})

const maxTotal = computed(() =>
  Math.max(...chartData.value.map((d) => d.total), 1)
)

function fmt(n: number) {
  return n.toLocaleString('ko-KR')
}

function barPct(val: number, max: number) {
  return max === 0 ? 0 : Math.round((val / max) * 100)
}

function rateColor(rate: number) {
  if (rate >= 0.9) return '#22c55e'
  if (rate >= 0.7) return '#f59e0b'
  return '#ef4444'
}

function formatDate(dateStr: string) {
  const d = new Date(dateStr)
  return `${d.getMonth() + 1}/${d.getDate()}`
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    stats.value = await fetchStats(selectedDays.value)
  } catch {
    error.value = '통계를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

function changePeriod(days: number) {
  selectedDays.value = days
  load()
}

onMounted(load)
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Syne:wght@600;700&family=DM+Sans:wght@400;500;600&family=DM+Mono:wght@500&display=swap');

.dashboard {
  font-family: 'DM Sans', sans-serif;
  max-width: 1100px;
}

/* ── Top bar ── */
.dash-topbar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 1.75rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.dash-eyebrow {
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #94a3b8;
  margin: 0 0 0.2rem;
}

.dash-heading {
  font-family: 'Syne', sans-serif;
  font-size: 1.5rem;
  font-weight: 700;
  color: #0d1530;
  margin: 0;
  letter-spacing: -0.02em;
}

.period-tabs {
  display: flex;
  background: #e9ecf3;
  border-radius: 8px;
  padding: 3px;
  gap: 2px;
}

.period-btn {
  padding: 0.35rem 0.875rem;
  background: none;
  border: none;
  border-radius: 6px;
  font-family: 'DM Sans', sans-serif;
  font-size: 0.825rem;
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
}

.period-btn.active {
  background: #fff;
  color: #0d1530;
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

/* ── Skeleton ── */
.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
  margin-bottom: 1.25rem;
}

.skeleton-card {
  height: 140px;
  background: linear-gradient(90deg, #e9ecf3 25%, #f4f6fb 50%, #e9ecf3 75%);
  background-size: 200% 100%;
  border-radius: 16px;
  animation: shimmer 1.4s infinite;
}

.skeleton-chart {
  height: 240px;
  background: linear-gradient(90deg, #e9ecf3 25%, #f4f6fb 50%, #e9ecf3 75%);
  background-size: 200% 100%;
  border-radius: 16px;
  animation: shimmer 1.4s infinite;
}

@keyframes shimmer {
  from { background-position: 200% 0; }
  to   { background-position: -200% 0; }
}

/* ── Stat grid ── */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
  margin-bottom: 1.25rem;
}

.stat-card {
  background: #fff;
  border-radius: 16px;
  padding: 1.375rem 1.25rem;
  border: 1px solid #eef0f6;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  transition: box-shadow 0.2s, transform 0.2s;
  animation: card-rise 0.5s cubic-bezier(0.16,1,0.3,1) both;
}

.stat-card:hover {
  box-shadow: 0 4px 16px rgba(0,0,0,0.08);
  transform: translateY(-2px);
}

@keyframes card-rise {
  from { opacity: 0; transform: translateY(12px); }
  to   { opacity: 1; transform: translateY(0); }
}

.stat-icon-wrap {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  margin-bottom: 0.875rem;
}

.stat-label {
  font-size: 0.775rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: #9ca3af;
  margin: 0 0 0.25rem;
}

.stat-value {
  font-family: 'DM Mono', monospace;
  font-size: 1.875rem;
  font-weight: 500;
  color: #0d1530;
  margin: 0 0 0.375rem;
  letter-spacing: -0.02em;
  line-height: 1;
}

.stat-sub {
  font-size: 0.775rem;
  color: #94a3b8;
  margin: 0;
}

.progress-bar {
  height: 4px;
  background: #e9ecf3;
  border-radius: 2px;
  overflow: hidden;
  margin-top: 0.5rem;
}

.progress-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.8s cubic-bezier(0.16,1,0.3,1);
}

/* ── Chart ── */
.chart-card, .channel-card {
  background: #fff;
  border-radius: 16px;
  padding: 1.5rem 1.5rem 1.25rem;
  border: 1px solid #eef0f6;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  margin-bottom: 1.25rem;
  animation: card-rise 0.5s 0.1s cubic-bezier(0.16,1,0.3,1) both;
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.25rem;
}

.chart-title {
  font-family: 'Syne', sans-serif;
  font-size: 0.95rem;
  font-weight: 600;
  color: #0d1530;
  margin: 0;
}

.chart-legend {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.775rem;
  color: #6b7280;
}

.legend-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 2px;
}

.bar-chart {
  display: flex;
  align-items: flex-end;
  gap: 3px;
  height: 160px;
  overflow-x: auto;
  padding-bottom: 1.5rem;
  position: relative;
}

.bar-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  flex: 1;
  min-width: 20px;
  animation: bar-rise 0.4s cubic-bezier(0.16,1,0.3,1) both;
}

@keyframes bar-rise {
  from { opacity: 0; transform: scaleY(0); transform-origin: bottom; }
  to   { opacity: 1; transform: scaleY(1); }
}

.bar-stack {
  width: 100%;
  height: 130px;
  display: flex;
  flex-direction: column-reverse;
  border-radius: 4px;
  overflow: hidden;
  background: #f4f6fb;
  cursor: pointer;
  transition: opacity 0.15s;
}

.bar-stack:hover { opacity: 0.8; }

.bar-segment {
  width: 100%;
  transition: height 0.6s cubic-bezier(0.16,1,0.3,1);
  min-height: 0;
}

.bar-segment.compliant  { background: #3b82f6; }
.bar-segment.violations { background: #ef4444; }

.bar-label {
  font-size: 0.6rem;
  color: #94a3b8;
  white-space: nowrap;
  transform: rotate(-30deg);
  transform-origin: center;
  display: block;
}

/* ── Channels ── */
.channel-list { display: flex; flex-direction: column; gap: 0.875rem; }

.channel-row {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.channel-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 130px;
}

.channel-badge {
  padding: 0.2rem 0.6rem;
  background: #f0f3f8;
  border-radius: 6px;
  font-family: 'DM Mono', monospace;
  font-size: 0.75rem;
  font-weight: 500;
  color: #374151;
}

.channel-count { font-size: 0.8rem; color: #9ca3af; }

.channel-bar-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.channel-bar-bg {
  flex: 1;
  height: 8px;
  background: #e9ecf3;
  border-radius: 4px;
  overflow: hidden;
}

.channel-bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.8s cubic-bezier(0.16,1,0.3,1);
}

.channel-pct {
  font-family: 'DM Mono', monospace;
  font-size: 0.8rem;
  font-weight: 500;
  min-width: 48px;
  text-align: right;
}

/* ── Error / Empty ── */
.error-state {
  text-align: center;
  padding: 3rem;
  color: #ef4444;
  font-size: 0.9rem;
}

.retry-btn {
  margin-top: 0.75rem;
  padding: 0.5rem 1.25rem;
  background: #0d1530;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-family: 'DM Sans', sans-serif;
  font-size: 0.875rem;
  cursor: pointer;
}

.empty-state {
  text-align: center;
  padding: 3rem 1.5rem;
  background: #fff;
  border-radius: 16px;
  border: 1px solid #eef0f6;
}

.empty-title {
  font-family: 'Syne', sans-serif;
  font-size: 1.1rem;
  font-weight: 600;
  color: #374151;
  margin: 1rem 0 0.5rem;
}

.empty-sub { font-size: 0.875rem; color: #94a3b8; margin: 0 0 1.25rem; }

.empty-cta {
  display: inline-block;
  padding: 0.6rem 1.25rem;
  background: #0d1530;
  color: #fff;
  border-radius: 8px;
  text-decoration: none;
  font-size: 0.875rem;
  font-weight: 500;
  transition: background 0.2s;
}

.empty-cta:hover { background: #1a2b4a; }

/* ── Responsive ── */
@media (max-width: 900px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 560px) {
  .stat-grid { grid-template-columns: 1fr; }
  .skeleton-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>

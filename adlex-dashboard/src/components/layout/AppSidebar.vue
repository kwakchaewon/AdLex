<template>
  <aside :class="['sidebar', { 'sidebar--open': open }]">
    <div class="grid-overlay" aria-hidden="true" />

    <!-- Logo -->
    <div class="sidebar-logo">
      <span class="logo-mark">A</span>
      <span class="logo-text">ADLEX</span>
    </div>

    <!-- Nav -->
    <nav class="sidebar-nav" aria-label="메인 메뉴">
      <RouterLink
        v-for="item in navItems"
        :key="item.to"
        :to="item.to"
        :class="['nav-item', { active: isActive(item.to) }]"
        @click="$emit('close')"
        :aria-current="isActive(item.to) ? 'page' : undefined"
      >
        <span class="nav-icon" aria-hidden="true" v-html="item.icon" />
        <span class="nav-label">{{ item.label }}</span>
        <span v-if="isActive(item.to)" class="nav-pip" aria-hidden="true" />
      </RouterLink>
    </nav>

    <!-- Footer -->
    <div class="sidebar-footer">
      <div class="sidebar-divider" />
      <p class="sidebar-version">AdLex v0.1.0</p>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'

defineProps<{ open: boolean }>()
defineEmits<{ close: [] }>()

const route = useRoute()

function isActive(to: string) {
  return route.path === to || route.path.startsWith(to + '/')
}

const navItems = [
  {
    to: '/dashboard',
    label: '대시보드',
    icon: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">
      <rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/>
      <rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/>
    </svg>`,
  },
  {
    to: '/api-keys',
    label: 'API Key',
    icon: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">
      <circle cx="7.5" cy="15.5" r="4.5"/><path d="M10.9 12.1L20 3"/><path d="M18 5l2 2"/><path d="M15 8l2 2"/>
    </svg>`,
  },
  {
    to: '/history',
    label: '검사 히스토리',
    icon: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">
      <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2"/>
      <rect x="9" y="3" width="6" height="4" rx="1.5"/><path d="M9 12h6"/><path d="M9 16h4"/>
    </svg>`,
  },
  {
    to: '/playground',
    label: 'Playground',
    icon: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">
      <polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/>
    </svg>`,
  },
  {
    to: '/settings',
    label: '설정',
    icon: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">
      <circle cx="12" cy="12" r="3"/>
      <path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z"/>
    </svg>`,
  },
]
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Bebas+Neue&family=DM+Sans:wght@400;500;600&display=swap');

.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  width: 240px;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #070d1f 0%, #0d1530 70%, #0a1828 100%);
  border-right: 1px solid rgba(59, 130, 246, 0.1);
  z-index: 50;
  overflow: hidden;
}

.grid-overlay {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(59,130,246,0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(59,130,246,0.03) 1px, transparent 1px);
  background-size: 32px 32px;
  pointer-events: none;
}

/* Logo */
.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 1.5rem 1.25rem 1.25rem;
  border-bottom: 1px solid rgba(255,255,255,0.05);
  flex-shrink: 0;
}

.logo-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: #3b82f6;
  color: #fff;
  font-family: 'Bebas Neue', sans-serif;
  font-size: 1.15rem;
  border-radius: 7px;
  flex-shrink: 0;
}

.logo-text {
  font-family: 'Bebas Neue', sans-serif;
  font-size: 1.5rem;
  color: #e8f0ff;
  letter-spacing: 0.1em;
}

/* Nav */
.sidebar-nav {
  flex: 1;
  padding: 1rem 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow-y: auto;
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.65rem 0.875rem;
  border-radius: 10px;
  text-decoration: none;
  color: rgba(180, 200, 240, 0.6);
  font-family: 'DM Sans', sans-serif;
  font-size: 0.875rem;
  font-weight: 500;
  transition: background 0.18s, color 0.18s;
  cursor: pointer;
}

.nav-item:hover {
  background: rgba(59, 130, 246, 0.08);
  color: rgba(200, 220, 255, 0.9);
}

.nav-item.active {
  background: rgba(59, 130, 246, 0.15);
  color: #93c5fd;
}

.nav-icon {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  width: 18px;
  height: 18px;
}

.nav-label { flex: 1; }

.nav-pip {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #3b82f6;
  box-shadow: 0 0 6px rgba(59, 130, 246, 0.8);
  flex-shrink: 0;
}

/* Footer */
.sidebar-footer {
  padding: 0.75rem 1.25rem 1.25rem;
  flex-shrink: 0;
}

.sidebar-divider {
  height: 1px;
  background: rgba(255,255,255,0.06);
  margin-bottom: 0.75rem;
}

.sidebar-version {
  font-family: 'DM Sans', sans-serif;
  font-size: 0.72rem;
  color: rgba(148, 163, 184, 0.4);
  letter-spacing: 0.04em;
  margin: 0;
}

/* Mobile */
@media (max-width: 768px) {
  .sidebar {
    transform: translateX(-100%);
    transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  }
  .sidebar--open {
    transform: translateX(0);
  }
}
</style>

<template>
  <div class="layout-root">
    <!-- Mobile backdrop -->
    <Transition name="backdrop-fade">
      <div v-if="sidebarOpen" class="mobile-backdrop" @click="sidebarOpen = false" aria-hidden="true" />
    </Transition>

    <AppSidebar :open="sidebarOpen" @close="sidebarOpen = false" />

    <div class="layout-body">
      <AppHeader @toggle-sidebar="sidebarOpen = !sidebarOpen" />
      <main class="layout-main">
        <RouterView v-slot="{ Component }">
          <Transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </Transition>
        </RouterView>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import AppSidebar from './AppSidebar.vue'
import AppHeader from './AppHeader.vue'

const sidebarOpen = ref(false)
</script>

<style scoped>
.layout-root {
  display: flex;
  min-height: 100vh;
  background: #f4f6fb;
  font-family: 'DM Sans', sans-serif;
}

.layout-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  margin-left: 240px;
  transition: margin-left 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.layout-main {
  flex: 1;
  padding: 1.75rem 2rem;
  min-height: 0;
}

.mobile-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(7, 13, 31, 0.6);
  z-index: 40;
  backdrop-filter: blur(2px);
}

.backdrop-fade-enter-active,
.backdrop-fade-leave-active { transition: opacity 0.25s; }
.backdrop-fade-enter-from,
.backdrop-fade-leave-to { opacity: 0; }

.page-fade-enter-active,
.page-fade-leave-active { transition: opacity 0.18s, transform 0.18s; }
.page-fade-enter-from { opacity: 0; transform: translateY(8px); }
.page-fade-leave-to   { opacity: 0; transform: translateY(-4px); }

@media (max-width: 768px) {
  .layout-body { margin-left: 0; }
  .layout-main { padding: 1.25rem 1rem; }
}
</style>

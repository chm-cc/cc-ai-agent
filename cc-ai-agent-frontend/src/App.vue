<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SiteFooter from './components/SiteFooter.vue'
import { isLoggedIn, getUsername, clearAuth } from './utils/auth'

const route = useRoute()
const router = useRouter()
const showFooter = computed(() => !route.meta.hideFooter)
const loggedIn = computed(() => isLoggedIn())
const username = computed(() => getUsername())

function handleLogout() {
  clearAuth()
  router.push({ name: 'Login' })
}
</script>

<template>
  <div class="app-layout">
    <header v-if="!route.meta.hideFooter" class="app-header">
      <router-link to="/" class="logo">AI Agent</router-link>
      <div class="header-actions">
        <template v-if="loggedIn">
          <span class="user-info">{{ username }}</span>
          <button class="logout-btn" @click="handleLogout">退出</button>
        </template>
        <router-link v-else to="/login" class="login-link">登录</router-link>
      </div>
    </header>
    <router-view />
    <SiteFooter v-if="showFooter" />
  </div>
</template>

<style scoped>
.app-layout {
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 32px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
}

.logo {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info {
  font-size: 13px;
  color: #64748b;
}

.logout-btn,
.login-link {
  font-size: 13px;
  font-weight: 600;
  color: #2563eb;
  background: #eff6ff;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  padding: 6px 14px;
  transition: background 0.2s;
}

.logout-btn:hover,
.login-link:hover {
  background: #dbeafe;
}
</style>

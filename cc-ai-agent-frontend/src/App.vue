<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SiteFooter from './components/SiteFooter.vue'
import { isLoggedIn, getUsername, isSuperAdmin, clearAuth } from './utils/auth'

const route = useRoute()
const router = useRouter()
const menuOpen = ref(false)
const showFooter = computed(() => !route.meta.hideFooter)
const loggedIn = computed(() => isLoggedIn())
const username = computed(() => getUsername())
const superAdmin = computed(() => isSuperAdmin())

function closeMenu() {
  menuOpen.value = false
}

function handleLogout() {
  clearAuth()
  menuOpen.value = false
  router.push({ name: 'Login' })
}
</script>

<template>
  <div class="app-layout">
    <header v-if="!route.meta.hideFooter" class="app-header">
      <router-link to="/" class="logo">AI Agent</router-link>

      <!-- 桌面端导航 -->
      <nav class="header-nav">
        <router-link to="/" class="nav-link" @click="closeMenu">首页</router-link>
        <router-link to="/dashboard" class="nav-link" @click="closeMenu">统计</router-link>
        <router-link to="/agents" class="nav-link" @click="closeMenu">管理</router-link>
        <router-link v-if="superAdmin" to="/admin/users" class="nav-link admin-link" @click="closeMenu">用户管理</router-link>
      </nav>

      <!-- 桌面端用户操作 -->
      <div class="header-actions">
        <template v-if="loggedIn">
          <span class="user-info">{{ username }}</span>
          <button class="logout-btn" @click="handleLogout">退出</button>
        </template>
        <router-link v-else to="/login" class="login-link">登录</router-link>
      </div>

      <!-- 移动端汉堡按钮 -->
      <button class="menu-toggle" :class="{ open: menuOpen }" @click="menuOpen = !menuOpen" aria-label="菜单">
        <span></span><span></span><span></span>
      </button>
    </header>

    <!-- 移动端下拉菜单 -->
    <Transition name="slide">
      <div v-if="menuOpen" class="mobile-menu" @click.self="closeMenu">
        <div class="mobile-menu-panel">
          <div class="mobile-menu-header">
            <span class="mobile-user">{{ loggedIn ? username : '未登录' }}</span>
          </div>
          <router-link to="/" class="mobile-nav-link" @click="closeMenu">🏠 首页</router-link>
          <router-link to="/dashboard" class="mobile-nav-link" @click="closeMenu">📊 统计</router-link>
          <router-link to="/agents" class="mobile-nav-link" @click="closeMenu">⚙️ 管理</router-link>
          <router-link v-if="superAdmin" to="/admin/users" class="mobile-nav-link mobile-admin-link" @click="closeMenu">👥 用户管理</router-link>
          <div class="mobile-menu-footer">
            <template v-if="loggedIn">
              <button class="mobile-logout-btn" @click="handleLogout">退出登录</button>
            </template>
            <router-link v-else to="/login" class="mobile-login-link" @click="closeMenu">登录</router-link>
          </div>
        </div>
      </div>
    </Transition>

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
  background: rgba(255,255,255,0.78);
  border-bottom: 1px solid rgba(148,163,184,0.18);
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(18px);
  box-shadow: 0 10px 30px rgba(30,41,59,0.04);
}

.logo {
  font-size: 16px;
  font-weight: 800;
  color: var(--ai-text);
  letter-spacing: -0.2px;
  flex-shrink: 0;
}

.logo::before {
  content: '';
  display: inline-block;
  width: 9px;
  height: 9px;
  margin-right: 8px;
  border-radius: 999px;
  background: linear-gradient(135deg, #7dd3fc, #8b5cf6);
  box-shadow: 0 0 18px rgba(139,92,246,0.55);
  vertical-align: 1px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.header-nav {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-link {
  padding: 6px 14px;
  font-size: 13px;
  font-weight: 650;
  color: var(--ai-muted);
  border-radius: 999px;
  transition: color 0.15s, background 0.15s, box-shadow 0.15s;
  white-space: nowrap;
}

.nav-link:hover {
  color: var(--ai-text);
  background: rgba(255,255,255,0.72);
  box-shadow: inset 0 0 0 1px rgba(148,163,184,0.18);
}

.nav-link.router-link-exact-active {
  color: #4f46e5;
  background: linear-gradient(135deg, rgba(238,242,255,0.95), rgba(240,249,255,0.82));
  box-shadow: inset 0 0 0 1px rgba(139,92,246,0.15), 0 8px 22px rgba(79,70,229,0.08);
}

.user-info {
  font-size: 13px;
  color: var(--ai-muted);
  white-space: nowrap;
}

.logout-btn,
.login-link {
  font-size: 13px;
  font-weight: 600;
  color: #4f46e5;
  background: linear-gradient(135deg, rgba(238,242,255,0.90), rgba(240,249,255,0.82));
  border: 1px solid rgba(139,92,246,0.16);
  border-radius: 999px;
  padding: 7px 15px;
  transition: background 0.2s, transform 0.2s, box-shadow 0.2s;
  white-space: nowrap;
  cursor: pointer;
}

.logout-btn:hover,
.login-link:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 24px rgba(79,70,229,0.10);
}

.admin-link {
  color: #dc2626 !important;
}

.admin-link:hover {
  color: #b91c1c !important;
  background: #fef2f2 !important;
}

.admin-link.router-link-exact-active {
  color: #dc2626 !important;
  background: #fef2f2 !important;
}

/* ========== 汉堡菜单按钮 ========== */
.menu-toggle {
  display: none;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 5px;
  width: 36px;
  height: 36px;
  padding: 6px;
  background: none;
  border: none;
  cursor: pointer;
  z-index: 200;
  position: relative;
}

.menu-toggle span {
  display: block;
  width: 20px;
  height: 2px;
  background: #374151;
  border-radius: 2px;
  transition: transform 0.25s, opacity 0.25s;
}

.menu-toggle.open span:nth-child(1) {
  transform: translateY(7px) rotate(45deg);
}

.menu-toggle.open span:nth-child(2) {
  opacity: 0;
}

.menu-toggle.open span:nth-child(3) {
  transform: translateY(-7px) rotate(-45deg);
}

/* ========== 移动端下拉菜单 ========== */
.mobile-menu {
  display: none;
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.3);
  z-index: 150;
  backdrop-filter: blur(2px);
}

.mobile-menu-panel {
  position: absolute;
  top: 0;
  right: 0;
  width: 280px;
  max-width: 80vw;
  height: 100%;
  background: rgba(255,255,255,0.92);
  box-shadow: -18px 0 48px rgba(30, 41, 59, 0.14);
  backdrop-filter: blur(18px);
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow-y: auto;
}

.mobile-menu-header {
  padding: 20px 20px 16px;
  border-bottom: 1px solid #e2e8f0;
}

.mobile-user {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
}

.mobile-nav-link {
  display: block;
  padding: 14px 20px;
  font-size: 15px;
  font-weight: 500;
  color: #374151;
  border-bottom: 1px solid #f1f5f9;
  transition: background 0.15s;
}

.mobile-nav-link:hover {
  background: #f9fafb;
}

.mobile-nav-link.router-link-exact-active {
  color: #4f46e5;
  background: rgba(238,242,255,0.9);
}

.mobile-admin-link {
  color: #dc2626 !important;
}

.mobile-menu-footer {
  margin-top: auto;
  padding: 16px 20px;
  border-top: 1px solid #e2e8f0;
}

.mobile-logout-btn,
.mobile-login-link {
  display: block;
  width: 100%;
  padding: 12px;
  font-size: 14px;
  font-weight: 600;
  text-align: center;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s;
}

.mobile-logout-btn {
  color: #dc2626;
  background: #fef2f2;
  border: 1px solid #fecaca;
}

.mobile-logout-btn:hover {
  background: #fee2e2;
}

.mobile-login-link {
  color: #2563eb;
  background: #eff6ff;
  border: 1px solid #dbeafe;
}

/* ========== 过渡动画 ========== */
.slide-enter-active,
.slide-leave-active {
  transition: opacity 0.25s;
}
.slide-enter-active .mobile-menu-panel,
.slide-leave-active .mobile-menu-panel {
  transition: transform 0.25s ease;
}
.slide-enter-from,
.slide-leave-to {
  opacity: 0;
}
.slide-enter-from .mobile-menu-panel {
  transform: translateX(100%);
}
.slide-leave-to .mobile-menu-panel {
  transform: translateX(100%);
}

/* ========== 平板适配 ========== */
@media (max-width: 900px) {
  .app-header {
    padding: 12px 20px;
  }

  .header-nav {
    gap: 4px;
  }

  .nav-link {
    padding: 5px 10px;
    font-size: 12px;
  }

  .user-info {
    font-size: 12px;
  }

  .logout-btn,
  .login-link {
    font-size: 12px;
    padding: 5px 10px;
  }
}

/* ========== 手机端适配 ========== */
@media (max-width: 680px) {
  .app-header {
    padding: 10px 16px;
  }

  /* 隐藏桌面导航和操作区 */
  .header-nav,
  .header-actions {
    display: none;
  }

  /* 显示汉堡菜单 */
  .menu-toggle {
    display: flex;
  }

  /* 显示移动端菜单覆盖层 */
  .mobile-menu {
    display: block;
  }

  .logo {
    font-size: 15px;
  }
}
</style>

<template>
  <main class="login-page">
    <div class="login-card">
      <div class="login-badge">🔐 内部系统</div>
      <h1 class="login-title">登录 AI Agent</h1>
      <p class="login-desc">请使用管理员账号登录后使用 AI 服务</p>

      <form class="login-form" @submit.prevent="handleLogin">
        <label class="form-label">
          用户名
          <input
            v-model="username"
            type="text"
            class="form-input"
            placeholder="请输入用户名"
            autocomplete="username"
            :disabled="loading"
          />
        </label>

        <label class="form-label">
          密码
          <input
            v-model="password"
            type="password"
            class="form-input"
            placeholder="请输入密码"
            autocomplete="current-password"
            :disabled="loading"
          />
        </label>

        <p v-if="error" class="error-msg">{{ error }}</p>

        <button type="submit" class="login-btn" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <router-link to="/" class="back-link">← 返回首页</router-link>
    </div>
  </main>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login } from '../api/auth'

const route = useRoute()
const router = useRouter()

const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

async function handleLogin() {
  error.value = ''
  if (!username.value.trim() || !password.value) {
    error.value = '请输入用户名和密码'
    return
  }

  loading.value = true
  try {
    await login(username.value.trim(), password.value)
    const redirect = route.query.redirect || '/'
    router.replace(redirect)
  } catch (e) {
    error.value = e.message || '登录失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  min-height: 100vh;
  background: linear-gradient(160deg, #f5f5f7 0%, #eef2ff 50%, #f5f5f7 100%);
}

.login-card {
  width: 100%;
  max-width: 420px;
  padding: 40px 36px;
  background: #fff;
  border-radius: 20px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 8px 32px rgba(37, 99, 235, 0.08);
}

.login-badge {
  display: inline-block;
  padding: 4px 14px;
  font-size: 12px;
  font-weight: 600;
  color: #2563eb;
  background: #eff6ff;
  border: 1px solid #dbeafe;
  border-radius: 20px;
  margin-bottom: 20px;
}

.login-title {
  font-size: 28px;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 8px;
}

.login-desc {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 32px;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.form-input {
  padding: 12px 14px;
  font-size: 15px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.form-input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.form-input:disabled {
  background: #f9fafb;
  cursor: not-allowed;
}

.error-msg {
  font-size: 13px;
  color: #dc2626;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  padding: 10px 12px;
}

.login-btn {
  padding: 13px;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  border-radius: 10px;
  transition: opacity 0.2s, transform 0.15s;
}

.login-btn:hover:not(:disabled) {
  opacity: 0.92;
  transform: translateY(-1px);
}

.login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.back-link {
  display: inline-block;
  margin-top: 24px;
  font-size: 13px;
  color: #64748b;
  transition: color 0.2s;
}

.back-link:hover {
  color: #2563eb;
}
</style>

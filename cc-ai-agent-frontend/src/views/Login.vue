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
  background:
    radial-gradient(circle at 18% 8%, rgba(167,139,250,0.22), transparent 32%),
    radial-gradient(circle at 86% 18%, rgba(125,211,252,0.20), transparent 30%),
    linear-gradient(160deg, #fbfbff 0%, #eef2ff 50%, #f7fbff 100%);
}

.login-card {
  width: 100%;
  max-width: 420px;
  padding: 40px 36px;
  background:
    radial-gradient(circle at 12% 0%, rgba(167,139,250,0.14), transparent 34%),
    rgba(255,255,255,0.88);
  border-radius: 24px;
  border: 1px solid rgba(148,163,184,0.20);
  box-shadow: var(--ai-shadow-lg);
  backdrop-filter: blur(18px);
}

.login-badge {
  display: inline-block;
  padding: 4px 14px;
  font-size: 12px;
  font-weight: 600;
  color: #4f46e5;
  background: linear-gradient(135deg, rgba(238,242,255,0.96), rgba(240,249,255,0.84));
  border: 1px solid rgba(139,92,246,0.16);
  border-radius: 20px;
  margin-bottom: 20px;
}

.login-title {
  font-size: 28px;
  font-weight: 800;
  color: var(--ai-text);
  margin-bottom: 8px;
}

.login-desc {
  font-size: 14px;
  color: var(--ai-muted);
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
  color: #344054;
}

.form-input {
  padding: 12px 14px;
  font-size: 15px;
  border: 1px solid rgba(148,163,184,0.28);
  border-radius: 12px;
  background: rgba(255,255,255,0.86);
  transition: border-color 0.2s, box-shadow 0.2s, background 0.2s;
}

.form-input:focus {
  border-color: rgba(109,93,252,0.55);
  box-shadow: 0 0 0 4px rgba(109,93,252,0.12);
  background: #fff;
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
  background: linear-gradient(135deg, #7c3aed, #3b82f6);
  border-radius: 12px;
  box-shadow: 0 14px 32px rgba(79,70,229,0.20);
  transition: opacity 0.2s, transform 0.15s, box-shadow 0.2s;
}

.login-btn:hover:not(:disabled) {
  opacity: 0.92;
  transform: translateY(-1px);
  box-shadow: 0 18px 38px rgba(79,70,229,0.26);
}

.login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.back-link {
  display: inline-block;
  margin-top: 24px;
  font-size: 13px;
  color: var(--ai-muted);
  transition: color 0.2s;
}

.back-link:hover {
  color: #4f46e5;
}

@media (max-width: 480px) {
  .login-page {
    padding: 24px 16px;
    align-items: flex-start;
    padding-top: 60px;
  }

  .login-card {
    padding: 28px 20px;
    border-radius: 14px;
  }

  .login-title {
    font-size: 22px;
  }

  .login-desc {
    font-size: 13px;
    margin-bottom: 24px;
  }

  .login-form {
    gap: 16px;
  }

  .form-input {
    padding: 10px 12px;
    font-size: 14px;
  }

  .login-btn {
    padding: 12px;
    font-size: 14px;
  }
}
</style>

<template>
  <main class="admin-page">
    <div class="page-header">
      <h1 class="page-title">👥 用户管理</h1>
      <p class="page-desc">管理测试用户账号，创建、查看和重置密码</p>
    </div>

    <!-- 创建用户表单 -->
    <section class="card create-section">
      <h2 class="section-title">创建测试用户</h2>
      <form class="create-form" @submit.prevent="handleCreate">
        <div class="form-row">
          <label class="form-label">
            用户名
            <input
              v-model="createForm.username"
              type="text"
              class="form-input"
              placeholder="请输入用户名"
              :disabled="creating"
            />
          </label>
          <label class="form-label">
            密码
            <input
              v-model="createForm.password"
              type="password"
              class="form-input"
              placeholder="请输入密码（至少4位）"
              :disabled="creating"
            />
          </label>
          <button type="submit" class="btn btn-primary" :disabled="creating">
            {{ creating ? '创建中...' : '创建用户' }}
          </button>
        </div>
        <p v-if="createError" class="error-msg">{{ createError }}</p>
        <p v-if="createSuccess" class="success-msg">{{ createSuccess }}</p>
      </form>
    </section>

    <!-- 用户列表 -->
    <section class="card">
      <div class="list-header">
        <h2 class="section-title">用户列表</h2>
        <span class="total-count">共 {{ total }} 个用户</span>
      </div>

      <div v-if="loading" class="loading-state">加载中...</div>

      <div v-else class="table-wrapper">
      <table class="user-table">
        <thead>
          <tr>
            <th>用户名</th>
            <th>角色</th>
            <th>状态</th>
            <th>创建时间</th>
            <th>更新时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td class="username-cell">
              <span class="user-avatar">{{ user.username.charAt(0).toUpperCase() }}</span>
              {{ user.username }}
            </td>
            <td>
              <span :class="['role-badge', user.role === 'SUPER_ADMIN' ? 'role-super' : user.role === 'ADMIN' ? 'role-admin' : 'role-user']">
                {{ roleLabel(user.role) }}
              </span>
            </td>
            <td>
              <span :class="['status-dot', user.enabled ? 'status-on' : 'status-off']"></span>
              {{ user.enabled ? '正常' : '禁用' }}
            </td>
            <td class="time-cell">{{ formatTime(user.createdAt) }}</td>
            <td class="time-cell">{{ formatTime(user.updatedAt) }}</td>
            <td class="actions-cell">
              <button class="btn btn-sm btn-outline" @click="showViewPwd(user)">查看密码</button>
              <button class="btn btn-sm btn-outline" @click="showResetPwd(user)">重置密码</button>
            </td>
          </tr>
          <tr v-if="users.length === 0">
            <td colspan="6" class="empty-cell">暂无用户数据</td>
          </tr>
        </tbody>
      </table>
      </div>

      <!-- 分页 -->
      <div v-if="total > pageSize" class="pagination">
        <button :disabled="page <= 1" @click="page--; loadUsers()" class="btn btn-sm">上一页</button>
        <span class="page-info">{{ page }} / {{ totalPages }}</span>
        <button :disabled="page >= totalPages" @click="page++; loadUsers()" class="btn btn-sm">下一页</button>
      </div>
    </section>

    <!-- 查看密码弹窗 -->
    <div v-if="pwdModal.user" class="modal-overlay" @click.self="pwdModal.user = null">
      <div class="modal-card">
        <h3 class="modal-title">查看密码</h3>
        <p class="modal-desc">用户：<strong>{{ pwdModal.user.username }}</strong></p>
        <div class="pwd-display">
          <span v-if="pwdModal.loading" class="pwd-loading">加载中...</span>
          <code v-else class="pwd-value">{{ pwdModal.password || '—' }}</code>
        </div>
        <div class="modal-actions">
          <button class="btn btn-cancel" @click="pwdModal.user = null">关闭</button>
        </div>
      </div>
    </div>

    <!-- 重置密码弹窗 -->
    <div v-if="resetModal.user" class="modal-overlay" @click.self="resetModal.user = null">
      <div class="modal-card">
        <h3 class="modal-title">重置密码</h3>
        <p class="modal-desc">用户：<strong>{{ resetModal.user.username }}</strong></p>
        <label class="form-label">
          新密码
          <input
            v-model="resetModal.password"
            type="password"
            class="form-input"
            placeholder="请输入新密码（至少4位）"
            :disabled="resetModal.loading"
          />
        </label>
        <p v-if="resetModal.error" class="error-msg">{{ resetModal.error }}</p>
        <p v-if="resetModal.success" class="success-msg">{{ resetModal.success }}</p>
        <div class="modal-actions">
          <button class="btn btn-cancel" @click="resetModal.user = null">取消</button>
          <button class="btn btn-danger" :disabled="resetModal.loading" @click="handleResetPwd">
            {{ resetModal.loading ? '重置中...' : '确认重置' }}
          </button>
        </div>
      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { createUser, fetchUsers, viewPassword, resetPassword } from '../api/admin'

const users = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 20
const loading = ref(false)

const createForm = ref({ username: '', password: '' })
const creating = ref(false)
const createError = ref('')
const createSuccess = ref('')

const pwdModal = ref({ user: null, password: '', loading: false })
const resetModal = ref({ user: null, password: '', loading: false, error: '', success: '' })

const totalPages = computed(() => Math.ceil(total.value / pageSize) || 1)

onMounted(() => loadUsers())

async function loadUsers() {
  loading.value = true
  try {
    const data = await fetchUsers({ page: page.value, size: pageSize })
    users.value = data.list
    total.value = data.total
  } catch (e) {
    console.error('加载用户列表失败:', e)
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  createError.value = ''
  createSuccess.value = ''

  if (!createForm.value.username.trim()) {
    createError.value = '请输入用户名'
    return
  }
  if (!createForm.value.password || createForm.value.password.length < 4) {
    createError.value = '密码至少4个字符'
    return
  }

  creating.value = true
  try {
    await createUser({
      username: createForm.value.username.trim(),
      password: createForm.value.password,
    })
    createSuccess.value = `用户 ${createForm.value.username} 创建成功！`
    createForm.value = { username: '', password: '' }
    page.value = 1
    await loadUsers()
  } catch (e) {
    createError.value = e.response?.data?.message || e.message || '创建失败'
  } finally {
    creating.value = false
  }
}

async function showViewPwd(user) {
  pwdModal.value = { user, password: '', loading: true }
  try {
    const data = await viewPassword(user.id)
    pwdModal.value.password = data.password
  } catch (e) {
    pwdModal.value.password = '获取失败: ' + (e.response?.data?.message || e.message)
  } finally {
    pwdModal.value.loading = false
  }
}

function showResetPwd(user) {
  resetModal.value = { user, password: '', loading: false, error: '', success: '' }
}

async function handleResetPwd() {
  resetModal.value.error = ''
  resetModal.value.success = ''

  if (!resetModal.value.password || resetModal.value.password.length < 4) {
    resetModal.value.error = '密码至少4个字符'
    return
  }

  resetModal.value.loading = true
  try {
    await resetPassword(resetModal.value.user.id, { password: resetModal.value.password })
    resetModal.value.success = '密码重置成功！'
    setTimeout(() => { resetModal.value = { user: null, password: '', loading: false, error: '', success: '' } }, 1500)
  } catch (e) {
    resetModal.value.error = e.response?.data?.message || e.message || '重置失败'
  } finally {
    resetModal.value.loading = false
  }
}

function roleLabel(role) {
  const map = { SUPER_ADMIN: '超级管理员', ADMIN: '管理员', USER: '普通用户' }
  return map[role] || role
}

function formatTime(t) {
  if (!t) return '—'
  const d = new Date(t)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
</script>

<style scoped>
.admin-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 32px 24px 64px;
}

.page-header {
  margin-bottom: 28px;
}

.page-title {
  font-size: 24px;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 6px;
}

.page-desc {
  font-size: 14px;
  color: #64748b;
}

.card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 16px;
}

.create-section {
  background: linear-gradient(135deg, #fafbff, #f8fafc);
  border-color: #dbeafe;
}

.create-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-row {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.form-label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  flex: 1;
}

.form-input {
  padding: 10px 12px;
  font-size: 14px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  transition: border-color 0.2s;
}

.form-input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.form-input:disabled {
  background: #f9fafb;
  cursor: not-allowed;
}

.btn {
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 600;
  border-radius: 8px;
  cursor: pointer;
  transition: opacity 0.2s;
  white-space: nowrap;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
}

.btn-primary:hover:not(:disabled) { opacity: 0.9; }

.btn-danger {
  background: linear-gradient(135deg, #dc2626, #b91c1c);
  color: #fff;
}

.btn-danger:hover:not(:disabled) { opacity: 0.9; }

.btn-outline {
  background: #fff;
  color: #374151;
  border: 1px solid #d1d5db;
}

.btn-outline:hover:not(:disabled) {
  background: #f9fafb;
  border-color: #9ca3af;
}

.btn-cancel {
  background: #f3f4f6;
  color: #374151;
}

.btn-cancel:hover { background: #e5e7eb; }

.btn-sm { padding: 6px 12px; font-size: 12px; }

.error-msg {
  font-size: 13px;
  color: #dc2626;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  padding: 8px 12px;
}

.success-msg {
  font-size: 13px;
  color: #16a34a;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 8px;
  padding: 8px 12px;
}

/* 列表 */
.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.list-header .section-title { margin-bottom: 0; }

.total-count {
  font-size: 13px;
  color: #94a3b8;
}

.loading-state {
  text-align: center;
  padding: 40px;
  color: #94a3b8;
  font-size: 14px;
}

.user-table {
  width: 100%;
  border-collapse: collapse;
}

.user-table th {
  text-align: left;
  padding: 10px 12px;
  font-size: 12px;
  font-weight: 600;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 1px solid #e2e8f0;
}

.user-table td {
  padding: 12px;
  font-size: 14px;
  color: #374151;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
}

.username-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #e0e7ff;
  color: #4338ca;
  font-size: 12px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.role-badge {
  display: inline-block;
  padding: 2px 10px;
  font-size: 12px;
  font-weight: 600;
  border-radius: 20px;
}

.role-super { background: #fef2f2; color: #dc2626; border: 1px solid #fecaca; }
.role-admin { background: #eff6ff; color: #2563eb; border: 1px solid #dbeafe; }
.role-user { background: #f0fdf4; color: #16a34a; border: 1px solid #bbf7d0; }

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
}

.status-on { background: #22c55e; }
.status-off { background: #ef4444; }

.time-cell { font-size: 13px; color: #94a3b8; white-space: nowrap; }

.actions-cell { display: flex; gap: 6px; }

.empty-cell {
  text-align: center;
  padding: 32px;
  color: #94a3b8;
}

/* 分页 */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
}

.page-info {
  font-size: 13px;
  color: #64748b;
}

/* 弹窗 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(2px);
}

.modal-card {
  background: #fff;
  padding: 28px;
  border-radius: 16px;
  width: 400px;
  max-width: 90vw;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.15);
}

.modal-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 8px;
}

.modal-desc {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 16px;
}

.pwd-display {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 16px;
  min-height: 42px;
  display: flex;
  align-items: center;
}

.pwd-value {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: 1px;
}

.pwd-loading {
  font-size: 13px;
  color: #94a3b8;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 8px;
}

.modal-card .form-label {
  margin-bottom: 16px;
}

.modal-card .error-msg,
.modal-card .success-msg {
  margin-bottom: 12px;
}

/* ========== 表格滚动容器 ========== */
.table-wrapper {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

/* ========== 平板适配 ========== */
@media (max-width: 768px) {
  .admin-page {
    padding: 20px 16px 48px;
  }

  .page-title {
    font-size: 20px;
  }

  .page-desc {
    font-size: 13px;
  }

  .card {
    padding: 18px;
    border-radius: 12px;
    margin-bottom: 18px;
  }

  .section-title {
    font-size: 15px;
    margin-bottom: 12px;
  }

  /* 创建表单：垂直排列 */
  .form-row {
    flex-direction: column;
    gap: 10px;
    align-items: stretch;
  }

  .form-row .btn {
    width: 100%;
    padding: 12px;
  }

  .list-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .user-table th,
  .user-table td {
    padding: 8px 10px;
    font-size: 13px;
  }

  .user-table th {
    font-size: 11px;
  }

  .time-cell {
    font-size: 12px;
  }

  .actions-cell {
    flex-direction: column;
    gap: 4px;
  }

  .btn-sm {
    padding: 5px 10px;
    font-size: 11px;
  }

  .modal-card {
    padding: 22px;
    border-radius: 14px;
  }

  .modal-title {
    font-size: 16px;
  }

  .pwd-value {
    font-size: 16px;
  }
}

/* ========== 手机端适配 ========== */
@media (max-width: 480px) {
  .admin-page {
    padding: 14px 10px 40px;
  }

  .page-title {
    font-size: 18px;
  }

  .page-desc {
    font-size: 12px;
  }

  .card {
    padding: 14px;
    border-radius: 10px;
    margin-bottom: 14px;
  }

  .section-title {
    font-size: 14px;
  }

  .form-label {
    font-size: 12px;
  }

  .form-input {
    padding: 10px 12px;
    font-size: 14px;
  }

  .btn {
    padding: 10px 16px;
    font-size: 13px;
    border-radius: 8px;
  }

  .user-table th,
  .user-table td {
    padding: 6px 8px;
    font-size: 12px;
  }

  .user-avatar {
    width: 24px;
    height: 24px;
    font-size: 10px;
  }

  .role-badge {
    font-size: 10px;
    padding: 2px 7px;
  }

  .modal-card {
    width: 100%;
    max-width: 100vw;
    padding: 20px 16px;
    border-radius: 12px 12px 0 0;
    align-self: flex-end;
  }

  .modal-actions {
    flex-direction: column;
  }

  .modal-actions .btn {
    width: 100%;
    text-align: center;
  }

  .pagination {
    gap: 10px;
  }

  .page-info {
    font-size: 12px;
  }
}

/* Premium AI platform skin */
.page-title {
  color: var(--ai-text);
  letter-spacing: -0.35px;
}

.page-desc,
.total-count,
.loading-state,
.time-cell,
.empty-cell,
.modal-desc {
  color: var(--ai-muted);
}

.card {
  background:
    radial-gradient(circle at 8% 0%, rgba(167,139,250,0.12), transparent 34%),
    linear-gradient(145deg, rgba(255,255,255,0.92), rgba(248,250,252,0.78));
  border: 1px solid rgba(148,163,184,0.20);
  border-radius: 22px;
  box-shadow: var(--ai-shadow-sm);
  backdrop-filter: blur(14px);
}

.create-section {
  background:
    radial-gradient(circle at 10% 0%, rgba(125,211,252,0.13), transparent 34%),
    linear-gradient(145deg, rgba(255,255,255,0.94), rgba(238,242,255,0.62));
  border-color: rgba(139,92,246,0.16);
}

.section-title,
.modal-title {
  color: var(--ai-text);
  font-weight: 780;
  letter-spacing: -0.15px;
}

.form-label {
  color: #344054;
  font-weight: 650;
}

.form-input {
  border-color: rgba(148,163,184,0.28);
  border-radius: 12px;
  background: rgba(255,255,255,0.86);
  transition: border-color 0.2s, box-shadow 0.2s, background 0.2s;
}

.form-input:focus {
  border-color: rgba(109,93,252,0.55);
  box-shadow: 0 0 0 4px rgba(109,93,252,0.12);
  background: #fff;
}

.btn {
  border-radius: 999px;
  transition: transform 0.15s, box-shadow 0.2s, opacity 0.2s, background 0.2s, border-color 0.2s;
}

.btn-primary {
  background: linear-gradient(135deg, #7c3aed, #3b82f6);
  box-shadow: 0 12px 28px rgba(79,70,229,0.18);
}

.btn-primary:hover:not(:disabled),
.btn-danger:hover:not(:disabled),
.btn-outline:hover:not(:disabled) {
  transform: translateY(-1px);
}

.btn-danger {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  box-shadow: 0 12px 26px rgba(220,38,38,0.16);
}

.btn-outline,
.btn-cancel {
  background: rgba(255,255,255,0.82);
  color: #4f46e5;
  border: 1px solid rgba(139,92,246,0.16);
}

.btn-outline:hover:not(:disabled),
.btn-cancel:hover {
  background: rgba(238,242,255,0.92);
  border-color: rgba(139,92,246,0.26);
}

.user-table th {
  color: var(--ai-muted);
  background: linear-gradient(135deg, rgba(248,250,252,0.96), rgba(238,242,255,0.62));
  border-bottom: 1px solid rgba(148,163,184,0.18);
}

.user-table td {
  color: #344054;
  border-bottom: 1px solid rgba(148,163,184,0.12);
}

.user-avatar {
  background: linear-gradient(135deg, rgba(238,242,255,0.96), rgba(224,242,254,0.86));
  color: #4f46e5;
  border: 1px solid rgba(139,92,246,0.16);
}

.role-badge {
  border-radius: 999px;
}

.role-super {
  background: rgba(254,242,242,0.92);
  color: #dc2626;
  border-color: rgba(248,113,113,0.24);
}

.role-admin {
  background: rgba(238,242,255,0.92);
  color: #4f46e5;
  border-color: rgba(139,92,246,0.16);
}

.role-user {
  background: rgba(236,253,245,0.92);
  color: #047857;
  border-color: rgba(16,185,129,0.18);
}

.pagination {
  border-top-color: rgba(148,163,184,0.14);
}

.modal-overlay {
  background: rgba(15,23,42,0.36);
  backdrop-filter: blur(10px);
}

.modal-card {
  background:
    radial-gradient(circle at 12% 0%, rgba(167,139,250,0.12), transparent 34%),
    rgba(255,255,255,0.94);
  border: 1px solid rgba(148,163,184,0.20);
  border-radius: 22px;
  box-shadow: var(--ai-shadow-lg);
  backdrop-filter: blur(18px);
}

.pwd-display {
  background: rgba(248,250,252,0.78);
  border-color: rgba(148,163,184,0.20);
  border-radius: 12px;
}

.pwd-value {
  color: var(--ai-text);
}
</style>

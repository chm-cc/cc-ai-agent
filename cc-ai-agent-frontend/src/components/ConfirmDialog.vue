<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: '确认操作' },
  description: { type: String, default: '' },
  detail: { type: String, default: '' },
  confirmText: { type: String, default: '确定' },
  cancelText: { type: String, default: '取消' },
  danger: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
})

const emit = defineEmits(['confirm', 'cancel'])

const dialogRef = ref(null)

function onConfirm() {
  if (props.loading) return
  emit('confirm')
}

function onCancel() {
  if (props.loading) return
  emit('cancel')
}

function onKeydown(e) {
  if (e.key === 'Escape') onCancel()
  if (e.key === 'Enter' && !props.loading) onConfirm()
}

watch(() => props.visible, (v) => {
  if (v) {
    document.body.style.overflow = 'hidden'
    // 延迟聚焦以等待 DOM 渲染
    setTimeout(() => dialogRef.value?.focus(), 100)
  } else {
    document.body.style.overflow = ''
  }
})
</script>

<template>
  <Teleport to="body">
    <Transition name="dialog">
      <div
        v-if="visible"
        class="dialog-overlay"
        @click.self="onCancel"
        @keydown="onKeydown"
      >
        <div
          ref="dialogRef"
          class="dialog-panel"
          role="alertdialog"
          aria-modal="true"
          tabindex="-1"
          :aria-label="title"
        >
          <!-- 图标区 -->
          <div class="dialog-icon" :class="{ danger }">
            <svg v-if="danger" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="8" x2="12" y2="12"/>
              <line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
            <svg v-else width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"/>
              <path d="M12 16v-4"/>
              <path d="M12 8h.01"/>
            </svg>
          </div>

          <!-- 标题 -->
          <h3 class="dialog-title">{{ title }}</h3>

          <!-- 描述 -->
          <p v-if="description" class="dialog-desc">{{ description }}</p>

          <!-- 详情（被删除对象名称等） -->
          <div v-if="detail" class="dialog-detail">
            <span class="detail-label">「</span>
            <span class="detail-value">{{ detail }}</span>
            <span class="detail-label">」</span>
          </div>

          <!-- 按钮区 -->
          <div class="dialog-actions">
            <button
              class="btn-cancel"
              :disabled="loading"
              @click="onCancel"
            >
              {{ cancelText }}
            </button>
            <button
              class="btn-confirm"
              :class="{ danger }"
              :disabled="loading"
              @click="onConfirm"
            >
              <span v-if="loading" class="btn-spinner"></span>
              {{ loading ? '处理中...' : confirmText }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
/* ====== 遮罩层 ====== */
.dialog-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.36);
  backdrop-filter: blur(10px);
  padding: 24px;
}

/* ====== 面板 ====== */
.dialog-panel {
  background:
    radial-gradient(circle at 12% 0%, rgba(167,139,250,0.12), transparent 34%),
    rgba(255,255,255,0.94);
  border: 1px solid rgba(148,163,184,0.20);
  border-radius: 22px;
  box-shadow: var(--ai-shadow-lg);
  backdrop-filter: blur(18px);
  padding: 32px;
  max-width: 420px;
  width: 100%;
  text-align: center;
  outline: none;
}

/* ====== 图标 ====== */
.dialog-icon {
  width: 48px;
  height: 48px;
  margin: 0 auto 16px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #f59e0b;
  background: #fffbeb;
  border: 1px solid #fde68a;
}

.dialog-icon.danger {
  color: #ef4444;
  background: #fef2f2;
  border-color: #fecaca;
}

/* ====== 文字 ====== */
.dialog-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--ai-text);
  margin: 0 0 8px;
}

.dialog-desc {
  font-size: 14px;
  color: var(--ai-muted);
  line-height: 1.6;
  margin: 0 0 16px;
}

.dialog-detail {
  background: rgba(248,250,252,0.78);
  border: 1px solid rgba(148,163,184,0.20);
  border-radius: 12px;
  padding: 10px 16px;
  margin-bottom: 24px;
  font-size: 14px;
  color: #334155;
  word-break: break-all;
}

.detail-label { color: #94a3b8; }
.detail-value { font-weight: 600; color: #0f172a; }

/* ====== 按钮 ====== */
.dialog-actions {
  display: flex;
  gap: 12px;
}

.btn-cancel,
.btn-confirm {
  flex: 1;
  padding: 10px 16px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
  border: 1px solid transparent;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.btn-cancel {
  background: rgba(255,255,255,0.82);
  border-color: rgba(148,163,184,0.28);
  color: #374151;
}

.btn-cancel:hover:not(:disabled) {
  background: #f9fafb;
  border-color: #9ca3af;
}

.btn-confirm {
  background: linear-gradient(135deg, #7c3aed, #3b82f6);
  color: #fff;
  box-shadow: 0 12px 28px rgba(79,70,229,0.18);
}

.btn-confirm:hover:not(:disabled) {
  background: linear-gradient(135deg, #6d28d9, #2563eb);
}

.btn-confirm.danger {
  background: #dc2626;
}

.btn-confirm.danger:hover:not(:disabled) {
  background: #b91c1c;
}

.btn-cancel:disabled,
.btn-confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ====== Spinner ====== */
.btn-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ====== 过渡动画 ====== */
.dialog-enter-active {
  transition: opacity 0.2s ease;
}
.dialog-enter-active .dialog-panel {
  transition: transform 0.2s ease, opacity 0.2s ease;
}
.dialog-leave-active {
  transition: opacity 0.15s ease;
}
.dialog-leave-active .dialog-panel {
  transition: transform 0.15s ease, opacity 0.15s ease;
}

.dialog-enter-from {
  opacity: 0;
}
.dialog-enter-from .dialog-panel {
  transform: scale(0.95) translateY(8px);
  opacity: 0;
}

.dialog-leave-to {
  opacity: 0;
}
.dialog-leave-to .dialog-panel {
  transform: scale(0.95) translateY(8px);
  opacity: 0;
}
</style>

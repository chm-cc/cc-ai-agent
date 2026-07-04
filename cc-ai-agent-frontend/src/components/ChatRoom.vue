<script setup>
import { ref, nextTick, watch } from 'vue'
import TypewriterText from './TypewriterText.vue'

const props = defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  chatId: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  theme: { type: String, default: 'default' },
})

const emit = defineEmits(['send', 'retry', 'abort'])

const inputText = ref('')
const messagesRef = ref(null)

const messages = defineModel('messages', { type: Array, default: () => [] })

// 新消息到来或最后一条消息内容变化时自动滚动到底部
watch(
  () => messages.value.length,
  async () => {
    await nextTick()
    scrollToBottom()
  },
)

watch(
  () => messages.value[messages.value.length - 1]?.content,
  async () => {
    await nextTick()
    scrollToBottom()
  },
)

watch(
  () => messages.value[messages.value.length - 1]?.thoughts,
  async () => {
    await nextTick()
    scrollToBottom()
  },
)

function scrollToBottom() {
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

function handleSend() {
  const text = inputText.value.trim()
  if (!text || props.loading) return
  inputText.value = ''
  emit('send', text)
}

function handleKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
    e.preventDefault()
    handleSend()
  }
}

function handleRetry() {
  emit('retry')
}

function handleAbort() {
  emit('abort')
}

/** 判断一条消息是否处于流式传输中 */
function isStreaming(msg) {
  return msg.status === 'streaming'
}

/** 判断一条消息是否发送中（等待首个响应） */
function isSending(msg) {
  return msg.status === 'sending'
}

/** 判断一条消息是否出错 */
function isError(msg) {
  return msg.status === 'error'
}

/** 判断一条消息是否有思考过程 */
function hasThoughts(msg) {
  return !!msg.thoughts
}

/** 生成思考过程摘要（工具调用次数等） */
function thinkingSummary(msg) {
  if (!msg.thoughts) return ''
  const tools = (msg.thoughts.match(/🔧/g) || []).length
  const parts = []
  if (tools > 0) parts.push(`${tools} 次工具调用`)
  return parts.length ? `· ${parts.join(' · ')}` : ''
}
</script>

<template>
  <div class="chat-room" :class="`theme-${theme}`">
    <header class="chat-header">
      <router-link to="/" class="back-btn" title="返回主页">←</router-link>
      <div class="header-info">
        <h1 class="title">{{ title }}</h1>
        <p v-if="subtitle" class="subtitle">{{ subtitle }}</p>
      </div>
    </header>

    <main ref="messagesRef" class="messages">
      <div v-if="messages.length === 0" class="empty-hint">
        开始对话吧，输入消息后按 Enter 发送
      </div>
      <div
        v-for="(msg, index) in messages"
        :key="index"
        class="message-row"
        :class="[msg.role, { 'msg-error': isError(msg) }]"
      >
        <div class="avatar">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
        <div class="bubble" :class="{ 'bubble-error': isError(msg) }">
          <!-- ====== 思考过程（可折叠） ====== -->
          <details
            v-if="msg.role === 'assistant' && hasThoughts(msg)"
            class="thinking-section"
            :open="isStreaming(msg)"
          >
            <summary class="thinking-header">
              <span class="thinking-status-icon">{{ isStreaming(msg) ? '💭' : '✅' }}</span>
              <span class="thinking-label">思考过程</span>
              <span class="thinking-summary">{{ thinkingSummary(msg) }}</span>
              <span v-if="isStreaming(msg)" class="thinking-badge">思考中</span>
              <span v-else class="thinking-done-badge">完成</span>
            </summary>
            <div class="thinking-content">{{ msg.thoughts }}</div>
          </details>

          <!-- 思考→回答 分隔 -->
          <div
            v-if="msg.role === 'assistant' && hasThoughts(msg) && msg.content"
            class="answer-divider"
          >
            <span class="divider-line"></span>
            <span class="divider-label">📝 回答</span>
            <span class="divider-line"></span>
          </div>

          <!-- ====== 回答内容 ====== -->
          <p class="content" v-if="msg.content || isStreaming(msg) || isSending(msg)">
            <TypewriterText
              v-if="msg.role === 'assistant'"
              :text="msg.content"
              :active="isStreaming(msg)"
            />
            <template v-else>{{ msg.content }}</template>
          </p>

          <!-- 发送中：等待动画 -->
          <span v-if="isSending(msg)" class="waiting-dots">
            <span class="dot">.</span><span class="dot">.</span><span class="dot">.</span>
          </span>

          <!-- 流式传输中：闪烁光标 -->
          <span v-else-if="isStreaming(msg)" class="cursor">▍</span>

          <!-- 出错：错误标记 + 重试按钮 -->
          <div v-else-if="isError(msg)" class="error-footer">
            <span class="error-badge" :title="msg.error">发送失败</span>
            <button class="retry-btn" :disabled="loading" @click="handleRetry">重试</button>
          </div>
        </div>
      </div>
    </main>

    <footer class="input-area">
      <textarea
        v-model="inputText"
        class="input-box"
        placeholder="输入消息，Enter 发送，Shift+Enter 换行"
        rows="2"
        :disabled="loading"
        @keydown="handleKeydown"
      />
      <button
        v-if="loading"
        class="stop-btn"
        @click="handleAbort"
      >
        ⏹ 停止生成
      </button>
      <button
        v-else
        class="send-btn"
        :disabled="!inputText.trim()"
        @click="handleSend"
      >
        发送
      </button>
    </footer>
  </div>
</template>

<style scoped>
.chat-room {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: min(1000px, 92vw);
  margin: 0 auto;
  background: #fff;
  box-shadow: 0 0 40px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  position: relative;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
  background: #fafafa;
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  font-size: 18px;
  color: #666;
  text-decoration: none;
  transition: background 0.2s;
}

.back-btn:hover {
  background: #eee;
}

.header-info {
  flex: 1;
}

.title {
  font-size: 18px;
  font-weight: 600;
}

.subtitle {
  margin-top: 2px;
  font-size: 13px;
  color: #888;
}

.messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.empty-hint {
  text-align: center;
  color: #bbb;
  margin-top: 40%;
  font-size: 14px;
}

.message-row {
  display: flex;
  gap: 10px;
  max-width: 80%;
}

.message-row.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-row.assistant {
  align-self: flex-start;
}

/* 出错消息轻微高亮 */
.message-row.msg-error .avatar {
  background: #fde8e8;
  color: #c0392b;
}

.avatar {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}

.user .avatar {
  background: #4f6ef7;
  color: #fff;
}

.assistant .avatar {
  background: #e8ecf4;
  color: #4f6ef7;
}

.bubble {
  padding: 10px 14px;
  border-radius: 12px;
  line-height: 1.6;
  font-size: 14px;
  word-break: break-word;
  white-space: pre-wrap;
}

.user .bubble {
  background: #4f6ef7;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.assistant .bubble {
  background: #f4f6fb;
  color: #333;
  border-bottom-left-radius: 4px;
}

/* ---- 思考过程（可折叠，与回答区域明确区分）---- */

.thinking-section {
  margin-bottom: 6px;
  border: 1px solid #e8e8ed;
  border-radius: 10px;
  background: #f8f8fb;
  overflow: hidden;
  transition: border-color 0.3s, box-shadow 0.3s;
}

.thinking-section[open] {
  border-color: #d0d0dd;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
}

.thinking-header {
  padding: 9px 14px;
  font-size: 13px;
  color: #8e8ea0;
  cursor: pointer;
  user-select: none;
  display: flex;
  align-items: center;
  gap: 7px;
  list-style: none;
  transition: background 0.15s, color 0.15s;
}

/* 隐藏默认的 details 三角箭头 */
.thinking-header::-webkit-details-marker { display: none; }
.thinking-header::marker { display: none; content: ''; }

.thinking-header:hover {
  background: #f0f0f5;
  color: #6b6b80;
}

.thinking-status-icon {
  font-size: 14px;
  line-height: 1;
}

.thinking-label {
  font-weight: 600;
  color: #5b5b70;
}

.thinking-summary {
  flex: 1;
  font-size: 11.5px;
  color: #b0b0c0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.thinking-badge {
  font-size: 10.5px;
  color: #7c3aed;
  background: #ede9fe;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
  animation: thinkPulse 1.5s ease-in-out infinite;
  flex-shrink: 0;
}

.thinking-done-badge {
  font-size: 10.5px;
  color: #059669;
  background: #d1fae5;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
  flex-shrink: 0;
}

@keyframes thinkPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.thinking-content {
  padding: 10px 14px 14px;
  font-size: 12.5px;
  line-height: 1.7;
  color: #8e8ea0;
  border-top: 1px solid #eeeef2;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 260px;
  overflow-y: auto;
}

/* ---- 思考→回答 分隔线 ---- */

.answer-divider {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 2px 0 10px;
}

.divider-line {
  flex: 1;
  height: 1px;
  background: #e8e8ed;
}

.divider-label {
  font-size: 11.5px;
  color: #b0b0c0;
  font-weight: 500;
  flex-shrink: 0;
}

/* ---- 出错消息气泡 ---- */
.bubble-error {
  border-left: 3px solid #e74c3c;
  background: #fef5f5;
}

.content {
  display: inline;
}

/* ---- 状态指示器 ---- */

/* 闪烁光标（streaming） */
.cursor {
  display: inline;
  animation: blink 1s step-end infinite;
  color: #4f6ef7;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

/* 等待动画（sending） */
.waiting-dots {
  display: inline;
  color: #aaa;
}

.waiting-dots .dot {
  animation: dotPulse 1.4s ease-in-out infinite both;
}

.waiting-dots .dot:nth-child(1) {
  animation-delay: 0s;
}

.waiting-dots .dot:nth-child(2) {
  animation-delay: 0.2s;
}

.waiting-dots .dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes dotPulse {
  0%, 80%, 100% {
    opacity: 0.2;
  }
  40% {
    opacity: 1;
  }
}

/* 错误底部栏 */
.error-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}

.error-badge {
  font-size: 12px;
  color: #c0392b;
  background: #fde8e8;
  padding: 2px 8px;
  border-radius: 4px;
}

.retry-btn {
  font-size: 12px;
  padding: 2px 10px;
  border: 1px solid #e74c3c;
  border-radius: 4px;
  color: #c0392b;
  background: #fff;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.retry-btn:hover:not(:disabled) {
  background: #e74c3c;
  color: #fff;
}

.retry-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ---- 输入区域 ---- */

.input-area {
  display: flex;
  gap: 10px;
  padding: 16px 20px;
  border-top: 1px solid #eee;
  background: #fafafa;
}

.input-box {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 10px;
  resize: none;
  font-size: 14px;
  line-height: 1.5;
  transition: border-color 0.2s;
}

.input-box:focus {
  border-color: #4f6ef7;
}

.input-box:disabled {
  background: #f5f5f5;
  color: #999;
}

.send-btn {
  align-self: flex-end;
  padding: 10px 24px;
  background: #4f6ef7;
  color: #fff;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  transition: background 0.2s, opacity 0.2s;
}

.send-btn:hover:not(:disabled) {
  background: #3d5ce5;
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ---- 停止生成按钮 ---- */
.stop-btn {
  align-self: flex-end;
  padding: 10px 20px;
  background: #fff;
  color: #ef4444;
  border: 1.5px solid #fecaca;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.stop-btn:hover {
  background: #fef2f2;
  border-color: #ef4444;
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.15);
}

/* ========================================
   响应式适配
   - 桌面（> 1024px）：默认样式，max-width 900px
   - 平板（641px - 1024px）：max-width 100%，缩小间距
   - 手机（≤ 640px）：全宽，取消圆角/阴影，增大触控区域
   ======================================== */

/* ---- 平板 ---- */
@media (max-width: 1024px) {
  .chat-room {
    width: 100%;
    box-shadow: none;
  }

  .message-row {
    max-width: 85%;
  }

  .messages {
    padding: 16px;
  }
}

/* ---- 手机 ---- */
@media (max-width: 640px) {
  .chat-room {
    height: 100vh;
    height: 100dvh;
    width: 100%;
    box-shadow: none;
    border-radius: 0;
    overflow: hidden;
  }

  .chat-header {
    padding: 12px 14px;
    gap: 8px;
  }

  .back-btn {
    width: 32px;
    height: 32px;
    font-size: 16px;
  }

  .title {
    font-size: 16px;
  }

  .subtitle {
    font-size: 12px;
  }

  .messages {
    padding: 12px;
    gap: 12px;
  }

  .message-row {
    max-width: 90%;
    gap: 6px;
  }

  .avatar {
    width: 30px;
    height: 30px;
    font-size: 10px;
  }

  .bubble {
    padding: 8px 12px;
    font-size: 13px;
    border-radius: 10px;
  }

  .user .bubble {
    border-bottom-right-radius: 3px;
  }

  .assistant .bubble {
    border-bottom-left-radius: 3px;
  }

  .input-area {
    padding: 10px 12px;
    /* 适配 iPhone 底部安全区 */
    padding-bottom: calc(10px + env(safe-area-inset-bottom, 0px));
  }

  .input-box {
    padding: 8px 10px;
    font-size: 14px;
    /* 移动端 16px 可防止 iOS 自动缩放 */
  }

  .send-btn {
    padding: 8px 16px;
    font-size: 13px;
    border-radius: 8px;
  }

  .empty-hint {
    font-size: 13px;
    margin-top: 45%;
  }
}

/* ---- 极小屏（iPhone SE 等 < 375px）---- */
@media (max-width: 374px) {
  .message-row {
    max-width: 95%;
    gap: 4px;
  }

  .avatar {
    width: 26px;
    height: 26px;
    font-size: 9px;
  }

  .bubble {
    padding: 6px 10px;
    font-size: 12px;
  }

  .input-area {
    padding: 8px 10px;
    gap: 6px;
  }

  .send-btn {
    padding: 8px 12px;
    font-size: 12px;
  }
}

/* ========================================
   极客简蓝主题（theme-car）
   风格：干净、理性、高效，蓝白灰配色
   ======================================== */

.theme-car.chat-room {
  background: #f8fafc;
  box-shadow:
    0 0 0 1px rgba(0, 0, 0, 0.04),
    0 4px 32px rgba(0, 0, 0, 0.04);
}

/* ---- 头部 ---- */
.theme-car .chat-header {
  background: #fff;
  border-bottom: 1px solid #e8ecf1;
  position: relative;
  z-index: 1;
}

.theme-car .title {
  color: #0f172a;
  font-weight: 700;
  letter-spacing: -0.3px;
}

.theme-car .title::before {
  content: '▶ ';
  font-size: 10px;
  color: #2563eb;
  vertical-align: middle;
}

.theme-car .subtitle {
  color: #94a3b8;
  font-family: 'SF Mono', 'Fira Code', 'Cascadia Code', monospace;
  font-size: 12px;
}

.theme-car .back-btn {
  color: #64748b;
  border-radius: 6px;
}

.theme-car .back-btn:hover {
  background: #f1f5f9;
  color: #2563eb;
}

/* ---- 消息区 ---- */
.theme-car .messages {
  background:
    linear-gradient(180deg, #fafbfc 0%, #f8fafc 100%);
  position: relative;
  z-index: 1;
}

.theme-car .empty-hint {
  color: #94a3b8;
  font-family: 'SF Mono', 'Fira Code', 'Cascadia Code', monospace;
  font-size: 13px;
}

/* ---- 头像 ---- */
.theme-car .user .avatar {
  background: #1e3a5f;
  color: #e0f2fe;
  font-weight: 600;
  border-radius: 8px;
}

.theme-car .assistant .avatar {
  background: #2563eb;
  color: #fff;
  font-weight: 600;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.15);
}

/* ---- 气泡 ---- */
.theme-car .user .bubble {
  background: #1e3a5f;
  color: #e8f0f8;
  border-radius: 12px 12px 4px 12px;
  font-size: 14px;
  line-height: 1.65;
}

.theme-car .assistant .bubble {
  background: #fff;
  color: #1e293b;
  border: 1px solid #e8ecf1;
  border-radius: 12px 12px 12px 4px;
  font-size: 14px;
  line-height: 1.7;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

/* ---- 思考过程 ---- */
.theme-car .thinking-section {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.theme-car .thinking-section[open] {
  border-color: #cbd5e1;
}

.theme-car .thinking-status-icon {
  font-size: 14px;
}

.theme-car .thinking-label {
  color: #475569;
}

.theme-car .thinking-summary {
  color: #94a3b8;
}

.theme-car .thinking-header {
  color: #64748b;
  font-size: 12.5px;
}

.theme-car .thinking-header:hover {
  background: #f1f5f9;
}

.theme-car .thinking-badge {
  color: #2563eb;
  background: #dbeafe;
}

.theme-car .thinking-done-badge {
  color: #059669;
  background: #d1fae5;
}

.theme-car .thinking-content {
  color: #64748b;
  border-top: 1px solid #e8ecf1;
  font-size: 12.5px;
}

.theme-car .answer-divider .divider-line {
  background: #e2e8f0;
}

.theme-car .answer-divider .divider-label {
  color: #94a3b8;
}

/* ---- 状态指示器 ---- */
.theme-car .cursor {
  color: #2563eb;
}

.theme-car .waiting-dots {
  color: #94a3b8;
}

/* ---- 出错消息 ---- */
.theme-car .bubble-error {
  border-left-color: #ef4444;
  background: #fef5f5;
}

.theme-car .message-row.msg-error .avatar {
  background: #fee2e2;
  color: #ef4444;
}

.theme-car .error-badge {
  color: #dc2626;
  background: #fee2e2;
}

.theme-car .retry-btn {
  color: #64748b;
  border: 1px solid #d1d5db;
  background: #fff;
  border-radius: 4px;
}

.theme-car .retry-btn:hover:not(:disabled) {
  background: #f1f5f9;
  color: #2563eb;
  border-color: #2563eb;
}

/* ---- 输入区域 ---- */
.theme-car .input-area {
  background: #fff;
  border-top: 1px solid #e8ecf1;
  position: relative;
  z-index: 1;
}

.theme-car .input-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  color: #1e293b;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.theme-car .input-box::placeholder {
  color: #94a3b8;
}

.theme-car .input-box:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.08);
  background: #fff;
}

.theme-car .input-box:disabled {
  background: #f1f5f9;
  color: #94a3b8;
}

/* ---- 发送按钮 ---- */
.theme-car .send-btn {
  background: #2563eb;
  color: #fff;
  font-weight: 600;
  font-size: 14px;
  border-radius: 8px;
  transition: all 0.2s;
}

.theme-car .send-btn:hover:not(:disabled) {
  background: #1d4ed8;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);
}

.theme-car .send-btn:disabled {
  background: #e2e8f0;
  color: #94a3b8;
}

.theme-car .stop-btn {
  background: #fff;
  color: #ef4444;
  border: 1.5px solid #fecaca;
}

.theme-car .stop-btn:hover {
  background: #fef2f2;
  border-color: #ef4444;
}

/* ========================================
   🤖 超级智能体卡通风格主题（theme-manus）
   ======================================== */

.theme-manus.chat-room {
  background: linear-gradient(170deg, #faf5ff 0%, #f0e6ff 30%, #e8f4fd 70%, #f5f0ff 100%);
  box-shadow:
    0 0 60px rgba(139, 92, 246, 0.08),
    0 0 0 1px rgba(139, 92, 246, 0.06);
}

/* ---- 浮动装饰圆点（卡通趣味）---- */
.theme-manus.chat-room::before {
  content: '';
  position: absolute;
  top: -60px;
  right: -40px;
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(168, 85, 247, 0.06) 0%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
  z-index: 0;
}

.theme-manus.chat-room::after {
  content: '';
  position: absolute;
  bottom: 80px;
  left: -50px;
  width: 160px;
  height: 160px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.05) 0%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
  z-index: 0;
}

/* ---- 头部 ---- */
.theme-manus .chat-header {
  background: linear-gradient(135deg, #7c3aed 0%, #a855f7 50%, #6366f1 100%);
  border-bottom: none;
  position: relative;
  z-index: 1;
  box-shadow: 0 4px 20px rgba(139, 92, 246, 0.2);
}

.theme-manus .header-info {
  color: #fff;
}

.theme-manus .title {
  color: #fff;
  font-weight: 700;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.theme-manus .subtitle {
  color: rgba(255, 255, 255, 0.8);
}

.theme-manus .back-btn {
  color: rgba(255, 255, 255, 0.85);
  background: rgba(255, 255, 255, 0.12);
  border-radius: 10px;
}

.theme-manus .back-btn:hover {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
}

/* ---- 消息区 ---- */
.theme-manus .messages {
  background: transparent;
  position: relative;
  z-index: 1;
}

.theme-manus .empty-hint {
  color: #c4b5e0;
  font-size: 15px;
}

/* ---- 头像 ---- */
.theme-manus .user .avatar {
  background: linear-gradient(135deg, #f97316 0%, #ef4444 100%);
  color: #fff;
  box-shadow: 0 3px 12px rgba(249, 115, 22, 0.3);
  border-radius: 40% 60% 50% 50%;
}

.theme-manus .assistant .avatar {
  background: linear-gradient(135deg, #8b5cf6 0%, #6366f1 100%);
  color: #fff;
  box-shadow: 0 3px 12px rgba(139, 92, 246, 0.3);
  border-radius: 50% 40% 40% 60%;
  font-size: 14px;
}

/* ---- 气泡 ---- */
.theme-manus .assistant .bubble {
  background: #fff;
  color: #374151;
  border-radius: 16px 16px 16px 4px;
  box-shadow: 0 2px 12px rgba(139, 92, 246, 0.08);
}

.theme-manus .user .bubble {
  background: linear-gradient(135deg, #7c3aed 0%, #a855f7 100%);
  color: #fff;
  border-radius: 16px 16px 4px 16px;
  box-shadow: 0 4px 16px rgba(139, 92, 246, 0.25);
}

/* ---- 思考过程 ---- */
.theme-manus .thinking-section {
  background: linear-gradient(135deg, #faf5ff, #f3e8ff);
  border: 1px solid rgba(139, 92, 246, 0.1);
  border-radius: 12px;
}

.theme-manus .thinking-section[open] {
  border-color: rgba(139, 92, 246, 0.2);
}

.theme-manus .thinking-status-icon {
  font-size: 15px;
}

.theme-manus .thinking-label {
  color: #7c3aed;
  font-weight: 600;
}

.theme-manus .thinking-summary {
  color: #b8a9e0;
}

.theme-manus .thinking-header {
  color: #8b5cf6;
}

.theme-manus .thinking-header:hover {
  background: rgba(139, 92, 246, 0.06);
}

.theme-manus .thinking-badge {
  color: #7c3aed;
  background: rgba(139, 92, 246, 0.1);
}

.theme-manus .thinking-done-badge {
  color: #059669;
  background: #d1fae5;
}

.theme-manus .thinking-content {
  color: #7c6f9a;
  border-top: 1px solid rgba(139, 92, 246, 0.08);
}

.theme-manus .answer-divider .divider-line {
  background: rgba(139, 92, 246, 0.1);
}

.theme-manus .answer-divider .divider-label {
  color: #a78bfa;
}

/* ---- 状态指示器 ---- */
.theme-manus .cursor {
  color: #a855f7;
}

.theme-manus .waiting-dots {
  color: #c4b5fd;
}

/* ---- 出错消息 ---- */
.theme-manus .bubble-error {
  border-left-color: #ef4444;
  background: #fef2f2;
}

.theme-manus .message-row.msg-error .avatar {
  background: #fee2e2;
  color: #ef4444;
}

/* ---- 输入区域 ---- */
.theme-manus .input-area {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  border-top: 1px solid rgba(139, 92, 246, 0.1);
  position: relative;
  z-index: 1;
}

.theme-manus .input-box {
  background: #fff;
  border: 2px solid rgba(139, 92, 246, 0.12);
  color: #374151;
  border-radius: 14px;
  transition: border-color 0.25s, box-shadow 0.25s;
}

.theme-manus .input-box::placeholder {
  color: #c4b5e0;
}

.theme-manus .input-box:focus {
  border-color: rgba(139, 92, 246, 0.5);
  box-shadow: 0 0 0 4px rgba(139, 92, 246, 0.08);
}

.theme-manus .input-box:disabled {
  background: #f5f3ff;
}

/* ---- 发送按钮 ---- */
.theme-manus .send-btn {
  background: linear-gradient(135deg, #7c3aed 0%, #a855f7 100%);
  color: #fff;
  font-weight: 600;
  border-radius: 14px;
  border: none;
  box-shadow: 0 4px 16px rgba(139, 92, 246, 0.3);
  transition: all 0.25s;
}

.theme-manus .send-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #8b5cf6 0%, #c084fc 100%);
  box-shadow: 0 6px 24px rgba(139, 92, 246, 0.45);
  transform: translateY(-2px) scale(1.03);
}

.theme-manus .send-btn:disabled {
  background: #e5e0f0;
  color: #c4b5e0;
  box-shadow: none;
}

.theme-manus .stop-btn {
  background: #fff;
  color: #ef4444;
  border: 1.5px solid #fecaca;
  border-radius: 14px;
}

.theme-manus .stop-btn:hover {
  background: #fef2f2;
  border-color: #ef4444;
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.15);
}
</style>

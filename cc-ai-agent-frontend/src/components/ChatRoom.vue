<script setup>
import { ref, nextTick, watch } from 'vue'

const props = defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  chatId: { type: String, default: '' },
  loading: { type: Boolean, default: false },
})

const emit = defineEmits(['send', 'retry'])

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
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

function handleRetry() {
  emit('retry')
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
</script>

<template>
  <div class="chat-room">
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
          <p class="content">{{ msg.content }}</p>

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
      <button class="send-btn" :disabled="loading || !inputText.trim()" @click="handleSend">
        {{ loading ? '回复中...' : '发送' }}
      </button>
    </footer>
  </div>
</template>

<style scoped>
.chat-room {
  display: flex;
  flex-direction: column;
  height: 100vh;
  max-width: 900px;
  margin: 0 auto;
  background: #fff;
  box-shadow: 0 0 40px rgba(0, 0, 0, 0.06);
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

/* 出错消息气泡 */
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

/* ========================================
   响应式适配
   - 桌面（> 1024px）：默认样式，max-width 900px
   - 平板（641px - 1024px）：max-width 100%，缩小间距
   - 手机（≤ 640px）：全宽，取消圆角/阴影，增大触控区域
   ======================================== */

/* ---- 平板 ---- */
@media (max-width: 1024px) {
  .chat-room {
    max-width: 100%;
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
    max-width: 100%;
    box-shadow: none;
    border-radius: 0;
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
</style>

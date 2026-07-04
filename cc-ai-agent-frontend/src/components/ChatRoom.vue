<script setup>
import { ref, nextTick, watch } from 'vue'

const props = defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  chatId: { type: String, default: '' },
  loading: { type: Boolean, default: false },
})

const emit = defineEmits(['send'])

const inputText = ref('')
const messagesRef = ref(null)

const messages = defineModel('messages', { type: Array, default: () => [] })

watch(
  () => messages.value.length,
  async () => {
    await nextTick()
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  },
)

watch(
  () => messages.value[messages.value.length - 1]?.content,
  async () => {
    await nextTick()
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  },
)

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
</script>

<template>
  <div class="chat-room">
    <header class="chat-header">
      <router-link to="/" class="back-btn" title="返回主页">←</router-link>
      <div class="header-info">
        <h1 class="title">{{ title }}</h1>
        <p v-if="subtitle" class="subtitle">{{ subtitle }}</p>
        <p v-if="chatId" class="chat-id">会话 ID: {{ chatId }}</p>
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
        :class="msg.role"
      >
        <div class="avatar">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
        <div class="bubble">
          <p class="content">{{ msg.content }}</p>
          <span v-if="msg.streaming" class="cursor">▍</span>
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

.chat-id {
  margin-top: 4px;
  font-size: 11px;
  color: #aaa;
  font-family: monospace;
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

.content {
  display: inline;
}

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
</style>

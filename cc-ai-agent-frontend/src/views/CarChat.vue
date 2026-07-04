<script setup>
import { ref, onMounted } from 'vue'
import ChatRoom from '../components/ChatRoom.vue'
import { doChatWithCarAppSse } from '../api/chat'
import { generateChatId } from '../utils/uuid'

const chatId = ref('')
const messages = ref([])
const loading = ref(false)
let abortController = null

onMounted(() => {
  chatId.value = generateChatId()
})

async function handleSend(text) {
  messages.value.push({ role: 'user', content: text })
  messages.value.push({ role: 'assistant', content: '', streaming: true })
  const aiIndex = messages.value.length - 1

  loading.value = true
  abortController?.abort()
  abortController = new AbortController()

  try {
    await doChatWithCarAppSse(text, chatId.value, {
      signal: abortController.signal,
      onChunk: (chunk) => {
        messages.value[aiIndex].content += chunk
      },
    })
  } catch (err) {
    if (err.name !== 'AbortError') {
      messages.value[aiIndex].content =
        messages.value[aiIndex].content || `请求出错: ${err.message}`
    }
  } finally {
    messages.value[aiIndex].streaming = false
    loading.value = false
  }
}
</script>

<template>
  <ChatRoom
    v-model:messages="messages"
    title="AI 选车大师"
    subtitle="专业选车顾问，为你提供个性化购车建议"
    :chat-id="chatId"
    :loading="loading"
    @send="handleSend"
  />
</template>

<script setup>
import { ref } from 'vue'
import ChatRoom from '../components/ChatRoom.vue'
import { doChatWithManus } from '../api/chat'

const messages = ref([])
const loading = ref(false)
let abortController = null

async function handleSend(text) {
  messages.value.push({ role: 'user', content: text })
  messages.value.push({ role: 'assistant', content: '', streaming: true })
  const aiIndex = messages.value.length - 1

  loading.value = true
  abortController?.abort()
  abortController = new AbortController()

  try {
    await doChatWithManus(text, {
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
    title="AI 超级智能体"
    subtitle="具备工具调用能力的智能体，可执行复杂任务"
    :loading="loading"
    @send="handleSend"
  />
</template>

<script setup>
import { ref } from 'vue'
import ChatRoom from '../components/ChatRoom.vue'
import { doChatWithCarAppSse } from '../api/chat'
import { useChat } from '../composables/useChat'
import { generateChatId } from '../utils/uuid'

const chatId = ref(generateChatId())

const { messages, loading, send, retry, abort } = useChat((message, { signal, onChunk }) =>
  doChatWithCarAppSse(message, chatId.value, { signal, onChunk }),
)
</script>

<template>
  <ChatRoom
    v-model:messages="messages"
    title="AI 选车大师"
    subtitle="专业选车顾问，为你提供个性化购车建议"
    :chat-id="chatId"
    :loading="loading"
    theme="car"
    @send="send"
    @retry="retry"
    @abort="abort"
  />
</template>

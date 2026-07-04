<script setup>
import ChatRoom from '../components/ChatRoom.vue'
import { doChatWithManus } from '../api/chat'
import { useChat } from '../composables/useChat'

const { messages, loading, send, retry } = useChat((message, { signal, onChunk }) =>
  doChatWithManus(message, { signal, onChunk }),
)
</script>

<template>
  <ChatRoom
    v-model:messages="messages"
    title="AI 超级智能体"
    subtitle="具备工具调用能力的智能体，可执行复杂任务"
    :loading="loading"
    @send="send"
    @retry="retry"
  />
</template>

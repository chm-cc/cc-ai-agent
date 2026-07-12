<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import ChatRoom from '../components/ChatRoom.vue'
import {
  fetchAgents,
  fetchConversations,
  createConversation,
  deleteConversation,
  doChatWithConversationStream
} from '../api/chat'
import { useChat } from '../composables/useChat'

const route = useRoute()
const agentId = computed(() => route.params.agentId)

const agent = ref(null)
const conversations = ref([])
const activeConvId = ref(null)
const sidebarOpen = ref(true)

// Load agent info
onMounted(async () => {
  try {
    const agents = await fetchAgents()
    agent.value = agents.find(a => a.id === agentId.value) || null
  } catch (e) { /* agent info optional */ }
  await refreshConversations()
})

async function refreshConversations() {
  try {
    const data = await fetchConversations(agentId.value)
    conversations.value = data.list || []
  } catch (e) { /* ignore */ }
}

// Chat composable for active conversation
const { messages, loading, send, retry, abort } = useChat((message, { signal, onChunk }) => {
  if (!activeConvId.value) throw new Error('No conversation selected')
  return doChatWithConversationStream(activeConvId.value, message, { onChunk, signal })
})

async function handleNewChat() {
  try {
    const conv = await createConversation(agentId.value, '新对话')
    activeConvId.value = conv.id
    messages.value = []
    await refreshConversations()
  } catch (e) {
    console.error('创建会话失败', e)
  }
}

async function handleSelectConv(conv) {
  activeConvId.value = conv.id
  // For MVP, start fresh (ChatMemory handles context server-side)
  messages.value = []
}

async function handleDeleteConv(id) {
  if (!confirm('确定删除该会话？')) return
  try {
    await deleteConversation(id)
    if (activeConvId.value === id) {
      activeConvId.value = null
      messages.value = []
    }
    await refreshConversations()
  } catch (e) {
    console.error('删除失败', e)
  }
}

function formatTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  const now = new Date()
  const diff = now - d
  if (diff < 86400000) return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}
</script>

<template>
  <div class="agent-chat-layout">
    <!-- 侧边栏：会话列表 -->
    <aside class="sidebar" :class="{ closed: !sidebarOpen }">
      <div class="sidebar-header">
        <div class="agent-info" v-if="agent">
          <span class="agent-icon">{{ agent.icon || '🤖' }}</span>
          <span class="agent-name">{{ agent.name }}</span>
        </div>
        <button class="btn-new" @click="handleNewChat">+ 新对话</button>
      </div>

      <div class="conv-list">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          class="conv-item"
          :class="{ active: conv.id === activeConvId }"
          @click="handleSelectConv(conv)"
        >
          <div class="conv-main">
            <div class="conv-title">{{ conv.title || '新对话' }}</div>
            <div class="conv-preview">{{ conv.lastMessage || '' }}</div>
          </div>
          <div class="conv-meta">
            <span class="conv-time">{{ formatTime(conv.updatedAt || conv.createdAt) }}</span>
            <button class="btn-del" @click.stop="handleDeleteConv(conv.id)" title="删除">×</button>
          </div>
        </div>

        <div v-if="conversations.length === 0" class="conv-empty">
          暂无对话，点击上方按钮开始
        </div>
      </div>
    </aside>

    <!-- 侧边栏切换按钮 -->
    <button class="sidebar-toggle" @click="sidebarOpen = !sidebarOpen">
      {{ sidebarOpen ? '◀' : '▶' }}
    </button>

    <!-- 主聊天区 -->
    <main class="chat-main">
      <ChatRoom
        v-if="activeConvId"
        v-model:messages="messages"
        :title="agent?.name || 'AI 对话'"
        :subtitle="agent?.description || ''"
        :chat-id="activeConvId"
        :loading="loading"
        :theme="agent?.category === 'productivity' ? 'manus' : 'car'"
        @send="send"
        @retry="retry"
        @abort="abort"
      />
      <div v-else class="chat-placeholder">
        <div class="placeholder-icon">💬</div>
        <p>选择一个会话或创建新对话开始体验</p>
      </div>
    </main>
  </div>
</template>

<style scoped>
.agent-chat-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* ====== 侧边栏 ====== */
.sidebar {
  width: 280px;
  min-width: 280px;
  background: #f9fafb;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  transition: margin-left 0.3s ease, opacity 0.3s ease;
  overflow: hidden;
}

.sidebar.closed {
  width: 0;
  min-width: 0;
  opacity: 0;
  pointer-events: none;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.agent-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-weight: 600;
  font-size: 15px;
  color: #111827;
}

.agent-icon { font-size: 20px; }

.btn-new {
  width: 100%;
  padding: 8px;
  border: 1px dashed #d1d5db;
  border-radius: 8px;
  background: #fff;
  color: #374151;
  font-size: 13px;
  cursor: pointer;
  transition: border-color 0.2s;
}

.btn-new:hover { border-color: #2563eb; color: #2563eb; }

/* ====== 会话列表 ====== */
.conv-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.conv-item {
  padding: 12px;
  border-radius: 10px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  transition: background 0.15s;
}

.conv-item:hover { background: #f3f4f6; }
.conv-item.active { background: #eff6ff; border: 1px solid #dbeafe; }

.conv-main { flex: 1; min-width: 0; }

.conv-title {
  font-size: 13px;
  font-weight: 500;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 2px;
}

.conv-preview {
  font-size: 12px;
  color: #9ca3af;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conv-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  margin-left: 8px;
  flex-shrink: 0;
}

.conv-time { font-size: 11px; color: #d1d5db; }

.btn-del {
  font-size: 16px;
  color: #d1d5db;
  background: none;
  border: none;
  cursor: pointer;
  line-height: 1;
  padding: 0;
}

.btn-del:hover { color: #ef4444; }

.conv-empty {
  padding: 40px 16px;
  text-align: center;
  color: #9ca3af;
  font-size: 13px;
}

/* ====== 切换按钮 ====== */
.sidebar-toggle {
  position: absolute;
  left: 280px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 10;
  width: 24px;
  height: 48px;
  border: 1px solid #e5e7eb;
  border-left: none;
  border-radius: 0 6px 6px 0;
  background: #fff;
  color: #6b7280;
  font-size: 10px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: left 0.3s ease;
}

.sidebar.closed ~ .sidebar-toggle { left: 0; }

/* ====== 主区域 ====== */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  margin-left: 24px;
}

.chat-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  gap: 12px;
}

.placeholder-icon { font-size: 48px; }

@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 20;
    box-shadow: 2px 0 12px rgba(0,0,0,0.1);
  }
  .sidebar.closed { margin-left: -280px; min-width: 280px; width: 280px; opacity: 1; pointer-events: auto; }
  .sidebar-toggle { display: none; }
  .chat-main { margin-left: 0; }
}
</style>

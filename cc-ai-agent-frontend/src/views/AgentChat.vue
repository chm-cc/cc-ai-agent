<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ChatRoom from '../components/ChatRoom.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import {
  fetchAgents,
  fetchConversations,
  fetchMessages,
  createConversation,
  deleteConversation,
  renameConversation,
  saveMessage,
  updateFeedback,
  generateConversationTitle,
  doChatWithConversationStream
} from '../api/chat'
import { useChat } from '../composables/useChat'

const route = useRoute()
const router = useRouter()
const agentId = computed(() => route.params.agentId)

const agent = ref(null)
const conversations = ref([])
const activeConvId = ref(null)
const sidebarOpen = ref(true)
const renamingId = ref(null)
const renameTitle = ref('')

// 删除确认弹窗状态
const deleteDialog = ref({
  visible: false,
  convId: null,
  convTitle: '',
  loading: false,
})

// 已尝试生成标题的会话 ID 集合，避免重复请求
const generatedTitles = new Set()

// 进入/切换 Agent 时的初始化逻辑
async function initAgent() {
  const isFromHome = route.query._new === '1'

  try {
    const agents = await fetchAgents()
    agent.value = agents.find(a => a.id === agentId.value) || null
  } catch (e) { /* ignore */ }
  await refreshConversations()

  if (isFromHome) {
    // 从首页点击进入 → 始终开启新会话
    await handleNewChat()
    // 清除 URL 标记，刷新后走"加载最近会话"逻辑
    router.replace({ query: {} })
  } else if (conversations.value.length > 0) {
    // 刷新页面 → 加载最近一个会话
    await handleSelectConv(conversations.value[0])
  } else {
    await handleNewChat()
  }
}

onMounted(initAgent)

// 切换 Agent 时重新初始化（同一路由不同 agentId 组件复用场景）
watch(agentId, async (newId, oldId) => {
  if (newId !== oldId) {
    await cleanupEmptyConv()
    activeConvId.value = null
    messages.value = []
    await initAgent()
  }
})

async function refreshConversations() {
  try {
    const data = await fetchConversations(agentId.value)
    conversations.value = data.list || []
  } catch (e) { /* ignore */ }
}

// AI 回复完成 → 存入数据库
async function onAiDone({ content }) {
  if (!activeConvId.value) return
  try {
    if (content) {
      await saveMessage(activeConvId.value, 'ASSISTANT', content)
      await refreshConversations()
    }
    // 首轮对话完成后，若标题仍为默认值则自动生成
    await tryGenerateTitle()
  } catch (e) { console.error('保存AI回复失败', e) }
}

async function tryGenerateTitle() {
  const cid = activeConvId.value
  if (!cid || generatedTitles.has(cid)) return
  // 找到当前会话，检查标题是否为默认值
  const conv = conversations.value.find(c => c.id === cid)
  if (!conv || conv.title !== '新对话') return

  generatedTitles.add(cid)
  try {
    const title = await generateConversationTitle(cid)
    if (title) {
      await refreshConversations()
    }
  } catch (e) { /* 生成失败不影响正常使用，用户可手动修改 */ }
}

const { messages, loading, send, retry, abort } = useChat(
  (message, { signal, onChunk }) => {
    if (!activeConvId.value) throw new Error('No conversation selected')
    return doChatWithConversationStream(activeConvId.value, message, { onChunk, signal })
  },
  { onDone: onAiDone }
)

// 清理空会话：当前会话没有任何消息时自动删除
async function cleanupEmptyConv() {
  if (activeConvId.value && messages.value.length === 0) {
    try {
      await deleteConversation(activeConvId.value)
    } catch (e) { /* ignore */ }
  }
}

// 离开页面时清理空会话
onBeforeUnmount(() => {
  cleanupEmptyConv()
})

async function handleNewChat() {
  try {
    await cleanupEmptyConv()
    const conv = await createConversation(agentId.value, '新对话')
    activeConvId.value = conv.id
    messages.value = []
    await refreshConversations()
  } catch (e) { console.error('创建会话失败', e) }
}

async function handleSelectConv(conv) {
  await cleanupEmptyConv()
  activeConvId.value = conv.id
  // 加载历史消息
  try {
    const data = await fetchMessages(conv.id)
    const list = data.list || []
    messages.value = list.map(m => ({
      id: m.id,
      role: m.role.toLowerCase(),
      content: m.content,
      feedback: m.feedback,
      status: 'done'
    }))
  } catch (e) {
    messages.value = []
  }
}

function handleDeleteConv(conv) {
  deleteDialog.value = {
    visible: true,
    convId: conv.id,
    convTitle: conv.title || '新对话',
    loading: false,
  }
}

async function confirmDelete() {
  deleteDialog.value.loading = true
  try {
    await deleteConversation(deleteDialog.value.convId)
    if (activeConvId.value === deleteDialog.value.convId) {
      activeConvId.value = null
      messages.value = []
    }
    await refreshConversations()
    deleteDialog.value.visible = false
  } catch (e) {
    console.error('删除失败', e)
  } finally {
    deleteDialog.value.loading = false
  }
}

function cancelDelete() {
  deleteDialog.value.visible = false
}

function startRename(conv) {
  renamingId.value = conv.id
  renameTitle.value = conv.title || ''
}

async function confirmRename() {
  if (!renamingId.value || !renameTitle.value.trim()) return
  try {
    await renameConversation(renamingId.value, renameTitle.value.trim())
    renamingId.value = null
    await refreshConversations()
  } catch (e) { console.error('重命名失败', e) }
}

function cancelRename() {
  renamingId.value = null
}

async function handleFeedback(msgId, fb) {
  if (!activeConvId.value || !msgId) return
  try {
    await updateFeedback(activeConvId.value, msgId, fb)
    // 更新本地消息状态
    const msg = messages.value.find(m => m.id === msgId)
    if (msg) msg.feedback = fb
  } catch (e) { console.error('反馈失败', e) }
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
          <div class="conv-main" @dblclick.stop="startRename(conv)">
            <div v-if="renamingId === conv.id" class="conv-rename">
              <input
                v-model="renameTitle"
                class="rename-input"
                @keyup.enter="confirmRename"
                @keyup.escape="cancelRename"
                @blur="cancelRename"
                @click.stop
                autofocus
              />
            </div>
            <div v-else class="conv-title">{{ conv.title || '新对话' }}</div>
            <div class="conv-preview">{{ conv.lastMessage || '' }}</div>
          </div>
          <div class="conv-meta">
            <span class="conv-time">{{ formatTime(conv.updatedAt || conv.createdAt) }}</span>
            <button class="btn-del" @click.stop="handleDeleteConv(conv)" title="删除">×</button>
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
        @feedback="handleFeedback"
      />
      <div v-else class="chat-placeholder">
        <div class="placeholder-icon">💬</div>
        <p>选择一个会话或创建新对话开始体验</p>
      </div>
    </main>
  </div>

  <!-- 删除确认弹窗 -->
  <ConfirmDialog
    :visible="deleteDialog.visible"
    title="删除会话"
    description="删除后将无法恢复，该会话下的所有消息记录将被永久清除。"
    :detail="deleteDialog.convTitle"
    confirm-text="删除"
    danger
    :loading="deleteDialog.loading"
    @confirm="confirmDelete"
    @cancel="cancelDelete"
  />
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

.conv-rename { margin-bottom: 2px; }

.rename-input {
  width: 100%;
  padding: 2px 6px;
  font-size: 13px;
  border: 1px solid #2563eb;
  border-radius: 4px;
  outline: none;
  font-family: inherit;
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

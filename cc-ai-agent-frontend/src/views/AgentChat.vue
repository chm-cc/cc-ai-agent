<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ChatRoom from '../components/ChatRoom.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import AgentIcon from '../components/AgentIcon.vue'
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
  if (!agentId.value) return
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
    conversations.value = []
    generatedTitles.clear()
    await initAgent()
  }
})

async function refreshConversations() {
  if (!agentId.value) return
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

// 移动端：选择会话后自动关闭侧边栏
function handleSelectConvMobile(conv) {
  handleSelectConv(conv)
  sidebarOpen.value = false
}

function handleNewChatMobile() {
  handleNewChat()
  sidebarOpen.value = false
}

function chatTheme(agent) {
  if (!agent) return 'general'
  if (agent.id === 'super-agent') return 'manus'
  if (agent.id === 'car-advisor') return 'advisor'
  if (isTravelAgent(agent)) return 'travel'
  if (agent.category === 'productivity') return 'manus'
  if (agent.category === 'advisor') return 'advisor'
  return 'general'
}

function isTravelAgent(agent) {
  const id = String(agent.id || '').toLowerCase()
  return id.includes('travel') || id.includes('trip') || id.includes('tour') || agent.name?.includes('旅游')
}
</script>

<template>
  <div class="agent-chat-layout">
    <!-- 移动端遮罩层 -->
    <div
      class="sidebar-backdrop"
      :class="{ visible: sidebarOpen }"
      @click="sidebarOpen = false"
    ></div>

    <!-- 侧边栏：会话列表 -->
    <aside class="sidebar" :class="{ closed: !sidebarOpen }">
      <div class="sidebar-header">
        <div class="agent-info" v-if="agent">
          <AgentIcon :icon="agent.icon || 'robot'" :size="20" theme="outline" fill="#6b7280" />
          <span class="agent-name">{{ agent.name }}</span>
        </div>
        <button class="btn-new" @click="handleNewChatMobile">+ 新对话</button>
        <!-- 移动端关闭按钮 -->
        <button class="sidebar-close-btn" @click="sidebarOpen = false">✕</button>
      </div>

      <div class="conv-list">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          class="conv-item"
          :class="{ active: conv.id === activeConvId }"
          @click="handleSelectConvMobile(conv)"
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

    <!-- 桌面端侧边栏切换按钮 -->
    <button class="sidebar-toggle" @click="sidebarOpen = !sidebarOpen" :title="sidebarOpen ? '收起侧栏' : '展开侧栏'">
      {{ sidebarOpen ? '◀' : '▶' }}
    </button>

    <!-- 主聊天区 -->
    <main class="chat-main">
      <!-- 移动端顶部栏：返回 + 汉堡菜单 + Agent 名称 + 新建 -->
      <div class="mobile-chat-header">
        <router-link to="/" class="mobile-back-btn" title="返回首页">←</router-link>
        <button class="hamburger-btn" @click="sidebarOpen = true">
          <span></span><span></span><span></span>
        </button>
        <span class="mobile-agent-title">{{ agent?.name || 'AI 对话' }}</span>
        <button class="mobile-new-btn" @click="handleNewChatMobile" title="新对话">+</button>
      </div>

      <ChatRoom
        v-if="activeConvId"
        v-model:messages="messages"
        :title="agent?.name || 'AI 对话'"
        :subtitle="agent?.description || ''"
        :chat-id="activeConvId"
        :loading="loading"
        :theme="chatTheme(agent)"
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
  position: relative;
  background:
    radial-gradient(circle at 0% 0%, rgba(167,139,250,0.12), transparent 34%),
    radial-gradient(circle at 100% 14%, rgba(125,211,252,0.12), transparent 30%),
    linear-gradient(180deg, #fbfbff, #f5f8fc);
}

/* ====== 移动端遮罩层 ====== */
.sidebar-backdrop {
  display: none;
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.4);
  z-index: 19;
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.sidebar-backdrop.visible {
  opacity: 1;
  pointer-events: auto;
}

/* ====== 侧边栏 ====== */
.sidebar {
  width: 280px;
  min-width: 280px;
  background: rgba(255,255,255,0.78);
  border-right: 1px solid rgba(148,163,184,0.18);
  backdrop-filter: blur(18px);
  display: flex;
  flex-direction: column;
  transition: margin-left 0.3s ease, width 0.3s ease, min-width 0.3s ease, opacity 0.3s ease;
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
  border-bottom: 1px solid rgba(148,163,184,0.16);
  position: relative;
}

.agent-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-weight: 600;
  font-size: 15px;
  color: var(--ai-text);
}

.agent-icon { display: flex; align-items: center; }

.btn-new {
  width: 100%;
  padding: 8px;
  border: 1px dashed rgba(139,92,246,0.26);
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(238,242,255,0.78), rgba(240,249,255,0.70));
  color: #4f46e5;
  font-size: 13px;
  cursor: pointer;
  transition: border-color 0.2s;
}

.btn-new:hover {
  border-color: rgba(109,93,252,0.54);
  color: #4f46e5;
  box-shadow: 0 10px 24px rgba(79,70,229,0.10);
}

/* 移动端侧边栏关闭按钮（桌面端隐藏） */
.sidebar-close-btn {
  display: none;
  position: absolute;
  top: 12px;
  right: 12px;
  width: 28px;
  height: 28px;
  border: none;
  background: #f3f4f6;
  color: #6b7280;
  font-size: 16px;
  border-radius: 6px;
  cursor: pointer;
  line-height: 1;
}

.sidebar-close-btn:hover {
  background: #e5e7eb;
  color: #111827;
}

/* ====== 会话列表 ====== */
.conv-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.conv-item {
  padding: 12px;
  border-radius: 14px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  transition: background 0.15s, border-color 0.15s, box-shadow 0.15s;
  border: 1px solid transparent;
}

.conv-item:hover {
  background: rgba(255,255,255,0.72);
  border-color: rgba(148,163,184,0.14);
}
.conv-item.active {
  background: linear-gradient(135deg, rgba(238,242,255,0.92), rgba(240,249,255,0.82));
  border: 1px solid rgba(139,92,246,0.18);
  box-shadow: 0 10px 24px rgba(79,70,229,0.08);
}

.conv-main { flex: 1; min-width: 0; }

.conv-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--ai-text);
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

/* ====== 桌面端侧边栏切换按钮 ====== */
.sidebar-toggle {
  position: absolute;
  left: 280px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 10;
  width: 24px;
  height: 48px;
  border: 1px solid rgba(148,163,184,0.20);
  border-left: none;
  border-radius: 0 6px 6px 0;
  background: rgba(255,255,255,0.84);
  color: #4f46e5;
  font-size: 10px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: left 0.3s ease;
}

.sidebar.closed ~ .sidebar-toggle { left: 0; }

/* ====== 移动端顶部栏（桌面端隐藏） ====== */
.mobile-chat-header {
  display: none;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: rgba(255,255,255,0.84);
  border-bottom: 1px solid rgba(148,163,184,0.16);
  backdrop-filter: blur(14px);
  flex-shrink: 0;
}

.mobile-back-btn {
  font-size: 18px;
  color: #374151;
  text-decoration: none;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 6px;
}

.mobile-back-btn:hover {
  background: #f3f4f6;
}

.hamburger-btn {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  width: 32px;
  height: 32px;
  padding: 5px;
  background: none;
  border: none;
  cursor: pointer;
  flex-shrink: 0;
}

.hamburger-btn span {
  display: block;
  width: 18px;
  height: 2px;
  background: #374151;
  border-radius: 2px;
}

.mobile-agent-title {
  flex: 1;
  font-size: 14px;
  font-weight: 600;
  color: var(--ai-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mobile-new-btn {
  width: 32px;
  height: 32px;
  border: 1px dashed rgba(139,92,246,0.26);
  border-radius: 10px;
  background: rgba(238,242,255,0.72);
  color: #4f46e5;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  line-height: 1;
}

.mobile-new-btn:hover {
  border-color: #2563eb;
  color: #2563eb;
}

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

/* ========== 平板 / 手机端 ========== */
@media (max-width: 768px) {
  .agent-chat-layout {
    flex-direction: column;
  }

  /* 遮罩层显示 */
  .sidebar-backdrop {
    display: block;
  }

  /* 侧边栏：固定覆盖层 */
  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 20;
    box-shadow: 2px 0 12px rgba(0, 0, 0, 0.1);
    transform: translateX(0);
    transition: transform 0.3s ease, box-shadow 0.3s ease;
  }

  .sidebar.closed {
    transform: translateX(-100%);
    width: 280px;
    min-width: 280px;
    opacity: 1;
    pointer-events: none;
    box-shadow: none;
  }

  /* 显示关闭按钮 */
  .sidebar-close-btn {
    display: flex;
    align-items: center;
    justify-content: center;
  }

  /* 隐藏桌面端切换按钮 */
  .sidebar-toggle {
    display: none;
  }

  /* 聊天区域占满宽度 */
  .chat-main {
    margin-left: 0;
  }

  /* 显示移动端顶部栏 */
  .mobile-chat-header {
    display: flex;
  }

  /* 隐藏 ChatRoom 内部的 header（移动端顶部栏已替代） */
  .chat-main :deep(.chat-header) {
    display: none;
  }

  /* ChatRoom 在移动端全屏适应 */
  .chat-main :deep(.chat-room) {
    width: 100% !important;
    max-width: 100% !important;
    border-radius: 0 !important;
    margin: 0 !important;
  }
}
</style>

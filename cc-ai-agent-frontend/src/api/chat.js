import request from './request'
import { getAuthHeaders, clearAuth } from '../utils/auth'
import router from '../router'

const BASE_URL = '/api'

/**
 * 处理 401 未授权，跳转登录页
 */
function handleUnauthorized() {
  clearAuth()
  const current = router.currentRoute.value
  if (current.name !== 'Login') {
    router.push({ name: 'Login', query: { redirect: current.fullPath } })
  }
  throw new Error('未登录，请先登录')
}

/**
 * 解析单个 SSE 事件块，提取 event / id / data 字段
 * 支持多行 data（按 SSE 规范，多个 data: 行用 \n 拼接）
 */
function parseSseEvent(block) {
  const event = { data: '', event: '', id: '' }
  const lines = block.split('\n')

  for (const line of lines) {
    if (line.startsWith('data:')) {
      // SSE 规范：data 后的第一个空格应被忽略（如果有的话）
      const value = line.slice(5).replace(/^ ?/, '')
      event.data += (event.data ? '\n' : '') + value
    } else if (line.startsWith('event:')) {
      event.event = line.slice(6).trim()
    } else if (line.startsWith('id:')) {
      event.id = line.slice(3).trim()
    }
    // 以 ':' 开头的是注释行，忽略
    // retry: 字段用于重连间隔，当前场景不需要
  }

  return event
}

/**
 * 解析 SSE 流式响应
 * - 支持标准 SSE 协议（event / id / data 字段、多行 data）
 * - 兼容旧调用签名：parseSseStream(response, onChunk, signal)
 *
 * @param {Response} response - fetch 响应对象
 * @param {Object|Function} options - { onChunk, signal } 或直接传 onChunk 回调
 * @param {AbortSignal} [signal] - 旧签名的第二个参数
 */
async function parseSseStream(response, options, signal) {
  // 兼容旧签名：parseSseStream(response, onChunk, signal)
  let onChunk, _signal
  if (typeof options === 'function') {
    onChunk = options
    _signal = signal
  } else {
    onChunk = options?.onChunk
    _signal = options?.signal
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    if (_signal?.aborted) {
      reader.cancel()
      break
    }

    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const parts = buffer.split('\n\n')
    // 最后一段可能不完整，保留到下次循环
    buffer = parts.pop() || ''

    for (const part of parts) {
      if (!part.trim()) continue
      const sseEvent = parseSseEvent(part)
      if (sseEvent.data && onChunk) {
        onChunk(sseEvent.data, sseEvent)
      }
    }
  }
}

/**
 * AI 选车大师 - SSE 流式对话
 * 对应后端 GET /ai/car/sse
 */
export async function doChatWithCarAppSse(message, chatId, { onChunk, signal } = {}) {
  const params = new URLSearchParams({ message, chatId })
  const response = await fetch(`${BASE_URL}/ai/car/sse?${params}`, {
    signal,
    headers: getAuthHeaders(),
  })

  if (response.status === 401) {
    handleUnauthorized()
  }
  if (!response.ok) {
    throw new Error(`请求失败: ${response.status}`)
  }

  await parseSseStream(response, { onChunk, signal })
}

/**
 * AI 超级智能体 - SSE 流式对话
 * 对应后端 GET /ai/car/manus/chat
 */
export async function doChatWithManus(message, { onChunk, signal } = {}) {
  const params = new URLSearchParams({ message })
  const response = await fetch(`${BASE_URL}/ai/car/manus/chat?${params}`, {
    signal,
    headers: getAuthHeaders(),
  })

  if (response.status === 401) {
    handleUnauthorized()
  }
  if (!response.ok) {
    throw new Error(`请求失败: ${response.status}`)
  }

  await parseSseStream(response, { onChunk, signal })
}

// ============================================================
// 一期：AI Agent Center 接口
// ============================================================

/**
 * 获取可用 Agent 列表
 */
export async function fetchAgents() {
  const res = await request.get('/v1/agents')
  return res.data.data
}

/**
 * 创建会话
 */
export async function createConversation(agentId, title) {
  const res = await request.post('/v1/conversations', { agentId, title })
  return res.data.data
}

/**
 * 获取会话列表
 */
export async function fetchConversations(agentId, page = 1, size = 20) {
  const params = { page, size }
  if (agentId) params.agentId = agentId
  const res = await request.get('/v1/conversations', { params })
  return res.data.data
}

/**
 * 删除会话
 */
export async function deleteConversation(id) {
  await request.delete(`/v1/conversations/${id}`)
}

/**
 * 获取会话历史消息
 */
export async function fetchMessages(conversationId, page = 1, size = 50) {
  const res = await request.get(`/v1/conversations/${conversationId}/messages`, { params: { page, size } })
  return res.data.data
}

/**
 * 基于会话的 SSE 流式对话
 */
export async function doChatWithConversationStream(conversationId, message, { onChunk, signal } = {}) {
  const response = await fetch(`${BASE_URL}/v1/conversations/${conversationId}/chat/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...getAuthHeaders(),
    },
    body: JSON.stringify({ message }),
    signal,
  })

  if (response.status === 401) {
    handleUnauthorized()
  }
  if (!response.ok) {
    throw new Error(`请求失败: ${response.status}`)
  }

  await parseSseStream(response, { onChunk, signal })
}

// ============================================================
// 二期：消息持久化 + 反馈 + 重命名
// ============================================================

/** 保存消息（AI 回复完成后由前端调用） */
export async function saveMessage(conversationId, role, content) {
  await request.post(`/v1/conversations/${conversationId}/messages`, { role, content })
}

/** 消息反馈 */
export async function updateFeedback(conversationId, msgId, feedback) {
  await request.put(`/v1/conversations/${conversationId}/messages/${msgId}/feedback`, { feedback })
}

/** 重命名会话 */
export async function renameConversation(id, title) {
  await request.put(`/v1/conversations/${id}`, { title })
}

export { request }

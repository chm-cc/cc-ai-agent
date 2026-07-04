import request from './request'

const BASE_URL = '/api'

/**
 * 解析 SSE 流式响应，逐段回调 onChunk
 */
async function parseSseStream(response, onChunk, signal) {
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    if (signal?.aborted) {
      reader.cancel()
      break
    }

    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const parts = buffer.split('\n\n')
    buffer = parts.pop() || ''

    for (const part of parts) {
      const lines = part.split('\n')
      for (const line of lines) {
        if (line.startsWith('data:')) {
          const data = line.slice(5).trimStart()
          if (data) onChunk(data)
        }
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
  const response = await fetch(`${BASE_URL}/ai/car/sse?${params}`, { signal })

  if (!response.ok) {
    throw new Error(`请求失败: ${response.status}`)
  }

  await parseSseStream(response, onChunk, signal)
}

/**
 * AI 超级智能体 - SSE 流式对话
 * 对应后端 GET /ai/car/manus/chat
 */
export async function doChatWithManus(message, { onChunk, signal } = {}) {
  const params = new URLSearchParams({ message })
  const response = await fetch(`${BASE_URL}/ai/car/manus/chat?${params}`, { signal })

  if (!response.ok) {
    throw new Error(`请求失败: ${response.status}`)
  }

  await parseSseStream(response, onChunk, signal)
}

export { request }

import { ref, onUnmounted } from 'vue'

/**
 * 聊天消息流管理 composable
 *
 * 封装了消息数组、SSE 流式调用、中断处理、状态转换的完整生命周期。
 *
 * @param {Function} streamFn - SSE 调用函数
 *   签名: (message, { signal, onChunk }) => Promise<void>
 *   - message: 用户输入的文本
 *   - signal: AbortSignal，用于取消请求
 *   - onChunk: (text: string) => void，收到一段流式文本时回调
 * @param {Object} [opts] - 额外选项
 * @param {Function} [opts.onDone] - SSE 流完成时的回调 ({ content, thoughts, role }) => void
 *
 * @returns {{ messages, loading, send, retry, abort, clear }}
 *   - messages: Ref<Message[]>  消息数组，传给 ChatRoom 的 v-model:messages
 *   - loading:  Ref<boolean>    是否正在等待 AI 回复
 *   - send:     (text) => void  发送消息
 *   - retry:    () => void      重试最后一条失败的消息
 *   - abort:    () => void      取消当前请求
 *   - clear:    () => void      清空消息
 *
 * 消息模型:
 *   { role: 'user'|'assistant', content: string, status?: 'sending'|'streaming'|'done'|'error', error?: string }
 *   - user 消息无 status 字段
 *   - assistant 消息状态流转: sending → streaming → done | error
 */
export function useChat(streamFn, opts = {}) {
  const messages = ref([])
  const loading = ref(false)
  const lastUserMessage = ref('')
  const onDone = opts.onDone
  let abortController = null

  onUnmounted(() => {
    abortController?.abort()
  })

  /**
   * 启动一次流式请求（内部方法）
   * - 推送 AI 占位消息（status: 'sending'）
   * - 调用 streamFn
   * - 在 onChunk 中追加内容和更新状态
   * - 处理成功/失败/中断
   */
  async function startStream(userMessage) {
    // 1. 创建 AI 回复占位消息
    const aiMsg = { role: 'assistant', content: '', thoughts: '', status: 'sending' }
    messages.value = [...messages.value, aiMsg]
    const aiIndex = messages.value.length - 1

    // 2. 启动加载状态，中断旧请求
    loading.value = true
    abortController?.abort()
    abortController = new AbortController()

    try {
      // 3. 调用 SSE API
      await streamFn(userMessage, {
        signal: abortController.signal,
        onChunk: (chunk, sseEvent) => {
          // 首次收到数据时切换为 streaming 状态
          if (messages.value[aiIndex].status === 'sending') {
            messages.value[aiIndex].status = 'streaming'
          }
          // 根据 SSE event 类型分流：thinking → 思考过程，answer → 最终回答
          const eventType = sseEvent?.event || 'answer'
          if (eventType === 'thinking') {
            messages.value[aiIndex].thoughts = (messages.value[aiIndex].thoughts || '') + chunk
          } else {
            messages.value[aiIndex].content += chunk
          }
        },
      })
    } catch (err) {
      // 4. 错误处理：区分主动中断和真实错误
      if (err.name !== 'AbortError') {
        messages.value[aiIndex].status = 'error'
        messages.value[aiIndex].error = err.message
        if (!messages.value[aiIndex].content) {
          messages.value[aiIndex].content = `请求出错: ${err.message}`
        }
      }
      // AbortError 不改变状态，消息保持 sending/streaming 后被 finally 处理
    } finally {
      loading.value = false
    }

    // 5. 非错误状态 → 完成锁定
    if (messages.value[aiIndex].status !== 'error') {
      messages.value[aiIndex].status = 'done'
      // 通知外部：AI 回复完成（即使 content 为空也触发，CcManus 可能只输出 thinking）
      if (onDone) {
        onDone({
          content: messages.value[aiIndex].content,
          thoughts: messages.value[aiIndex].thoughts,
          role: 'ASSISTANT',
        })
      }
    }
  }

  /**
   * 发送一条用户消息
   * @param {string} text - 用户输入的文本
   */
  async function send(text) {
    if (!text?.trim() || loading.value) return

    const trimmed = text.trim()
    lastUserMessage.value = trimmed

    // 推送用户消息（无 status，视为已完成）
    messages.value = [...messages.value, { role: 'user', content: trimmed }]

    // 启动 AI 流式回复
    await startStream(trimmed)
  }

  /**
   * 重试最后一条失败的消息
   * 移除失败的 AI 回复，用相同的用户消息重新发起请求
   */
  async function retry() {
    if (!lastUserMessage.value || loading.value) return

    // 移除最后一条 AI 消息（失败的占位）
    if (messages.value.length > 0) {
      const last = messages.value[messages.value.length - 1]
      if (last.role === 'assistant' && last.status === 'error') {
        messages.value = messages.value.slice(0, -1)
      }
    }

    await startStream(lastUserMessage.value)
  }

  /** 取消当前正在进行的请求 */
  function abort() {
    abortController?.abort()
  }

  /** 清空所有消息 */
  function clear() {
    messages.value = []
  }

  return { messages, loading, send, retry, abort, clear }
}

<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'

/**
 * 打字机效果文本组件
 *
 * 将传入的 text 逐字输出，支持流式追加（SSE chunks 不断到达时自动继续打字）。
 * 通过 burst 机制动态调整每帧输出字符数，保证不落后于数据到达速度。
 *
 * Props:
 *   text   - 目标完整文本（可随时间增长）
 *   active - 是否启用打字机动画，false 时直接展示完整文本
 *   speed  - 每帧间隔（ms），默认 20
 */

const props = defineProps({
  text: { type: String, default: '' },
  active: { type: Boolean, default: false },
  speed: { type: Number, default: 20 },
})

const display = ref('')
let timer = null

/**
 * 打字主循环：
 * - 根据落后字符数动态决定每帧输出字符数（burst），避免堆积
 * - 目标文本还在增长时持续轮询等待新内容
 */
function tick() {
  clearTimeout(timer)

  const currentLen = display.value.length
  const targetLen = props.text.length

  // 已追上目标文本
  if (currentLen >= targetLen) {
    if (props.active) {
      // SSE 可能还在传输，稍后重试
      timer = setTimeout(tick, 50)
    }
    return
  }

  // burst 机制：落后越多，每帧输出越多
  const behind = targetLen - currentLen
  let chars = 1
  if (behind > 80) chars = 8
  else if (behind > 40) chars = 4
  else if (behind > 10) chars = 2

  display.value = props.text.slice(0, currentLen + chars)
  timer = setTimeout(tick, props.speed)
}

// text 增长时触发/继续打字
watch(
  () => props.text,
  () => {
    if (props.active && display.value.length < props.text.length) {
      tick()
    }
  },
)

// active 切换
watch(
  () => props.active,
  (val) => {
    if (!val) {
      clearTimeout(timer)
      display.value = props.text // 立即展示完整文本
    } else {
      tick()
    }
  },
  { immediate: true },
)

onBeforeUnmount(() => clearTimeout(timer))
</script>

<template>
  <span>{{ active ? display : text }}</span>
</template>

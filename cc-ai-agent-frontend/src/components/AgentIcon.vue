<script setup>
import { computed } from 'vue'
import {
  // 通用
  Home, Setting, User, More, AppSwitch, ApplicationMenu, Link, Power,
  // AI & 智能
  Robot, Brain, Chip, Cpu, Voice, VoiceMessage, Like, Tips, Light, Key,
  // 办公 & 效率
  DocDetail, Calendar, Mail, Edit, Search, ChartLine, ChartPie, Analysis, FileText, Folder,
  // 技术 & 工具
  Code, Terminal, Tool, HammerAndAnvil, ApiApp, Data, SettingConfig, Undo, Protection,
  // 行业 & 场景
  ShoppingCart, Car, Airplane, Bank, Hospital, School, MusicList, Camera, Globe,
  // 经典
  Star, Fire, Trophy, Diamond, Gift, TeaDrink, ThumbsUp, SmilingFace, Heart, Success,
} from '@icon-park/vue-next'

// 所有已注册图标名称映射
const iconMap = {
  // 通用
  'home': Home, 'setting': Setting, 'user': User, 'more': More, 'apps': AppSwitch,
  'menu': ApplicationMenu, 'link': Link, 'power': Power,
  // AI & 智能
  'robot': Robot, 'brain': Brain, 'chip': Chip, 'cpu': Cpu, 'voice': Voice,
  'voice-message': VoiceMessage, 'like': Like, 'tips': Tips, 'lightbulb': Light,
  'key': Key,
  // 办公 & 效率
  'document': DocDetail, 'calendar': Calendar, 'mail': Mail, 'edit': Edit,
  'search': Search, 'chart-line': ChartLine, 'chart-pie': ChartPie,
  'analysis': Analysis, 'file-text': FileText, 'folder': Folder,
  // 技术 & 工具
  'code': Code, 'terminal': Terminal, 'tool': Tool, 'hammer': HammerAndAnvil,
  'api': ApiApp, 'data': Data, 'config': SettingConfig, 'undo': Undo,
  'protection': Protection,
  // 行业 & 场景
  'shopping-cart': ShoppingCart, 'car': Car, 'airplane': Airplane, 'bank': Bank,
  'hospital': Hospital, 'school': School, 'music-list': MusicList, 'camera': Camera,
  'globe': Globe,
  // 经典
  'star': Star, 'fire': Fire, 'trophy': Trophy, 'diamond': Diamond, 'gift': Gift,
  'coffee': TeaDrink, 'like-outlined': ThumbsUp, 'smiling-face': SmilingFace,
  'heart': Heart, 'success': Success,
}

const props = defineProps({
  icon: { type: String, default: 'robot' },
  size: { type: [Number, String], default: 24 },
  theme: { type: String, default: 'outline' }, // outline | filled | two-tone | multi-color
  fill: { type: String, default: 'currentColor' },
})

// 判断是否为 emoji（单个或双字符的 Unicode 表情）
const isEmoji = computed(() => {
  if (!props.icon) return false
  const trimmed = props.icon.trim()
  // emoji 通常是 1-4 个字符，且包含非 ASCII 字符
  if (trimmed.length <= 4) {
    for (const ch of trimmed) {
      if (ch.charCodeAt(0) > 127) return true
    }
  }
  return false
})

const iconComponent = computed(() => {
  return iconMap[props.icon] || iconMap['robot']
})
</script>

<template>
  <span v-if="isEmoji" class="agent-emoji" :style="{ fontSize: typeof size === 'number' ? size + 'px' : size }">
    {{ icon }}
  </span>
  <component
    v-else
    :is="iconComponent"
    :size="size"
    :theme="theme"
    :fill="fill"
    class="agent-icon"
  />
</template>

<style scoped>
.agent-emoji {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  flex-shrink: 0;
}
.agent-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
</style>

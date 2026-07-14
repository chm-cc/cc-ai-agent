<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { fetchAgents } from '../api/chat'
import AgentIcon from '../components/AgentIcon.vue'

const router = useRouter()
const agents = ref([])
const loading = ref(true)
const error = ref('')
const activeCategory = ref('all')

const categoryFilters = [
  { value: 'all', label: '全部' },
  { value: 'advisor', label: '顾问型' },
  { value: 'productivity', label: '生产力' },
]

const filteredAgents = computed(() => {
  if (activeCategory.value === 'all') return agents.value
  return agents.value.filter(agent => agent.category === activeCategory.value)
})

onMounted(async () => {
  try {
    agents.value = await fetchAgents()
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
})

function cardClass(agent) {
  if (agent.id === 'super-agent') return 'card-manus'
  if (agent.id === 'car-advisor') return 'card-advisor'
  if (isTravelAgent(agent)) return 'card-travel'
  if (agent.category === 'productivity') return 'card-manus'
  if (agent.category === 'advisor') return 'card-advisor'
  return 'card-general'
}

function isTravelAgent(agent) {
  const id = String(agent.id || '').toLowerCase()
  return id.includes('travel') || id.includes('trip') || id.includes('tour') || agent.name?.includes('旅游')
}

function goChat(agent) {
  router.push({ name: 'AgentChat', params: { agentId: agent.id }, query: { _new: '1' } })
}

function switchCategory(category) {
  activeCategory.value = category
}
</script>

<template>
  <main class="home">
    <header class="hero">
      <div class="hero-badge">✨ AI-Powered</div>
      <h1 class="hero-title">AI Agent 应用中心</h1>
      <p class="hero-desc">选择下方智能体，开始你的 AI 对话体验</p>
      <div class="hero-line"></div>
    </header>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else-if="agents.length === 0" class="empty">暂无可用的智能体</div>

    <template v-else>
      <div class="filter-bar" aria-label="智能体分类筛选">
        <button
          v-for="filter in categoryFilters"
          :key="filter.value"
          type="button"
          class="filter-btn"
          :class="{ active: activeCategory === filter.value }"
          @click="switchCategory(filter.value)"
        >
          {{ filter.label }}
        </button>
      </div>

      <div v-if="filteredAgents.length === 0" class="empty">该分类暂无可用的智能体</div>

      <section v-else class="app-grid" aria-label="应用列表">
      <div
        v-for="agent in filteredAgents"
        :key="agent.id"
        class="app-card"
        :class="cardClass(agent)"
        @click="goChat(agent)"
      >
        <div class="card-glow"></div>
        <div class="card-icon" :class="agent.category === 'productivity' ? 'manus-icon' : 'car-icon'" aria-hidden="true">
          <AgentIcon :icon="agent.icon || 'robot'" :size="32" theme="outline" />
        </div>
        <h2 class="card-title">{{ agent.name }}</h2>
        <p class="card-desc">{{ agent.description }}</p>
        <div class="card-tags">
          <span v-for="tag in agent.tags" :key="tag" class="tag">{{ tag }}</span>
        </div>
        <span class="card-action">
          进入体验
          <span class="arrow">→</span>
        </span>
      </div>
      </section>
    </template>
  </main>
</template>

<style scoped>
.home {
  flex: 1;
  padding: 80px 24px 48px;
  max-width: 1000px;
  margin: 0 auto;
  width: 100%;
}

.loading, .error, .empty {
  text-align: center;
  padding: 60px 20px;
  color: #6b7280;
  font-size: 15px;
}

.error { color: #dc2626; }

.hero {
  text-align: center;
  margin-bottom: 56px;
}

.hero-badge {
  display: inline-block;
  padding: 5px 16px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 1px;
  color: #7c3aed;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.08), rgba(59, 130, 246, 0.06));
  border: 1px solid rgba(139, 92, 246, 0.12);
  border-radius: 20px;
  margin-bottom: 20px;
}

.hero-title {
  font-size: 40px;
  font-weight: 800;
  letter-spacing: -1px;
  background: linear-gradient(135deg, #1a1a2e 0%, #374151 50%, #1a1a2e 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 16px;
}

.hero-desc {
  font-size: 17px;
  color: #6b7280;
  margin-bottom: 24px;
}

.hero-line {
  width: 48px;
  height: 3px;
  background: linear-gradient(90deg, #2563eb, #7c3aed);
  border-radius: 2px;
  margin: 0 auto;
}

.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(360px, 1fr));
  gap: 28px;
}

.filter-bar {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin: -20px auto 34px;
  padding: 8px;
  width: fit-content;
  max-width: 100%;
  border: 1px solid rgba(148,163,184,0.18);
  border-radius: 999px;
  background: rgba(255,255,255,0.66);
  box-shadow: var(--ai-shadow-sm);
  backdrop-filter: blur(14px);
}

.filter-btn {
  min-width: 88px;
  padding: 9px 18px;
  border-radius: 999px;
  color: var(--ai-muted);
  background: transparent;
  border: 1px solid transparent;
  font-size: 14px;
  font-weight: 700;
  transition: transform 0.18s, color 0.18s, background 0.18s, box-shadow 0.18s, border-color 0.18s;
}

.filter-btn:hover {
  color: var(--ai-text);
  background: rgba(255,255,255,0.78);
  border-color: rgba(148,163,184,0.18);
}

.filter-btn.active {
  color: #fff;
  background: linear-gradient(135deg, #7c3aed, #3b82f6);
  border-color: rgba(139,92,246,0.18);
  box-shadow: 0 12px 28px rgba(79,70,229,0.18);
  transform: translateY(-1px);
}

.app-card {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 32px;
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.app-card:hover {
  transform: translateY(-6px);
}

.app-card:hover .arrow {
  transform: translateX(4px);
}

.card-glow {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  pointer-events: none;
  z-index: 0;
  transition: opacity 0.3s;
}

.card-car {
  background: #fff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.card-car:hover {
  box-shadow: 0 8px 30px rgba(37,99,235,0.1), 0 0 0 1px rgba(37,99,235,0.2);
}

.card-car .card-glow {
  background: radial-gradient(ellipse at 50% 0%, rgba(37,99,235,0.04) 0%, transparent 60%);
}

.card-car .card-title { color: #0f172a; }
.card-car .card-desc { color: #64748b; }
.card-car .card-action { color: #2563eb; }

.card-car .tag {
  background: #eff6ff;
  color: #2563eb;
  border: 1px solid #dbeafe;
  font-family: 'SF Mono', 'Fira Code', 'Cascadia Code', monospace;
  font-size: 11px;
}

.car-icon {
  background: #eff6ff;
  border: 1px solid #dbeafe;
}

.card-manus {
  background: linear-gradient(160deg, #faf5ff 0%, #f3e8ff 40%, #e8f0fe 100%);
  border: 1px solid rgba(139,92,246,0.12);
  box-shadow: 0 4px 24px rgba(139,92,246,0.06);
}

.card-manus:hover {
  box-shadow: 0 12px 40px rgba(139,92,246,0.12), 0 0 0 1px rgba(139,92,246,0.25);
}

.card-manus .card-glow {
  background: radial-gradient(ellipse at 50% 0%, rgba(139,92,246,0.06) 0%, transparent 60%);
}

.card-manus .card-title { color: #5b21b6; }
.card-manus .card-desc { color: #786e92; }
.card-manus .card-action { color: #7c3aed; }

.card-manus .tag {
  background: rgba(139,92,246,0.06);
  color: #7c3aed;
  border: 1px solid rgba(139,92,246,0.1);
}

.manus-icon {
  background: linear-gradient(135deg, rgba(139,92,246,0.1), rgba(99,102,241,0.06));
  border: 1px solid rgba(139,92,246,0.18);
}

.card-icon {
  position: relative;
  z-index: 1;
  width: 60px;
  height: 60px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  margin-bottom: 20px;
}

.card-title {
  position: relative;
  z-index: 1;
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 10px;
}

.card-desc {
  position: relative;
  z-index: 1;
  flex: 1;
  font-size: 14px;
  line-height: 1.7;
  margin-bottom: 16px;
}

.card-tags {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.tag {
  font-size: 11px;
  font-weight: 500;
  padding: 3px 10px;
  border-radius: 6px;
}

.card-action {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
}

.arrow {
  display: inline-block;
  transition: transform 0.25s ease;
}

/* Premium AI platform skin */
.loading, .error, .empty { color: var(--ai-muted); }

.hero-badge {
  padding: 6px 16px;
  letter-spacing: 0.7px;
  color: #6250d8;
  background: linear-gradient(135deg, rgba(255,255,255,0.86), rgba(238,242,255,0.72));
  border-color: rgba(139, 92, 246, 0.18);
  box-shadow: 0 10px 28px rgba(109, 93, 252, 0.08);
  backdrop-filter: blur(14px);
}

.hero-title {
  letter-spacing: -0.8px;
  background: linear-gradient(135deg, #15172f 0%, #4f46e5 46%, #0f172a 100%);
  -webkit-background-clip: text;
  background-clip: text;
}

.hero-desc { color: var(--ai-muted); }

.hero-line {
  width: 58px;
  background: linear-gradient(90deg, #7dd3fc, #8b5cf6, #f0abfc);
  box-shadow: 0 8px 22px rgba(139,92,246,0.24);
}

.app-card {
  min-height: 292px;
  border-radius: 24px;
  border: 1px solid var(--card-border);
  background: var(--card-bg);
  box-shadow: var(--ai-shadow-sm);
  isolation: isolate;
  transition: transform 0.3s ease, box-shadow 0.3s ease, border-color 0.3s ease;
}

.app-card:hover {
  box-shadow: var(--card-shadow);
  border-color: var(--card-border-hover);
}

.card-glow {
  inset: 0;
  background:
    radial-gradient(ellipse at 18% 0%, var(--card-glow) 0%, transparent 52%),
    linear-gradient(135deg, rgba(255,255,255,0.30), transparent 42%);
  opacity: 0.95;
}

.card-manus {
  --card-bg: linear-gradient(155deg, #fbf7ff 0%, #f3e8ff 42%, #e8f1ff 100%);
  --card-border: rgba(139,92,246,0.16);
  --card-border-hover: rgba(139,92,246,0.30);
  --card-glow: rgba(139,92,246,0.15);
  --card-accent: #7c3aed;
  --card-title: #4c1d95;
  --card-desc: #74658d;
  --card-tag-bg: rgba(139,92,246,0.08);
  --card-tag-border: rgba(139,92,246,0.14);
  --card-icon-bg: linear-gradient(135deg, rgba(139,92,246,0.14), rgba(99,102,241,0.08));
  --card-shadow: 0 18px 52px rgba(124,58,237,0.16), 0 0 0 1px rgba(139,92,246,0.10);
}

.card-advisor {
  --card-bg: linear-gradient(155deg, #f2fbff 0%, #e6f7fb 45%, #edf6ff 100%);
  --card-border: rgba(14,165,233,0.16);
  --card-border-hover: rgba(14,165,233,0.30);
  --card-glow: rgba(14,165,233,0.14);
  --card-accent: #0284c7;
  --card-title: #075985;
  --card-desc: #5e7888;
  --card-tag-bg: rgba(14,165,233,0.08);
  --card-tag-border: rgba(14,165,233,0.15);
  --card-icon-bg: linear-gradient(135deg, rgba(14,165,233,0.13), rgba(45,212,191,0.08));
  --card-shadow: 0 18px 52px rgba(14,165,233,0.14), 0 0 0 1px rgba(14,165,233,0.10);
}

.card-general,
.card-car {
  --card-bg: linear-gradient(155deg, #fffdf7 0%, #fff7ed 46%, #f2fbf8 100%);
  --card-border: rgba(245,158,11,0.16);
  --card-border-hover: rgba(245,158,11,0.30);
  --card-glow: rgba(245,158,11,0.13);
  --card-accent: #d97706;
  --card-title: #7c3f09;
  --card-desc: #7d705e;
  --card-tag-bg: rgba(245,158,11,0.08);
  --card-tag-border: rgba(245,158,11,0.15);
  --card-icon-bg: linear-gradient(135deg, rgba(245,158,11,0.13), rgba(16,185,129,0.08));
  --card-shadow: 0 18px 52px rgba(245,158,11,0.13), 0 0 0 1px rgba(245,158,11,0.10);
}

.card-travel {
  --card-bg: linear-gradient(155deg, #fff7fb 0%, #ffeaf0 44%, #fff6e8 100%);
  --card-border: rgba(244,114,182,0.17);
  --card-border-hover: rgba(244,114,182,0.32);
  --card-glow: rgba(244,114,182,0.14);
  --card-accent: #db2777;
  --card-title: #9d174d;
  --card-desc: #846070;
  --card-tag-bg: rgba(244,114,182,0.08);
  --card-tag-border: rgba(244,114,182,0.16);
  --card-icon-bg: linear-gradient(135deg, rgba(244,114,182,0.13), rgba(251,191,36,0.10));
  --card-shadow: 0 18px 52px rgba(244,114,182,0.14), 0 0 0 1px rgba(244,114,182,0.10);
}

.card-icon,
.car-icon,
.manus-icon {
  background: var(--card-icon-bg);
  border: 1px solid var(--card-border-hover);
  border-radius: 18px;
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.55), 0 12px 26px rgba(30,41,59,0.07);
}

.card-title {
  color: var(--card-title);
  font-weight: 760;
  letter-spacing: -0.2px;
}

.card-desc { color: var(--card-desc); }

.tag {
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--card-tag-bg);
  color: var(--card-accent);
  border: 1px solid var(--card-tag-border);
  font-weight: 600;
  backdrop-filter: blur(10px);
}

.card-action {
  color: var(--card-accent);
  font-weight: 700;
}

@media (max-width: 768px) {
  .home { padding: 48px 20px 32px; }
  .hero-title { font-size: 30px; }
  .hero-desc { font-size: 15px; }
  .filter-bar { margin: -18px auto 28px; }
  .app-grid { grid-template-columns: 1fr; max-width: 480px; margin: 0 auto; }
  .app-card { padding: 24px; }
}

@media (max-width: 480px) {
  .hero-title { font-size: 26px; }
  .filter-bar {
    width: 100%;
    justify-content: stretch;
    border-radius: 18px;
  }
  .filter-btn {
    flex: 1;
    min-width: 0;
    padding: 9px 10px;
    font-size: 13px;
  }
  .app-card { padding: 20px; border-radius: 16px; }
  .card-icon { width: 48px; height: 48px; font-size: 24px; border-radius: 12px; }
  .card-title { font-size: 18px; }
}
</style>

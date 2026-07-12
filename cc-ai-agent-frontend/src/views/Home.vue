<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { fetchAgents } from '../api/chat'

const router = useRouter()
const agents = ref([])
const loading = ref(true)
const error = ref('')

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
  if (agent.category === 'productivity') return 'card-manus'
  return 'card-car'
}

function goChat(agent) {
  router.push({ name: 'AgentChat', params: { agentId: agent.id }, query: { _new: '1' } })
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

    <section v-else class="app-grid" aria-label="应用列表">
      <div
        v-for="agent in agents"
        :key="agent.id"
        class="app-card"
        :class="cardClass(agent)"
        @click="goChat(agent)"
      >
        <div class="card-glow"></div>
        <div class="card-icon" :class="agent.category === 'productivity' ? 'manus-icon' : 'car-icon'" aria-hidden="true">
          {{ agent.icon || '🤖' }}
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

@media (max-width: 768px) {
  .home { padding: 48px 20px 32px; }
  .hero-title { font-size: 30px; }
  .hero-desc { font-size: 15px; }
  .app-grid { grid-template-columns: 1fr; max-width: 480px; margin: 0 auto; }
  .app-card { padding: 24px; }
}

@media (max-width: 480px) {
  .hero-title { font-size: 26px; }
  .app-card { padding: 20px; border-radius: 16px; }
  .card-icon { width: 48px; height: 48px; font-size: 24px; border-radius: 12px; }
  .card-title { font-size: 18px; }
}
</style>

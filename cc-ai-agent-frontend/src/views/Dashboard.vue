<script setup>
import { ref, computed, onMounted } from 'vue'
import { fetchStatistics } from '../api/dashboard'
import StatCard from '../components/StatCard.vue'

const days = ref(30)
const loading = ref(true)
const error = ref('')
const stats = ref(null)

const dayOptions = [
  { label: '近7天', value: 7 },
  { label: '近30天', value: 30 },
  { label: '近90天', value: 90 },
]

async function load() {
  loading.value = true
  error.value = ''
  try {
    stats.value = await fetchStatistics(days.value)
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function switchDays(d) {
  days.value = d
  load()
}

const maxTrendValue = computed(() => {
  if (!stats.value?.dailyTrend?.length) return 1
  return Math.max(
    ...stats.value.dailyTrend.map(d => Math.max(d.conversations, d.messages)),
    1
  )
})

function barHeight(val) {
  return Math.max(2, Math.round((val / maxTrendValue.value) * 100))
}

function dayLabel(dateStr) {
  if (!dateStr) return ''
  return dateStr.slice(5) // MM-DD
}

onMounted(load)
</script>

<template>
  <main class="dashboard">
    <header class="dash-header">
      <div class="dash-title-row">
        <h1 class="dash-title">📊 用量统计看板</h1>
        <div class="day-tabs">
          <button
            v-for="opt in dayOptions"
            :key="opt.value"
            class="day-tab"
            :class="{ active: days === opt.value }"
            @click="switchDays(opt.value)"
          >{{ opt.label }}</button>
        </div>
      </div>
    </header>

    <div v-if="loading" class="dash-loading">加载中...</div>
    <div v-else-if="error" class="dash-error">{{ error }}</div>

    <template v-else-if="stats">
      <!-- 总览卡片 -->
      <section class="overview-grid">
        <StatCard
          label="总会话数"
          :value="stats.overview.totalConversations"
          icon="💬"
          color="#2563eb"
        />
        <StatCard
          label="总消息数"
          :value="stats.overview.totalMessages"
          icon="📝"
          color="#7c3aed"
        />
        <StatCard
          label="今日新增会话"
          :value="stats.overview.todayConversations"
          icon="🆕"
          color="#059669"
        />
        <StatCard
          label="今日新增消息"
          :value="stats.overview.todayMessages"
          icon="⚡"
          color="#d97706"
        />
      </section>

      <!-- 每日趋势图 -->
      <section class="section">
        <h2 class="section-title">每日对话趋势</h2>
        <div class="chart-wrap">
          <div class="chart-legend">
            <span class="legend-item"><i class="legend-dot conv-dot"></i>新增会话</span>
            <span class="legend-item"><i class="legend-dot msg-dot"></i>新增消息</span>
          </div>
          <div class="bar-chart" v-if="stats.dailyTrend.length">
            <div
              v-for="(d, i) in stats.dailyTrend"
              :key="d.date"
              class="bar-col"
              :title="`${d.date}: ${d.conversations}会话 ${d.messages}消息`"
            >
              <div class="bar-stack">
                <div
                  class="bar bar-msg"
                  :style="{ height: barHeight(d.messages) + '%' }"
                ></div>
                <div
                  class="bar bar-conv"
                  :style="{ height: barHeight(d.conversations) + '%' }"
                ></div>
              </div>
              <span class="bar-day" v-if="i % Math.ceil(stats.dailyTrend.length / 12) === 0">
                {{ dayLabel(d.date) }}
              </span>
            </div>
          </div>
          <div v-else class="chart-empty">暂无数据</div>
        </div>
      </section>

      <!-- Agent 分解 -->
      <section class="section">
        <h2 class="section-title">各 Agent 用量</h2>
        <div class="agent-table-wrap" v-if="stats.agentBreakdown.length">
          <table class="agent-table">
            <thead>
              <tr>
                <th>Agent</th>
                <th class="num">会话数</th>
                <th class="num">消息数</th>
                <th class="num">平均消息/会话</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="ab in stats.agentBreakdown" :key="ab.agentId">
                <td class="agent-name-cell">{{ ab.agentName }}</td>
                <td class="num">{{ ab.conversations.toLocaleString() }}</td>
                <td class="num">{{ ab.messages.toLocaleString() }}</td>
                <td class="num">{{ ab.avgPerConv }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="chart-empty">暂无数据</div>
      </section>
    </template>
  </main>
</template>

<style scoped>
.dashboard {
  flex: 1;
  padding: 48px 24px 48px;
  max-width: 1000px;
  margin: 0 auto;
  width: 100%;
}

.dash-header {
  margin-bottom: 32px;
}

.dash-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
}

.dash-title {
  font-size: 28px;
  font-weight: 700;
  color: #111827;
}

.dash-loading, .dash-error {
  text-align: center;
  padding: 60px 20px;
  color: #6b7280;
  font-size: 15px;
}
.dash-error { color: #dc2626; }

/* ====== 时间按钮组 ====== */
.day-tabs {
  display: flex;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
}

.day-tab {
  padding: 7px 16px;
  font-size: 13px;
  font-weight: 500;
  color: #6b7280;
  background: #fff;
  border: none;
  border-right: 1px solid #e5e7eb;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.day-tab:last-child { border-right: none; }

.day-tab:hover { background: #f9fafb; }

.day-tab.active {
  background: #2563eb;
  color: #fff;
}

/* ====== 总览卡片 ====== */
.overview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 36px;
}

/* ====== 区块 ====== */
.section {
  margin-bottom: 36px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 16px;
}

/* ====== 图表 ====== */
.chart-wrap {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 24px 16px 16px;
}

.chart-legend {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
  font-size: 12px;
  color: #6b7280;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  display: inline-block;
}

.conv-dot { background: #2563eb; }
.msg-dot { background: #7c3aed; }

.bar-chart {
  display: flex;
  align-items: flex-end;
  gap: 2px;
  height: 200px;
  padding-bottom: 20px;
}

.bar-col {
  flex: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  position: relative;
  min-width: 0;
}

.bar-stack {
  width: 100%;
  max-width: 24px;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  gap: 1px;
}

.bar {
  width: 100%;
  border-radius: 2px 2px 0 0;
  transition: height 0.3s ease;
  min-height: 0;
}

.bar-conv { background: #2563eb; }
.bar-msg { background: #7c3aed; }

.bar-day {
  position: absolute;
  bottom: -18px;
  font-size: 10px;
  color: #9ca3af;
  white-space: nowrap;
}

.chart-empty {
  text-align: center;
  padding: 40px;
  color: #9ca3af;
  font-size: 13px;
}

/* ====== Agent 表格 ====== */
.agent-table-wrap {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  overflow: hidden;
}

.agent-table {
  width: 100%;
  border-collapse: collapse;
}

.agent-table th {
  padding: 12px 16px;
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  text-align: left;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
}

.agent-table th.num { text-align: right; }

.agent-table td {
  padding: 14px 16px;
  font-size: 14px;
  color: #374151;
  border-bottom: 1px solid #f3f4f6;
}

.agent-table tr:last-child td { border-bottom: none; }

.agent-table td.num {
  text-align: right;
  font-variant-numeric: tabular-nums;
  font-family: 'SF Mono', 'Fira Code', 'Cascadia Code', monospace;
}

.agent-name-cell { font-weight: 500; }

@media (max-width: 768px) {
  .dashboard { padding: 32px 16px 24px; }
  .dash-title { font-size: 22px; }
  .overview-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>

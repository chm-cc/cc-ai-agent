<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { fetchAllAgents, createAgent, updateAgent, deleteAgent } from '../api/agent'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import AgentIcon from '../components/AgentIcon.vue'

const agents = ref([])
const loading = ref(true)
const error = ref('')
const editMode = ref(false) // false=新建, true=编辑
const editId = ref(null)

// 编辑弹窗
const showForm = ref(false)
const saving = ref(false)

const defaultForm = () => ({
  id: '',
  name: '',
  description: '',
  icon: 'robot',
  category: 'general',
  tags: [],
  systemPrompt: '',
  model: '',
  temperature: 0.7,
  maxTokens: 2000,
  tools: [],
  status: 'ACTIVE',
  sortOrder: 0,
})

const form = reactive(defaultForm())

const tagInput = ref('')

// 删除确认
const deleteDialog = ref({
  visible: false,
  agentId: null,
  agentName: '',
  loading: false,
})

// 可选工具列表
const availableTools = [
  'WebSearch', 'WebScraping', 'PDFGeneration',
  'FileOperation', 'TerminalOperation', 'ResourceDownload', 'Terminate',
  'Weather',
]

// 图标选项（使用 IconPark 图标库）
const iconCategories = [
  {
    name: 'AI & 智能',
    icons: ['robot', 'brain', 'chip', 'cpu', 'voice', 'voice-message', 'like', 'tips', 'lightbulb', 'key'],
  },
  {
    name: '办公 & 效率',
    icons: ['document', 'calendar', 'mail', 'edit', 'search', 'chart-line', 'chart-pie', 'analysis', 'file-text', 'folder'],
  },
  {
    name: '技术 & 工具',
    icons: ['code', 'terminal', 'tool', 'hammer', 'api', 'data', 'config', 'undo', 'protection', 'setting'],
  },
  {
    name: '行业 & 场景',
    icons: ['shopping-cart', 'car', 'airplane', 'bank', 'hospital', 'school', 'music-list', 'camera', 'globe', 'home'],
  },
  {
    name: '经典',
    icons: ['star', 'fire', 'trophy', 'diamond', 'gift', 'coffee', 'like-outlined', 'smiling-face', 'heart', 'success'],
  },
]

function selectIcon(icon) {
  form.icon = icon
}
const categories = [
  { value: 'advisor', label: '顾问型' },
  { value: 'productivity', label: '生产力' },
  { value: 'general', label: '通用型' },
]

async function load() {
  loading.value = true
  error.value = ''
  try {
    agents.value = await fetchAllAgents()
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function openNew() {
  editMode.value = false
  editId.value = null
  Object.assign(form, defaultForm())
  showForm.value = true
}

function openEdit(agent) {
  editMode.value = true
  editId.value = agent.id
  form.id = agent.id
  form.name = agent.name || ''
  form.description = agent.description || ''
  form.icon = agent.icon || 'robot'
  form.category = agent.category || 'general'
  form.tags = agent.tags ? [...agent.tags] : []
  form.systemPrompt = agent.systemPrompt || ''
  form.model = agent.model || ''
  form.temperature = agent.temperature ?? 0.7
  form.maxTokens = agent.maxTokens ?? 2000
  form.tools = agent.tools ? [...agent.tools] : []
  form.status = agent.status || 'ACTIVE'
  form.sortOrder = agent.sortOrder ?? 0
  showForm.value = true
}

function closeForm() {
  showForm.value = false
}

function addTag() {
  const t = tagInput.value.trim()
  if (t && !form.tags.includes(t)) {
    form.tags.push(t)
  }
  tagInput.value = ''
}

function removeTag(tag) {
  form.tags = form.tags.filter(t => t !== tag)
}

function toggleTool(tool) {
  const idx = form.tools.indexOf(tool)
  if (idx >= 0) {
    form.tools.splice(idx, 1)
  } else {
    form.tools.push(tool)
  }
}

async function saveForm() {
  // 前端校验
  if (!form.id.trim()) {
    alert('Agent ID 不能为空')
    return
  }
  if (!form.name.trim()) {
    alert('Agent 名称不能为空')
    return
  }
  if (!form.systemPrompt.trim()) {
    alert('System Prompt 不能为空，请描述 Agent 的功能和行为规范')
    return
  }

  saving.value = true
  try {
    const payload = {
      ...form,
      tags: form.tags.length ? form.tags : [],
      tools: form.tools.length ? form.tools : [],
    }
    if (editMode.value) {
      await updateAgent(editId.value, payload)
    } else {
      await createAgent(payload)
    }
    showForm.value = false
    await load()
  } catch (e) {
    alert('保存失败: ' + (e.response?.data?.message || e.message))
  } finally {
    saving.value = false
  }
}

function confirmDeleteAgent(agent) {
  deleteDialog.value = {
    visible: true,
    agentId: agent.id,
    agentName: agent.name || agent.id,
    loading: false,
  }
}

async function doDelete() {
  deleteDialog.value.loading = true
  try {
    await deleteAgent(deleteDialog.value.agentId)
    deleteDialog.value.visible = false
    await load()
  } catch (e) {
    alert('删除失败: ' + (e.response?.data?.message || e.message))
  } finally {
    deleteDialog.value.loading = false
  }
}

function cancelDelete() {
  deleteDialog.value.visible = false
}

async function toggleStatus(agent) {
  const newStatus = agent.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  try {
    await updateAgent(agent.id, { ...agent, status: newStatus, tags: agent.tags || [], tools: agent.tools || [] })
    agent.status = newStatus
  } catch (e) {
    alert('操作失败: ' + (e.response?.data?.message || e.message))
  }
}

function categoryLabel(cat) {
  const found = categories.find(c => c.value === cat)
  return found ? found.label : cat
}

onMounted(load)
</script>

<template>
  <main class="manage">
    <header class="manage-header">
      <div class="header-row">
        <h1 class="manage-title">🤖 Agent 管理</h1>
        <button class="btn-primary" @click="openNew">+ 新建 Agent</button>
      </div>
    </header>

    <div v-if="loading" class="manage-loading">加载中...</div>
    <div v-else-if="error" class="manage-error">{{ error }}</div>

    <section v-else class="table-section">
      <table class="agent-table" v-if="agents.length">
        <thead>
          <tr>
            <th>名称</th>
            <th>ID</th>
            <th>分类</th>
            <th>状态</th>
            <th>排序</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="a in agents" :key="a.id">
            <td class="name-cell">
              <AgentIcon :icon="a.icon || 'robot'" :size="20" theme="outline" />
              {{ a.name }}
            </td>
            <td class="mono">{{ a.id }}</td>
            <td>{{ categoryLabel(a.category) }}</td>
            <td>
              <span
                class="status-badge"
                :class="a.status === 'ACTIVE' ? 'active' : 'inactive'"
                @click="toggleStatus(a)"
                title="点击切换状态"
              >
                {{ a.status === 'ACTIVE' ? '启用' : '停用' }}
              </span>
            </td>
            <td class="mono">{{ a.sortOrder }}</td>
            <td class="actions">
              <button class="btn-sm" @click="openEdit(a)">编辑</button>
              <button
                class="btn-sm btn-danger"
                @click="confirmDeleteAgent(a)"
                :disabled="a.id === 'car-advisor' || a.id === 'super-agent'"
                :title="a.id === 'car-advisor' || a.id === 'super-agent' ? '系统保留 Agent，不可删除' : ''"
              >删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty">暂无 Agent，点击上方按钮新建</div>
    </section>

    <!-- 编辑弹窗 -->
    <div v-if="showForm" class="modal-overlay" @click.self="closeForm">
      <div class="modal">
        <div class="modal-header">
          <h2>{{ editMode ? '编辑 Agent' : '新建 Agent' }}</h2>
          <button class="modal-close" @click="closeForm">&times;</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <label class="form-label">ID <span class="req">*</span></label>
            <input v-model="form.id" class="form-input" :disabled="editMode" placeholder="英文标识，如 my-agent" />
          </div>
          <div class="form-row">
            <label class="form-label">名称 <span class="req">*</span></label>
            <input v-model="form.name" class="form-input" placeholder="如：AI 助手" />
          </div>
          <div class="form-row">
            <label class="form-label">描述</label>
            <textarea v-model="form.description" class="form-textarea" rows="2" placeholder="简短描述 Agent 的功能"></textarea>
          </div>
          <div class="form-row form-row-2col">
            <div>
              <label class="form-label">分类</label>
              <select v-model="form.category" class="form-input">
                <option v-for="c in categories" :key="c.value" :value="c.value">{{ c.label }}</option>
              </select>
            </div>
            <div>
              <label class="form-label">状态</label>
              <select v-model="form.status" class="form-input">
                <option value="ACTIVE">启用</option>
                <option value="INACTIVE">停用</option>
              </select>
            </div>
          </div>
          <div class="form-row">
            <label class="form-label">图标</label>
            <div class="icon-picker">
              <div class="icon-preview">
                <AgentIcon :icon="form.icon" :size="36" theme="outline" fill="#2563eb" />
                <input
                  v-model="form.icon"
                  class="form-input icon-input"
                  placeholder="输入图标名或 emoji（如 robot）"
                />
              </div>
              <div v-for="cat in iconCategories" :key="cat.name" class="icon-cat">
                <p class="icon-cat-name">{{ cat.name }}</p>
                <div class="icon-grid">
                  <button
                    v-for="icon in cat.icons"
                    :key="icon"
                    class="icon-btn"
                    :class="{ selected: form.icon === icon }"
                    :title="icon"
                    @click="selectIcon(icon)"
                  >
                    <AgentIcon :icon="icon" :size="20" theme="outline" />
                  </button>
                </div>
              </div>
            </div>
          </div>
          <div class="form-row">
            <label class="form-label">排序</label>
            <input v-model.number="form.sortOrder" type="number" class="form-input" />
          </div>
          <div class="form-row">
            <label class="form-label">标签</label>
            <div class="tag-input-row">
              <input v-model="tagInput" class="form-input" placeholder="输入标签后回车" @keyup.enter="addTag" />
              <button class="btn-sm" @click="addTag" type="button">添加</button>
            </div>
            <div class="tag-chips" v-if="form.tags.length">
              <span v-for="t in form.tags" :key="t" class="chip">
                {{ t }} <button class="chip-x" @click="removeTag(t)">&times;</button>
              </span>
            </div>
          </div>

          <fieldset class="form-fieldset">
            <legend>模型配置</legend>
            <div class="form-row form-row-2col">
              <div>
                <label class="form-label">Temperature ({{ form.temperature }})</label>
                <input v-model.number="form.temperature" type="range" min="0" max="2" step="0.1" class="form-range" />
              </div>
              <div>
                <label class="form-label">Max Tokens</label>
                <input v-model.number="form.maxTokens" type="number" class="form-input" min="100" max="32000" />
              </div>
            </div>
          </fieldset>

          <div class="form-row">
            <label class="form-label">System Prompt <span class="req">*</span></label>
            <textarea
              v-model="form.systemPrompt"
              class="form-textarea code"
              :class="{ 'input-error': !form.systemPrompt.trim() }"
              rows="8"
              placeholder="请详细描述该 Agent 的角色、能力和行为规范，这将作为系统提示词发送给 AI 模型。&#10;&#10;建议包含以下内容：&#10;1. 角色定位：你是谁？擅长什么领域？&#10;2. 核心能力：你能帮用户做什么？&#10;3. 行为规范：回答的风格、格式要求、禁止事项"
            ></textarea>
            <p class="form-hint">💡 System Prompt 是 AI 的核心行为指令，直接影响 Agent 的回答质量和风格。请清晰、详细地描述 Agent 的功能定位和行为规范。</p>
          </div>

          <div class="form-row">
            <label class="form-label">工具配置</label>
            <div class="tool-grid">
              <label v-for="t in availableTools" :key="t" class="tool-check">
                <input type="checkbox" :checked="form.tools.includes(t)" @change="toggleTool(t)" />
                {{ t }}
              </label>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-secondary" @click="closeForm">取消</button>
          <button class="btn-primary" @click="saveForm" :disabled="saving">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 删除确认 -->
    <ConfirmDialog
      :visible="deleteDialog.visible"
      title="删除 Agent"
      :description="`确定要删除「${deleteDialog.agentName}」吗？该 Agent 下的所有会话将被级联删除且不可恢复。`"
      confirm-text="删除"
      danger
      :loading="deleteDialog.loading"
      @confirm="doDelete"
      @cancel="cancelDelete"
    />
  </main>
</template>

<style scoped>
.manage {
  flex: 1;
  padding: 48px 24px 48px;
  max-width: 1000px;
  margin: 0 auto;
  width: 100%;
}

.manage-header { margin-bottom: 32px; }

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
}

.manage-title {
  font-size: 28px;
  font-weight: 700;
  color: #111827;
}

.manage-loading, .manage-error, .empty {
  text-align: center;
  padding: 60px 20px;
  color: #6b7280;
  font-size: 15px;
}
.manage-error { color: #dc2626; }

/* ====== 表格 ====== */
.table-section {
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

.agent-table td {
  padding: 14px 16px;
  font-size: 14px;
  color: #374151;
  border-bottom: 1px solid #f3f4f6;
}

.agent-table tr:last-child td { border-bottom: none; }

.name-cell {
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
}

.mono {
  font-family: 'SF Mono', 'Fira Code', 'Cascadia Code', monospace;
  font-size: 13px;
  color: #6b7280;
}

.status-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.15s;
}
.status-badge:hover { opacity: 0.8; }
.status-badge.active { background: #ecfdf5; color: #059669; }
.status-badge.inactive { background: #f3f4f6; color: #9ca3af; }

.actions { display: flex; gap: 8px; }

/* ====== 按钮 ====== */
.btn-primary, .btn-secondary, .btn-sm, .btn-danger {
  font-size: 13px;
  font-weight: 600;
  border-radius: 8px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: background 0.15s, opacity 0.15s;
}
.btn-primary:disabled, .btn-secondary:disabled { opacity: 0.5; cursor: not-allowed; }

.btn-primary {
  padding: 8px 18px;
  background: #2563eb;
  color: #fff;
}
.btn-primary:hover:not(:disabled) { background: #1d4ed8; }

.btn-secondary {
  padding: 8px 18px;
  background: #fff;
  color: #374151;
  border-color: #d1d5db;
}
.btn-secondary:hover { background: #f9fafb; }

.btn-sm {
  padding: 4px 12px;
  color: #2563eb;
  background: #eff6ff;
  border-color: #dbeafe;
}
.btn-sm:hover { background: #dbeafe; }

.btn-danger {
  color: #dc2626;
  background: #fef2f2;
  border-color: #fecaca;
}
.btn-danger:hover:not(:disabled) { background: #fecaca; }
.btn-danger:disabled { opacity: 0.4; cursor: not-allowed; }

/* ====== 模态框 ====== */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  z-index: 100;
  overflow-y: auto;
  padding: 40px 16px;
}

.modal {
  background: #fff;
  border-radius: 20px;
  width: 100%;
  max-width: 640px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 28px 0;
}

.modal-header h2 {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
}

.modal-close {
  font-size: 22px;
  background: none;
  border: none;
  color: #9ca3af;
  cursor: pointer;
  line-height: 1;
}
.modal-close:hover { color: #111827; }

.modal-body {
  padding: 20px 28px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 28px 24px;
}

/* ====== 表单 ====== */
.form-row { display: flex; flex-direction: column; gap: 4px; }
.form-row-2col { flex-direction: row; gap: 16px; }
.form-row-2col > * { flex: 1; }

.form-label { font-size: 13px; font-weight: 600; color: #374151; }
.req { color: #dc2626; }

.form-input, .form-textarea, select.form-input {
  padding: 8px 12px;
  font-size: 14px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  outline: none;
  font-family: inherit;
  transition: border-color 0.15s;
  background: #fff;
}
.form-input:focus, .form-textarea:focus { border-color: #2563eb; box-shadow: 0 0 0 3px rgba(37,99,235,0.1); }
.form-input:disabled { background: #f3f4f6; color: #9ca3af; }

.form-textarea { resize: vertical; }
.form-textarea.code {
  font-family: 'SF Mono', 'Fira Code', 'Cascadia Code', monospace;
  font-size: 13px;
}
.form-textarea.input-error {
  border-color: #dc2626;
}
.form-textarea.input-error:focus {
  box-shadow: 0 0 0 3px rgba(220,38,38,0.1);
}

.form-hint {
  margin: 0;
  font-size: 12px;
  color: #6b7280;
  line-height: 1.5;
}

.form-range { width: 100%; accent-color: #2563eb; }

.tag-input-row { display: flex; gap: 8px; }
.tag-input-row .form-input { flex: 1; }
.tag-chips { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 4px; }
.chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 10px;
  font-size: 12px;
  background: #eff6ff;
  color: #2563eb;
  border: 1px solid #dbeafe;
  border-radius: 6px;
}
.chip-x { background: none; border: none; color: #2563eb; cursor: pointer; font-size: 14px; line-height: 1; padding: 0; }

.form-fieldset {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px;
}
.form-fieldset legend {
  font-size: 13px;
  font-weight: 600;
  color: #6b7280;
  padding: 0 8px;
}

.tool-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 8px;
}
.tool-check {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #374151;
  cursor: pointer;
}

/* ====== 图标选择器 ====== */
.icon-picker {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 12px;
  max-height: 340px;
  overflow-y: auto;
}

.icon-preview {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f3f4f6;
}

.icon-input {
  flex: 1;
}

.icon-cat {
  margin-bottom: 10px;
}

.icon-cat:last-child { margin-bottom: 0; }

.icon-cat-name {
  font-size: 11px;
  font-weight: 600;
  color: #9ca3af;
  margin: 0 0 6px 0;
}

.icon-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.icon-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid transparent;
  border-radius: 8px;
  background: #f9fafb;
  cursor: pointer;
  transition: all 0.15s;
  padding: 0;
  color: #4b5563;
}

.icon-btn:hover {
  background: #eff6ff;
  border-color: #bfdbfe;
  color: #2563eb;
  transform: scale(1.1);
}

.icon-btn.selected {
  background: #dbeafe;
  border-color: #2563eb;
  color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37,99,235,0.15);
}

/* Premium AI platform skin */
.manage-title {
  color: var(--ai-text);
  font-weight: 800;
  letter-spacing: -0.35px;
}

.manage-loading, .manage-error, .empty { color: var(--ai-muted); }

.table-section {
  background:
    radial-gradient(circle at 8% 0%, rgba(167,139,250,0.12), transparent 34%),
    linear-gradient(145deg, rgba(255,255,255,0.92), rgba(248,250,252,0.78));
  border: 1px solid rgba(148,163,184,0.20);
  border-radius: 22px;
  box-shadow: var(--ai-shadow-sm);
  backdrop-filter: blur(14px);
}

.agent-table th {
  color: var(--ai-muted);
  background: linear-gradient(135deg, rgba(248,250,252,0.96), rgba(238,242,255,0.62));
  border-bottom: 1px solid rgba(148,163,184,0.18);
}

.agent-table td {
  color: #344054;
  border-bottom: 1px solid rgba(148,163,184,0.12);
}

.name-cell { color: var(--ai-text); font-weight: 650; }
.mono { color: #667085; }

.status-badge {
  border-radius: 999px;
  padding: 3px 11px;
  font-weight: 650;
}

.status-badge.active {
  color: #047857;
  background: linear-gradient(135deg, rgba(209,250,229,0.86), rgba(236,253,245,0.92));
  border: 1px solid rgba(16,185,129,0.18);
}

.status-badge.inactive {
  background: rgba(241,245,249,0.9);
  color: #94a3b8;
  border: 1px solid rgba(148,163,184,0.18);
}

.btn-primary, .btn-secondary, .btn-sm, .btn-danger {
  border-radius: 999px;
  transition: transform 0.15s, box-shadow 0.2s, background 0.2s, border-color 0.2s;
}

.btn-primary {
  background: linear-gradient(135deg, #7c3aed, #3b82f6);
  box-shadow: 0 12px 28px rgba(79,70,229,0.18);
}

.btn-primary:hover:not(:disabled) {
  background: linear-gradient(135deg, #6d28d9, #2563eb);
  transform: translateY(-1px);
  box-shadow: 0 16px 34px rgba(79,70,229,0.24);
}

.btn-secondary {
  background: rgba(255,255,255,0.82);
  border-color: rgba(148,163,184,0.28);
  color: #344054;
}

.btn-secondary:hover { background: #fff; box-shadow: var(--ai-shadow-sm); }

.btn-sm {
  color: #4f46e5;
  background: linear-gradient(135deg, rgba(238,242,255,0.92), rgba(240,249,255,0.80));
  border-color: rgba(139,92,246,0.15);
}

.btn-sm:hover {
  background: rgba(238,242,255,0.95);
  transform: translateY(-1px);
}

.btn-danger {
  color: #dc2626;
  background: linear-gradient(135deg, rgba(254,242,242,0.95), rgba(255,247,237,0.90));
  border-color: rgba(248,113,113,0.24);
}

.modal-overlay {
  background: rgba(15,23,42,0.34);
  backdrop-filter: blur(10px);
}

.modal {
  background:
    radial-gradient(circle at 10% 0%, rgba(167,139,250,0.12), transparent 34%),
    rgba(255,255,255,0.94);
  border: 1px solid rgba(148,163,184,0.20);
  border-radius: 24px;
  box-shadow: var(--ai-shadow-lg);
  backdrop-filter: blur(18px);
}

.modal-header h2 {
  color: var(--ai-text);
  font-weight: 800;
}

.form-label {
  color: #344054;
  font-weight: 650;
}

.form-input, .form-textarea, select.form-input {
  border-color: rgba(148,163,184,0.28);
  border-radius: 12px;
  background: rgba(255,255,255,0.86);
  transition: border-color 0.15s, box-shadow 0.15s, background 0.15s;
}

.form-input:focus, .form-textarea:focus {
  border-color: rgba(109,93,252,0.55);
  box-shadow: 0 0 0 4px rgba(109,93,252,0.12);
  background: #fff;
}

.form-hint {
  color: var(--ai-muted);
  background: rgba(238,242,255,0.55);
  border: 1px solid rgba(139,92,246,0.10);
  border-radius: 12px;
  padding: 10px 12px;
}

.chip {
  background: rgba(238,242,255,0.86);
  color: #4f46e5;
  border-color: rgba(139,92,246,0.16);
  border-radius: 999px;
}

.form-fieldset,
.icon-picker {
  border-color: rgba(148,163,184,0.20);
  background: rgba(248,250,252,0.56);
  border-radius: 16px;
}

.tool-check {
  background: rgba(255,255,255,0.72);
  border: 1px solid rgba(148,163,184,0.16);
  border-radius: 12px;
  padding: 9px 10px;
}

.icon-btn {
  border-radius: 12px;
  background: rgba(255,255,255,0.76);
  border-color: rgba(148,163,184,0.14);
}

.icon-btn:hover {
  background: rgba(238,242,255,0.92);
  border-color: rgba(139,92,246,0.24);
  color: #4f46e5;
}

.icon-btn.selected {
  background: linear-gradient(135deg, rgba(238,242,255,0.98), rgba(240,249,255,0.88));
  border-color: rgba(109,93,252,0.70);
  color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(109,93,252,0.12);
}

.table-section {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

@media (max-width: 768px) {
  .manage { padding: 24px 14px 32px; }
  .manage-title { font-size: 22px; }
  .header-row { flex-direction: column; align-items: flex-start; }
  .form-row-2col { flex-direction: column; }
  .modal { border-radius: 16px; }
  .modal-header { padding: 18px 20px 0; }
  .modal-body { padding: 16px 20px; gap: 12px; }
  .modal-footer { padding: 14px 20px 20px; }
  .actions { flex-direction: column; }
  .tool-grid { grid-template-columns: repeat(auto-fill, minmax(130px, 1fr)); }
  .agent-table th, .agent-table td { padding: 10px 12px; font-size: 13px; }
}

@media (max-width: 480px) {
  .manage { padding: 14px 10px 24px; }
  .manage-title { font-size: 19px; }
  .table-section { border-radius: 12px; }
  .modal-overlay { padding: 0; align-items: flex-end; }
  .modal {
    max-width: 100%;
    border-radius: 16px 16px 0 0;
    max-height: 90vh;
    overflow-y: auto;
  }
  .modal-header { padding: 16px 16px 0; }
  .modal-body { padding: 14px 16px; gap: 10px; }
  .modal-footer { padding: 12px 16px 16px; flex-direction: column; }
  .modal-footer .btn-primary,
  .modal-footer .btn-secondary { width: 100%; text-align: center; }
  .agent-table th, .agent-table td { padding: 8px 10px; font-size: 12px; }
  .agent-table th { font-size: 10px; }
  .tool-grid { grid-template-columns: repeat(2, 1fr); }
  .icon-picker { max-height: 240px; }
  .icon-btn { width: 34px; height: 34px; }
  .tag-input-row { flex-direction: column; }
}
</style>

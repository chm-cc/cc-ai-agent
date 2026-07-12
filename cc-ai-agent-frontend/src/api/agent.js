import request from './request'

/**
 * 获取所有 Agent（含停用）
 */
export async function fetchAllAgents() {
  const res = await request.get('/v1/agents/all')
  return res.data.data
}

/**
 * 创建 Agent
 */
export async function createAgent(data) {
  const res = await request.post('/v1/agents', data)
  return res.data.data
}

/**
 * 更新 Agent
 */
export async function updateAgent(id, data) {
  const res = await request.put(`/v1/agents/${id}`, data)
  return res.data.data
}

/**
 * 删除 Agent
 */
export async function deleteAgent(id) {
  await request.delete(`/v1/agents/${id}`)
}

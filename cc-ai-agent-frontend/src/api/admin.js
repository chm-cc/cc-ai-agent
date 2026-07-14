import request from './request'

/** 创建测试用户 */
export async function createUser(data) {
  const res = await request.post('/v1/admin/users', data)
  return res.data.data
}

/** 获取用户列表（分页） */
export async function fetchUsers(params = {}) {
  const res = await request.get('/v1/admin/users', { params })
  return res.data.data
}

/** 查看用户密码 */
export async function viewPassword(userId) {
  const res = await request.get(`/v1/admin/users/${userId}/password`)
  return res.data.data
}

/** 重置用户密码 */
export async function resetPassword(userId, data) {
  await request.put(`/v1/admin/users/${userId}/password`, data)
}

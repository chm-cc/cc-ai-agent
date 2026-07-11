import request from './request'
import { setAuth, clearAuth } from '../utils/auth'

export async function login(username, password) {
  const { data } = await request.post('/auth/login', { username, password })
  if (data.code !== 0) {
    throw new Error(data.message || '登录失败')
  }
  setAuth(data.data.token, data.data.username)
  return data.data
}

export async function getCurrentUser() {
  const { data } = await request.get('/auth/me')
  if (data.code !== 0) {
    throw new Error(data.message || '获取用户信息失败')
  }
  return data.data
}

export function logout() {
  clearAuth()
}

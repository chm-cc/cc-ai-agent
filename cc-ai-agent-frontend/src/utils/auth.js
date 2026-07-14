const TOKEN_KEY = 'ai_agent_token'
const USERNAME_KEY = 'ai_agent_username'
const ROLE_KEY = 'ai_agent_role'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setAuth(token, username, role) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USERNAME_KEY, username)
  if (role) localStorage.setItem(ROLE_KEY, role)
}

export function getUsername() {
  return localStorage.getItem(USERNAME_KEY)
}

export function getRole() {
  return localStorage.getItem(ROLE_KEY)
}

export function isSuperAdmin() {
  return getRole() === 'SUPER_ADMIN'
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USERNAME_KEY)
  localStorage.removeItem(ROLE_KEY)
}

export function isLoggedIn() {
  return !!getToken()
}

export function getAuthHeaders() {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

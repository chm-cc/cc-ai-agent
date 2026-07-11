const TOKEN_KEY = 'ai_agent_token'
const USERNAME_KEY = 'ai_agent_username'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setAuth(token, username) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USERNAME_KEY, username)
}

export function getUsername() {
  return localStorage.getItem(USERNAME_KEY)
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USERNAME_KEY)
}

export function isLoggedIn() {
  return !!getToken()
}

export function getAuthHeaders() {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

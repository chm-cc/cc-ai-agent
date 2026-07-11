import axios from 'axios'
import { getAuthHeaders, clearAuth } from '../utils/auth'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 60000,
})

request.interceptors.request.use((config) => {
  Object.assign(config.headers, getAuthHeaders())
  return config
})

request.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearAuth()
      const current = router.currentRoute.value
      if (current.name !== 'Login') {
        router.push({ name: 'Login', query: { redirect: current.fullPath } })
      }
    }
    return Promise.reject(error)
  },
)

export default request

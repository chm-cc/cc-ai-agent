import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import AgentChat from '../views/AgentChat.vue'
import Login from '../views/Login.vue'
import { routeSeo } from '../config/site'
import { updatePageSeo } from '../utils/seo'
import { trackPageView } from '../utils/monitor'
import { isLoggedIn } from '../utils/auth'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home,
    meta: { seo: routeSeo.Home },
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { seo: { title: '登录' }, hideFooter: true, public: true },
  },
  {
    path: '/car',
    redirect: '/chat/car-advisor',
  },
  {
    path: '/manus',
    redirect: '/chat/super-agent',
  },
  {
    path: '/chat/:agentId',
    name: 'AgentChat',
    component: AgentChat,
    meta: { seo: { title: 'AI 对话' }, hideFooter: true, requiresAuth: true },
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('../views/Dashboard.vue'),
    meta: { seo: routeSeo.Dashboard, requiresAuth: true },
  },
  {
    path: '/agents',
    name: 'AgentManage',
    component: () => import('../views/AgentManage.vue'),
    meta: { seo: routeSeo.AgentManage, requiresAuth: true },
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !isLoggedIn()) {
    return { name: 'Login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'Login' && isLoggedIn()) {
    return { path: '/' }
  }
})

router.afterEach((to) => {
  const seo = to.meta.seo || {}
  updatePageSeo({ ...seo, path: to.fullPath })
  trackPageView(to.fullPath, document.title)
})

export default router

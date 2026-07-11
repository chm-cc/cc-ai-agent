import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import CarChat from '../views/CarChat.vue'
import ManusChat from '../views/ManusChat.vue'
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
    name: 'CarChat',
    component: CarChat,
    meta: { seo: routeSeo.CarChat, hideFooter: true, requiresAuth: true },
  },
  {
    path: '/manus',
    name: 'ManusChat',
    component: ManusChat,
    meta: { seo: routeSeo.ManusChat, hideFooter: true, requiresAuth: true },
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

import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import CarChat from '../views/CarChat.vue'
import ManusChat from '../views/ManusChat.vue'
import { routeSeo } from '../config/site'
import { updatePageSeo } from '../utils/seo'
import { trackPageView } from '../utils/monitor'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home,
    meta: { seo: routeSeo.Home },
  },
  {
    path: '/car',
    name: 'CarChat',
    component: CarChat,
    meta: { seo: routeSeo.CarChat, hideFooter: true },
  },
  {
    path: '/manus',
    name: 'ManusChat',
    component: ManusChat,
    meta: { seo: routeSeo.ManusChat, hideFooter: true },
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

router.afterEach((to) => {
  const seo = to.meta.seo || {}
  updatePageSeo({ ...seo, path: to.fullPath })
  trackPageView(to.fullPath, document.title)
})

export default router

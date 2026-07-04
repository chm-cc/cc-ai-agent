import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { initMonitor } from './utils/monitor'
import './styles/global.css'

initMonitor()

createApp(App).use(router).mount('#app')

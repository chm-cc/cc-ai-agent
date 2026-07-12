<script setup>
import { computed } from 'vue'
import { siteConfig } from '../config/site'

const currentYear = computed(() => new Date().getFullYear())
</script>

<template>
  <footer class="site-footer">
    <div class="footer-inner">
      <p class="copyright">
        &copy; {{ currentYear }} {{ siteConfig.copyrightHolder }}
      </p>
      <span class="sep" v-if="siteConfig.wechat">·</span>
      <p v-if="siteConfig.wechat" class="wechat">
        微信号：{{ siteConfig.wechat }}
      </p>
      <span class="sep" v-if="siteConfig.icpNumber">·</span>
      <p v-if="siteConfig.icpNumber" class="icp">
        <a
          href="https://beian.miit.gov.cn/"
          target="_blank"
          rel="noopener noreferrer"
        >
          {{ siteConfig.icpNumber }}
        </a>
      </p>
    </div>
  </footer>
</template>

<style scoped>
.site-footer {
  padding: 20px 16px;
  text-align: center;
  background: transparent;
  border-top: 1px solid rgba(0, 0, 0, 0.04);
}

.footer-inner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  max-width: 960px;
  margin: 0 auto;
  font-size: 12px;
  color: #aaa;
}

.sep {
  color: #ddd;
}

.wechat {
  color: #aaa;
}

.icp a {
  color: #aaa;
  transition: color 0.2s;
}

.icp a:hover {
  color: #666;
}
</style>

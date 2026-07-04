<script setup>
import { computed } from 'vue'
import { siteConfig } from '../config/site'

const currentYear = computed(() => new Date().getFullYear())
</script>

<template>
  <footer class="site-footer">
    <div class="footer-inner">
      <p class="copyright">
        © {{ currentYear }} {{ siteConfig.copyrightHolder }}. All rights reserved.
      </p>
      <p v-if="siteConfig.icpNumber" class="icp">
        <a
          href="https://beian.miit.gov.cn/"
          target="_blank"
          rel="noopener noreferrer"
        >
          {{ siteConfig.icpNumber }}
        </a>
      </p>
      <p class="powered">Powered by {{ siteConfig.name }}</p>
    </div>
  </footer>
</template>

<style scoped>
.site-footer {
  padding: 24px 16px;
  text-align: center;
  color: #999;
  font-size: 12px;
  line-height: 1.8;
  border-top: 1px solid #e8e8e8;
  background: #fafafa;
}

.footer-inner {
  max-width: 960px;
  margin: 0 auto;
}

.copyright {
  color: #888;
}

.icp a {
  color: #999;
  transition: color 0.2s;
}

.icp a:hover {
  color: #4f6ef7;
}

.powered {
  color: #bbb;
}
</style>

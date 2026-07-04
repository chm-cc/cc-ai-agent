<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import SiteFooter from './components/SiteFooter.vue'

const route = useRoute()
const showFooter = computed(() => !route.meta.hideFooter)
</script>

<template>
  <div class="app-layout">
    <router-view />
    <SiteFooter v-if="showFooter" />
  </div>
</template>

<style scoped>
.app-layout {
  min-height: 100%;
  display: flex;
  flex-direction: column;
}
</style>

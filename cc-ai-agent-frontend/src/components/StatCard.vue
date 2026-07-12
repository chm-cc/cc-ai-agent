<script setup>
defineProps({
  label: { type: String, required: true },
  value: { type: [Number, String], required: true },
  unit: { type: String, default: '' },
  icon: { type: String, default: '' },
  color: { type: String, default: '#2563eb' },
})
</script>

<template>
  <div class="stat-card" :style="{ '--accent': color }">
    <div class="stat-icon" v-if="icon">{{ icon }}</div>
    <div class="stat-body">
      <span class="stat-value">
        {{ typeof value === 'number' ? value.toLocaleString() : value }}
        <small v-if="unit" class="stat-unit">{{ unit }}</small>
      </span>
      <span class="stat-label">{{ label }}</span>
    </div>
  </div>
</template>

<style scoped>
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  transition: box-shadow 0.2s, transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(0,0,0,0.06), 0 0 0 1px var(--accent, #2563eb) inset;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: color-mix(in srgb, var(--accent) 10%, transparent);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}

.stat-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #111827;
  line-height: 1.2;
}

.stat-unit {
  font-size: 14px;
  font-weight: 400;
  color: #9ca3af;
  margin-left: 2px;
}

.stat-label {
  font-size: 13px;
  color: #6b7280;
}
</style>

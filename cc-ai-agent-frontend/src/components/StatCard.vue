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
  background:
    radial-gradient(circle at 15% 0%, color-mix(in srgb, var(--accent) 16%, transparent), transparent 48%),
    linear-gradient(145deg, rgba(255,255,255,0.92), color-mix(in srgb, var(--accent) 7%, #ffffff));
  border: 1px solid color-mix(in srgb, var(--accent) 18%, rgba(148,163,184,0.18));
  border-radius: 20px;
  box-shadow: var(--ai-shadow-sm);
  transition: box-shadow 0.2s, transform 0.2s, border-color 0.2s;
  position: relative;
  overflow: hidden;
}

.stat-card::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.34), transparent 45%);
  pointer-events: none;
}

.stat-card:hover {
  transform: translateY(-4px);
  border-color: color-mix(in srgb, var(--accent) 30%, rgba(148,163,184,0.18));
  box-shadow: 0 16px 42px color-mix(in srgb, var(--accent) 18%, rgba(30,41,59,0.08));
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 15px;
  background: color-mix(in srgb, var(--accent) 12%, #ffffff);
  border: 1px solid color-mix(in srgb, var(--accent) 20%, rgba(255,255,255,0.40));
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.70), 0 10px 24px rgba(30,41,59,0.07);
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
  position: relative;
  z-index: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 780;
  color: var(--ai-text);
  line-height: 1.2;
  letter-spacing: -0.4px;
}

.stat-unit {
  font-size: 14px;
  font-weight: 400;
  color: #9ca3af;
  margin-left: 2px;
}

.stat-label {
  font-size: 13px;
  color: var(--ai-muted);
  font-weight: 550;
}
</style>

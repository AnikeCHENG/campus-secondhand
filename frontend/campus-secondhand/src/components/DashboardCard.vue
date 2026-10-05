<template>
  <div
    class="stat-card"
    :class="{ clickable }"
    :role="clickable ? 'button' : undefined"
    :tabindex="clickable ? 0 : undefined"
    @click="handleClick"
    @keydown.enter.prevent="handleClick"
    @keydown.space.prevent="handleClick"
  >
    <div class="stat-icon">
      <el-icon><component :is="iconName" /></el-icon>
    </div>
    <div class="stat-meta">
      <div class="stat-value">{{ value }}</div>
      <div class="stat-title">{{ title }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ShoppingCart, List, ChatLineRound, User } from '@element-plus/icons-vue'

const props = defineProps({
  title: String,
  value: [String, Number],
  icon: String,
  color: String,
  clickable: { type: Boolean, default: false },
})
const emit = defineEmits(['click'])

const iconName = computed(() => {
  const icons = { shopping: ShoppingCart, order: List, message: ChatLineRound, user: User }
  return icons[props.icon] || ShoppingCart
})

function handleClick() {
  if (props.clickable) emit('click')
}
</script>

<style scoped>
.stat-card {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-5);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  transition: border-color var(--dur) var(--ease), box-shadow var(--dur) var(--ease);
}

.stat-card.clickable {
  cursor: pointer;
}

.stat-card.clickable:hover {
  border-color: var(--border-strong);
  box-shadow: var(--shadow-sm);
}

.stat-icon {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius);
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 20px;
}

.stat-value {
  font-size: var(--text-xl);
  font-weight: var(--weight-semibold);
  letter-spacing: -0.02em;
  color: var(--text);
  line-height: 1.1;
}

.stat-title {
  margin-top: 2px;
  font-size: var(--text-sm);
  color: var(--text-2);
}
</style>

<template>
  <div class="recom-list">
    <button
      v-for="(r, i) in items"
      :key="i"
      class="recom"
      type="button"
      @click="$emit('click', r)"
    >
      <div class="body">
        <div class="title">{{ r.title }}</div>
        <div class="desc">{{ r.desc }}</div>
      </div>
      <svg class="arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
        <polyline points="9 6 15 12 9 18" />
      </svg>
    </button>

    <div v-if="!items || items.length === 0" class="empty">暂无推荐</div>
  </div>
</template>

<script setup>
defineProps({ items: { type: Array, default: () => [] } })
defineEmits(['click'])
</script>

<style scoped>
.recom-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.recom {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  width: 100%;
  padding: var(--space-3) var(--space-4);
  text-align: left;
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius);
  transition: background var(--dur-fast) var(--ease), border-color var(--dur-fast) var(--ease);
}

.recom:hover {
  background: var(--surface-2);
  border-color: var(--border);
}

.body { flex: 1; min-width: 0; }

.title {
  font-size: var(--text-base);
  font-weight: var(--weight-medium);
  color: var(--text);
}

.desc {
  margin-top: 2px;
  font-size: var(--text-sm);
  color: var(--text-2);
}

.arrow {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  color: var(--text-3);
  transition: transform var(--dur-fast) var(--ease), color var(--dur-fast) var(--ease);
}
.recom:hover .arrow {
  color: var(--accent);
  transform: translateX(2px);
}

.empty {
  padding: var(--space-6);
  text-align: center;
  font-size: var(--text-sm);
  color: var(--text-3);
}
</style>

<template>
  <div class="recent-list">
    <button
      v-for="(it, idx) in items"
      :key="idx"
      class="item"
      type="button"
      @click="$emit('view', it)"
    >
      <div class="thumb">
        <img v-if="it.img" :src="it.img" :alt="it.title" />
        <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <rect x="3" y="3" width="18" height="18" rx="2" />
          <circle cx="9" cy="9" r="2" />
          <path d="M21 15l-5-5L5 21" />
        </svg>
      </div>
      <div class="info">
        <div class="title">{{ it.title }}</div>
        <div class="meta">{{ it.price }} · 校园在售</div>
      </div>
      <svg class="arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
        <polyline points="9 6 15 12 9 18" />
      </svg>
    </button>

    <div v-if="!items || items.length === 0" class="empty">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <rect x="3" y="3" width="18" height="18" rx="2" />
        <path d="M3 9h18M9 21V9" />
      </svg>
      <p>暂无商品，快去发布第一个吧</p>
    </div>
  </div>
</template>

<script setup>
defineProps({ items: { type: Array, default: () => [] } })
defineEmits(['view'])
</script>

<style scoped>
.recent-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.item {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  width: 100%;
  padding: var(--space-3);
  text-align: left;
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius);
  transition: background var(--dur-fast) var(--ease), border-color var(--dur-fast) var(--ease);
}

.item:hover {
  background: var(--surface-2);
  border-color: var(--border);
}

.thumb {
  width: 56px;
  height: 56px;
  flex-shrink: 0;
  border-radius: var(--radius-sm);
  overflow: hidden;
  background: var(--surface-3);
  color: var(--text-3);
  display: flex;
  align-items: center;
  justify-content: center;
}
.thumb svg { width: 24px; height: 24px; }
.thumb img { width: 100%; height: 100%; object-fit: cover; }

.info {
  flex: 1;
  min-width: 0;
}

.title {
  font-size: var(--text-base);
  font-weight: var(--weight-medium);
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta {
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
.item:hover .arrow {
  color: var(--accent);
  transform: translateX(2px);
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-10) var(--space-4);
  color: var(--text-3);
  text-align: center;
}
.empty svg { width: 32px; height: 32px; }
.empty p { font-size: var(--text-sm); }
</style>

<script setup>
/**
 * 五星评价组件。
 *
 * <p>交互要点：hover 时点亮到当前悬停位置，鼠标移开恢复为已选值。
 * 这是纯 CSS 的 :hover 无法单独做到的——它只知道当前元素，
 * 需要 JS 维护一个 hover 索引才能让 1~4 星同时点亮。</p>
 */
import { ref } from 'vue'

defineProps({
  /** 当前评分 0~5，0 表示未选 */
  modelValue: { type: Number, default: 0 },
  /** 是否只读展示 */
  readonly: { type: Boolean, default: false },
  size: { type: Number, default: 20 }
})
const emit = defineEmits(['update:modelValue'])

const hoverIndex = ref(0)

function pick(index) {
  emit('update:modelValue', index)
}
</script>

<template>
  <div
    class="star-rating"
    role="radiogroup"
    aria-label="评分"
    @mouseleave="hoverIndex = 0"
  >
    <button
      v-for="i in 5"
      :key="i"
      type="button"
      class="star"
      :class="{ 'star--on': i <= (hoverIndex || modelValue) }"
      :style="{ fontSize: size + 'px' }"
      :role="readonly ? 'img' : 'radio'"
      :aria-checked="!readonly && modelValue === i"
      :aria-label="readonly ? `${i} 星` : `打 ${i} 星`"
      :disabled="readonly"
      @mouseenter="!readonly && (hoverIndex = i)"
      @focus="!readonly && (hoverIndex = i)"
      @click="!readonly && pick(i)"
    >★</button>
    <span v-if="!readonly" class="star-rating__hint">{{ hoverIndex || modelValue ? `${hoverIndex || modelValue} 星` : '点击星星评分' }}</span>
  </div>
</template>

<style scoped>
.star-rating { display: inline-flex; align-items: center; gap: var(--space-1); }

.star {
  padding: 0;
  border: 0;
  background: none;
  color: var(--text-3);
  cursor: pointer;
  transition: color var(--dur) var(--ease), transform var(--dur) var(--ease);
  line-height: 1;
}
.star:hover:not(:disabled) { transform: scale(1.15); }
.star:focus-visible { outline: 2px solid var(--accent); outline-offset: 2px; }
/* 未点亮用浅灰描边感，点亮用赭色——赭色与 --accent 同色系，不引入新色 */
.star--on { color: #d4a017; }
.star:disabled { cursor: default; }

.star-rating__hint {
  margin-left: var(--space-2);
  font-size: var(--text-sm);
  color: var(--text-2);
}
</style>
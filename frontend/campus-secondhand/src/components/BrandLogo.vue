<template>
  <span
    class="brand-logo"
    :style="{ width: `${size}px`, height: `${size}px`, borderRadius: `${Math.round(size * 0.24)}px` }"
    aria-hidden="true"
  >
    <svg :width="iconSize" :height="iconSize" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <path d="M3 12a9 9 0 0 1 15-6.7L21 8" />
      <path d="M21 3v5h-5" />
      <path d="M21 12a9 9 0 0 1-15 6.7L3 16" />
      <path d="M3 21v-5h5" />
    </svg>
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  /** 图标外框尺寸（px），SVG 图标按 55% 比例内缩 */
  size: { type: Number, default: 44 },
})

const iconSize = computed(() => Math.round(props.size * 0.55))
</script>

<style scoped>
/*
 * 尺寸与外观内聚在此组件内，不再依赖全局 .brand-mark 类。
 * 全项目仍有 7 处旧 .brand-mark 重复（Home/Messages/Products/PostProduct/
 * ProductDetail/Drafts/Payment，尺寸散落 44/30/14），属已知遗留，
 * 待答辩后统一迁移至此组件。
 */
.brand-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.32);
  color: #fff;
}
</style>
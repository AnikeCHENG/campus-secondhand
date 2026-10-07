<script setup>
/**
 * 学生认证徽章。
 *
 * <p>全站统一用一个组件而不是各处手写 `<span>🎓</span>`，因为徽章的语义
 * （这个人是否已通过学生认证）在商品详情、聊天窗口、个人中心三处反复出现，
 * 散着写迟早会出现某一处漏传字段或样式不一致。</p>
 *
 * <p>未认证时默认不渲染任何东西——徽章是有信息量的标识，
 * 给普通用户挂一个空徽章只会制造噪声。</p>
 */
defineProps({
  /** 是否已认证 */
  verified: { type: Boolean, default: false },
  /** 尺寸；sm 用于昵称旁，md 用于卡片 */
  size: { type: String, default: 'md' },
  /** 是否展示文字标签 */
  showLabel: { type: Boolean, default: false }
})
</script>

<template>
  <span
    v-if="verified"
    class="verified-badge"
    :class="`verified-badge--${size}`"
    :title="showLabel ? undefined : '已通过学生认证'"
  >
    <span class="verified-badge__icon" aria-hidden="true">🎓</span>
    <span v-if="showLabel" class="verified-badge__label">学生认证</span>
    <span class="sr-only">已通过学生认证</span>
  </span>
</template>

<style scoped>
/*
 * 只读屏幕阅读器：不隐藏元素本身，仅把文字移出可视区域。
 * 徽章的图标对读屏软件没有意义，真正的信息在 .sr-only 那句文字里。
 */
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.verified-badge {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  border-radius: var(--radius-sm, 4px);
  /* 底色用 --accent 的极浅版本，保证与主题一致而不引入新色 */
  background: color-mix(in srgb, var(--accent) 10%, transparent);
  color: var(--accent);
  font-weight: var(--weight-medium);
  line-height: 1;
  white-space: nowrap;
}

.verified-badge--sm {
  padding: 2px 4px;
  font-size: var(--text-xs);
}

.verified-badge--md {
  padding: 3px var(--space-2);
  font-size: var(--text-xs);
}

.verified-badge--lg {
  padding: var(--space-1) var(--space-3);
  font-size: var(--text-sm);
}

.verified-badge__icon {
  /* emoji 字形高度不一，统一约束避免徽章高度随内容跳动 */
  display: inline-block;
  line-height: 1;
}
</style>
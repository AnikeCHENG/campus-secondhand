<script setup>
/**
 * 商品缩略图沿二次贝塞尔曲线飞入导航购物车图标。
 *
 * <p>用单个绝对定位的克隆节点 + {@code requestAnimationFrame} 推进进度，
 * 而不是 CSS 动画：曲线的控制点依赖「起点 → 终点」的实时坐标，
 * CSS 无法在不知道终点位置的情况下描述这条路径。</p>
 *
 * <p>飞行动画是纯装饰，任何异常都不能影响加购结果，因此全部静默失败。</p>
 */
import { onBeforeUnmount, ref } from 'vue'

const flying = ref(null)
const rafId = ref(0)

/** 二次贝塞尔：B(t) = (1-t)^2·P0 + 2(1-t)t·P1 + t^2·P2 */
function bezier(p0, p1, p2, t) {
  const inv = 1 - t
  return {
    x: inv * inv * p0.x + 2 * inv * t * p1.x + t * t * p2.x,
    y: inv * inv * p0.y + 2 * inv * t * p1.y + t * t * p2.y
  }
}

/**
 * @param {string} image 商品缩略图（可为 null，无图时飞一个占位方块）
 * @param {Element|null} sourceEl 触发按钮，用于取起点矩形
 */
function fly(image, sourceEl) {
  const target = document.querySelector('[data-cart-icon]')
  if (!target) return
  const srcRect = sourceEl?.getBoundingClientRect()
  const dstRect = target.getBoundingClientRect()
  if (!srcRect || !dstRect) return

  cancelAnimationFrame(rafId.value)

  // 控制点抬高，形成抛物线观感；水平方向推到视口中部再收束
  const start = { x: srcRect.left + srcRect.width / 2, y: srcRect.top + srcRect.height / 2 }
  const end = { x: dstRect.left + dstRect.width / 2, y: dstRect.top + dstRect.height / 2 }
  const ctrl = {
    x: (start.x + end.x) / 2,
    y: Math.min(start.y, end.y) - Math.min(220, Math.abs(end.x - start.x) * 0.4 + 90)
  }

  flying.value = {
    image,
    left: start.x,
    top: start.y,
    w: Math.max(28, Math.min(srcRect.width, 72))
  }

  const duration = 620
  let started = 0
  const step = (ts) => {
    if (!started) started = ts
    const t = Math.min(1, (ts - started) / duration)
    const point = bezier(start, ctrl, end, t)
    if (!flying.value) return
    // 越接近终点越小，模拟「飞进图标里」
    const scale = 1 - 0.72 * t
    flying.value = { ...flying.value, left: point.x, top: point.y, w: flying.value.w * 0 + Math.max(12, 72 * scale) }
    if (t < 1) {
      rafId.value = requestAnimationFrame(step)
    } else {
      flying.value = null
      // 抵达后让角标弹一下，收尾反馈
      target.classList.remove('cart-icon--pulse')
      void target.offsetWidth
      target.classList.add('cart-icon--pulse')
    }
  }
  rafId.value = requestAnimationFrame(step)
}

onBeforeUnmount(() => cancelAnimationFrame(rafId.value))

defineExpose({ fly })
</script>

<template>
  <div
    v-if="flying"
    class="fly-chip"
    :style="{
      left: flying.left + 'px',
      top: flying.top + 'px',
      width: flying.w + 'px',
      height: flying.w + 'px'
    }"
    aria-hidden="true"
  >
    <img v-if="flying.image" :src="flying.image" alt="" />
    <span v-else class="fly-chip__placeholder">🛒</span>
  </div>
</template>

<style scoped>
.fly-chip {
  position: fixed;
  z-index: 3000;
  transform: translate(-50%, -50%);
  border-radius: var(--radius-sm, 4px);
  overflow: hidden;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.2);
  pointer-events: none;
}
.fly-chip img { width: 100%; height: 100%; object-fit: cover; display: block; }
.fly-chip__placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: var(--surface-3);
  font-size: 14px;
}
</style>
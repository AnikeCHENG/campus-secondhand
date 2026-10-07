<template>
  <div class="auth-shell" ref="shellEl">
    <!-- 视频背景保留但压到 16% 当纹理，不改动 VideoBackground 组件本身 -->
    <div class="video-dim" aria-hidden="true">
      <VideoBackground :toggle="false" />
    </div>

    <!-- Aurora 光斑 -->
    <div class="auth-shell__aurora" aria-hidden="true">
      <span class="aurora-blob aurora-blob--green"></span>
      <span class="aurora-blob aurora-blob--blue"></span>
      <span class="aurora-blob aurora-blob--gold"></span>
    </div>

    <!-- 漂浮光点粒子（位置/delay 固定写死，可复现） -->
    <div class="auth-shell__particles" aria-hidden="true">
      <span
        v-for="(p, i) in particles"
        :key="i"
        class="particle"
        :style="{
          left: p.left,
          top: p.top,
          width: `${p.size}px`,
          height: `${p.size}px`,
          '--delay': p.delay,
          '--dur': p.dur,
        }"
      ></span>
    </div>

    <div class="auth-shell__stage" :class="`auth-shell__stage--${variant}`">
      <!-- 左品牌区（split 变体） -->
      <section v-if="showBrand" class="auth-brand">
        <slot name="brand" />

        <span
          v-for="(f, i) in floaters"
          :key="`f-${i}`"
          class="auth-floater"
          :style="{ top: f.top, left: f.left, fontSize: f.fontSize, animationDelay: f.delay }"
          aria-hidden="true"
        >{{ f.emoji }}</span>
      </section>

      <!-- 表单区 -->
      <slot />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import VideoBackground from './VideoBackground.vue'

defineProps({
  /** split = 两栏（左品牌区 + 右表单）；compact = 单栏居中 */
  variant: { type: String, default: 'compact' },
  /** 是否渲染左品牌区 */
  showBrand: { type: Boolean, default: false },
})

const shellEl = ref(null)

// 36 个光点粒子，位置/尺寸/延时固定写死，避免 Math.random 造成每次渲染跳动
const particles = [
  { left: '8%', top: '14%', size: 3, delay: '-0.4s', dur: '6.5s' },
  { left: '19%', top: '62%', size: 2, delay: '-1.8s', dur: '8s' },
  { left: '27%', top: '31%', size: 4, delay: '-3.2s', dur: '7s' },
  { left: '35%', top: '81%', size: 2, delay: '-0.9s', dur: '9s' },
  { left: '43%', top: '22%', size: 3, delay: '-4.1s', dur: '6s' },
  { left: '52%', top: '68%', size: 2, delay: '-2.3s', dur: '8.5s' },
  { left: '61%', top: '9%', size: 4, delay: '-5.6s', dur: '7.5s' },
  { left: '69%', top: '47%', size: 2, delay: '-1.1s', dur: '6.8s' },
  { left: '77%', top: '74%', size: 3, delay: '-3.7s', dur: '9.2s' },
  { left: '86%', top: '28%', size: 2, delay: '-0.6s', dur: '7.2s' },
  { left: '94%', top: '58%', size: 3, delay: '-2.9s', dur: '8.2s' },
  { left: '12%', top: '88%', size: 2, delay: '-4.5s', dur: '6.4s' },
  { left: '23%', top: '45%', size: 3, delay: '-1.4s', dur: '9.5s' },
  { left: '31%', top: '7%', size: 2, delay: '-5.1s', dur: '7.8s' },
  { left: '39%', top: '55%', size: 4, delay: '-2.1s', dur: '6.2s' },
  { left: '47%', top: '35%', size: 2, delay: '-3.3s', dur: '8.8s' },
  { left: '56%', top: '92%', size: 3, delay: '-0.2s', dur: '7.1s' },
  { left: '64%', top: '14%', size: 2, delay: '-4.8s', dur: '9.8s' },
  { left: '72%', top: '89%', size: 4, delay: '-1.7s', dur: '6.6s' },
  { left: '81%', top: '40%', size: 2, delay: '-2.6s', dur: '8.4s' },
  { left: '89%', top: '16%', size: 3, delay: '-5.9s', dur: '7.6s' },
  { left: '97%', top: '80%', size: 2, delay: '-0.8s', dur: '9.1s' },
  { left: '5%', top: '36%', size: 3, delay: '-3.9s', dur: '6.9s' },
  { left: '15%', top: '4%', size: 2, delay: '-2.2s', dur: '8.1s' },
  { left: '25%', top: '72%', size: 4, delay: '-4.3s', dur: '7.3s' },
  { left: '34%', top: '48%', size: 2, delay: '-1.5s', dur: '9.4s' },
  { left: '44%', top: '4%', size: 3, delay: '-5.3s', dur: '6.5s' },
  { left: '53%', top: '44%', size: 2, delay: '-0.4s', dur: '8.6s' },
  { left: '62%', top: '79%', size: 3, delay: '-2.8s', dur: '7.7s' },
  { left: '71%', top: '60%', size: 2, delay: '-4.1s', dur: '6.3s' },
  { left: '80%', top: '5%', size: 4, delay: '-1.9s', dur: '9.6s' },
  { left: '88%', top: '66%', size: 3, delay: '-3.5s', dur: '8.3s' },
  { left: '96%', top: '35%', size: 2, delay: '-5.7s', dur: '7.9s' },
  { left: '10%', top: '55%', size: 3, delay: '-2.5s', dur: '6.1s' },
  { left: '29%', top: '95%', size: 2, delay: '-4.6s', dur: '8.7s' },
  { left: '58%', top: '25%', size: 3, delay: '-1.3s', dur: '7.4s' },
  { left: '75%', top: '96%', size: 2, delay: '-3.1s', dur: '9.3s' },
]

// 品牌区漂浮 emoji，负 delay 让首帧即错开
const floaters = [
  { emoji: '📚', top: '12%', left: '10%', fontSize: '44px', delay: '0s' },
  { emoji: '🚲', top: '68%', left: '14%', fontSize: '40px', delay: '-1.2s' },
  { emoji: '🎧', top: '22%', left: '78%', fontSize: '38px', delay: '-2.4s' },
  { emoji: '💻', top: '78%', left: '72%', fontSize: '42px', delay: '-3.6s' },
  { emoji: '🎸', top: '46%', left: '4%', fontSize: '36px', delay: '-4.8s' },
  { emoji: '🪴', top: '6%', left: '52%', fontSize: '38px', delay: '-6s' },
]

const cardEl = computed(() => shellEl.value?.querySelector('.glass-card') ?? null)

function prefersReducedMotion() {
  return typeof window !== 'undefined' &&
    window.matchMedia &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

/** 只有「宽屏 + 不减弱动效」时才启用视差与 Tilt */
function effectsEnabled() {
  return window.innerWidth > 768 && !prefersReducedMotion()
}

function onPointerMove(e) {
  const shell = shellEl.value
  if (!shell) return

  // 视差：背景层随鼠标反向位移 ±10px
  const nx = (e.clientX / window.innerWidth) * 2 - 1
  const ny = (e.clientY / window.innerHeight) * 2 - 1
  shell.style.setProperty('--parallax-x', `${(-nx * 10).toFixed(2)}px`)
  shell.style.setProperty('--parallax-y', `${(-ny * 10).toFixed(2)}px`)

  // Tilt：卡片绕中心倾斜 ±6°
  const card = cardEl.value
  if (!card) return
  const rect = card.getBoundingClientRect()
  const dx = (e.clientX - (rect.left + rect.width / 2)) / (rect.width / 2)
  const dy = (e.clientY - (rect.top + rect.height / 2)) / (rect.height / 2)
  card.style.setProperty('--tilt-y', `${(dx * 6).toFixed(2)}deg`)
  card.style.setProperty('--tilt-x', `${(-dy * 6).toFixed(2)}deg`)
}

function resetCard() {
  const card = cardEl.value
  if (!card) return
  card.style.setProperty('--tilt-x', '0deg')
  card.style.setProperty('--tilt-y', '0deg')
}

function onPointerLeave() {
  const shell = shellEl.value
  if (shell) {
    shell.style.setProperty('--parallax-x', '0px')
    shell.style.setProperty('--parallax-y', '0px')
  }
  resetCard()
}

function onResize() {
  if (!effectsEnabled()) {
    onPointerLeave()
    return
  }
  window.addEventListener('pointermove', onPointerMove, { passive: true })
  window.addEventListener('pointerleave', onPointerLeave)
}

function detachPointer() {
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerleave', onPointerLeave)
}

onMounted(() => {
  if (effectsEnabled()) {
    window.addEventListener('pointermove', onPointerMove, { passive: true })
    window.addEventListener('pointerleave', onPointerLeave)
    window.addEventListener('resize', onResize)
  }
})

onBeforeUnmount(() => {
  detachPointer()
  window.removeEventListener('resize', onResize)
})
</script>
<template>
  <div class="video-bg" :class="[`mode-${mode}`, { 'is-failed': failed }]" aria-hidden="true">
    <video
      ref="videoEl"
      class="video-el"
      :poster="poster"
      autoplay
      muted
      loop
      playsinline
      preload="metadata"
      @error="onError"
    >
      <source :src="src" type="video/mp4" />
    </video>

    <div v-if="overlay" class="video-overlay" :style="{ background: overlayColor, opacity: overlayOpacity }"></div>

    <button
      v-if="toggle"
      class="video-toggle"
      type="button"
      :aria-label="playing ? '暂停背景视频' : '播放背景视频'"
      :title="playing ? '暂停背景视频' : '播放背景视频'"
      @click="togglePlay"
    >
      <svg v-if="playing" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
        <rect x="6" y="5" width="4" height="14" rx="1" />
        <rect x="14" y="5" width="4" height="14" rx="1" />
      </svg>
      <svg v-else viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
        <path d="M8 5v14l11-7z" />
      </svg>
    </button>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

defineProps({
  src: { type: String, default: '/bg.mp4' },
  poster: { type: String, default: '/bg-poster.jpg' },
  mode: { type: String, default: 'fixed' }, // 'fixed' | 'section'
  toggle: { type: Boolean, default: true },
  overlay: { type: Boolean, default: true },
  overlayColor: { type: String, default: '#0b1a16' },
  overlayOpacity: { type: Number, default: 0.4 }
})

const videoEl = ref(null)
const playing = ref(true)
const failed = ref(false)
let mql = null

function prefersReducedMotion() {
  return typeof window !== 'undefined' &&
    window.matchMedia &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

function pause() {
  if (videoEl.value) {
    videoEl.value.pause()
    playing.value = false
  }
}

function play() {
  const v = videoEl.value
  if (!v) return
  const p = v.play()
  if (p && typeof p.then === 'function') {
    p.then(() => { playing.value = true }).catch(() => { playing.value = false })
  } else {
    playing.value = true
  }
}

function togglePlay() {
  if (playing.value) pause()
  else play()
}

function onError() {
  failed.value = true
  playing.value = false
}

function onVisibilityChange() {
  if (document.hidden) pause()
  else if (!prefersReducedMotion() && !failed.value) play()
}

function onMotionChange(e) {
  if (e.matches) pause()
  else play()
}

onMounted(() => {
  if (prefersReducedMotion()) {
    pause()
  } else {
    play()
  }

  mql = window.matchMedia ? window.matchMedia('(prefers-reduced-motion: reduce)') : null
  if (mql && mql.addEventListener) mql.addEventListener('change', onMotionChange)

  document.addEventListener('visibilitychange', onVisibilityChange)
})

onBeforeUnmount(() => {
  if (mql && mql.removeEventListener) mql.removeEventListener('change', onMotionChange)
  document.removeEventListener('visibilitychange', onVisibilityChange)
})
</script>

<style scoped>
.video-bg {
  overflow: hidden;
  background: #0b1a16;
  background-image: v-bind('`url(${poster})`');
  background-size: cover;
  background-position: center;
}

.video-bg.mode-fixed {
  position: fixed;
  inset: 0;
  z-index: 0;
}

.video-bg.mode-section {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.video-el {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.is-failed .video-el {
  display: none;
}

.video-overlay {
  position: absolute;
  inset: 0;
}

.video-toggle {
  position: absolute;
  right: var(--space-4);
  bottom: var(--space-4);
  z-index: 2;
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-full);
  border: 1px solid rgba(255, 255, 255, 0.35);
  background: rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(8px);
  color: #fff;
  cursor: pointer;
  transition: background var(--dur-fast) var(--ease), border-color var(--dur-fast) var(--ease);
}

.video-toggle:hover {
  background: rgba(0, 0, 0, 0.55);
  border-color: rgba(255, 255, 255, 0.6);
}

.video-toggle svg {
  width: 18px;
  height: 18px;
}

/* mode-fixed：按钮固定在视口右下角 */
.mode-fixed .video-toggle {
  position: fixed;
}

@media (prefers-reduced-motion: reduce) {
  .video-toggle { display: none; }
}
</style>

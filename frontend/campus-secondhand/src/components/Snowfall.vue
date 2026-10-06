<template>
  <canvas ref="canvasEl" class="snowfall" aria-hidden="true"></canvas>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

const canvasEl = ref(null)
let ctx = null
let flakes = []
let animationId = 0
let width = 0
let height = 0
let running = false

function prefersReducedMotion() {
  return window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

function resize() {
  const canvas = canvasEl.value
  if (!canvas) return
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  width = window.innerWidth
  height = window.innerHeight
  canvas.width = Math.floor(width * dpr)
  canvas.height = Math.floor(height * dpr)
  canvas.style.width = `${width}px`
  canvas.style.height = `${height}px`
  ctx = canvas.getContext('2d')
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
}

function createFlakes() {
  const count = Math.max(90, Math.min(130, Math.floor((width * height) / 18000)))
  flakes = Array.from({ length: count }, () => ({
    x: Math.random() * width,
    y: Math.random() * height,
    r: 1 + Math.random() * 2.6,
    speed: 0.6 + Math.random() * 1.8,
    drift: -0.4 + Math.random() * 0.8,
    opacity: 0.35 + Math.random() * 0.5,
  }))
}

function draw() {
  if (!ctx) return
  ctx.clearRect(0, 0, width, height)
  ctx.fillStyle = '#fff'
  for (const flake of flakes) {
    ctx.globalAlpha = flake.opacity
    ctx.beginPath()
    ctx.arc(flake.x, flake.y, flake.r, 0, Math.PI * 2)
    ctx.fill()
  }
  ctx.globalAlpha = 1
}

function tick() {
  if (!running) return
  for (const flake of flakes) {
    flake.y += flake.speed
    flake.x += flake.drift
    if (flake.y > height + flake.r) {
      flake.y = -flake.r
      flake.x = Math.random() * width
    }
    if (flake.x < -flake.r) flake.x = width + flake.r
    if (flake.x > width + flake.r) flake.x = -flake.r
  }
  draw()
  animationId = requestAnimationFrame(tick)
}

function start() {
  if (running || prefersReducedMotion()) return
  running = true
  animationId = requestAnimationFrame(tick)
}

function stop() {
  running = false
  cancelAnimationFrame(animationId)
}

function onResize() {
  resize()
  createFlakes()
}

function onVisibilityChange() {
  if (document.hidden) stop()
  else start()
}

onMounted(() => {
  resize()
  createFlakes()
  draw()
  if (!prefersReducedMotion()) start()
  window.addEventListener('resize', onResize)
  document.addEventListener('visibilitychange', onVisibilityChange)
})

onBeforeUnmount(() => {
  stop()
  window.removeEventListener('resize', onResize)
  document.removeEventListener('visibilitychange', onVisibilityChange)
})
</script>

<style scoped>
.snowfall {
  position: fixed;
  inset: 0;
  z-index: 9999;
  pointer-events: none;
}
</style>

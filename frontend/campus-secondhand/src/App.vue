<script setup>
import { computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppNavbar from '@/components/AppNavbar.vue'
import WallpaperBackground from '@/components/WallpaperBackground.vue'
import Snowfall from '@/components/Snowfall.vue'
import { useMessageNotify } from '@/composables/useMessageNotify'

const route = useRoute()
const showWallpaper = computed(() => Boolean(route.meta?.requiresAuth) && !route.meta?.requiresAdmin)

/**
 * 导航是否显示。
 *
 * <p>全局挂载而非逐页 import：逐页引入就是"每页各写一份"的病根本身，
 * 第 10 个新页面还会再犯。登录/注册/找回密码/404 由 meta.navbar=false 排除，
 * 管理区由 requiresAdmin 排除（保留自己的头）。
 *
 * <p>AppNavbar 内部自带 fixed 导航 + 64px 占位块，所以各页无需再写
 * padding-top 补偿。</p>
 */
const showNavbar = computed(
  () => route.meta?.navbar !== false && !route.meta?.requiresAdmin
)

const { startNotify } = useMessageNotify()
// 未读消息轮询在此单例启动一次，全站共享；不放在各页面
startNotify(10000)

watch(
  showWallpaper,
  (value) => {
    document.body.classList.toggle('has-wallpaper', value)
  },
  { immediate: true },
)
</script>

<template>
  <WallpaperBackground v-if="showWallpaper" />
  <Snowfall v-if="showWallpaper" />
  <AppNavbar v-if="showNavbar" />
  <router-view />
</template>

<style>
html, body, #app {
  height: 100%;
  margin: 0;
}
</style>

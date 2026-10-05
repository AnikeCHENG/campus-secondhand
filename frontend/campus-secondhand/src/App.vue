<script setup>
import { computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import WallpaperBackground from '@/components/WallpaperBackground.vue'
import Snowfall from '@/components/Snowfall.vue'

const route = useRoute()
const showWallpaper = computed(() => Boolean(route.meta?.requiresAuth) && !route.meta?.requiresAdmin)

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
  <router-view />
</template>

<style>
html, body, ***REMOVED***app {
  height: 100%;
  margin: 0;
}
</style>

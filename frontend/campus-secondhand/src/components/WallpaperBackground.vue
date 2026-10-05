<template>
  <div class="wallpaper-bg" aria-hidden="true">
    <div class="wallpaper-img"></div>
    <div class="wallpaper-overlay"></div>
  </div>
</template>

<style scoped>
.wallpaper-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
}

.wallpaper-img {
  position: absolute;
  inset: 0;
  background-image: url('/wallpaper.jpg');
  background-size: cover;
  background-position: center;
}

.wallpaper-overlay {
  position: absolute;
  inset: 0;
  background: rgba(8, 14, 20, 0.5);
}
</style>

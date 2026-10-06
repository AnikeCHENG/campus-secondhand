<template>
  <div class="page nf-page">
    <div class="nf-wrap">
      <p class="eyebrow">404</p>
      <h1 class="nf-title">页面不存在</h1>
      <p class="nf-desc">你访问的地址可能已失效或输入有误。</p>
      <div class="nf-actions">
        <router-link to="/" class="btn btn-primary">返回首页</router-link>
        <router-link to="/products" class="btn btn-outline">去逛逛商品</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
// 404 页面无需访问路由参数，仅依赖 router-link 声明式跳转
</script>

<style scoped>
.nf-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: var(--space-6);
}

.nf-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
  text-align: center;
  padding: var(--space-16) var(--space-8);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  max-width: 460px;
}

.nf-title {
  font-size: var(--text-2xl);
  font-weight: var(--weight-semibold);
  letter-spacing: -0.02em;
  color: var(--text);
}

.nf-desc {
  font-size: var(--text-sm);
  color: var(--text-2);
}

.nf-actions {
  display: flex;
  gap: var(--space-3);
  margin-top: var(--space-4);
}
</style>

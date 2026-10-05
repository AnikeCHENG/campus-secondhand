<template>
  <section class="hero">
    <div class="container hero-inner">
      <div class="hero-left">
        <p class="eyebrow">校园二手</p>
        <h1 class="hero-title">欢迎回到校园二手平台</h1>
        <p class="lead">安全、便捷、环保 —— 买卖闲置，延长物品寿命。</p>
        <div class="hero-actions">
          <el-input
            v-model="query"
            placeholder="搜索想要的物品"
            clearable
            class="search-input"
            @keyup.enter="onSearch"
          >
            <template ***REMOVED***append>
              <el-button type="primary" @click="onSearch">搜索</el-button>
            </template>
          </el-input>
          <button class="btn btn-outline" type="button" @click="go('/post')">我要发布</button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
const router = useRouter()
const query = ref('')
function onSearch() {
  if (query.value) router.push({ path: '/products', query: { search: query.value } })
}
function go(path) {
  router.push(path)
}
</script>

<style scoped>
.hero {
  padding: var(--space-12) 0 var(--space-10);
  background: var(--surface);
  border-bottom: 1px solid var(--border);
}
.hero-inner { display: flex; align-items: center; justify-content: space-between; gap: var(--space-8); }
.hero-left { max-width: 640px; }
.eyebrow {
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--accent);
  margin-bottom: var(--space-3);
}
.hero-title {
  font-size: var(--text-3xl);
  font-weight: var(--weight-semibold);
  letter-spacing: -0.02em;
  color: var(--text);
  margin-bottom: var(--space-3);
}
.lead { color: var(--text-2); margin-bottom: var(--space-6); }
.hero-actions { display: flex; align-items: center; gap: var(--space-3); }
.search-input { width: 360px; }
@media (max-width: 900px) {
  .hero-inner { flex-direction: column; align-items: flex-start; }
  .search-input { width: 100%; }
}
</style>

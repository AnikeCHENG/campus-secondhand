<template>
  <div class="admin">
    <div class="page-main">
      <header class="page-head">
        <div>
          <p class="eyebrow">ADMIN</p>
          <h1 class="page-title">管理控制台</h1>
          <p class="page-sub">欢迎回来，{{ user?.username }}</p>
        </div>
        <router-link to="/" class="btn btn-outline">返回首页</router-link>
      </header>

      <nav class="admin-menu" aria-label="管理菜单">
        <router-link to="/admin/users" class="menu-item">
          <span class="menu-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M23 21v-2a4 4 0 0 0-3-3.87" /><path d="M16 3.13a4 4 0 0 1 0 7.75" /></svg>
          </span>
          <span>用户管理</span>
        </router-link>
        <router-link to="/admin/products" class="menu-item">
          <span class="menu-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z" /><line x1="3" y1="6" x2="21" y2="6" /><path d="M16 10a4 4 0 0 1-8 0" /></svg>
          </span>
          <span>商品管理</span>
        </router-link>
        <router-link to="/admin/orders" class="menu-item">
          <span class="menu-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" /><polyline points="14 2 14 8 20 8" /><line x1="16" y1="13" x2="8" y2="13" /><line x1="16" y1="17" x2="8" y2="17" /></svg>
          </span>
          <span>订单管理</span>
        </router-link>
        <router-link to="/admin/categories" class="menu-item">
          <span class="menu-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" /></svg>
          </span>
          <span>分类管理</span>
        </router-link>
        <router-link to="/admin/messages" class="menu-item">
          <span class="menu-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" /></svg>
          </span>
          <span>消息管理</span>
        </router-link>
      </nav>

      <div class="admin-stats">
        <div class="stat-card card">
          <h3>用户总数</h3>
          <p class="stat-number">{{ userCount }}</p>
        </div>
        <div class="stat-card card">
          <h3>商品总数</h3>
          <p class="stat-number">{{ productCount }}</p>
        </div>
        <div class="stat-card card">
          <h3>订单总数</h3>
          <p class="stat-number">{{ orderCount }}</p>
        </div>
        <div class="stat-card card">
          <h3>消息总数</h3>
          <p class="stat-number">{{ messageCount }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const user = ref(JSON.parse(localStorage.getItem('user') || sessionStorage.getItem('user') || '{}'))
const userCount = ref(0)
const productCount = ref(0)
const orderCount = ref(0)
const messageCount = ref(0)

const fetchStats = async () => {
  try {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token')
    const statsResponse = await axios.get('/api/admin/stats', {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (statsResponse.data && statsResponse.data.data) {
      userCount.value = statsResponse.data.data.userCount || 0
      productCount.value = statsResponse.data.data.productCount || 0
      orderCount.value = statsResponse.data.data.orderCount || 0
      messageCount.value = statsResponse.data.data.messageCount || 0
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

onMounted(fetchStats)
</script>

<style scoped>
.page-head { display: flex; align-items: flex-end; justify-content: space-between; gap: var(--space-4); margin-bottom: var(--space-6); }
.page-title { font-size: var(--text-2xl); font-weight: var(--weight-semibold); letter-spacing: -0.02em; }
.page-sub { margin-top: var(--space-1); color: var(--text-2); font-size: var(--text-sm); }

.admin-menu { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: var(--space-3); margin-bottom: var(--space-8); }
.menu-item { display: flex; flex-direction: column; align-items: center; gap: var(--space-3); padding: var(--space-5); background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-lg); color: var(--text); font-size: var(--text-sm); font-weight: var(--weight-medium); transition: border-color var(--dur) var(--ease), background var(--dur) var(--ease); }
.menu-item:hover { border-color: var(--accent); color: var(--accent); background: var(--accent-soft); }
.menu-icon { width: 40px; height: 40px; display: inline-flex; align-items: center; justify-content: center; border-radius: var(--radius); background: var(--surface-3); color: var(--text-2); }
.menu-item:hover .menu-icon { background: var(--accent-soft-strong); color: var(--accent); }
.menu-icon svg { width: 20px; height: 20px; }

.admin-stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: var(--space-5); }
.stat-card { text-align: left; padding: var(--space-6); }
.stat-card h3 { font-size: var(--text-sm); color: var(--text-2); font-weight: var(--weight-medium); margin-bottom: var(--space-3); }
.stat-number { font-size: var(--text-3xl); font-weight: var(--weight-semibold); letter-spacing: -0.03em; color: var(--text); }
</style>

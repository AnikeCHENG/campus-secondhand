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
        <router-link to="/admin/reports" class="menu-item">
          <span class="menu-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 21V4a2 2 0 0 1 2-2h12l4 4v15a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2z" /><path d="M16 2v4h4" /><path d="M12 12v4" /><path d="M10 14h4" /></svg>
          </span>
          <span>举报管理</span>
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

      <!-- 财务概览：呼应平台 0.3% 佣金设计 -->
      <div class="admin-stats admin-stats--finance">
        <div class="stat-card card">
          <h3>累计成交额（GMV）</h3>
          <p class="stat-number">¥{{ finance.totalGmv ?? '0.00' }}</p>
          <span class="stat-hint">已支付且未取消的订单</span>
        </div>
        <div class="stat-card card stat-card--accent">
          <h3>平台手续费累计</h3>
          <p class="stat-number">¥{{ finance.totalServiceFee ?? '0.00' }}</p>
          <span class="stat-hint">费率 {{ finance.serviceFeeRate || '0.3%' }}，实际综合 {{ finance.effectiveRate || '—' }}</span>
        </div>
        <div class="stat-card card">
          <h3>卖家应收合计</h3>
          <p class="stat-number">¥{{ finance.sellerIncomeTotal ?? '0.00' }}</p>
          <span class="stat-hint">成交额扣除平台手续费</span>
        </div>
        <div class="stat-card card">
          <h3>已支付订单</h3>
          <p class="stat-number">{{ finance.paidOrderCount ?? 0 }}</p>
          <span class="stat-hint">待支付与已取消不计入</span>
        </div>
      </div>

      <!-- 数据大屏 -->
      <div class="chart-grid">
        <div class="card chart-card chart-card--wide">
          <div class="chart-head">
            <h3 class="section-title">近 30 天新增趋势</h3>
            <p class="section-sub">用户 / 商品 / 订单每日增量</p>
          </div>
          <div ref="trendRef" class="chart-body"></div>
        </div>

        <div class="card chart-card">
          <div class="chart-head">
            <h3 class="section-title">商品分类热度</h3>
            <p class="section-sub">按在架与已售出商品数量统计</p>
          </div>
          <div ref="categoryRef" class="chart-body"></div>
        </div>

        <div class="card chart-card">
          <div class="chart-head">
            <h3 class="section-title">近 6 个月手续费</h3>
            <p class="section-sub">按支付时间归集</p>
          </div>
          <div ref="feeRef" class="chart-body"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import axios from 'axios'
import * as echarts from 'echarts/core'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import {
  GridComponent, TooltipComponent, LegendComponent, TitleComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  LineChart, BarChart, PieChart,
  GridComponent, TooltipComponent, LegendComponent, TitleComponent,
  CanvasRenderer
])

const user = ref(JSON.parse(localStorage.getItem('user') || sessionStorage.getItem('user') || '{}'))
const userCount = ref(0)
const productCount = ref(0)
const orderCount = ref(0)
const messageCount = ref(0)

const finance = ref({})

const trendRef = ref(null)
const categoryRef = ref(null)
const feeRef = ref(null)
let trendChart = null
let categoryChart = null
let feeChart = null

const authHeaders = () => {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  return { headers: { Authorization: `Bearer ${token}` } }
}

const fetchStats = async () => {
  try {
    const statsResponse = await axios.get('/api/admin/stats', authHeaders())
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

const fetchFinance = async () => {
  try {
    const res = await axios.get('/api/admin/stats/finance', authHeaders())
    if (res.data && res.data.code === 200) finance.value = res.data.data || {}
  } catch (error) {
    console.error('获取财务统计失败:', error)
  }
}

// 配色沿用 theme.css 既有令牌，不引入新色系
const COLORS = {
  accent: '#0b6e54',
  accentSoft: 'rgba(11,110,84,0.14)',
  warning: '#b07d12',
  info: '#2563eb',
  text2: '#6b6b64',
  text3: '#9a9a92',
  border: '#e7e7e2'
}
const PIE_COLORS = ['#0b6e54', '#b07d12', '#2563eb', '#6b6b64', '#9a9a92', '#1f8a4c', '#c0392b', '#d8d8d2']

const baseTooltip = {
  trigger: 'axis',
  axisPointer: { type: 'line' },
  textStyle: { color: COLORS.text2, fontSize: 12 }
}

async function renderTrend() {
  await nextTick()
  if (!trendRef.value) return
  let series = { userSeries: [], productSeries: [], orderSeries: [] }
  try {
    const res = await axios.get('/api/admin/stats/trend?days=30', authHeaders())
    if (res.data && res.data.code === 200) series = res.data.data || series
  } catch (error) {
    console.error('获取趋势失败:', error)
  }
  const dates = (series.userSeries || []).map((d) => d.date.slice(5))
  const pick = (key) => (series[key] || []).map((d) => d.count)

  trendChart = echarts.init(trendRef.value)
  trendChart.setOption({
    tooltip: baseTooltip,
    legend: { data: ['新增用户', '新增商品', '新增订单'], textStyle: { color: COLORS.text2 }, top: 0 },
    grid: { left: 40, right: 16, top: 36, bottom: 28 },
    xAxis: {
      type: 'category', data: dates, boundaryGap: false,
      axisLine: { lineStyle: { color: COLORS.border } },
      axisLabel: { color: COLORS.text3, fontSize: 11 }
    },
    yAxis: {
      type: 'value', minInterval: 1,
      axisLine: { show: false },
      axisLabel: { color: COLORS.text3, fontSize: 11 },
      splitLine: { lineStyle: { color: COLORS.border } }
    },
    series: [
      { name: '新增用户', type: 'line', smooth: true, showSymbol: false, data: pick('userSeries'), lineStyle: { color: COLORS.accent, width: 2 }, itemStyle: { color: COLORS.accent }, areaStyle: { color: COLORS.accentSoft } },
      { name: '新增商品', type: 'line', smooth: true, showSymbol: false, data: pick('productSeries'), lineStyle: { color: COLORS.warning, width: 2 }, itemStyle: { color: COLORS.warning } },
      { name: '新增订单', type: 'line', smooth: true, showSymbol: false, data: pick('orderSeries'), lineStyle: { color: COLORS.info, width: 2 }, itemStyle: { color: COLORS.info } }
    ]
  })
}

async function renderCategory() {
  await nextTick()
  if (!categoryRef.value) return
  let rows = []
  try {
    const res = await axios.get('/api/admin/stats/category', authHeaders())
    if (res.data && res.data.code === 200) rows = res.data.data || []
  } catch (error) {
    console.error('获取分类热度失败:', error)
  }
  categoryChart = echarts.init(categoryRef.value)
  categoryChart.setOption({
    tooltip: { trigger: 'item', textStyle: { color: COLORS.text2, fontSize: 12 } },
    legend: { orient: 'vertical', right: 0, top: 'center', textStyle: { color: COLORS.text2, fontSize: 12 } },
    series: [{
      type: 'pie',
      radius: ['45%', '68%'],
      center: ['38%', '50%'],
      avoidLabelOverlap: true,
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      data: rows.map((r) => ({ name: r.label || r.category, value: r.productCount || 0 })),
      color: PIE_COLORS
    }]
  })
}

async function renderFee() {
  await nextTick()
  if (!feeRef.value) return
  const months = (finance.value.monthlyTrend || [])
  feeChart = echarts.init(feeRef.value)
  feeChart.setOption({
    tooltip: baseTooltip,
    grid: { left: 52, right: 16, top: 20, bottom: 28 },
    xAxis: {
      type: 'category',
      data: months.map((m) => m.month.slice(2)),
      axisLine: { lineStyle: { color: COLORS.border } },
      axisLabel: { color: COLORS.text3, fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisLabel: { color: COLORS.text3, fontSize: 11 },
      splitLine: { lineStyle: { color: COLORS.border } }
    },
    series: [{
      type: 'bar',
      barMaxWidth: 28,
      data: months.map((m) => Number(m.serviceFee || 0)),
      itemStyle: { color: COLORS.accent, borderRadius: [4, 4, 0, 0] }
    }]
  })
}

function resizeCharts() {
  trendChart?.resize()
  categoryChart?.resize()
  feeChart?.resize()
}

onMounted(async () => {
  await fetchStats()
  await fetchFinance()
  await renderTrend()
  await renderCategory()
  await renderFee()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  trendChart?.dispose()
  categoryChart?.dispose()
  feeChart?.dispose()
})
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

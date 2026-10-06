<template>
  <div class="page">
    <!-- 顶部导航 -->
    <header class="site-header">
      <div class="container header-inner">
        <router-link to="/" class="brand">
          <span class="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 12a9 9 0 0 1 15-6.7L21 8" />
              <path d="M21 3v5h-5" />
              <path d="M21 12a9 9 0 0 1-15 6.7L3 16" />
              <path d="M3 21v-5h5" />
            </svg>
          </span>
          <span class="brand-name">校园二手</span>
        </router-link>

        <nav class="main-nav" aria-label="主导航">
          <router-link to="/" class="nav-link">首页</router-link>
          <router-link to="/plaza" class="nav-link">大厅</router-link>
          <router-link to="/products" class="nav-link">商品</router-link>
          <router-link to="/post" class="nav-link">发布</router-link>
          <router-link to="/messages" class="nav-link">消息</router-link>
          <router-link to="/profile" class="nav-link">我的</router-link>
          <router-link v-if="user.role === 1" to="/admin" class="nav-link">管理</router-link>
        </nav>

        <div class="header-actions">
          <button class="icon-btn" type="button" aria-label="搜索" @click="toggleSearch">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
          </button>

          <button class="icon-btn" type="button" aria-label="消息" @click="go('/messages')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
              <path d="M13.73 21a2 2 0 0 1-3.46 0" />
            </svg>
            <span v-if="notificationStore.unreadCount > 0" class="notify-badge">
              {{ notificationStore.unreadCount > 99 ? '99+' : notificationStore.unreadCount }}
            </span>
          </button>

          <button class="icon-btn" type="button" :aria-label="soundEnabled ? '关闭提示音' : '开启提示音'" @click="toggleSound">
            <svg v-if="soundEnabled" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path d="M11 5 6 9H2v6h4l5 4V5z" />
              <path d="M15.54 8.46a5 5 0 0 1 0 7.07" />
              <path d="M19.07 4.93a10 10 0 0 1 0 14.14" />
            </svg>
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path d="M11 5 6 9H2v6h4l5 4V5z" />
              <line x1="23" y1="9" x2="17" y2="15" />
              <line x1="17" y1="9" x2="23" y2="15" />
            </svg>
          </button>

          <button class="icon-btn" type="button" aria-label="退出登录" @click="handleLogout">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
              <polyline points="16 17 21 12 16 7" />
              <line x1="21" y1="12" x2="9" y2="12" />
            </svg>
          </button>

          <button class="avatar" type="button" aria-label="个人中心" @click="go('/profile')">
            <img
              src="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 40 40'%3E%3Crect width='40' height='40' fill='%230b6e54'/%3E%3Ccircle cx='20' cy='16' r='6' fill='%23ffffff'/%3E%3Cpath d='M8 36c0-6.6 5.4-12 12-12s12 5.4 12 12' fill='%23ffffff'/%3E%3C/svg%3E"
              alt="用户头像"
            />
          </button>
        </div>
      </div>
    </header>

    <!-- 搜索弹层 -->
    <div v-if="showSearch" class="search-overlay" @click.self="toggleSearch" role="dialog" aria-label="搜索">
      <div class="search-panel">
        <div class="search-bar">
          <svg class="search-bar-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <circle cx="11" cy="11" r="8" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input
            ref="searchInput"
            v-model="searchKeyword"
            type="text"
            placeholder="搜索商品名称、描述…"
            aria-label="搜索关键词"
            @keyup.enter="handleSearch"
          />
          <button v-if="searchKeyword" class="clear-btn" type="button" aria-label="清空" @click="clearSearch">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
          <button class="btn btn-primary btn-sm" type="button" @click="handleSearch">搜索</button>
        </div>

        <div class="search-body">
          <section v-if="searchHistory.length > 0 && !searchKeyword" class="search-section">
            <div class="search-section-head">
              <span class="eyebrow">搜索历史</span>
              <button class="link-btn" type="button" @click="clearSearchHistory">清空</button>
            </div>
            <button
              v-for="(item, index) in searchHistory.slice(0, 8)"
              :key="`history-${index}`"
              class="search-item"
              type="button"
              @click="selectSearchItem(item)"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <circle cx="12" cy="12" r="10" />
                <polyline points="12 6 12 12 16 14" />
              </svg>
              <span>{{ item }}</span>
            </button>
          </section>

          <section v-if="searchSuggestions.length > 0" class="search-section">
            <div class="search-section-head">
              <span class="eyebrow">搜索建议</span>
            </div>
            <button
              v-for="(item, index) in searchSuggestions"
              :key="`suggestion-${index}`"
              class="search-item"
              type="button"
              @click="selectSearchItem(item)"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
              <span>{{ item }}</span>
            </button>
          </section>

          <div v-if="!searchKeyword && searchHistory.length === 0" class="search-empty">
            <p>输入关键词搜索商品</p>
          </div>
        </div>
      </div>
    </div>

    <main class="page-main">
      <!-- Hero -->
      <section class="hero">
        <VideoBackground mode="section" :overlay-opacity="0.5" />
        <div class="hero-copy">
          <p class="eyebrow">校园二手交易平台</p>
          <h1 class="hero-title">
            让闲置物品<br />找到新的主人
          </h1>
          <p class="hero-sub">环保从循环开始 —— 发布、浏览、交易，一站完成。</p>
          <div class="hero-actions">
            <button class="btn btn-primary" type="button" @click="go('/post')">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <line x1="12" y1="5" x2="12" y2="19" />
                <line x1="5" y1="12" x2="19" y2="12" />
              </svg>
              发布闲置
            </button>
            <button class="btn btn-outline" type="button" @click="go('/products')">
              逛一逛
            </button>
          </div>
        </div>

        <dl class="hero-stats">
          <div class="stat">
            <dt class="stat-label">活跃用户</dt>
            <dd class="stat-value">2,580</dd>
          </div>
          <div class="stat">
            <dt class="stat-label">在售商品</dt>
            <dd class="stat-value">1,245</dd>
          </div>
          <div class="stat">
            <dt class="stat-label">成交订单</dt>
            <dd class="stat-value">8,920</dd>
          </div>
        </dl>
      </section>

      <!-- 数据卡片 -->
      <section class="dashboard-row">
        <dashboard-card
          v-for="(c, i) in cards"
          :key="i"
          :title="c.title"
          :value="c.value"
          :icon="c.icon"
          :color="c.color"
          :clickable="true"
          @click="handleCardClick(c)"
        />
        <div class="card active-users-card">
          <div class="section-head">
            <h2 class="section-title">活跃用户</h2>
          </div>
          <div v-if="!activeUsers.length" class="active-user-empty">暂无近期活跃</div>
          <div v-for="u in activeUsers" :key="u.id" class="active-user-row">
            <img :src="u.avatar || defaultAvatar" alt="头像" />
            <span>{{ u.username }}</span>
          </div>
        </div>
      </section>

      <!-- 内容网格 -->
      <div class="content-grid">
        <div class="main-column">
          <section class="card">
            <div class="section-head">
              <h2 class="section-title">最新上架</h2>
              <button class="link-btn" type="button" @click="viewAllProducts">查看全部 →</button>
            </div>
            <RecentItems :items="recentItems" @view="viewProduct" />
          </section>

          <section class="card">
            <div class="section-head">
              <h2 class="section-title">活动与公告</h2>
            </div>
            <div class="activities">
              <button
                v-for="(a, idx) in activities"
                :key="idx"
                class="activity"
                type="button"
                @click="showActivityDetail(a)"
              >
                <div class="activity-date">
                  <span class="activity-day">{{ getDay(a.time) }}</span>
                  <span class="activity-month">{{ getMonth(a.time) }}</span>
                </div>
                <div class="activity-body">
                  <h3 class="activity-title">{{ a.title }}</h3>
                  <p class="activity-desc">{{ a.desc }}</p>
                </div>
                <span class="badge" :class="idx === 0 ? 'badge-accent' : ''">
                  {{ idx === 0 ? '热门' : '公告' }}
                </span>
              </button>
            </div>
          </section>
        </div>

        <aside class="side-column">
          <section class="card">
            <div class="section-head">
              <h2 class="section-title">为你推荐</h2>
            </div>
            <Recommendations :items="recommendations" @click="handleRecommendationClick" />
          </section>

          <section class="card">
            <div class="section-head">
              <h2 class="section-title">快速操作</h2>
            </div>
            <div class="quick-grid">
              <button class="quick" type="button" @click="go('/post')">
                <span class="quick-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                    <line x1="12" y1="5" x2="12" y2="19" />
                    <line x1="5" y1="12" x2="19" y2="12" />
                  </svg>
                </span>
                发布闲置
              </button>
              <button class="quick" type="button" @click="go('/messages')">
                <span class="quick-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                  </svg>
                </span>
                我的消息
              </button>
              <button class="quick" type="button" @click="go('/profile')">
                <span class="quick-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                    <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                  </svg>
                </span>
                我的收藏
              </button>
              <button class="quick" type="button" @click="go('/profile')">
                <span class="quick-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                    <polyline points="14 2 14 8 20 8" />
                  </svg>
                </span>
                我的订单
              </button>
            </div>
          </section>

          <section class="card">
            <div class="section-head">
              <h2 class="section-title">热门分类</h2>
            </div>
            <div class="tags">
              <button class="tag" type="button" @click="go('/products')">教材书籍</button>
              <button class="tag" type="button" @click="go('/products')">电子产品</button>
              <button class="tag" type="button" @click="go('/products')">出行工具</button>
              <button class="tag" type="button" @click="go('/products')">游戏数码</button>
              <button class="tag" type="button" @click="go('/products')">服饰穿搭</button>
              <button class="tag" type="button" @click="go('/products')">生活用品</button>
            </div>
          </section>
        </aside>
      </div>
    </main>

    <footer class="site-footer">
      <div class="container footer-inner">
        <div class="footer-brand">
          <span class="brand-name">校园二手</span>
          <p>让闲置物品重新发挥价值</p>
        </div>
        <div class="footer-cols">
          <div class="footer-col">
            <h4>快速链接</h4>
            <router-link to="/">首页</router-link>
            <router-link to="/products">商品列表</router-link>
            <router-link to="/post">发布商品</router-link>
          </div>
          <div class="footer-col">
            <h4>帮助中心</h4>
            <a href="#">常见问题</a>
            <a href="#">交易指南</a>
            <a href="#">联系我们</a>
          </div>
        </div>
      </div>
      <div class="footer-bottom">
        <div class="container">© 2025 校园二手交易平台</div>
      </div>
    </footer>

    <!-- 活动详情弹层 -->
    <div v-if="showActivityModal" class="search-overlay" @click.self="closeActivityModal" role="dialog" aria-label="活动详情">
      <div class="modal-panel">
        <div class="modal-head">
          <span class="badge" :class="currentActivity.isNew ? 'badge-accent' : ''">
            {{ currentActivity.isNew ? '热门' : '公告' }}
          </span>
          <button class="icon-btn" type="button" aria-label="关闭" @click="closeActivityModal">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <p class="eyebrow">{{ currentActivity.time }}</p>
          <h2 class="modal-title">{{ currentActivity.title }}</h2>
          <p class="modal-desc">{{ currentActivity.fullDesc || currentActivity.desc }}</p>
          <ul v-if="currentActivity.details" class="modal-list">
            <li v-for="(detail, idx) in currentActivity.details" :key="idx">{{ detail }}</li>
          </ul>
        </div>
        <div class="modal-foot">
          <button class="btn btn-outline" type="button" @click="closeActivityModal">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import DashboardCard from '../components/DashboardCard.vue'
import RecentItems from '../components/RecentItems.vue'
import Recommendations from '../components/Recommendations.vue'
import VideoBackground from '../components/VideoBackground.vue'
import { ref, computed, onMounted, nextTick } from 'vue'
import { logout } from '../api/auth'
import { getProductList } from '../api/product'
import { getDashboardStats } from '../api/dashboard'
import { useNotificationStore } from '../stores/notification'
import { useUserStore } from '../stores/user'
import { useMessageNotify } from '../composables/useMessageNotify'

const router = useRouter()
const notificationStore = useNotificationStore()
const userStore = useUserStore()
const { soundEnabled, toggleSound } = useMessageNotify()

const showSearch = ref(false)
const searchKeyword = ref('')
const searchHistory = ref([])
const allProducts = ref([])
const searchInput = ref(null)
const showActivityModal = ref(false)
const currentActivity = ref({})
const user = ref(JSON.parse(localStorage.getItem('user') || sessionStorage.getItem('user') || '{"role": 0}'))

function go(path) { router.push(path) }

function handleCardClick(card) {
  switch (card.icon) {
    case 'shopping':
      router.push('/products')
      break
    case 'order':
      router.push('/profile')
      break
    case 'message':
      router.push('/messages')
      break
    case 'user':
      router.push('/profile')
      break
  }
}

function viewProduct(item) {
  if (item.id) {
    router.push(`/products/${item.id}`)
  } else {
    router.push('/products')
  }
}

function viewAllProducts() {
  router.push('/products')
}

function showActivityDetail(activity) {
  currentActivity.value = activity
  showActivityModal.value = true
}

function closeActivityModal() {
  showActivityModal.value = false
  currentActivity.value = {}
}

function handleRecommendationClick(recommendation) {
  router.push({
    path: '/products',
    query: { category: recommendation.category }
  })
}

const searchSuggestions = computed(() => {
  if (!searchKeyword.value || searchKeyword.value.length < 1) {
    return []
  }

  const keyword = searchKeyword.value.toLowerCase()
  const suggestions = new Set()

  allProducts.value.forEach(product => {
    if (product.title && product.title.toLowerCase().includes(keyword)) {
      suggestions.add(product.title)
    }
    if (product.description && product.description.toLowerCase().includes(keyword)) {
      const words = product.description.split(/\s+/).filter(word =>
        word.toLowerCase().includes(keyword) && word.length > 1
      )
      words.forEach(word => suggestions.add(word))
    }
  })

  return Array.from(suggestions).slice(0, 6)
})

async function toggleSearch() {
  showSearch.value = !showSearch.value
  if (showSearch.value) {
    await nextTick()
    if (searchInput.value) {
      searchInput.value.focus()
    }
    await loadProducts()
  }
}

function clearSearch() {
  searchKeyword.value = ''
  if (searchInput.value) {
    searchInput.value.focus()
  }
}

function selectSearchItem(item) {
  searchKeyword.value = item
  handleSearch()
}

function handleSearch() {
  if (searchKeyword.value.trim()) {
    saveSearchHistory(searchKeyword.value)
    showSearch.value = false
    router.push({
      path: '/products',
      query: { search: searchKeyword.value }
    })
  }
}

function saveSearchHistory(keyword) {
  if (!keyword.trim()) return

  const index = searchHistory.value.indexOf(keyword)
  if (index > -1) {
    searchHistory.value.splice(index, 1)
  }

  searchHistory.value.unshift(keyword)
  searchHistory.value = searchHistory.value.slice(0, 10)

  localStorage.setItem('homeSearchHistory', JSON.stringify(searchHistory.value))
}

function clearSearchHistory() {
  searchHistory.value = []
  localStorage.removeItem('homeSearchHistory')
}

function loadSearchHistory() {
  const saved = localStorage.getItem('homeSearchHistory')
  if (saved) {
    try {
      searchHistory.value = JSON.parse(saved)
    } catch (e) {
      console.error('Failed to parse search history:', e)
    }
  }
}

async function loadProducts() {
  try {
    const res = await getProductList()
    if (res.code === 200 && res.data) {
      allProducts.value = res.data
      recentItems.value = res.data.slice(0, 3).map(item => ({
        id: item.id,
        title: item.title,
        price: `¥${item.price}`,
        img: item.images || ''
      }))
    }
  } catch (e) {
    console.error('Failed to load products:', e)
  }
}

async function loadDashboard() {
  try {
    const res = await getDashboardStats()
    if (res.code === 200 && res.data) {
      cards.value[0].value = res.data.sellingCount ?? 0
      cards.value[1].value = res.data.pendingOrderCount ?? 0
      cards.value[2].value = res.data.unreadMessageCount ?? 0
      activeUsers.value = res.data.activeUsers || []
    }
  } catch (e) {
    console.error('Failed to load dashboard stats:', e)
  }
}

async function handleLogout() {
  try {
    await logout()
  } catch {
    console.log('Logout API call completed')
  }
  // 轮询由 App.vue 的 useMessageNotify 统一负责，登出后其 poll 因无 token
  // 自动跳过请求；这里只需清掉徽标
  notificationStore.resetUnreadCount()
  // 清空身份缓存，避免上一位用户的 role 残留在内存里
  userStore.reset()
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  sessionStorage.removeItem('justLoggedIn')
  router.push('/login')
}

const cards = ref([
  { title: '待售商品', value: 0, icon: 'shopping', color: '#ff7a45' },
  { title: '待处理订单', value: 0, icon: 'order', color: '#36b37e' },
  { title: '未读私信', value: 0, icon: 'message', color: '#5b8ff9' },
])
const activeUsers = ref([])
const defaultAvatar = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"%3E%3Crect width="40" height="40" fill="%231b1b18"/%3E%3Ccircle cx="20" cy="16" r="6" fill="%23ffffff"/%3E%3Cpath d="M8 36c0-6.6 5.4-12 12-12s12 5.4 12 12" fill="%23ffffff"/%3E%3C/svg%3E'

const recentItems = ref([])

const recommendations = ref([
  { title: '环保小物合集', desc: '省钱又环保的好选择', category: 'living' },
  { title: '闲置电子产品专场', desc: '高价回收 / 低价购入', category: 'electronics' }
])

const activities = ref([
  {
    time: '2026-01-01',
    title: '新年活动：二手换购',
    desc: '参与换购赢取积分',
    isNew: true,
    fullDesc: '为庆祝新年的到来，平台特别推出二手换购活动！所有用户均可参与，用你的闲置物品换取你需要的物品，还能额外获得平台积分奖励！',
    details: [
      '活动时间：2026年1月1日 - 2026年1月15日',
      '参与方式：发布闲置物品即可获得参与资格',
      '积分奖励：每成功交易一次可获得100积分',
      '积分用途：可用于抵扣平台服务费'
    ]
  },
  {
    time: '2026-01-03',
    title: '平台维护公告',
    desc: '将于周末进行短暂维护',
    isNew: false,
    fullDesc: '为了给用户提供更好的服务体验，平台将于本周末进行系统升级维护。维护期间部分功能可能无法正常使用，请用户提前做好安排。',
    details: [
      '维护时间：2026年1月4日 22:00 - 2026年1月5日 06:00',
      '影响范围：商品发布、消息发送功能',
      '备用方案：维护期间可浏览商品，无法进行交易',
      '补偿措施：维护后所有用户将获得50积分补偿'
    ]
  },
  {
    time: '2025-12-28',
    title: '冬季保暖用品',
    desc: '棉被棉衣低价出售',
    isNew: false,
    fullDesc: '天气转凉，平台特别推出冬季保暖用品专区！各类棉被、棉衣、暖手宝等商品低价出售，让你温暖过冬！',
    details: [
      '专区商品：棉被、羽绒服、毛衣、暖手宝等',
      '优惠活动：专区商品满100减20',
      '品质保证：所有商品均经过平台审核',
      '售后保障：7天无理由退换'
    ]
  }
])

onMounted(() => {
  // 未读消息轮询统一由 App.vue 的 useMessageNotify 负责（10 秒一次），
  // 它会通过 store.setUnreadCount 更新徽标。此处不再重复调用
  // notificationStore.startPolling，否则同一接口会被轮询两次。
  loadSearchHistory()
  loadProducts()
  loadDashboard()
})

function getDay(time) {
  return time.split('-')[2]
}

function getMonth(time) {
  const months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
  return months[parseInt(time.split('-')[1]) - 1]
}
</script>

<style scoped>
/* ---------- 顶部导航 ---------- */
.site-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: saturate(180%) blur(12px);
  border-bottom: 1px solid var(--border);
}

.header-inner {
  height: var(--header-h);
  display: flex;
  align-items: center;
  gap: var(--space-8);
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--text);
  flex-shrink: 0;
}
.brand:hover { color: var(--text); }

.brand-mark {
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  background: var(--accent);
  color: #fff;
}
.brand-mark svg { width: 17px; height: 17px; }

.brand-name {
  font-size: var(--text-lg);
  font-weight: var(--weight-semibold);
  letter-spacing: -0.01em;
}

.main-nav {
  display: flex;
  gap: var(--space-1);
  flex: 1;
}

.nav-link {
  padding: 8px 12px;
  font-size: var(--text-base);
  color: var(--text-2);
  border-radius: var(--radius-sm);
  transition: color var(--dur-fast) var(--ease), background var(--dur-fast) var(--ease);
}
.nav-link:hover { color: var(--text); background: var(--surface-3); }
.nav-link.router-link-exact-active { color: var(--accent); font-weight: var(--weight-medium); }

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.icon-btn {
  position: relative;
  width: 38px;
  height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid transparent;
  border-radius: var(--radius);
  background: transparent;
  color: var(--text-2);
  transition: background var(--dur-fast) var(--ease), color var(--dur-fast) var(--ease);
}
.icon-btn:hover { background: var(--surface-3); color: var(--text); }
.icon-btn svg { width: 20px; height: 20px; }

.notify-badge {
  position: absolute;
  top: 3px;
  right: 3px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: var(--radius-full);
  background: var(--danger);
  color: #fff;
  font-size: 10px;
  line-height: 16px;
  font-weight: var(--weight-semibold);
  text-align: center;
}

.avatar {
  width: 36px;
  height: 36px;
  padding: 0;
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  overflow: hidden;
  background: var(--surface-3);
}
.avatar img { width: 100%; height: 100%; object-fit: cover; }

/* ---------- 搜索弹层 / 模态 ---------- */
.search-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(20, 20, 18, 0.35);
  backdrop-filter: blur(3px);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 12vh var(--space-4) var(--space-4);
  animation: overlay-in var(--dur) var(--ease);
}
@keyframes overlay-in { from { opacity: 0; } to { opacity: 1; } }

.search-panel,
.modal-panel {
  width: 100%;
  max-width: 640px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  overflow: hidden;
  animation: panel-in var(--dur) var(--ease);
}
.modal-panel { max-width: 560px; margin-top: 4vh; }
@keyframes panel-in {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}

.search-bar {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4);
  border-bottom: 1px solid var(--border);
}
.search-bar-icon { width: 20px; height: 20px; color: var(--text-3); flex-shrink: 0; }
.search-bar input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: var(--text-md);
  color: var(--text);
}
.search-bar input::placeholder { color: var(--text-3); }
.clear-btn {
  width: 28px; height: 28px;
  display: inline-flex; align-items: center; justify-content: center;
  border: none; background: var(--surface-3); border-radius: var(--radius-sm);
  color: var(--text-2); cursor: pointer;
}
.clear-btn svg { width: 15px; height: 15px; }

.search-body { padding: var(--space-4); max-height: 50vh; overflow-y: auto; }
.search-section { margin-bottom: var(--space-4); }
.search-section:last-child { margin-bottom: 0; }
.search-section-head {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: var(--space-2);
}
.link-btn {
  border: none; background: none; cursor: pointer;
  color: var(--accent); font-size: var(--text-sm); padding: 0;
}
.link-btn:hover { color: var(--accent-hover); }

.search-item {
  display: flex; align-items: center; gap: var(--space-3);
  width: 100%; padding: 10px 12px; text-align: left;
  border: none; background: transparent; border-radius: var(--radius-sm);
  color: var(--text-2); font-size: var(--text-base); cursor: pointer;
  transition: background var(--dur-fast) var(--ease), color var(--dur-fast) var(--ease);
}
.search-item:hover { background: var(--surface-2); color: var(--text); }
.search-item svg { width: 17px; height: 17px; flex-shrink: 0; color: var(--text-3); }
.search-item span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.search-empty { padding: var(--space-10) var(--space-4); text-align: center; color: var(--text-3); font-size: var(--text-sm); }

/* ---------- Hero（视频背景卡片） ---------- */
.hero {
  position: relative;
  overflow: hidden;
  border-radius: var(--radius-xl);
  padding: var(--space-12) var(--space-10);
  margin-bottom: var(--space-8);
  min-height: 420px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-10);
}
.hero-copy { position: relative; z-index: 1; max-width: 560px; }
.hero .eyebrow { color: rgba(255, 255, 255, 0.85); }
.hero-title {
  font-size: var(--text-4xl);
  line-height: var(--leading-tight);
  letter-spacing: -0.03em;
  font-weight: var(--weight-semibold);
  color: #fff;
  margin: var(--space-3) 0 var(--space-4);
}
.hero-sub { color: rgba(255, 255, 255, 0.85); margin-bottom: var(--space-6); }
.hero-actions { display: flex; gap: var(--space-3); }
.hero-actions .btn-primary {
  background: #fff;
  color: var(--text);
}
.hero-actions .btn-primary:hover { background: rgba(255, 255, 255, 0.9); color: var(--text); }
.hero-actions .btn-outline {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.55);
  color: #fff;
}
.hero-actions .btn-outline:hover { background: rgba(255, 255, 255, 0.22); border-color: rgba(255, 255, 255, 0.8); }

.hero-stats {
  position: relative;
  z-index: 1;
  display: flex;
  gap: var(--space-8);
  margin: 0;
}
.stat { text-align: right; }
.stat-label { font-size: var(--text-sm); color: rgba(255, 255, 255, 0.72); }
.stat-value {
  font-size: var(--text-2xl);
  font-weight: var(--weight-semibold);
  letter-spacing: -0.02em;
  color: #fff;
  margin-top: 2px;
}

/* ---------- 数据卡片行 ---------- */
.dashboard-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}

.active-users-card { padding: var(--space-5); }
.active-user-empty { color: var(--text-3); font-size: var(--text-sm); }
.active-user-row { display: flex; align-items: center; gap: var(--space-3); padding: 6px 0; }
.active-user-row img { width: 32px; height: 32px; border-radius: 999px; object-fit: cover; }
.active-user-row span { font-size: var(--text-sm); color: var(--text); }

/* ---------- 内容网格 ---------- */
.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: var(--space-5);
}
.main-column { display: flex; flex-direction: column; gap: var(--space-5); }
.side-column { display: flex; flex-direction: column; gap: var(--space-5); }

/* ---------- 活动列表 ---------- */
.activities { display: flex; flex-direction: column; gap: var(--space-1); }
.activity {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  width: 100%;
  padding: var(--space-3);
  text-align: left;
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius);
  transition: background var(--dur-fast) var(--ease), border-color var(--dur-fast) var(--ease);
}
.activity:hover { background: var(--surface-2); border-color: var(--border); }

.activity-date {
  width: 48px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--space-2) 0;
  border-radius: var(--radius-sm);
  background: var(--surface-3);
}
.activity-day { font-size: var(--text-lg); font-weight: var(--weight-semibold); color: var(--text); line-height: 1; }
.activity-month { font-size: var(--text-xs); color: var(--text-3); margin-top: 2px; }

.activity-body { flex: 1; min-width: 0; }
.activity-title { font-size: var(--text-base); font-weight: var(--weight-medium); color: var(--text); margin-bottom: 2px; }
.activity-desc { font-size: var(--text-sm); color: var(--text-2); }

/* ---------- 快速操作 ---------- */
.quick-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: var(--space-3); }
.quick {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-4);
  padding: var(--space-4);
  background: var(--surface-2);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  color: var(--text);
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  transition: background var(--dur-fast) var(--ease), border-color var(--dur-fast) var(--ease);
}
.quick:hover { background: var(--surface-3); border-color: var(--border-strong); }
.quick-icon {
  width: 34px; height: 34px;
  display: inline-flex; align-items: center; justify-content: center;
  border-radius: var(--radius-sm);
  background: var(--accent-soft); color: var(--accent);
}
.quick-icon svg { width: 18px; height: 18px; }

/* ---------- 分类标签 ---------- */
.tags { display: flex; flex-wrap: wrap; gap: var(--space-2); }
.tag {
  padding: 7px 14px;
  font-size: var(--text-sm);
  color: var(--text-2);
  background: var(--surface-2);
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  transition: color var(--dur-fast) var(--ease), border-color var(--dur-fast) var(--ease), background var(--dur-fast) var(--ease);
}
.tag:hover { color: var(--accent); border-color: var(--accent); background: var(--accent-soft); }

/* ---------- 模态内容 ---------- */
.modal-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--border);
}
.modal-body { padding: var(--space-5); }
.modal-title { font-size: var(--text-xl); font-weight: var(--weight-semibold); letter-spacing: -0.01em; margin: var(--space-2) 0 var(--space-4); }
.modal-desc { color: var(--text-2); line-height: var(--leading-relaxed); }
.modal-list { margin: var(--space-4) 0 0; padding-left: var(--space-5); color: var(--text-2); font-size: var(--text-sm); line-height: var(--leading-relaxed); }
.modal-list li { margin-bottom: var(--space-1); }
.modal-foot { padding: var(--space-4) var(--space-5); border-top: 1px solid var(--border); display: flex; justify-content: flex-end; }

/* ---------- 页脚 ---------- */
.site-footer { margin-top: var(--space-16); background: var(--surface); border-top: 1px solid var(--border); }
.footer-inner {
  display: flex;
  justify-content: space-between;
  gap: var(--space-10);
  padding-top: var(--space-12);
  padding-bottom: var(--space-10);
}
.footer-brand .brand-name { font-size: var(--text-lg); font-weight: var(--weight-semibold); }
.footer-brand p { margin-top: var(--space-2); color: var(--text-2); font-size: var(--text-sm); }
.footer-cols { display: flex; gap: var(--space-16); }
.footer-col { display: flex; flex-direction: column; gap: var(--space-3); }
.footer-col h4 { font-size: var(--text-sm); font-weight: var(--weight-semibold); color: var(--text); margin-bottom: var(--space-1); }
.footer-col a { font-size: var(--text-sm); color: var(--text-2); }
.footer-col a:hover { color: var(--accent); }
.footer-bottom { border-top: 1px solid var(--border); padding: var(--space-5) 0; font-size: var(--text-sm); color: var(--text-3); }

/* ---------- 响应式 ---------- */
@media (max-width: 1024px) {
  .content-grid { grid-template-columns: 1fr; }
  .hero { flex-direction: column; align-items: flex-start; }
  .hero-stats { width: 100%; justify-content: flex-start; }
  .stat { text-align: left; }
}
@media (max-width: 720px) {
  .main-nav { display: none; }
  .dashboard-row { grid-template-columns: repeat(2, 1fr); }
  .hero-title { font-size: var(--text-3xl); }
  .hero-stats { gap: var(--space-6); }
  .footer-inner { flex-direction: column; gap: var(--space-8); }
  .footer-cols { gap: var(--space-10); }
}
</style>

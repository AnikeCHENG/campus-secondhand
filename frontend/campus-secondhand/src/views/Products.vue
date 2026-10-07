<template>
  <div class="page">
    <header class="site-header">
      <div class="container header-inner">
        <router-link to="/" class="brand">
          <span class="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 12a9 9 0 0 1 15-6.7L21 8" /><path d="M21 3v5h-5" />
              <path d="M21 12a9 9 0 0 1-15 6.7L3 16" /><path d="M3 21v-5h5" />
            </svg>
          </span>
          <span class="brand-name">校园二手</span>
        </router-link>
        <nav class="main-nav" aria-label="主导航">
          <router-link to="/" class="nav-link">首页</router-link>
          <router-link to="/products" class="nav-link">商品</router-link>
          <router-link to="/post" class="nav-link">发布</router-link>
          <router-link to="/messages" class="nav-link">消息</router-link>
          <router-link to="/profile" class="nav-link">我的</router-link>
        </nav>
        <div class="header-actions">
          <button class="icon-btn" type="button" aria-label="退出登录" @click="handleLogout">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
              <polyline points="16 17 21 12 16 7" /><line x1="21" y1="12" x2="9" y2="12" />
            </svg>
          </button>
          <button class="avatar" type="button" aria-label="个人中心" @click="go('/profile')">
            <img src="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 40 40'%3E%3Crect width='40' height='40' fill='%230b6e54'/%3E%3Ccircle cx='20' cy='16' r='6' fill='%23ffffff'/%3E%3Cpath d='M8 36c0-6.6 5.4-12 12-12s12 5.4 12 12' fill='%23ffffff'/%3E%3C/svg%3E" alt="用户头像" />
          </button>
        </div>
      </div>
    </header>

    <main class="page-main">
      <div class="page-head">
        <div>
          <h1 class="page-title">商品列表</h1>
          <p class="page-sub">发现校园里的实惠好物</p>
        </div>
        <button class="btn btn-primary" type="button" @click="go('/post')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          发布商品
        </button>
      </div>

      <!-- 搜索 -->
      <div class="search-wrap">
        <div class="search-bar">
          <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input
            type="text"
            v-model="searchKeyword"
            placeholder="搜索商品名称、描述…"
            @keyup.enter="handleSearch"
            @focus="showSearchSuggestions = true"
            @blur="hideSearchSuggestions"
          />
          <button v-if="searchKeyword" class="clear-btn" type="button" aria-label="清空" @click="clearSearch">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
          <button class="btn btn-primary btn-sm" type="button" @click="handleSearch">搜索</button>
        </div>

        <div v-if="showSearchSuggestions && (searchSuggestions.length > 0 || searchHistory.length > 0)" class="suggestions">
          <div v-if="searchHistory.length > 0" class="suggestion-section">
            <div class="suggestion-head">
              <span class="eyebrow">搜索历史</span>
              <button class="link-btn" type="button" @click="clearSearchHistory">清空</button>
            </div>
            <button
              v-for="(item, index) in searchHistory.slice(0, 5)"
              :key="`history-${index}`"
              class="suggestion-item"
              type="button"
              @mousedown="selectSuggestion(item)"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" />
              </svg>
              <span>{{ item }}</span>
            </button>
          </div>
          <div v-if="searchSuggestions.length > 0" class="suggestion-section">
            <div class="suggestion-head"><span class="eyebrow">搜索建议</span></div>
            <button
              v-for="(item, index) in searchSuggestions"
              :key="`suggestion-${index}`"
              class="suggestion-item"
              type="button"
              @mousedown="selectSuggestion(item)"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
              <span>{{ item }}</span>
            </button>
          </div>
        </div>

        <button class="btn btn-outline btn-sm filter-toggle" type="button" @click="showAdvancedFilters = !showAdvancedFilters">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <line x1="4" y1="21" x2="4" y2="14" /><line x1="4" y1="10" x2="4" y2="3" />
            <line x1="12" y1="21" x2="12" y2="12" /><line x1="12" y1="8" x2="12" y2="3" />
            <line x1="20" y1="21" x2="20" y2="16" /><line x1="20" y1="12" x2="20" y2="3" />
          </svg>
          高级筛选
        </button>

        <div v-if="showAdvancedFilters" class="card filters-panel">
          <div class="filter-group">
            <label class="field-label">价格范围</label>
            <div class="price-range">
              <input type="number" v-model="priceMin" placeholder="最低" class="input" min="0" step="0.01" @input="validatePriceRange" />
              <span class="range-sep">—</span>
              <input type="number" v-model="priceMax" placeholder="最高" class="input" min="0" step="0.01" @input="validatePriceRange" />
            </div>
            <p v-if="priceError" class="error-msg">{{ priceError }}</p>
          </div>
          <div class="filter-group">
            <label class="field-label">成色</label>
            <div class="chip-row">
              <button
                v-for="cond in conditions"
                :key="cond.value"
                class="chip"
                :class="{ active: selectedCondition === cond.value }"
                type="button"
                @click="toggleCondition(cond.value)"
              >
                {{ cond.label }}
              </button>
            </div>
          </div>
          <div class="filter-group">
            <label class="field-label">排序方式</label>
            <div class="chip-row">
              <button
                v-for="sort in sortOptions"
                :key="sort.value"
                class="chip"
                :class="{ active: selectedSort === sort.value }"
                type="button"
                @click="selectSort(sort.value)"
              >
                {{ sort.label }}
              </button>
            </div>
          </div>
          <div class="filter-actions">
            <button class="btn btn-primary" type="button" @click="applyFilters">应用筛选</button>
            <button class="btn btn-outline" type="button" @click="resetFilters">重置</button>
          </div>
        </div>

        <div class="chip-row category-row">
          <button class="chip" :class="{ active: activeCategory === 'all' }" type="button" @click="selectCategory('all')">全部</button>
          <button
            v-for="cat in categories"
            :key="cat.value"
            class="chip"
            :class="{ active: activeCategory === cat.value }"
            type="button"
            @click="selectCategory(cat.value)"
          >
            {{ cat.label }}
          </button>
        </div>
      </div>

      <!-- 列表 -->
      <div class="products-section">
        <div v-if="loading" class="empty">
          <p class="loading-text">[ LOADING… ]</p>
        </div>

        <div v-else-if="error" class="empty">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
          <p>{{ error }}</p>
          <button class="btn btn-outline" type="button" @click="fetchProducts">重新加载</button>
        </div>

        <div v-else-if="filteredProducts.length === 0" class="empty">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <p v-if="searchKeyword">没有找到包含“{{ searchKeyword }}”的商品</p>
          <p v-else-if="activeCategory !== 'all'">该分类下暂无商品</p>
          <p v-else-if="priceMin || priceMax || selectedCondition">没有符合筛选条件的商品</p>
          <p v-else>暂无商品</p>
          <div class="empty-actions">
            <button v-if="searchKeyword || priceMin || priceMax || selectedCondition" class="btn btn-outline" type="button" @click="clearAllFilters">清除所有筛选</button>
            <button class="btn btn-primary" type="button" @click="go('/post')">发布商品</button>
          </div>
        </div>

        <div v-else class="products-grid">
          <article
            v-for="product in filteredProducts"
            :key="product.id"
            class="product-card"
            tabindex="0"
            role="button"
            @click="viewProduct(product.id)"
            @keydown.enter="viewProduct(product.id)"
            @mousemove="onCardMove"
            @mouseleave="onCardLeave"
          >
            <div class="product-image">
              <img v-if="product.images" :src="product.images" :alt="product.title" class="product-img" />
              <div v-else class="image-placeholder">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <rect x="3" y="3" width="18" height="18" rx="2" /><circle cx="8.5" cy="8.5" r="1.5" /><polyline points="21 15 16 10 5 21" />
                </svg>
              </div>
              <span class="badge product-status" :class="getStatusClass(product.status)">{{ getStatusText(product.status) }}</span>
              <div class="product-overlay">
                <div class="overlay-tags">
                  <span>{{ product.category || '闲置' }}</span>
                  <span>{{ product.condition || '成色待看' }}</span>
                </div>
                <div class="overlay-center">
                  <h3>{{ product.title }}</h3>
                  <button type="button" class="overlay-btn" @click.stop="viewProduct(product.id)">前往 →</button>
                </div>
              </div>
            </div>
            <div class="product-info">
              <h3 class="product-title">{{ product.title }}</h3>
              <p class="product-desc">{{ product.description }}</p>
              <div class="product-meta">
                <span v-if="product.category" class="tag">{{ product.category }}</span>
                <span v-if="product.condition" class="tag">{{ product.condition }}</span>
              </div>
              <div class="product-footer">
                <div class="product-price">
                  <span class="price-symbol">¥</span><span class="price-value">{{ formatPrice(product.price) }}</span>
                </div>
                <span class="product-views">{{ product.viewCount || 0 }} 人浏览</span>
              </div>
            </div>
          </article>
        </div>
      <!-- 服务端分页：total 取自后端 count，翻页时按条件重新请求 -->
      <div v-if="paging.total > paging.size && !loading && !error" class="pager">
        <el-pagination
          layout="total, prev, pager, next"
          :current-page="paging.page"
          :page-size="paging.size"
          :total="paging.total"
          @current-change="onPageChange"
        />
      </div>
      </div>
    </main>

    <footer class="site-footer">
      <div class="container">© 2025 校园二手交易平台</div>
    </footer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getProductList } from '../api/product'

const router = useRouter()
const route = useRoute()
const allProducts = ref([])
const loading = ref(true)
const error = ref('')
const searchKeyword = ref('')
const activeCategory = ref('all')
const showAdvancedFilters = ref(false)
const priceMin = ref('')
const priceMax = ref('')
const selectedCondition = ref('')
const selectedSort = ref('newest')
const showSearchSuggestions = ref(false)
const searchHistory = ref([])
const priceError = ref('')

const conditions = [
  { value: 'new', label: '全新' },
  { value: 'like-new', label: '几乎全新' },
  { value: 'good', label: '良好' },
  { value: 'fair', label: '一般' }
]

const sortOptions = [
  { value: 'newest', label: '最新发布' },
  { value: 'price-low', label: '价格从低到高' },
  { value: 'price-high', label: '价格从高到低' },
  { value: 'views', label: '浏览量最多' }
]

const categories = [
  { value: 'books', label: '教材书籍' },
  { value: 'electronics', label: '电子产品' },
  { value: 'transport', label: '出行工具' },
  { value: 'gaming', label: '游戏数码' },
  { value: 'clothing', label: '服饰穿搭' },
  { value: 'living', label: '生活用品' },
  { value: 'other', label: '其他' }
]

const searchSuggestions = computed(() => {
  if (!searchKeyword.value || searchKeyword.value.length < 1) return []
  const keyword = searchKeyword.value.toLowerCase()
  const suggestions = new Set()
  allProducts.value.forEach(product => {
    if (product.title && product.title.toLowerCase().includes(keyword)) suggestions.add(product.title)
    if (product.description && product.description.toLowerCase().includes(keyword)) {
      const words = product.description.split(/\s+/).filter(word => word.toLowerCase().includes(keyword) && word.length > 1)
      words.forEach(word => suggestions.add(word))
    }
  })
  return Array.from(suggestions).slice(0, 6)
})

/**
 * 服务端分页与筛选状态。
 *
 * <p>筛选与排序已下推后端：它们必须和分页在同一条 SQL 内完成。
 * 若保留「先取全量再在内存里filter/sort」那套逻辑，筛选只会作用在当前页
 * 的十几条记录上，用户看到的总数与结果都不对。</p>
 */
const paging = reactive({ page: 1, size: 12, total: 0 })

/** 前端选项值 → 后端排序参数 */
const SORT_PARAM = {
  newest: 'newest',
  'price-low': 'priceAsc',
  'price-high': 'priceDesc',
  views: 'hot'
}

const filteredProducts = computed(() => allProducts.value)

function currentFilters() {
  return {
    category: activeCategory.value !== 'all' ? activeCategory.value : '',
    keyword: searchKeyword.value.trim(),
    minPrice: priceMin.value,
    maxPrice: priceMax.value,
    condition: selectedCondition.value,
    sort: SORT_PARAM[selectedSort.value] || 'newest'
  }
}

/** 任一筛选条件变化都回到第一页：留在第 3 页会得到空列表 */
function reloadFromFirstPage() {
  paging.page = 1
  fetchProducts()
}

function onPageChange(page) {
  paging.page = page
  fetchProducts()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function go(path) { router.push(path) }
function hideSearchSuggestions() { setTimeout(() => { showSearchSuggestions.value = false }, 200) }
function selectSuggestion(suggestion) { searchKeyword.value = suggestion; showSearchSuggestions.value = false; handleSearch() }
function clearSearch() { searchKeyword.value = ''; showSearchSuggestions.value = false }

function saveSearchHistory(keyword) {
  if (!keyword.trim()) return
  const index = searchHistory.value.indexOf(keyword)
  if (index > -1) searchHistory.value.splice(index, 1)
  searchHistory.value.unshift(keyword)
  searchHistory.value = searchHistory.value.slice(0, 10)
  localStorage.setItem('searchHistory', JSON.stringify(searchHistory.value))
}
function clearSearchHistory() { searchHistory.value = []; localStorage.removeItem('searchHistory') }
function loadSearchHistory() {
  const saved = localStorage.getItem('searchHistory')
  if (saved) { try { searchHistory.value = JSON.parse(saved) } catch (e) { console.error(e) } }
}

async function fetchProducts() {
  loading.value = true
  error.value = ''
  try {
    const res = await getProductList(paging.page, paging.size, currentFilters())
    if (res.code === 200 && res.data?.list) {
      allProducts.value = res.data.list
      // total 来自后端 count 查询，是筛选后的总命中数，不是当页条数
      paging.total = res.data.total ?? 0
    }
  } catch (e) {
    error.value = e.message || '加载商品失败'
  } finally {
    loading.value = false
  }
}

async function handleSearch() { saveSearchHistory(searchKeyword.value); showSearchSuggestions.value = false; reloadFromFirstPage() }
async function selectCategory(category) { activeCategory.value = category; showAdvancedFilters.value = false; reloadFromFirstPage() }
function applyFilters() { validatePriceRange(); if (!priceError.value) { showAdvancedFilters.value = false; reloadFromFirstPage() } }
/** 成色：再点一次取消筛选，等价于不传该参数 */
function toggleCondition(value) {
  selectedCondition.value = selectedCondition.value === value ? '' : value
  reloadFromFirstPage()
}
/** 排序：排序变化同样必须回到第一页，否则第 3 页在新的顺序下毫无意义 */
function selectSort(value) {
  selectedSort.value = value
  reloadFromFirstPage()
}
function resetFilters() { priceMin.value = ''; priceMax.value = ''; priceError.value = ''; selectedCondition.value = ''; selectedSort.value = 'newest'; reloadFromFirstPage() }
function validatePriceRange() {
  priceError.value = ''
  if (priceMin.value && priceMax.value) {
    if (parseFloat(priceMin.value) > parseFloat(priceMax.value)) priceError.value = '最低价格不能大于最高价格'
  }
}
function clearAllFilters() {
  searchKeyword.value = ''; activeCategory.value = 'all'; priceMin.value = ''; priceMax.value = ''
  priceError.value = ''; selectedCondition.value = ''; selectedSort.value = 'newest'; showAdvancedFilters.value = false
  reloadFromFirstPage()
}
function viewProduct(id) { router.push(`/products/${id}`) }
function onCardMove(event) {
  const card = event.currentTarget
  const rect = card.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top
  const rotateY = ((x - rect.width / 2) / rect.width) * 20
  const rotateX = ((rect.height / 2 - y) / rect.height) * 16
  card.style.transform = `perspective(900px) rotateX(${rotateX.toFixed(2)}deg) rotateY(${rotateY.toFixed(2)}deg) scale(1.04)`
}
function onCardLeave(event) {
  event.currentTarget.style.transform = ''
}
function formatPrice(price) { if (!price) return '0'; return parseFloat(price).toFixed(2) }
function getStatusClass(status) { return ({ 0: 'badge', 1: 'badge-success', 2: 'badge-danger' })[status] || 'badge-success' }
function getStatusText(status) { return ({ 0: '已下架', 1: '在售', 2: '已售' })[status] || '在售' }
async function handleLogout() {
  localStorage.removeItem('token'); localStorage.removeItem('username'); sessionStorage.removeItem('justLoggedIn'); router.push('/login')
}

onMounted(() => {
  loadSearchHistory()
  if (route.query.search) searchKeyword.value = route.query.search
  if (route.query.category) activeCategory.value = route.query.category
  fetchProducts()
})

watch(() => route.fullPath, () => {
  if (route.query.search) searchKeyword.value = route.query.search
  if (route.query.category) activeCategory.value = route.query.category
  fetchProducts()
})
</script>

<style scoped>
/* 服务端分页控件 */
.pager { display: flex; justify-content: center; margin-top: var(--space-6); }

/* 头部（与首页一致） */
.site-header {
  position: sticky; top: 0; z-index: 100;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: saturate(180%) blur(12px);
  border-bottom: 1px solid var(--border);
}
.header-inner { height: var(--header-h); display: flex; align-items: center; gap: var(--space-8); }
.brand { display: inline-flex; align-items: center; gap: var(--space-2); color: var(--text); flex-shrink: 0; }
.brand:hover { color: var(--text); }
.brand-mark { width: 30px; height: 30px; display: inline-flex; align-items: center; justify-content: center; border-radius: var(--radius-sm); background: var(--accent); color: #fff; }
.brand-mark svg { width: 17px; height: 17px; }
.brand-name { font-size: var(--text-lg); font-weight: var(--weight-semibold); letter-spacing: -0.01em; }
.main-nav { display: flex; gap: var(--space-1); flex: 1; }
.nav-link { padding: 8px 12px; font-size: var(--text-base); color: var(--text-2); border-radius: var(--radius-sm); transition: color var(--dur-fast) var(--ease), background var(--dur-fast) var(--ease); }
.nav-link:hover { color: var(--text); background: var(--surface-3); }
.nav-link.router-link-exact-active { color: var(--accent); font-weight: var(--weight-medium); }
.header-actions { display: flex; align-items: center; gap: var(--space-2); }
.icon-btn { width: 38px; height: 38px; display: inline-flex; align-items: center; justify-content: center; border: 1px solid transparent; border-radius: var(--radius); background: transparent; color: var(--text-2); transition: background var(--dur-fast) var(--ease), color var(--dur-fast) var(--ease); }
.icon-btn:hover { background: var(--surface-3); color: var(--text); }
.icon-btn svg { width: 20px; height: 20px; }
.avatar { width: 36px; height: 36px; padding: 0; border: 1px solid var(--border); border-radius: var(--radius-full); overflow: hidden; background: var(--surface-3); }
.avatar img { width: 100%; height: 100%; object-fit: cover; }

/* 页头 */
.page-head { display: flex; align-items: flex-end; justify-content: space-between; gap: var(--space-4); margin-bottom: var(--space-6); }
.page-title { font-size: var(--text-2xl); font-weight: var(--weight-semibold); letter-spacing: -0.02em; }
.page-sub { margin-top: var(--space-1); color: var(--text-2); font-size: var(--text-sm); }

/* 搜索 */
.search-wrap { position: relative; margin-bottom: var(--space-6); }
.search-bar { display: flex; align-items: center; gap: var(--space-3); padding: var(--space-2) var(--space-2) var(--space-2) var(--space-4); background: var(--surface); border: 1px solid var(--border-strong); border-radius: var(--radius); transition: border-color var(--dur-fast) var(--ease), box-shadow var(--dur-fast) var(--ease); }
.search-bar:focus-within { border-color: var(--accent); box-shadow: 0 0 0 3px var(--accent-soft); }
.search-icon { width: 20px; height: 20px; color: var(--text-3); flex-shrink: 0; }
.search-bar input { flex: 1; border: none; outline: none; background: transparent; font-size: var(--text-base); color: var(--text); }
.search-bar input::placeholder { color: var(--text-3); }
.clear-btn { width: 30px; height: 30px; display: inline-flex; align-items: center; justify-content: center; border: none; background: var(--surface-3); border-radius: var(--radius-sm); color: var(--text-2); }
.clear-btn svg { width: 16px; height: 16px; }

.suggestions { position: absolute; top: calc(100% + 6px); left: 0; right: 0; z-index: 50; background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius); box-shadow: var(--shadow-md); padding: var(--space-2); }
.suggestion-section + .suggestion-section { border-top: 1px solid var(--border); margin-top: var(--space-2); padding-top: var(--space-2); }
.suggestion-head { display: flex; align-items: center; justify-content: space-between; padding: var(--space-1) var(--space-2); }
.suggestion-item { display: flex; align-items: center; gap: var(--space-3); width: 100%; padding: 9px 10px; text-align: left; border: none; background: transparent; border-radius: var(--radius-sm); color: var(--text-2); font-size: var(--text-base); }
.suggestion-item:hover { background: var(--surface-2); color: var(--text); }
.suggestion-item svg { width: 16px; height: 16px; color: var(--text-3); flex-shrink: 0; }
.suggestion-item span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.filter-toggle { margin-top: var(--space-3); }
.filters-panel { margin-top: var(--space-3); display: flex; flex-direction: column; gap: var(--space-5); }
.filter-group .field-label { margin-bottom: var(--space-2); }
.price-range { display: flex; align-items: center; gap: var(--space-3); max-width: 380px; }
.range-sep { color: var(--text-3); }
.error-msg { color: var(--danger); font-size: var(--text-xs); margin-top: var(--space-2); }
.chip-row { display: flex; flex-wrap: wrap; gap: var(--space-2); }
.category-row { margin-top: var(--space-4); }
.chip { padding: 7px 14px; font-size: var(--text-sm); color: var(--text-2); background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-full); transition: all var(--dur-fast) var(--ease); }
.chip:hover { border-color: var(--border-strong); color: var(--text); }
.chip.active { background: var(--accent); border-color: var(--accent); color: #fff; }
.filter-actions { display: flex; gap: var(--space-3); }

/* 商品网格 */
.products-section { min-height: 400px; margin-top: var(--space-6); background: #1a1a1a; padding: var(--space-6); border-radius: var(--radius-lg); }
.products-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; }
.product-card { background: var(--surface); border: 1px solid var(--border); border-radius: 12px; overflow: hidden; cursor: pointer; display: flex; flex-direction: column; transition: transform .16s ease, box-shadow .2s ease, border-color .2s ease; }
.product-card:hover { border-color: var(--border-strong); box-shadow: var(--shadow-sm); }
.product-image { position: relative; aspect-ratio: 16 / 10; background: var(--surface-3); }
.product-img { width: 100%; height: 100%; object-fit: cover; transition: transform .35s ease; }
.product-card:hover .product-img { transform: scale(1.05); }
.image-placeholder { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; color: var(--text-3); }
.image-placeholder svg { width: 44px; height: 44px; }
.product-status { position: absolute; top: var(--space-3); right: var(--space-3); z-index: 2; }
.product-overlay { position: absolute; inset: 0; z-index: 1; display: flex; flex-direction: column; justify-content: space-between; padding: 12px; background: linear-gradient(to top, rgba(0, 0, 0, 0.72), rgba(0, 0, 0, 0.14) 55%, rgba(0, 0, 0, 0.45)); opacity: 0; transition: opacity .25s ease; }
.product-image:hover .product-overlay { opacity: 1; }
.overlay-tags { display: flex; flex-wrap: wrap; gap: 8px; }
.overlay-tags span { padding: 4px 10px; border-radius: 999px; background: rgba(255, 255, 255, 0.2); color: #fff; font-size: var(--text-xs); backdrop-filter: blur(4px); }
.overlay-center { margin: auto; text-align: center; color: #fff; transform: translateY(4px); transition: transform .25s ease; }
.product-image:hover .overlay-center { transform: translateY(0); }
.overlay-center h3 { margin: 0 0 10px; font-size: var(--text-lg); }
.overlay-btn { border: 0; border-radius: 999px; padding: 8px 16px; background: #111; color: #fff; cursor: pointer; }
.product-info { padding: var(--space-4); flex: 1; }
.product-title { font-size: var(--text-base); font-weight: var(--weight-medium); color: var(--text); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.product-desc { margin-top: 4px; font-size: var(--text-sm); color: var(--text-2); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.product-meta { display: flex; gap: var(--space-2); margin: var(--space-3) 0; }
.tag { padding: 2px 10px; font-size: var(--text-xs); color: var(--text-2); background: var(--surface-3); border-radius: var(--radius-full); }
.product-footer { display: flex; align-items: baseline; justify-content: space-between; }
.product-price { display: flex; align-items: baseline; gap: 1px; color: var(--accent); }
.price-symbol { font-size: var(--text-sm); font-weight: var(--weight-semibold); }
.price-value { font-size: var(--text-xl); font-weight: var(--weight-semibold); letter-spacing: -0.02em; }
.product-views { font-size: var(--text-xs); color: var(--text-3); }

.empty-actions { display: flex; gap: var(--space-3); }

@media (max-width: 1100px) {
  .products-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 720px) {
  .products-grid { grid-template-columns: 1fr; }
  .main-nav { display: none; }
  .page-head { flex-direction: column; align-items: flex-start; }
  .site-footer { margin-top: var(--space-12); background: var(--surface); border-top: 1px solid var(--border); padding: var(--space-6) 0; color: var(--text-3); font-size: var(--text-sm); }
}
.site-footer { margin-top: var(--space-16); background: var(--surface); border-top: 1px solid var(--border); padding: var(--space-6) 0; color: var(--text-3); font-size: var(--text-sm); }
</style>

<template>
  <!--
    data-plaza-dark：暗色玻璃隔离舱，与 Plaza.vue 复用同一个属性名和
    同一套 token 写法（不发明第二套机制）。属性语义是「本页为暗」而非
    「这是广场」，两页样式将来可互相借鉴；theme.css 零改动，
    其他浅色页面不受影响。
  -->
  <div class="page" data-plaza-dark>

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

        <button class="btn btn-sm btn-ghost-glass filter-toggle" type="button" @click="showAdvancedFilters = !showAdvancedFilters">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <line x1="4" y1="21" x2="4" y2="14" /><line x1="4" y1="10" x2="4" y2="3" />
            <line x1="12" y1="21" x2="12" y2="12" /><line x1="12" y1="8" x2="12" y2="3" />
            <line x1="20" y1="21" x2="20" y2="16" /><line x1="20" y1="12" x2="20" y2="3" />
          </svg>
          高级筛选
        </button>

        <div v-if="showAdvancedFilters" class="filters-panel">
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
            <button class="btn btn-ghost-glass" type="button" @click="resetFilters">重置</button>
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
          <button class="btn btn-ghost-glass" type="button" @click="fetchProducts">重新加载</button>
        </div>

        <div v-else-if="filteredProducts.length === 0" class="empty">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <p v-if="searchKeyword">没有找到包含“{{ searchKeyword }}”的商品</p>
          <p v-else-if="activeCategory !== 'all'">该分类下暂无商品</p>
          <p v-else-if="priceMin || priceMax || selectedCondition !== null">没有符合筛选条件的商品</p>
          <p v-else>暂无商品</p>
          <div class="empty-actions">
          <button v-if="searchKeyword || priceMin || priceMax || selectedCondition !== null" class="btn btn-ghost-glass" type="button" @click="clearAllFilters">清除所有筛选</button>
            <button class="btn btn-primary" type="button" @click="go('/post')">发布商品</button>
          </div>
        </div>

        <div v-else class="products-grid">
          <article
            v-for="(product, i) in filteredProducts"
            :key="product.id"
            class="product-card"
            v-reveal="{ delay: Math.min(i, 5) * 80 }"
            tabindex="0"
            role="button"
            @click="viewProduct(product.id)"
            @keydown.enter="viewProduct(product.id)"
            @mousemove="onCardMove"
            @mouseleave="onCardLeave"
          >
            <div class="product-image">
              <img v-if="product.images" :src="product.images" :alt="product.title" class="product-img" />
              <div v-else class="image-placeholder" :class="'ph-' + (product.category || 'other')">
                <span class="ph-emoji" aria-hidden="true">{{ categoryEmoji(product.category) }}</span>
                <span class="ph-label">{{ categoryLabel(product.category) }}</span>
              </div>
              <span class="badge product-status" :class="getStatusClass(product.status)">{{ getStatusText(product.status) }}</span>
              <div class="product-overlay">
                <div class="overlay-tags">
                  <span>{{ categoryLabel(product.category) }}</span>
                  <span>{{ product.condition || '成色良好' }}</span>
                </div>
                <div class="overlay-center">
                  <h3>{{ product.title }}</h3>
                  <button type="button" class="overlay-btn" @click.stop="viewProduct(product.id)">前往 →</button>
                </div>
              </div>
            </div>
            <div class="product-info">
              <h3 class="product-title">{{ product.title }}</h3>
              <div class="product-meta">
                <span v-if="product.category" class="tag">{{ categoryLabel(product.category) }}</span>
                <!-- 成色标签：越新颜色越绿，越旧越红，买家扫一眼就能判断 -->
                <span
                  v-if="product.conditionLevel !== null && product.conditionLevel !== undefined"
                  class="tag tag-condition"
                  :class="'level-' + product.conditionLevel"
                >{{ conditionLabel(product.conditionLevel) }}</span>
                <!-- 有瑕疵时给一个警示标记，具体描述在详情页 -->
                <span v-if="product.flawDescription" class="tag tag-flaw">有瑕疵</span>
              </div>
              <p class="product-desc">{{ product.description }}</p>
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
// 成色筛选：null 表示不筛选；用 0 作为'未选'会与'全新'冲突，故以 null 为空
const selectedCondition = ref(null)
const selectedSort = ref('newest')
const showSearchSuggestions = ref(false)
const searchHistory = ref([])
const priceError = ref('')

/**
 * 成色筛选项，value 为 0~4 整数。
 *
 * <p>此前这里是 {@code new/like-new/good/fair} 四个字符串，而后端存的是中文文本，
 * 两边对不上——这个筛选从上线起就没生效过。现改为与 condition_level 对齐的整数。</p>
 */
const conditions = [
  { value: 0, label: '全新' },
  { value: 1, label: '99新' },
  { value: 2, label: '95新' },
  { value: 3, label: '9成新' },
  { value: 4, label: '8成新以下' }
]

const sortOptions = [
  { value: 'newest', label: '最新发布' },
  { value: 'price-low', label: '价格从低到高' },
  { value: 'price-high', label: '价格从高到低' },
  { value: 'views', label: '浏览量最多' }
]

const categories = [
  { value: 'books', label: '教材书籍', emoji: '📚' },
  { value: 'electronics', label: '电子产品', emoji: '💻' },
  { value: 'transport', label: '出行工具', emoji: '🚲' },
  { value: 'gaming', label: '游戏数码', emoji: '🎮' },
  { value: 'clothing', label: '服饰穿搭', emoji: '👕' },
  { value: 'living', label: '生活用品', emoji: '🧺' },
  { value: 'other', label: '其他', emoji: '📦' }
]

/**
 * 分类代码转中文标签。
 *
 * <p>此前 chips（筛选行）用 categories 数组的中文 label，而商品卡片直接输出
 * 原始代码 {{ product.category }} —— 同一页面上「电子产品」和「electronics」
 * 并存。查表而不是各写一份 if-else，保证新增分类只改 categories 一处。</p>
 *
 * <p>查不到时回退到原值而非「其他」：数据里的未知分类仍应被看见，
 * 统一显示「其他」会让多个不同的未知分类混作一谈。</p>
 */
function categoryLabel(code) {
  if (!code) return '其他'
  return categories.find(c => c.value === code)?.label || code
}

/** 分类代码转 emoji，用于无图商品的占位。查不到时给中性 📦。 */
function categoryEmoji(code) {
  return categories.find(c => c.value === code)?.emoji || '📦'
}

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
    conditionLevel: selectedCondition.value,
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
  // 再点一次取消筛选：null 而非 ''，否则会把 conditionLevel=0（全新）误当成未选
  selectedCondition.value = selectedCondition.value === value ? null : value
  reloadFromFirstPage()
}
/** 排序：排序变化同样必须回到第一页，否则第 3 页在新的顺序下毫无意义 */
function selectSort(value) {
  selectedSort.value = value
  reloadFromFirstPage()
}
function resetFilters() { priceMin.value = ''; priceMax.value = ''; priceError.value = ''; selectedCondition.value = null; selectedSort.value = 'newest'; reloadFromFirstPage() }
function validatePriceRange() {
  priceError.value = ''
  if (priceMin.value && priceMax.value) {
    if (parseFloat(priceMin.value) > parseFloat(priceMax.value)) priceError.value = '最低价格不能大于最高价格'
  }
}
function clearAllFilters() {
  searchKeyword.value = ''; activeCategory.value = 'all'; priceMin.value = ''; priceMax.value = ''
  priceError.value = ''; selectedCondition.value = null; selectedSort.value = 'newest'; showAdvancedFilters.value = false
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
/** 成色等级 0~4 → 中文标签，与后端 ConditionLevel 一一对应 */
function conditionLabel(level) {
  return { 0: '全新', 1: '99新', 2: '95新', 3: '9成新', 4: '8成新及以下' }[level] ?? '未标注成色'
}

function formatPrice(price) { if (!price) return '0'; return parseFloat(price).toFixed(2) }
function getStatusClass(status) { return ({ 0: 'badge', 1: 'badge-success', 2: 'badge-danger' })[status] || 'badge-success' }
function getStatusText(status) { return ({ 0: '已下架', 1: '在售', 2: '已售' })[status] || '在售' }
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
/* ============================================================================
 * 暗色玻璃隔离舱 —— 与 Plaza.vue 同一套机制，不发明第二套
 *
 * 只重定义自定义属性，不写组件样式规则。CSS 变量沿 DOM 树继承且不受
 * Vue scoped 影响，能自然流到所有子组件；反之写成 [data-plaza-dark] .card
 * 会因为 scoped 属性不匹配而失效。
 *
 * token 数值与 Plaza 逐条对齐（含 --accent 从深绿 #0b6e54 提亮到 #2dd4bf：
 * 深绿在星空背景上对比度约 1.4:1，几乎不可见）。
 * ==========================================================================*/
[data-plaza-dark] {
  --bg: transparent;
  --bg-subtle: transparent;

  --surface: rgba(255, 255, 255, 0.06);
  --surface-2: rgba(255, 255, 255, 0.04);
  --surface-3: rgba(255, 255, 255, 0.03);

  --border: rgba(255, 255, 255, 0.10);
  --border-strong: rgba(255, 255, 255, 0.18);

  --text: rgba(255, 255, 255, 0.92);
  --text-2: rgba(255, 255, 255, 0.60);
  --text-3: rgba(255, 255, 255, 0.38);

  --accent: #2dd4bf;
  --accent-hover: #5eead4;
  --accent-active: #14b8a6;
  --accent-soft: rgba(45, 212, 191, 0.14);
  --accent-soft-strong: rgba(45, 212, 191, 0.22);
}

/* 让壁纸透出来：.page 默认 background: var(--bg)，隔离舱已把 --bg 设为
   transparent，这里补 position/z-index 让内容压在壁纸层之上。 */
.page { position: relative; z-index: 1; }

/* 服务端分页控件 */
.pager { display: flex; justify-content: center; margin-top: var(--space-6); }

/* 头部：暗玻璃，与 Plaza 的 .plaza-header 同一视觉语言。
   未连带改造其他 7 个页面的导航——它们各自复制了一份（nav-link 数量还各不相同，
   Home 7 项 / Plaza 4 项 / 其余 5 项），抽共享组件是另一个量级的重构。
   各页保持内部自洽：暗页面配暗导航，浅页面配浅导航。 */
.brand-mark svg { width: 17px; height: 17px; }
/* 当前项渐变下划线：用 ::after 而非 border-bottom，
   border 会占盒模型高度导致按下时整行跳动。 */
.icon-btn svg { width: 20px; height: 20px; }
.avatar img { width: 100%; height: 100%; object-fit: cover; }

/* 页头 */
.page-head { display: flex; align-items: flex-end; justify-content: space-between; gap: var(--space-4); margin-bottom: var(--space-6); }

/* 渐变大字。修复原 bug：.page-title 没有任何 color，继承 --text
   （浅色主题下是 #1b1b18 深灰），而 .page 在 has-wallpaper 下背景透明
   —— 深字压在星空上，几乎看不见。这里补渐变字，并保留 color 兜底：
   不支持 background-clip:text 的浏览器仍是可读的白字而非透明消失。 */
.page-title {
  font-size: var(--text-2xl); font-weight: var(--weight-semibold); letter-spacing: -0.02em;
  color: var(--text);
  background: linear-gradient(135deg, #2dd4bf, #38bdf8);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.page-sub { margin-top: var(--space-1); color: var(--text-3); font-size: var(--text-sm); }

/* 搜索：内层暗玻璃（比 .glass 更暗且不 blur）——
   输入区需要「凹进去」的暗示，与外层卡片同材质会失去层级差。 */
.search-wrap { position: relative; margin-bottom: var(--space-6); }
.search-bar {
  display: flex; align-items: center; gap: var(--space-3);
  padding: var(--space-2) var(--space-2) var(--space-2) var(--space-4);
  background: rgba(0, 0, 0, 0.22);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: var(--radius);
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.3);
  transition: border-color var(--dur) var(--ease), box-shadow var(--dur) var(--ease);
}
/* 聚焦渐变描边 + 光晕。用三层 box-shadow 而非 border-image：
   border-image 做渐变会让圆角失效。 */
.search-bar:focus-within {
  border-color: rgba(45, 212, 191, 0.6);
  box-shadow:
    inset 0 1px 2px rgba(0, 0, 0, 0.3),
    0 0 0 3px rgba(45, 212, 191, 0.16),
    0 0 12px rgba(45, 212, 191, 0.2);
}
.search-icon { width: 20px; height: 20px; color: var(--text-3); flex-shrink: 0; }
.search-bar input { flex: 1; border: none; outline: none; background: transparent; font-size: var(--text-base); color: var(--text); }
.search-bar input::placeholder { color: var(--text-3); }
.clear-btn { width: 30px; height: 30px; display: inline-flex; align-items: center; justify-content: center; border: none; background: var(--surface-3); border-radius: var(--radius-sm); color: var(--text-2); }
.clear-btn svg { width: 16px; height: 16px; }

/* 搜索建议下拉：玻璃浮层。必须是实心深底而非半透明——它浮在商品卡上方，
   半透明会让下方卡片内容透上来叠字。 */
.suggestions {
  position: absolute; top: calc(100% + 6px); left: 0; right: 0; z-index: 50;
  background: rgba(12, 19, 22, 0.96);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: var(--radius);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.5);
  padding: var(--space-2);
}
.suggestion-section + .suggestion-section { border-top: 1px solid var(--border); margin-top: var(--space-2); padding-top: var(--space-2); }
.suggestion-head { display: flex; align-items: center; justify-content: space-between; padding: var(--space-1) var(--space-2); }
.suggestion-item { display: flex; align-items: center; gap: var(--space-3); width: 100%; padding: 9px 10px; text-align: left; border: none; background: transparent; border-radius: var(--radius-sm); color: var(--text-2); font-size: var(--text-base); }
.suggestion-item:hover { background: var(--surface-2); color: var(--text); }
.suggestion-item svg { width: 16px; height: 16px; color: var(--text-3); flex-shrink: 0; }
.suggestion-item span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.filter-toggle { margin-top: var(--space-3); }
/* 高级筛选面板：玻璃。展开后是压在大面积列表上的一层浮层，
   需要比卡片略实一点才有「浮起」的层级感。 */
.filters-panel {
  margin-top: var(--space-3); display: flex; flex-direction: column; gap: var(--space-5);
  padding: var(--space-4);
  border-radius: var(--radius);
  background: rgba(15, 23, 26, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

/* 筛选面板内的输入框。theme.css 的 .input 用 var(--surface) 做底，
   隔离舱已把 --surface 改成白色 6% 叠加，理论上能跟着变暗——
   但底色太浅，深色面板上仍显"浮"，所以这里显式压深并加内阴影，
   与搜索框、Plaza 的 composer 同一套内层暗玻璃。 */
.filters-panel .input,
.filters-panel .textarea,
.filters-panel .select {
  background: rgba(0, 0, 0, 0.22);
  border-color: rgba(255, 255, 255, 0.12);
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.3);
  transition: border-color var(--dur) var(--ease), box-shadow var(--dur) var(--ease);
}
.filters-panel .input:hover,
.filters-panel .textarea:hover,
.filters-panel .select:hover {
  border-color: rgba(255, 255, 255, 0.22);
}
.filters-panel .input:focus,
.filters-panel .textarea:focus,
.filters-panel .select:focus {
  border-color: rgba(45, 212, 191, 0.6);
  box-shadow:
    inset 0 1px 2px rgba(0, 0, 0, 0.3),
    0 0 0 3px rgba(45, 212, 191, 0.16),
    0 0 12px rgba(45, 212, 191, 0.2);
}

/* 幽灵按钮：高级筛选的展开/收起、搜索建议项。
   透明底 + 细描边，hover 才浮现填充与光晕。 */
.btn-ghost-glass {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.16);
  color: var(--text-2);
  border-radius: var(--radius);
  transition: border-color var(--dur) var(--ease), color var(--dur) var(--ease),
    background var(--dur) var(--ease), box-shadow var(--dur) var(--ease);
}
.btn-ghost-glass:hover {
  border-color: rgba(45, 212, 191, 0.5);
  color: var(--text);
  background: rgba(45, 212, 191, 0.09);
  box-shadow: 0 0 12px rgba(45, 212, 191, 0.18);
}
.filter-group .field-label { margin-bottom: var(--space-2); }
.price-range { display: flex; align-items: center; gap: var(--space-3); max-width: 380px; }
.range-sep { color: var(--text-3); }
.error-msg { color: var(--danger); font-size: var(--text-xs); margin-top: var(--space-2); }
.chip-row { display: flex; flex-wrap: wrap; gap: var(--space-2); }
.category-row { margin-top: var(--space-4); }
/* 分类 chips：选中=渐变胶囊+光晕，未选中=幽灵描边（与 Plaza 筛选同款） */
.chip {
  padding: 7px 14px; font-size: var(--text-sm);
  color: var(--text-2); background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: border-color var(--dur) var(--ease), color var(--dur) var(--ease),
    background var(--dur) var(--ease), box-shadow var(--dur) var(--ease),
    transform var(--dur) var(--ease);
}
.chip:hover {
  border-color: rgba(45, 212, 191, 0.45);
  color: var(--text);
  background: rgba(45, 212, 191, 0.08);
  transform: translateX(3px);
}
.chip.active {
  background: var(--grad-brand); border-color: transparent;
  color: #04211d; font-weight: var(--weight-semibold);
  box-shadow: 0 4px 14px rgba(45, 212, 191, 0.32);
}
.chip.active:hover { transform: translateX(3px); }
.filter-actions { display: flex; gap: var(--space-3); }

/* 商品网格 */
/* 列表容器：改为玻璃面板。原本是纯黑 #1a1a1a 硬块，在星空背景上像贴了块胶 */
.products-section {
  min-height: 400px; margin-top: var(--space-6);
  padding: var(--space-6);
  border-radius: 16px;
  background: rgba(15, 23, 26, 0.45);
  -webkit-backdrop-filter: blur(18px) saturate(140%);
  backdrop-filter: blur(18px) saturate(140%);
  border: 1px solid rgba(255, 255, 255, 0.07);
}
.products-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; }

/* 商品卡：玻璃 + hover 上浮 + 青色光晕 + 边框提亮 */
.product-card {
  background: rgba(15, 23, 26, 0.55);
  -webkit-backdrop-filter: blur(18px) saturate(140%);
  backdrop-filter: blur(18px) saturate(140%);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  overflow: hidden; cursor: pointer;
  display: flex; flex-direction: column;
  transition: transform var(--dur-slow) var(--ease),
    box-shadow var(--dur-slow) var(--ease),
    border-color var(--dur) var(--ease);
}
.product-card:hover {
  transform: translateY(-4px);
  border-color: rgba(45, 212, 191, 0.45);
  box-shadow: 0 12px 32px rgba(45, 212, 191, 0.16);
}
.product-image { position: relative; aspect-ratio: 16 / 10; background: rgba(255, 255, 255, 0.03); overflow: hidden; }
.product-img { width: 100%; height: 100%; object-fit: cover; transition: transform .5s var(--ease); }
.product-card:hover .product-img { transform: scale(1.05); }

/* 图片区斜向扫光：与 Plaza 的 feed-images 同一套。
   静态时 opacity:0 完全不可见，靠 opacity 切换而不是 display，
   否则 hover 时会闪一帧。 */
.product-image::after {
  content: "";
  position: absolute; inset: 0; z-index: 1;
  background: var(--grad-sheen);
  opacity: 0; transform: translateX(-100%);
  pointer-events: none;
}
.product-image:hover::after {
  opacity: 1;
  animation: img-sheen-sweep 900ms var(--ease) forwards;
}
@keyframes img-sheen-sweep {
  from { transform: translateX(-100%); }
  to { transform: translateX(100%); }
}

.product-status { position: absolute; top: var(--space-3); right: var(--space-3); z-index: 3; }
/* overlay 提到 z-index 2：必须压过 .product-image::after（扫光层，z-index 1），
   否则扫光会盖在「查看详情」按钮上并挡住点击。 */
.product-overlay { position: absolute; inset: 0; z-index: 2; display: flex; flex-direction: column; justify-content: space-between; padding: 12px; background: linear-gradient(to top, rgba(0, 0, 0, 0.72), rgba(0, 0, 0, 0.14) 55%, rgba(0, 0, 0, 0.45)); opacity: 0; transition: opacity .25s ease; }
.product-image:hover .product-overlay { opacity: 1; }
.overlay-tags { display: flex; flex-wrap: wrap; gap: 8px; }
.overlay-tags span { padding: 4px 10px; border-radius: 999px; background: rgba(255, 255, 255, 0.2); color: #fff; font-size: var(--text-xs); backdrop-filter: blur(4px); }
.overlay-center { margin: auto; text-align: center; color: #fff; transform: translateY(4px); transition: transform .25s ease; }
.product-image:hover .overlay-center { transform: translateY(0); }
.overlay-center h3 { margin: 0 0 10px; font-size: var(--text-lg); }
/* overlay 里的「查看详情」：深色玻璃底 + 渐变描边 hover */
.overlay-btn {
  border: 1px solid rgba(255, 255, 255, 0.3); border-radius: 999px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.1);
  -webkit-backdrop-filter: blur(8px);
  backdrop-filter: blur(8px);
  color: #fff; cursor: pointer;
  transition: background var(--dur) var(--ease), border-color var(--dur) var(--ease),
    box-shadow var(--dur) var(--ease);
}
.overlay-btn:hover {
  border-color: rgba(45, 212, 191, 0.7);
  background: rgba(45, 212, 191, 0.22);
  box-shadow: 0 0 14px rgba(45, 212, 191, 0.35);
}
.product-info { padding: var(--space-4); flex: 1; }
.product-title { font-size: var(--text-base); font-weight: var(--weight-medium); color: var(--text); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.product-desc { margin-top: 4px; font-size: var(--text-sm); color: var(--text-2); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.product-meta { display: flex; gap: var(--space-2); margin: var(--space-3) 0; }
/* ---- 成色标签与瑕疵标记 ---- */
.tag-condition { font-weight: var(--weight-medium); }
/* 成色徽章：保留语义色（越新越绿、越旧越红），但改为渐变底 + 微光晕。
   浅色主题下这些是纯色浅底，深色玻璃上会发灰变脏。 */
.tag-condition.level-0 {
  background: linear-gradient(135deg, #10b981, #2dd4bf); color: #04211d; border-color: transparent;
  box-shadow: 0 2px 10px rgba(16, 185, 129, 0.35);
}
.tag-condition.level-1 {
  background: linear-gradient(135deg, #3b82f6, #38bdf8); color: #05203a; border-color: transparent;
  box-shadow: 0 2px 10px rgba(59, 130, 246, 0.35);
}
.tag-condition.level-2 {
  background: linear-gradient(135deg, #fbbf24, #f59e0b); color: #2b1a02; border-color: transparent;
  box-shadow: 0 2px 10px rgba(251, 191, 36, 0.32);
}
.tag-condition.level-3 {
  background: linear-gradient(135deg, #fb923c, #f97316); color: #2b1200; border-color: transparent;
  box-shadow: 0 2px 10px rgba(251, 146, 60, 0.32);
}
.tag-condition.level-4 {
  background: linear-gradient(135deg, #ef4444, #dc2626); color: #fff; border-color: transparent;
  box-shadow: 0 2px 10px rgba(239, 68, 68, 0.35);
}
.tag-flaw {
  background: linear-gradient(135deg, #ef4444, #b91c1c); color: #fff; border-color: transparent;
  box-shadow: 0 2px 10px rgba(239, 68, 68, 0.32);
}
.tag { padding: 2px 10px; font-size: var(--text-xs); color: var(--text-2); background: rgba(255, 255, 255, 0.07); border-radius: var(--radius-full); }

/* ============================================================================
 * 无图商品占位：分类渐变底 + emoji
 *
 * 原为一个中性灰线框 SVG，所有无图商品长得一模一样，演示时像"图片加载失败"。
 * 改成按分类给不同的渐变与 emoji，让占位本身成为分类的视觉线索。
 * ==========================================================================*/
.image-placeholder {
  width: 100%; height: 100%;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: var(--space-2);
  color: rgba(255, 255, 255, 0.9);
}
.ph-emoji {
  font-size: 40px; line-height: 1;
  /* emoji 在深色渐变上需要一点点投影才不"贴"在背景上 */
  filter: drop-shadow(0 4px 12px rgba(0, 0, 0, 0.4));
}
.ph-label {
  font-size: var(--text-xs);
  color: rgba(255, 255, 255, 0.72);
  letter-spacing: 0.08em;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.5);
}
/* 每个分类一套渐变。用 data 驱动避免堆 7 个重复规则 */
.ph-books       { background: linear-gradient(135deg, #f59e0b, #f97316); }
.ph-electronics { background: linear-gradient(135deg, #3b82f6, #6366f1); }
.ph-transport   { background: linear-gradient(135deg, #10b981, #2dd4bf); }
.ph-gaming      { background: linear-gradient(135deg, #a855f7, #6366f1); }
.ph-clothing    { background: linear-gradient(135deg, #ec4899, #f43f5e); }
.ph-living      { background: linear-gradient(135deg, #14b8a6, #06b6d4); }
.ph-other       { background: linear-gradient(135deg, #64748b, #475569); }
.product-footer { display: flex; align-items: baseline; justify-content: space-between; }

/* 价格：渐变字 + 微光晕。保留 color 兜底——不支持 background-clip:text
   的浏览器上文字仍是 --accent 可读色，而不是透明消失。 */
.product-price {
  display: flex; align-items: baseline; gap: 1px;
  color: var(--accent);
  background: linear-gradient(135deg, #2dd4bf, #38bdf8);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.price-symbol { font-size: var(--text-sm); font-weight: var(--weight-semibold); }
.price-value {
  font-size: var(--text-xl); font-weight: var(--weight-semibold); letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
  filter: drop-shadow(0 0 8px rgba(45, 212, 191, 0.3));
}
/* 浏览数：弱色，不与价格抢注意力 */
.product-views { font-size: var(--text-xs); color: var(--text-3); }

.empty-actions { display: flex; gap: var(--space-3); }

/* 站内搜索建议项：幽灵描边 */
.suggestion-item {
  border: 1px solid transparent;
  transition: background var(--dur) var(--ease), border-color var(--dur) var(--ease);
}
.suggestion-item:hover {
  background: rgba(45, 212, 191, 0.09);
  border-color: rgba(45, 212, 191, 0.3);
}

/* 「在售」chip：玻璃底。深色背景上实心绿会过重，用玻璃底 + 细描边更克制 */
.product-status {
  background: rgba(15, 23, 26, 0.6) !important;
  -webkit-backdrop-filter: blur(8px);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.14);
  color: var(--text);
}

@media (max-width: 1100px) {
  .products-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 720px) {
  .products-grid { grid-template-columns: 1fr; }
  .page-head { flex-direction: column; align-items: flex-start; }
  /* 背景/边框继承下方 .site-footer 的玻璃样式，这里只改间距 */
  .site-footer { margin-top: var(--space-12); }
}
.site-footer {
  margin-top: var(--space-16);
  background: rgba(15, 23, 26, 0.45);
  -webkit-backdrop-filter: blur(18px) saturate(140%);
  backdrop-filter: blur(18px) saturate(140%);
  border-top: 1px solid rgba(255, 255, 255, 0.07);
  padding: var(--space-6) 0;
  color: var(--text-3); font-size: var(--text-sm);
}

/* ============================================================================
 * 降级为静态等价物，而非冻住动画 —— 与 Plaza.vue 的降级表逐条对应
 *
 * 上浮取消后改为「仅边框提亮」：边框变化不依赖运动，仍能传达
 * 「指向了这一项」。若一律写成 animation:none / transition:none，
 * hover 就彻底没有反馈了，用户无法知道哪张卡片可点。
 * ==========================================================================*/
@media (prefers-reduced-motion: reduce) {
  /* 卡片上浮 → 仅边框提亮 + 光晕 */
  .product-card:hover {
    transform: none;
    border-color: rgba(45, 212, 191, 0.45);
    box-shadow: 0 12px 32px rgba(45, 212, 191, 0.16);
  }

  /* 图片放大与扫光取消 */
  .product-card:hover .product-img { transform: none; }
  .product-image::after { animation: none; opacity: 0; }

  /* chips 的平移取消，保留填充与描边 */
  .chip:hover,
  .chip.active:hover { transform: none; }

  /* 搜索建议项保留背景变化 */
  .suggestion-item { transition: none; }

  /* 按钮与输入框的过渡关闭 */
  .chip,
  .product-card,
  .btn-ghost-glass,
  .search-bar,
  .overlay-btn,
  .filters-panel .input,
  .filters-panel .textarea,
  .filters-panel .select { transition: none; }
}
</style>

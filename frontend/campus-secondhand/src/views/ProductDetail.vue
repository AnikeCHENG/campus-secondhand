<template>
  <div class="page">
    <header class="site-header">
      <div class="container header-inner">
        <button class="btn btn-ghost btn-sm" type="button" @click="goBack">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <line x1="19" y1="12" x2="5" y2="12" /><polyline points="12 19 5 12 12 5" />
          </svg>
          返回
        </button>
        <router-link to="/" class="brand">
          <span class="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 12a9 9 0 0 1 15-6.7L21 8" /><path d="M21 3v5h-5" />
              <path d="M21 12a9 9 0 0 1-15 6.7L3 16" /><path d="M3 21v-5h5" />
            </svg>
          </span>
          <span class="brand-name">校园二手</span>
        </router-link>
        <div class="header-actions">
          <button class="icon-btn" type="button" aria-label="退出登录" @click="handleLogout">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
              <polyline points="16 17 21 12 16 7" /><line x1="21" y1="12" x2="9" y2="12" />
            </svg>
          </button>
        </div>
      </div>
    </header>

    <main class="page-main detail-main">
      <div v-if="loading" class="empty">
        <p class="loading-text">[ LOADING… ]</p>
      </div>

      <div v-else-if="error" class="empty">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
        </svg>
        <p>{{ error }}</p>
        <button class="btn btn-outline" type="button" @click="fetchProduct">重新加载</button>
      </div>

      <div v-else-if="product" class="detail">
        <div class="gallery">
          <div class="main-image">
            <img :src="currentImage" :alt="product.title" />
          </div>
          <div v-if="productImages.length > 1" class="thumbnails">
            <button
              v-for="(img, idx) in productImages"
              :key="idx"
              class="thumbnail"
              :class="{ active: currentImage === img }"
              type="button"
              @click="currentImage = img"
            >
              <img :src="img" :alt="`缩略图 ${idx + 1}`" />
            </button>
          </div>
        </div>

        <div class="info">
          <span class="badge" :class="getStatusClass(product.status)">{{ getStatusText(product.status) }}</span>

          <h1 class="title">{{ product.title }}</h1>

          <div class="price">
            <span class="price-symbol">¥</span><span class="price-value">{{ formatPrice(product.price) }}</span>
          </div>

          <div class="meta-row">
            <span class="meta-item">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" /><circle cx="12" cy="10" r="3" />
              </svg>
              {{ product.location || '校园' }}
            </span>
            <span class="meta-item">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <rect x="3" y="4" width="18" height="18" rx="2" /><line x1="16" y1="2" x2="16" y2="6" /><line x1="8" y1="2" x2="8" y2="6" /><line x1="3" y1="10" x2="21" y2="10" />
              </svg>
              {{ formatDate(product.createdTime) }}
            </span>
            <span class="meta-item">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
              {{ product.viewCount || 0 }} 次浏览
            </span>
          </div>

          <div class="tags">
            <span class="tag">{{ getCategoryLabel(product.category) }}</span>
            <span class="tag">{{ getConditionLabel(product.condition) }}</span>
          </div>

          <section class="card block">
            <h3 class="block-title">商品描述</h3>
            <p class="description">{{ product.description || '暂无描述' }}</p>
          </section>

          <section class="card block">
            <h3 class="block-title">卖家信息</h3>
            <div class="seller">
              <div class="seller-avatar">
                <img :src="seller.avatar || '/sample/phone.svg'" :alt="seller.username" />
              </div>
              <div class="seller-info">
                <div class="seller-name">{{ seller.username || '卖家' }}</div>
                <span class="badge badge-accent">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:12px;height:12px">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" /><polyline points="22 4 12 14.01 9 11.01" />
                  </svg>
                  已认证
                </span>
              </div>
              <button class="btn btn-outline" type="button" @click="contactSeller">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                </svg>
                联系卖家
              </button>
            </div>
          </section>

          <div class="actions">
            <button class="btn btn-outline fav-btn" :class="{ 'is-favorited': isFavorited }"
                    type="button" :disabled="favBusy" @click="toggleFavorite">
              <svg v-if="!isFavorited" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78l1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
              </svg>
              <svg v-else :key="favPopKey" class="fav-pop" viewBox="0 0 24 24" fill="currentColor" stroke="none">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78l1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
              </svg>
              {{ isFavorited ? '已收藏' : '收藏' }}
            </button>
            <button v-if="product.status === 1" class="btn btn-primary" type="button" :disabled="buying" @click="buyProduct">
              {{ buying ? '处理中…' : '立即购买' }}
            </button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProductDetail } from '../api/product'
import { getUserById } from '../api/user'
import { createOrder } from '../api/order'
import { checkFavorite, addFavorite, removeFavoriteById } from '../api/favorites'

const router = useRouter()
const route = useRoute()
const product = ref(null)
const seller = ref({})
const loading = ref(true)
const error = ref('')
const currentImage = ref('')
const isFavorited = ref(false)
const favBusy = ref(false)
/** 自增 key，用于重播收藏成功的弹跳动画 */
const favPopKey = ref(0)
const buying = ref(false)

const productImages = computed(() => {
  if (!product.value?.images) return ['/sample/phone.svg']
  const images = product.value.images.split(',')
  return images.length > 0 ? images : ['/sample/phone.svg']
})

async function fetchProduct() {
  loading.value = true
  error.value = ''
  try {
    const res = await getProductDetail(route.params.id)
    if (res.code === 200 && res.data) {
      product.value = res.data
      currentImage.value = productImages.value[0]
      if (product.value.userId) await fetchSellerInfo(product.value.userId)
      await fetchFavoriteState()
    }
  } catch (e) {
    error.value = e.message || '加载商品详情失败'
  } finally {
    loading.value = false
  }
}

/**
 * 切换收藏。
 *
 * 未登录时先引导登录，不直接发请求——否则必然 401 再弹错，体验很差。
 * 收藏/取消均由后端保证幂等，这里不做乐观更新：等后端返回再改状态，
 * 避免「界面显示已收藏但实际没存上」。
 */
async function toggleFavorite() {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录后再收藏')
    router.push({ path: '/login', query: { redirect: `/products/${product.value.id}` } })
    return
  }
  if (favBusy.value) return
  favBusy.value = true
  try {
    const wasFavorited = isFavorited.value
    if (wasFavorited) {
      await removeFavoriteById(product.value.id)
    } else {
      await addFavorite(product.value.id)
    }
    isFavorited.value = !wasFavorited
    // 自增 key 重播弹跳动画
    favPopKey.value++
    ElMessage.success(wasFavorited ? '已取消收藏' : '收藏成功')
  } catch (error) {
    ElMessage.error(error?.cause?.message || error?.message || '操作失败，请稍后重试')
  } finally {
    favBusy.value = false
  }
}

/** 回显收藏状态；未登录静默跳过 */
async function fetchFavoriteState() {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  if (!token) return
  try {
    const res = await checkFavorite(product.value.id)
    if (res.code === 200) isFavorited.value = !!res.data
  } catch {
    // 未登录/网络异常不影响商品展示
  }
}

async function fetchSellerInfo(userId) {
  try {
    const res = await getUserById(userId)
    if (res.code === 200 && res.data) seller.value = res.data
  } catch (e) {
    console.error('Failed to fetch seller info:', e)
  }
}

function goBack() { router.back() }


function contactSeller() {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  if (!token) { alert('请先登录后再联系卖家'); router.push('/login'); return }
  if (!seller.value.username) { alert('获取卖家信息失败，请稍后重试'); return }
  router.push({ path: '/messages', query: { seller: seller.value.username, sellerId: product.value.userId } })
}

async function buyProduct() {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录后再购买商品')
    router.push({ path: '/login', query: { redirect: `/products/${product.value.id}` } })
    return
  }
  buying.value = true
  try {
    const res = await createOrder(product.value.id)
    if (res.code === 200) {
      // 必须用后端返回的订单主键跳转收银台；
      // 原先跳的 /orders 路由并不存在，且无兜底路由，导致渲染空白页
      const orderId = res.data?.id
      if (!orderId) {
        ElMessage.error('订单创建成功但未返回订单号，请重试')
        return
      }
      ElMessage.success('订单已创建，请在 30 分钟内完成支付')
      router.push(`/payment/${orderId}`)
    } else {
      ElMessage.error(res.message || '购买失败，请重试')
    }
  } catch (error) {
    console.error('创建订单错误:', error)
    ElMessage.error(error?.cause?.message || error?.message || '购买失败，请检查网络连接')
  } finally {
    buying.value = false
  }
}

function formatPrice(price) { if (!price) return '0'; return parseFloat(price).toFixed(2) }

function formatDate(timestamp) {
  if (!timestamp) return '未知时间'
  const date = new Date(timestamp)
  const diff = new Date() - date
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  if (minutes < 1) return '刚刚发布'
  if (minutes < 60) return `${minutes}分钟前发布`
  if (hours < 24) return `${hours}小时前发布`
  if (days < 7) return `${days}天前发布`
  return date.toLocaleDateString('zh-CN')
}

function getCategoryLabel(category) {
  const categories = { books: '教材书籍', electronics: '电子产品', transport: '出行工具', gaming: '游戏数码', clothing: '服饰穿搭', living: '生活用品', other: '其他' }
  return categories[category] || category || '未分类'
}
function getConditionLabel(condition) {
  const conditions = { new: '全新', 'like-new': '几乎全新', good: '良好', fair: '一般' }
  return conditions[condition] || condition || '未标明'
}
function getStatusClass(status) { return ({ 0: 'badge', 1: 'badge-success', 2: 'badge-danger' })[status] || 'badge-success' }
function getStatusText(status) { return ({ 0: '已下架', 1: '在售', 2: '已售' })[status] || '在售' }

async function handleLogout() {
  localStorage.removeItem('token'); localStorage.removeItem('username'); sessionStorage.removeItem('justLoggedIn'); router.push('/login')
}

onMounted(fetchProduct)
</script>

<style scoped>
.site-header { position: sticky; top: 0; z-index: 100; background: rgba(255,255,255,0.85); backdrop-filter: saturate(180%) blur(12px); border-bottom: 1px solid var(--border); }
.header-inner { height: var(--header-h); display: flex; align-items: center; gap: var(--space-4); }
.brand { display: inline-flex; align-items: center; gap: var(--space-2); color: var(--text); flex: 1; }
.brand:hover { color: var(--text); }
.brand-mark { width: 30px; height: 30px; display: inline-flex; align-items: center; justify-content: center; border-radius: var(--radius-sm); background: var(--accent); color: ***REMOVED***fff; }
.brand-mark svg { width: 17px; height: 17px; }
.brand-name { font-size: var(--text-lg); font-weight: var(--weight-semibold); letter-spacing: -0.01em; }
.header-actions { display: flex; align-items: center; gap: var(--space-2); }
.icon-btn { width: 38px; height: 38px; display: inline-flex; align-items: center; justify-content: center; border: 1px solid transparent; border-radius: var(--radius); background: transparent; color: var(--text-2); }
.icon-btn:hover { background: var(--surface-3); color: var(--text); }
.icon-btn svg { width: 20px; height: 20px; }

.detail-main { max-width: 1080px; }
.detail { display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-8); }

.gallery { position: sticky; top: calc(var(--header-h) + var(--space-6)); height: fit-content; }
.main-image { width: 100%; aspect-ratio: 1; border-radius: var(--radius-lg); overflow: hidden; background: var(--surface-3); border: 1px solid var(--border); }
.main-image img { width: 100%; height: 100%; object-fit: cover; }
.thumbnails { display: flex; gap: var(--space-2); overflow-x: auto; margin-top: var(--space-3); }
.thumbnail { width: 72px; height: 72px; padding: 0; border-radius: var(--radius-sm); overflow: hidden; border: 1px solid var(--border); background: var(--surface-3); flex-shrink: 0; }
.thumbnail.active { border-color: var(--accent); box-shadow: 0 0 0 2px var(--accent-soft); }
.thumbnail img { width: 100%; height: 100%; object-fit: cover; }

.info { display: flex; flex-direction: column; gap: var(--space-4); }
.title { font-size: var(--text-2xl); font-weight: var(--weight-semibold); letter-spacing: -0.02em; line-height: var(--leading-tight); }
.price { display: flex; align-items: baseline; gap: 4px; color: var(--accent); }
.price-symbol { font-size: var(--text-lg); font-weight: var(--weight-semibold); }
.price-value { font-size: var(--text-4xl); font-weight: var(--weight-semibold); letter-spacing: -0.03em; }

.meta-row { display: flex; gap: var(--space-6); flex-wrap: wrap; }
.meta-item { display: flex; align-items: center; gap: 6px; font-size: var(--text-sm); color: var(--text-2); }
.meta-item svg { width: 16px; height: 16px; color: var(--text-3); }

.tags { display: flex; gap: var(--space-2); flex-wrap: wrap; }
.tag { padding: 4px 12px; font-size: var(--text-sm); color: var(--text-2); background: var(--surface-3); border-radius: var(--radius-full); }

.block { padding: var(--space-5); }
.block-title { font-size: var(--text-base); font-weight: var(--weight-semibold); margin-bottom: var(--space-3); }
.description { font-size: var(--text-base); color: var(--text-2); line-height: var(--leading-relaxed); white-space: pre-wrap; }

.seller { display: flex; align-items: center; gap: var(--space-4); }
.seller-avatar { width: 52px; height: 52px; border-radius: var(--radius-full); overflow: hidden; flex-shrink: 0; border: 1px solid var(--border); background: var(--surface-3); }
.seller-avatar img { width: 100%; height: 100%; object-fit: cover; }
.seller-info { flex: 1; display: flex; flex-direction: column; gap: 6px; align-items: flex-start; }
.seller-name { font-size: var(--text-base); font-weight: var(--weight-medium); }

.actions { display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-3); }
.actions .btn { height: 46px; }

@media (max-width: 900px) {
  .detail { grid-template-columns: 1fr; }
  .gallery { position: static; }
  .title { font-size: var(--text-xl); }
  .price-value { font-size: var(--text-3xl); }
}

/* 收藏按钮：已收藏用 danger 色 + 实心爱心，附弹跳反馈 */
.fav-btn.is-favorited {
  border-color: var(--danger);
  color: var(--danger);
  background: var(--danger-soft);
}
.fav-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.fav-pop {
  animation: fav-pop 420ms var(--ease);
}
@keyframes fav-pop {
  0%   { transform: scale(1); }
  35%  { transform: scale(1.35); }
  60%  { transform: scale(0.92); }
  100% { transform: scale(1); }
}</style>

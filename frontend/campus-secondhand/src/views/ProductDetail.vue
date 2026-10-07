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
            <!-- 成色标签：越新越高亮，用 level-0..4 区分 -->
            <span
              v-if="product.conditionLevel !== null && product.conditionLevel !== undefined"
              class="tag tag-condition"
              :class="'level-' + product.conditionLevel"
            >
              {{ conditionLabel(product.conditionLevel) }}
            </span>
          </div>

          <!-- 瑕疵说明单独成块：这是买家判断值不值钱的关键信息，不能埋在描述里 -->
          <section class="card block flaw-block" :class="{ 'flaw-block--free': isFlawFree }">
            <h3 class="block-title">
              瑕疵说明
              <span v-if="isFlawFree" class="flaw-free-badge">卖家承诺无明显瑕疵</span>
            </h3>
            <p v-if="isFlawFree" class="description flaw-free-text">
              卖家未填写瑕疵说明，交易前建议当面验货确认。
            </p>
            <p v-else class="description flaw-text">{{ product.flawDescription }}</p>
          </section>

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
                <div class="seller-name">
                  {{ seller.username || '卖家' }}
                  <VerifiedBadge :verified="seller.isStudentVerified" size="sm" />
                </div>
                <!-- 好评率：SQL 实时聚合，无评价时显示「暂无评价」而不是 0% -->
                <div v-if="reviewStats.hasReview" class="seller-rating">
                  <StarRating :model-value="Math.round(reviewStats.averageRating || 0)" :readonly="true" :size="13" />
                  <span class="seller-rating__text">
                    好评率 <strong>{{ reviewStats.goodRate }}%</strong>
                    <span class="seller-rating__muted">（{{ reviewStats.total }} 条评价）</span>
                  </span>
                </div>
                <div v-else class="seller-rating">
                  <span class="seller-rating__muted">暂无评价</span>
                </div>
              </div>
<button class="btn btn-outline" type="button"
                      :disabled="isOwnProduct"
                      :title="isOwnProduct ? '这是你自己的商品' : '与卖家沟通'"
                      @click="contactSeller">
                <svg v-if="!isOwnProduct" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                </svg>
                {{ isOwnProduct ? '这是我的商品' : '联系卖家' }}
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
            <!--
  加购按钮四态：
  已加购 1.5s → 已在购物车（点击跳购物车）
  非在售   → 不可购买（禁用）
  自己的   → 整块不渲染
  默认     → 加入购物车
-->
            <button
              v-if="!isOwnProduct"
              ref="cartBtnRef"
              class="btn btn-outline cart-btn"
              :class="{ 'cart-btn--incart': inCart }"
              type="button"
              :disabled="addingToCart || product.status !== 1 || justAdded"
              @click="onCartBtnClick"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
                <circle cx="9" cy="21" r="1.5" /><circle cx="20" cy="21" r="1.5" />
                <path d="M1 1h4l2.7 13.4a2 2 0 0 0 2 1.6h9.7a2 2 0 0 0 2-1.6L23 6H6" />
              </svg>
              {{ cartBtnText }}
            </button>
            <button
              v-if="!isOwnProduct && product.status !== 1"
              class="btn btn-outline cart-btn"
              type="button"
              disabled
            >
              该商品不可购买
            </button>
            <button v-if="product.status === 1" class="btn btn-primary" type="button" :disabled="buying" @click="buyProduct">
              {{ buying ? '处理中…' : '立即购买' }}
            </button>
          </div>
        </div>
      </div>
    </main>
    <!-- 动作 Toast 与飞入动画：加购成功的第2、3 层反馈 -->
    <CartActionToast v-if="toast" :key="toast.key" :message="toast.message" :actions="toast.actions" />
    <FlyToCart ref="flyRef" />

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProductDetail } from '../api/product'
import { getReviewStats } from '../api/review'
import VerifiedBadge from '../components/VerifiedBadge.vue'
import StarRating from '../components/StarRating.vue'
import { getUserById } from '../api/user'
import { createOrder } from '../api/order'
import { addToCart, getCartCount, getCartList, checkoutCart, cartErrorMessage } from '../api/cart'
import CartActionToast from '../components/CartActionToast.vue'
import FlyToCart from '../components/FlyToCart.vue'
import { checkFavorite, addFavorite, removeFavoriteById } from '../api/favorites'

const router = useRouter()
const route = useRoute()
const product = ref(null)
const seller = ref({})
/** 卖家评价统计；默认值对应"暂无评价"，避免首屏渲染时闪出 0% */
const EMPTY_STATS = { total: 0, good: 0, averageRating: 0, goodRate: 0, hasReview: false }
const reviewStats = ref(EMPTY_STATS)
const loading = ref(true)
const error = ref('')
const currentImage = ref('')
const isFavorited = ref(false)
const favBusy = ref(false)
/** 自增 key，用于重播收藏成功的弹跳动画 */
const favPopKey = ref(0)
const buying = ref(false)
const addingToCart = ref(false)
/** 商品是否已在购物车：进页面查一次 /cart/list，不是每次点都靠后端纠正 */
const inCart = ref(false)
/** 加购成功后的 1.5s 过渡态 */
const justAdded = ref(false)
const checkingOut = ref(false)
const cartBtnRef = ref(null)
const flyRef = ref(null)
/** 当前展示的动作 Toast；用 key 强制重建以便连续触发时重新计时 */
const toast = ref(null)

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
      await fetchCartState()
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

/**
 * 进页面查一次购物车，判断该商品是否已加购。
 *
 * <p>不做这一步的话按钮永远显示「加入购物车」，用户会反复点，
 * 而后端每次都回 400「已在购物车」——状态不真实。</p>
 */
async function fetchCartState() {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  if (!token) {
    inCart.value = false
    return
  }
  try {
    const res = await getCartList()
    inCart.value = (res?.data?.items || []).some(
      (item) => Number(item.productId) === Number(product.value.id)
    )
  } catch {
    // 查不到就按未加购处理：多给一个按钮好过让用户失去加购入口
    inCart.value = false
  }
}

async function fetchSellerInfo(userId) {
  try {
    const [info, stats] = await Promise.all([
      getUserById(userId),
      // 好评率拉取失败不应让整个卖家卡片消失，故各自兜底而非一起 await 抛出
      getReviewStats(userId).catch(() => null)
    ])
    if (info.code === 200 && info.data) seller.value = info.data
    reviewStats.value = stats?.code === 200 && stats.data ? stats.data : EMPTY_STATS
  } catch (e) {
    console.error('Failed to fetch seller info:', e)
  }
}

function goBack() { router.back() }


/** 商品是否属于当前登录用户（用于禁用「联系卖家」） */
const isOwnProduct = computed(() => {
  const owner = product.value?.userId
  if (!owner) return false
  const uid = resolveUserId()
  return uid > 0 && Number(owner) === uid
})

/**
 * 取当前登录用户 ID。
 * 兼容两种存储：独立的 userId 键，或 user JSON 里的 id 字段。
 */
function resolveUserId() {
  for (const store of [localStorage, sessionStorage]) {
    const direct = parseInt(store.getItem('userId') || '', 10)
    if (Number.isFinite(direct) && direct > 0) return direct
  }
  for (const store of [localStorage, sessionStorage]) {
    try {
      const raw = store.getItem('user')
      if (!raw) continue
      const id = parseInt(JSON.parse(raw)?.id, 10)
      if (Number.isFinite(id) && id > 0) return id
    } catch { /* 忽略 */ }
  }
  return 0
}

function contactSeller() {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  if (!token) { ElMessage.warning('请先登录后再联系卖家'); router.push('/login'); return }
  if (isOwnProduct.value) { ElMessage.info('这是你自己的商品'); return }
  if (!seller.value.username) { ElMessage.error('获取卖家信息失败，请稍后再试'); return }
  // 同时传 sellerId（聊天页按 ID 开会话）与 productId（带商品上下文）
  router.push({
    path: '/messages',
    query: {
      sellerId: String(product.value.userId),
      productId: String(product.value.id),
      seller: seller.value.username
    }
  })
}

/**
 * 加入购物车：二手孤品，无数量概念。
 *
 * <p>加购成功后走三层反馈——按钮转「已在购物车」、带按钮的动作 Toast、
 * 缩略图飞入导航角标——三处同步，避免用户怀疑点击没生效。</p>
 */
async function addToCartNow() {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录后再加入购物车')
    router.push({ path: '/login', query: { redirect: `/products/${product.value.id}` } })
    return
  }
  addingToCart.value = true
  try {
    await addToCart(product.value.id)
    inCart.value = true
    justAdded.value = true
    // 1.5 秒后由「已加入 ✓」转为永久的「已在购物车」
    setTimeout(() => { justAdded.value = false }, 1500)

    // ② 动作 Toast
    showCartToast('已加入购物车', [
      { label: '查看购物车', onClick: () => router.push('/cart') },
      { label: '去结算', kind: 'primary', onClick: goCheckout }
    ])

    // ③ 缩略图飞入导航购物车图标
    flyRef.value?.fly(product.value.images, cartBtnRef.value)

    // 角标：先本地 +1 让反馈即时，再由服务端计数纠正
    window.dispatchEvent(new CustomEvent('cart:add'))
    try {
      const countRes = await getCartCount()
      window.dispatchEvent(new CustomEvent('cart:count', { detail: countRes?.data?.count ?? null }))
    } catch { /* 忽略：角标稍后会被下次拉取纠正 */ }
  } catch (error) {
    const msg = cartErrorMessage(error, '加入购物车失败，请重试')
    if (msg.includes('已在购物车')) {
      // 重复加购不是错误：给出「去结算」出口，让用户不必先去购物车页
      inCart.value = true
      showCartToast('已在购物车', [{ label: '去结算', kind: 'primary', onClick: goCheckout }])
    } else {
      ElMessage.error(msg)
    }
  } finally {
    addingToCart.value = false
  }
}

/** 加购按钮点击：已在购物车则直接跳购物车，否则执行加购 */
function onCartBtnClick() {
  if (inCart.value) {
    router.push('/cart')
    return
  }
  addToCartNow()
}

const cartBtnText = computed(() => {
  if (addingToCart.value) return '加入中…'
  if (justAdded.value) return '已加入 ✓'
  if (inCart.value) return '已在购物车'
  return '加入购物车'
})

/** 展示带按钮的动作 Toast */
function showCartToast(message, actions) {
  toast.value = { message, actions, key: Date.now() }
}

/**
 * 一键直达批量收银台。
 *
 * <p>取购物车里全部有效商品直接结算，跳过购物车页——想慢慢挑的用户走「查看购物车」。</p>
 */
async function goCheckout() {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  if (!token) {
    router.push({ path: '/login', query: { redirect: `/products/${product.value.id}` } })
    return
  }
  checkingOut.value = true
  try {
    const res = await getCartList()
    const validIds = (res?.data?.items || []).filter((i) => i.valid).map((i) => i.productId)
    if (!validIds.length) {
      ElMessage.warning('购物车里没有可结算的商品')
      return
    }
    const co = await checkoutCart(validIds)
    const orderIds = co?.data?.orderIds || []
    const skipped = co?.data?.skipped || []
    if (!orderIds.length) {
      ElMessage.error('结算失败，购物车内的商品均无法下单')
      return
    }
    if (skipped.length) {
      ElMessage.warning(`有 ${skipped.length} 件商品未能下单，已为你结算其余商品`)
    }
    router.push({ path: '/payment/batch', query: { orderIds: orderIds.join(',') } })
  } catch (e) {
    ElMessage.error(cartErrorMessage(e, '结算失败，请稍后重试'))
  } finally {
    checkingOut.value = false
  }
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
/**
 * 成色等级 0~4 → 中文标签，与后端 {@code ConditionLevel} 一一对应。
 *
 * <p>未知等级返回「未标注成色」而不是悄悄显示"全新"——
 * 成色直接影响买家对价格的预期，错标比不标更糟。</p>
 */
function conditionLabel(level) {
  return { 0: '全新', 1: '99新', 2: '95新', 3: '9成新', 4: '8成新及以下' }[level] ?? '未标注成色'
}

/** 瑕疵说明为空（含 null / 空串 / 纯空白）即视为卖家声明无明显瑕疵 */
const isFlawFree = computed(() => !product.value?.flawDescription?.trim())
function getStatusClass(status) { return ({ 0: 'badge', 1: 'badge-success', 2: 'badge-danger' })[status] || 'badge-success' }
function getStatusText(status) { return ({ 0: '已下架', 1: '在售', 2: '已售' })[status] || '在售' }

async function handleLogout() {
  localStorage.removeItem('token'); localStorage.removeItem('username'); sessionStorage.removeItem('justLoggedIn'); router.push('/login')
}

onMounted(fetchProduct)
</script>

<style scoped>

/* ---- 成色标签与瑕疵说明 ---- */
.tag-condition { font-weight: var(--weight-medium); border-color: var(--border); }
.tag-condition.level-0 { background: #e8f5ee; color: #0b6e54; border-color: #b7e0cc; }
.tag-condition.level-1 { background: #eef4ff; color: #2563eb; border-color: #c7d7fb; }
.tag-condition.level-2 { background: #fdf6e3; color: #b07d12; border-color: #f0dfae; }
.tag-condition.level-3 { background: #fdf1e7; color: #b45309; border-color: #f2d5b8; }
.tag-condition.level-4 { background: #fdecec; color: #c0392b; border-color: #f3c9c9; }
.flaw-block { border-left: 3px solid var(--border); }
.flaw-block--free { border-left-color: #0b6e54; background: #f7fbf9; }
.flaw-free-badge {
  margin-left: var(--space-2);
  padding: 2px var(--space-2);
  border-radius: var(--radius-sm, 4px);
  background: #e8f5ee;
  color: #0b6e54;
  font-size: var(--text-xs);
  font-weight: var(--weight-normal);
}
.flaw-free-text { color: var(--text-3); font-size: var(--text-sm); }
.flaw-text { color: var(--text); white-space: pre-wrap; }
.site-header { position: sticky; top: 0; z-index: 100; background: rgba(255,255,255,0.85); backdrop-filter: saturate(180%) blur(12px); border-bottom: 1px solid var(--border); }
.header-inner { height: var(--header-h); display: flex; align-items: center; gap: var(--space-4); }
.brand { display: inline-flex; align-items: center; gap: var(--space-2); color: var(--text); flex: 1; }
.brand:hover { color: var(--text); }
.brand-mark { width: 30px; height: 30px; display: inline-flex; align-items: center; justify-content: center; border-radius: var(--radius-sm); background: var(--accent); color: #fff; }
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
.seller-name { font-size: var(--text-base); font-weight: var(--weight-medium); display: inline-flex; align-items: center; gap: var(--space-1); }
.seller-rating { display: inline-flex; align-items: center; gap: var(--space-2); }
.seller-rating__text { font-size: var(--text-sm); color: var(--text-2); }
.seller-rating__text strong { color: var(--accent); font-weight: var(--weight-semibold); }
.seller-rating__muted { font-size: var(--text-xs); color: var(--text-3); }

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

<template>
  <div class="page cart-page">
    

    <main class="container cart-main">
      <div class="cart-title-row">
        <h1 class="cart-title">购物车</h1>
        <p class="cart-sub">二手孤品，一物一行，不支持数量选择</p>
      </div>

      <!-- 骨架屏 -->
      <div v-if="loading" class="cart-list">
        <div v-for="i in 3" :key="i" class="card cart-skeleton">
          <div class="sk sk-check"></div>
          <div class="sk sk-thumb"></div>
          <div class="sk sk-lines">
            <div class="sk sk-line"></div>
            <div class="sk sk-line sk-line--short"></div>
          </div>
        </div>
      </div>

      <!-- 加载失败 -->
      <div v-else-if="loadError" class="card cart-state">
        <span class="state-icon state-icon--danger" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="9" /><path d="M12 8v5" /><path d="M12 16h.01" />
          </svg>
        </span>
        <h2 class="state-title">{{ loadError }}</h2>
        <p class="state-desc">请检查网络连接后重试。</p>
        <button type="button" class="btn btn-primary" @click="loadCart">重新加载</button>
      </div>

      <!-- 空状态 -->
      <div v-else-if="items.length === 0" class="card cart-state">
        <span class="state-icon state-icon--muted" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <circle cx="9" cy="21" r="1.5" /><circle cx="20" cy="21" r="1.5" />
            <path d="M1 1h4l2.7 13.4a2 2 0 0 0 2 1.6h9.7a2 2 0 0 0 2-1.6L23 6H6" />
          </svg>
        </span>
        <h2 class="state-title">购物车空空的，去大厅逛逛</h2>
        <p class="state-desc">看到心仪的宝贝可以先加入购物车，稍后再结算。</p>
        <router-link to="/plaza" class="btn btn-primary">去大厅逛逛</router-link>
      </div>

      <!-- 列表 -->
      <template v-else>
        <ul class="cart-list">
          <li
            v-for="item in items"
            :key="item.productId"
            class="card cart-item"
            :class="{ 'is-invalid': !item.valid }"
          >
            <!-- 失效项不可勾选 -->
            <label class="cart-check" :class="{ 'is-disabled': !item.valid }">
              <input
                type="checkbox"
                :checked="selected.has(item.productId)"
                :disabled="!item.valid || checkingOut"
                :aria-label="`选择 ${item.title}`"
                @change="toggleSelect(item.productId)"
              />
              <span class="check-box" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                  <path d="M20 6L9 17l-5-5" />
                </svg>
              </span>
            </label>

            <router-link :to="`/products/${item.productId}`" class="cart-thumb" tabindex="-1" aria-hidden="true">
              <img v-if="item.image && !brokenImages.has(item.productId)" :src="item.image" :alt="item.title"
                   @error="markBroken(item.productId)" />
              <span v-else class="thumb-fallback" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <rect x="3" y="3" width="18" height="18" rx="2" />
                  <circle cx="8.5" cy="8.5" r="1.5" /><path d="M21 15l-5-5L5 21" />
                </svg>
              </span>
            </router-link>

            <div class="cart-info">
              <div class="cart-info-head">
                <router-link :to="`/products/${item.productId}`" class="cart-item-title">{{ item.title }}</router-link>
                <span v-if="!item.valid" class="invalid-tag">{{ invalidReason(item) }}</span>
              </div>
              <p class="cart-seller">卖家：{{ item.sellerName || '未知卖家' }}</p>
              <p class="cart-price mono">¥{{ money(item.price) }}</p>
            </div>

            <button
              type="button"
              class="cart-remove"
              :disabled="removingId === item.productId"
              :aria-label="`移除 ${item.title}`"
              @click="removeItem(item)"
            >
              {{ removingId === item.productId ? '移除中…' : '移除' }}
            </button>
          </li>
        </ul>

        <!-- 底部结算栏 -->
        <div class="cart-bar">
          <button
            type="button"
            class="link-btn"
            :disabled="!invalidItems.length || clearing || checkingOut"
            @click="clearInvalid"
          >
            清空失效{{ invalidItems.length ? `（${invalidItems.length}）` : '' }}
          </button>

          <div class="bar-summary">
            <span class="bar-count">已选 <strong>{{ selectedCount }}</strong> 件</span>
            <span class="bar-total mono">合计 ¥{{ money(selectedTotal) }}</span>
          </div>

          <button
            type="button"
            class="btn btn-primary checkout-btn"
            :disabled="selectedCount === 0 || checkingOut"
            @click="doCheckout"
          >
            {{ checkingOut ? '结算中…' : `结算选中${selectedCount ? `（${selectedCount}）` : ''}` }}
          </button>
        </div>
      </template>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCartList, removeFromCart, checkoutCart, cartErrorMessage } from '../api/cart'

const router = useRouter()

const items = ref([])
const loading = ref(true)
const loadError = ref('')
const checkingOut = ref(false)
const clearing = ref(false)
const removingId = ref(null)
/** 图片加载失败的 productId；用 ref 包裹 Set 便于模板响应式读取 */
const brokenImages = ref(new Set())
/** 已勾选的有效商品 productId 集合。用 Set 保证不会重复计入合计 */
const selected = ref(new Set())

const validItems = computed(() => items.value.filter((i) => i.valid))
const invalidItems = computed(() => items.value.filter((i) => !i.valid))
const selectedCount = computed(() => selected.value.size)
const selectedTotal = computed(() =>
  validItems.value
    .filter((i) => selected.value.has(i.productId))
    .reduce((sum, i) => sum + (Number(i.price) || 0), 0)
)

function money(v) {
  const n = Number(v ?? 0)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

/** 与后端 ProductStatus 对齐：2 已售出、0 已下架 */
function invalidReason(item) {
  if (item.status === 2) return '已失效 · 已被抢下'
  if (item.status === 0) return '已失效 · 已下架'
  return '已失效'
}

function markBroken(productId) {
  const next = new Set(brokenImages.value)
  next.add(productId)
  brokenImages.value = next
}

function toggleSelect(productId) {
  const next = new Set(selected.value)
  if (next.has(productId)) next.delete(productId)
  else next.add(productId)
  selected.value = next
}

async function loadCart() {
  try {
    loading.value = true
    loadError.value = ''
    const res = await getCartList()
    const list = res?.data?.items || []
    items.value = list.map((it) => ({ ...it, valid: it.valid !== false }))
    // 失效项永不勾选；有效项默认全选，减少一次多余点击
    selected.value = new Set(items.value.filter((it) => it.valid).map((it) => it.productId))
  } catch (err) {
    console.error('获取购物车失败:', err)
    loadError.value = cartErrorMessage(err, '购物车加载失败')
    items.value = []
  } finally {
    loading.value = false
  }
}

async function removeItem(item) {
  removingId.value = item.productId
  try {
    await removeFromCart(item.productId)
    items.value = items.value.filter((i) => i.productId !== item.productId)
    const next = new Set(selected.value)
    next.delete(item.productId)
    selected.value = next
    ElMessage.success('已移出购物车')
  } catch (err) {
    ElMessage.error(cartErrorMessage(err, '移出失败'))
  } finally {
    removingId.value = null
  }
}

async function clearInvalid() {
  if (!invalidItems.value.length) return
  clearing.value = true
  const ids = invalidItems.value.map((i) => i.productId)
  try {
    // 逐个删除而非批量：契约只提供 DELETE /cart/{productId} 单条接口
    const results = await Promise.allSettled(ids.map((id) => removeFromCart(id)))
    const failed = results.filter((r) => r.status === 'rejected').length
    if (failed > 0) {
      ElMessage.warning(`已清理 ${ids.length - failed} 项，${failed} 项清理失败`)
    } else {
      ElMessage.success(`已清理 ${ids.length} 项失效商品`)
    }
    await loadCart()
  } finally {
    clearing.value = false
  }
}

async function doCheckout() {
  const productIds = Array.from(selected.value)
  if (!productIds.length) return
  checkingOut.value = true
  try {
    const res = await checkoutCart(productIds)
    const orderIds = res?.data?.orderIds || []
    const skipped = res?.data?.skipped || []

    if (!orderIds.length) {
      ElMessage.error('所选商品均已失效，无法结算')
      await loadCart()
      return
    }

    // 失效项被后端跳过：先警告，再进收银台，避免用户以为商品丢了
    if (skipped.length) {
      const lines = skipped.map((s) => `#${s.productId} ${s.reason || ''}`.trim()).join('\n')
      ElMessageBox.alert(
        `以下商品已被抢下 / 下架，已跳过：\n${lines}`,
        '部分商品未能结算',
        { confirmButtonText: '继续支付', type: 'warning' }
      )
        .catch(() => {})
        .then(() => goBatchPayment(orderIds))
      return
    }

    goBatchPayment(orderIds)
  } catch (err) {
    ElMessage.error(cartErrorMessage(err, '结算失败，请稍后重试'))
    // 结算失败后刷新：可能已被他人抢走，购物车状态需要同步
    await loadCart()
  } finally {
    checkingOut.value = false
  }
}

function goBatchPayment(orderIds) {
  router.push({ path: '/payment/batch', query: { orderIds: orderIds.join(',') } })
}

onMounted(loadCart)
</script>

<style scoped>
.cart-page { min-height: 100vh; background: var(--bg); }



.cart-main { padding: var(--space-8) var(--space-6) var(--space-16); }

.cart-title-row { margin-bottom: var(--space-6); }
.cart-title { font-size: var(--text-2xl); font-weight: var(--weight-semibold); letter-spacing: -0.02em; margin: 0; }
.cart-sub { margin-top: var(--space-1); font-size: var(--text-sm); color: var(--text-2); }

.cart-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: var(--space-3); }

.cart-item {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-4);
}

/* 失效项整体置灰 */
.cart-item.is-invalid { opacity: 0.6; }
.cart-item.is-invalid .cart-thumb { filter: grayscale(1); }

.cart-check { position: relative; display: inline-flex; flex-shrink: 0; cursor: pointer; }
.cart-check.is-disabled { cursor: not-allowed; }
.cart-check input { position: absolute; opacity: 0; width: 0; height: 0; }
.check-box {
  width: 20px; height: 20px; display: inline-flex; align-items: center; justify-content: center;
  border: 1.5px solid var(--border-strong); border-radius: var(--radius-sm);
  background: var(--surface); transition: all var(--dur-fast) var(--ease);
}
.check-box svg { width: 12px; height: 12px; color: #fff; opacity: 0; transition: opacity var(--dur-fast) var(--ease); }
.cart-check input:checked + .check-box { background: var(--accent); border-color: var(--accent); }
.cart-check input:checked + .check-box svg { opacity: 1; }
.cart-check input:focus-visible + .check-box { box-shadow: 0 0 0 3px var(--accent-soft); }
.cart-check.is-disabled .check-box { background: var(--surface-3); border-color: var(--border); }

.cart-thumb {
  width: 84px; height: 84px; flex-shrink: 0; overflow: hidden;
  border-radius: var(--radius); background: var(--surface-3); display: block;
}
.cart-thumb img { width: 100%; height: 100%; object-fit: cover; display: block; }
.thumb-fallback {
  width: 100%; height: 100%; display: inline-flex; align-items: center; justify-content: center;
  color: var(--text-3);
}
.thumb-fallback svg { width: 22px; height: 22px; }

.cart-info { flex: 1; min-width: 0; }
.cart-info-head { display: flex; align-items: center; gap: var(--space-2); flex-wrap: wrap; }
.cart-item-title {
  font-size: var(--text-base); font-weight: var(--weight-medium); color: var(--text);
  text-decoration: none; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 420px;
}
.cart-item-title:hover { color: var(--accent); }

.invalid-tag {
  flex-shrink: 0; padding: 2px 8px; border-radius: var(--radius-full);
  font-size: var(--text-xs); font-weight: var(--weight-medium);
  background: var(--warning-soft); color: var(--warning);
}

.cart-seller { margin: var(--space-1) 0 0; font-size: var(--text-sm); color: var(--text-2); }
.cart-price { margin: var(--space-1) 0 0; font-size: var(--text-md); font-weight: var(--weight-semibold); color: var(--text); }

.cart-remove {
  flex-shrink: 0; padding: 6px 14px; font-size: var(--text-sm);
  color: var(--text-2); background: var(--surface); border: 1px solid var(--border-strong);
  border-radius: var(--radius-sm); cursor: pointer; transition: all var(--dur-fast) var(--ease);
}
.cart-remove:hover:not(:disabled) { color: var(--danger); border-color: var(--danger); background: var(--danger-soft); }
.cart-remove:disabled { opacity: 0.5; cursor: not-allowed; }

/* 底部结算栏 */
.cart-bar {
  position: sticky;
  bottom: var(--space-4);
  margin-top: var(--space-6);
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-4) var(--space-5);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
}

.link-btn {
  background: none; border: none; padding: 0; font-size: var(--text-sm);
  color: var(--text-2); cursor: pointer; text-decoration: underline;
}
.link-btn:hover:not(:disabled) { color: var(--danger); }
.link-btn:disabled { opacity: 0.45; cursor: not-allowed; text-decoration: none; }

.bar-summary { flex: 1; display: flex; align-items: baseline; justify-content: flex-end; gap: var(--space-4); }
.bar-count { font-size: var(--text-sm); color: var(--text-2); }
.bar-count strong { color: var(--text); font-weight: var(--weight-semibold); }
.bar-total { font-size: var(--text-xl); font-weight: var(--weight-semibold); color: var(--text); }

.checkout-btn { flex-shrink: 0; }

/* 骨架屏 */
.cart-skeleton { display: flex; align-items: center; gap: var(--space-4); padding: var(--space-4); }
.sk-lines { flex: 1; display: flex; flex-direction: column; gap: var(--space-2); }
.sk { background: var(--surface-3); border-radius: var(--radius-sm); animation: sk-pulse 1.4s ease-in-out infinite; }
.sk-check { width: 20px; height: 20px; flex-shrink: 0; }
.sk-thumb { width: 84px; height: 84px; flex-shrink: 0; border-radius: var(--radius); }
.sk-line { height: 14px; }
.sk-line--short { width: 40%; }
@keyframes sk-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* 状态页 */
.cart-state {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: var(--space-3); padding: var(--space-16) var(--space-6); text-align: center;
}
.state-icon { width: 56px; height: 56px; display: inline-flex; align-items: center; justify-content: center; border-radius: 50%; }
.state-icon svg { width: 28px; height: 28px; }
.state-icon--muted { background: var(--surface-3); color: var(--text-3); }
.state-icon--danger { background: var(--danger-soft); color: var(--danger); }
.state-title { margin: 0; font-size: var(--text-lg); font-weight: var(--weight-semibold); color: var(--text); }
.state-desc { margin: 0 0 var(--space-2); font-size: var(--text-sm); color: var(--text-2); }

@media (max-width: 640px) {
  .cart-main { padding: var(--space-6) var(--space-4) var(--space-16); }
  .cart-item { flex-wrap: wrap; gap: var(--space-3); }
  .cart-item-title { max-width: 200px; }
  .cart-remove { margin-left: auto; }
  .cart-bar { flex-wrap: wrap; gap: var(--space-3); }
  .bar-summary { order: -1; width: 100%; }
  .checkout-btn { width: 100%; }
}

@media (prefers-reduced-motion: reduce) {
  .sk { animation: none; }
}
</style>
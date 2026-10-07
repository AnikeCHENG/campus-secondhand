<template>
  <div class="page pay-page">
    <!-- 极简 header：支付流程中不挂主导航 -->
    <header class="pay-header">
      <div class="pay-header-inner">
        <button type="button" class="back-btn" @click="goBack">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <path d="M15 18l-6-6 6-6" />
          </svg>
          <span>返回</span>
        </button>
        <span class="pay-header-title">批量收银台</span>
        <router-link to="/" class="pay-brand" aria-label="校园集市首页">
          <span class="pay-brand-mark" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 12a9 9 0 0 1 15-6.7L21 8" /><path d="M21 3v5h-5" />
              <path d="M21 12a9 9 0 0 1-15 6.7L3 16" /><path d="M3 21v-5h5" />
            </svg>
          </span>
          <span class="pay-brand-name">校园集市</span>
        </router-link>
      </div>
    </header>

    <main class="pay-main">
      <!-- 骨架屏：避免合计金额闪现 ¥0.00 -->
      <div v-if="loading" class="pay-layout">
        <div class="card pay-skeleton">
          <div class="sk sk-hero"></div>
          <div class="sk sk-row"></div>
          <div class="sk sk-row"></div>
          <div class="sk sk-block"></div>
        </div>
        <div class="card pay-skeleton">
          <div class="sk sk-row"></div>
          <div class="sk sk-block"></div>
        </div>
      </div>

      <!-- 订单明细部分加载失败：绝不用不完整数据拼合计金额 -->
      <div v-else-if="loadError" class="card pay-state">
        <span class="state-icon state-icon--danger" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="9" /><path d="M12 8v5" /><path d="M12 16h.01" />
          </svg>
        </span>
        <h2 class="state-title">{{ loadError }}</h2>
        <p class="state-desc">
          共 {{ orderIds.length }} 笔订单，其中 {{ failedIds.length }} 笔信息未能加载。
          为避免金额计算错误，需全部加载成功后再支付。
        </p>
        <div class="state-actions">
          <button type="button" class="btn btn-primary" :disabled="loading" @click="loadOrders">
            {{ loading ? '重新加载中…' : '重试' }}
          </button>
          <router-link to="/cart" class="btn btn-outline">返回购物车</router-link>
        </div>
      </div>

      <!-- 无订单号 -->
      <div v-else-if="orderIds.length === 0" class="card pay-state">
        <span class="state-icon state-icon--muted" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="9" /><path d="M15 9l-6 6" /><path d="M9 9l6 6" />
          </svg>
        </span>
        <h2 class="state-title">没有待支付的订单</h2>
        <p class="state-desc">请从购物车发起结算，或查看我的订单。</p>
        <router-link to="/cart" class="btn btn-primary">返回购物车</router-link>
      </div>

      <!-- 支付成功 -->
      <div v-else-if="paySuccess" class="card pay-state">
        <span class="state-icon state-icon--success" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20 6L9 17l-5-5" />
          </svg>
        </span>
        <h2 class="state-title">{{ payFailedCount ? '部分订单支付成功' : '支付成功' }}</h2>
        <p class="state-desc">
          已支付 <strong class="state-amount">{{ money(payTotal) }}</strong> 元，共 {{ successCount }} 笔订单，
          资金由平台托管，确认收货后打给卖家。
        </p>
        <!-- 部分失败必须说出来：只显示"支付成功"会让用户以为全部付掉了 -->
        <p v-if="payFailedCount" class="state-desc state-desc--warn">
          另有 {{ payFailedCount }} 笔订单支付失败（{{ failedReasonText }}），请返回购物车重试。
        </p>
        <p v-if="tradeNo" class="state-trade-no">
          支付流水号 <span class="mono">{{ tradeNo }}</span>
          <span v-if="paymentChannel" class="state-channel">（{{ paymentChannel }} 渠道 · 模拟支付）</span>
        </p>
        <div class="state-actions">
          <router-link to="/profile" class="btn btn-primary">查看订单</router-link>
          <button type="button" class="btn btn-outline" @click="goBack">继续逛逛</button>
        </div>
      </div>

      <!-- 已超时 -->
      <div v-else-if="remain <= 0" class="card pay-state">
        <span class="state-icon state-icon--muted" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="9" /><path d="M15 9l-6 6" /><path d="M9 9l6 6" />
          </svg>
        </span>
        <h2 class="state-title">支付已超时</h2>
        <p class="state-desc">订单已自动取消，商品恢复为在售状态。</p>
        <router-link to="/products" class="btn btn-primary">去看看其他商品</router-link>
      </div>

      <!-- 正常收银台：仅在全部订单加载成功后才渲染，合计金额可信 -->
      <div v-else class="pay-layout">
        <div class="pay-col-main">
          <section class="card pay-hero">
            <div class="hero-left">
              <p class="hero-label">支付金额（共 {{ orders.length }} 笔）</p>
              <p class="hero-amount" :aria-label="`支付金额 ${totalAmount} 元`">
                <span class="hero-cny" aria-hidden="true">¥</span>{{ money(totalAmount) }}
              </p>
              <p class="hero-order-no">订单编号 {{ orders.map(o => o.orderNo).join('、') }}</p>
            </div>
            <div class="hero-right">
              <p class="countdown-label">支付剩余时间</p>
              <p class="countdown mono"
                 :class="{ 'is-urgent': remain < 300 }"
                 :aria-live="remain > 0 ? 'off' : 'assertive'"
                 :role="remain > 0 ? undefined : 'alert'">
                {{ countdownText }}
              </p>
              <p class="countdown-hint">以最早一笔订单为准，超时订单将自动取消</p>
            </div>
          </section>

          <section class="card pay-block">
            <h2 class="block-title">订单清单</h2>
            <ul class="order-list">
              <li v-for="o in orders" :key="o.id" class="order-item">
                <div class="goods-thumb">
                  <img v-if="o.product?.image && !brokenImages.has(o.id)" :src="o.product.image"
                       :alt="o.product.title" @error="markBroken(o.id)" />
                  <span v-else class="goods-thumb-fallback" aria-hidden="true">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                      <rect x="3" y="3" width="18" height="18" rx="2" />
                      <circle cx="8.5" cy="8.5" r="1.5" /><path d="M21 15l-5-5L5 21" />
                    </svg>
                  </span>
                </div>
                <div class="order-info">
                  <p class="order-title">{{ o.product?.title || '商品已删除' }}</p>
                  <p class="order-no mono">{{ o.orderNo }}</p>
                  <p class="order-seller">
                    卖家 {{ o.seller?.nickname || o.seller?.username || '未知卖家' }}
                    <span v-if="o.seller?.studentVerified" class="badge badge-accent">认证学生 · 免服务费</span>
                  </p>
                </div>
                <div class="order-amount">
                  <p class="order-price mono">{{ money(o.total) }} 元</p>
                  <span class="badge badge-warning">{{ orderStatusText(o.status) }}</span>
                </div>
              </li>
            </ul>
          </section>

          <section class="card pay-block">
            <h2 class="block-title">选择支付方式</h2>
            <div class="methods" role="radiogroup" aria-label="支付方式">
              <label v-for="m in methods" :key="m.value"
                     class="method" :class="{ 'is-active': method === m.value }">
                <input type="radio" name="batch-pay-method" :value="m.value"
                       v-model="method" class="method-input" />
                <span class="method-icon" :style="{ background: m.color }" aria-hidden="true">{{ m.glyph }}</span>
                <span class="method-name">{{ m.label }}</span>
                <svg v-if="method === m.value" class="method-check"
                     viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
                  <path d="M20 6L9 17l-5-5" />
                </svg>
              </label>
            </div>
          </section>

          <button type="button"
                  class="btn btn-primary btn-block pay-submit"
                  :disabled="paying || remain <= 0"
                  @click="onPay">
            <span v-if="paying" class="spinner" aria-hidden="true"></span>
            {{ paying ? '支付处理中…' : `确认支付  ¥${money(totalAmount)}` }}
          </button>
          <p class="pay-terms">
            点击「确认支付」即表示你已阅读并同意平台交易担保规则；本操作将一次性支付全部 {{ orders.length }} 笔订单
          </p>

          <div class="escrow">
            <span class="escrow-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <path d="M12 3l7 3v6c0 4.5-3 8.2-7 9-4-.8-7-4.5-7-9V6l7-3z" />
                <path d="M9.5 12l1.8 1.8L15 10" />
              </svg>
            </span>
            <p>资金由平台托管，确认收货后打给卖家</p>
          </div>
        </div>

        <aside class="pay-col-side">
          <section class="card card-flat side-card">
            <h2 class="block-title">金额明细</h2>
            <dl class="amount-list">
              <div class="amount-row">
                <dt>商品金额</dt>
                <dd class="mono">{{ money(goodsTotal) }}</dd>
              </div>
              <div class="amount-row">
                <dt>运费<span class="amount-sub">（校内自提免运费）</span></dt>
                <dd class="mono">{{ money(shippingTotal) }}</dd>
              </div>
              <div class="amount-row">
                <dt>服务费<span class="amount-sub">（0.3%，由卖家承担）</span></dt>
                <dd class="mono amount-fee">{{ money(serviceTotal) }}</dd>
              </div>
              <hr class="divider-line" />
              <div class="amount-row amount-row--total">
                <dt>实付总计</dt>
                <dd class="mono">{{ money(totalAmount) }}</dd>
              </div>
            </dl>
          </section>

          <section class="card card-flat side-card">
            <h2 class="block-title">批量支付说明</h2>
            <ul class="notes">
              <li>一次支付全部 {{ orders.length }} 笔订单，任意一笔失败则整批不支付</li>
              <li>每笔订单生成独立支付流水，可在订单详情中追溯</li>
              <li>仅「待支付」状态的订单可参与本次支付</li>
            </ul>
          </section>
        </aside>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOrderDetail, batchPayOrders } from '../api/order'
import { cartErrorMessage } from '../api/cart'

const route = useRoute()
const router = useRouter()

const orders = ref([])
const failedIds = ref([])
const loading = ref(true)
const loadError = ref('')
const paying = ref(false)
const paySuccess = ref(false)
const successCount = ref(0)
/** 本次支付中失败的订单 [{orderId, reason}] */
const payFailed = ref([])
const tradeNo = ref('')
const paymentChannel = ref('')
const remain = ref(0)
const method = ref('alipay')
const brokenImages = ref(new Set())
const payTotal = ref(0)

let timer = null

const methods = [
  { value: 'alipay', label: '支付宝', glyph: '支', color: '#1677ff' },
  { value: 'wechat', label: '微信支付', glyph: '微', color: '#07c160' },
  { value: 'balance', label: '余额', glyph: '¥', color: '#0b6e54' }
]

/** 从 query 解析 orderIds（逗号分隔）；非法项丢弃 */
const orderIds = computed(() =>
  String(route.query.orderIds || '')
    .split(',')
    .map((s) => s.trim())
    .filter((s) => /^\d+$/.test(s))
    .map(Number)
)

const goodsTotal = computed(() => orders.value.reduce((s, o) => s + num(o.price), 0))
const shippingTotal = computed(() => orders.value.reduce((s, o) => s + num(o.shippingFee), 0))
const serviceTotal = computed(() => orders.value.reduce((s, o) => s + num(o.serviceFee), 0))
/** 合计一律以各单 total 之和为准，与服务端对账口径一致 */
const totalAmount = computed(() => orders.value.reduce((s, o) => s + num(o.total), 0))

const countdownText = computed(() => {
  const total = Math.max(remain.value, 0)
  const mm = String(Math.floor(total / 60)).padStart(2, '0')
  const ss = String(total % 60).padStart(2, '0')
  return `${mm}:${ss}`
})

function num(v) {
  const n = Number(v ?? 0)
  return Number.isFinite(n) ? n : 0
}

function money(v) {
  return num(v).toFixed(2)
}

function orderStatusText(status) {
  return status === 0 ? '待支付' : `状态${status}`
}

function markBroken(id) {
  const next = new Set(brokenImages.value)
  next.add(id)
  brokenImages.value = next
}

function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/cart')
}

/**
 * 并发加载全部订单明细。
 *
 * 契约未提供批量明细接口（TODO：建议后端补 GET /order/batch-detail），
 * 故复用单笔 GET /orders/{id}。用 Promise.allSettled 而非 all：
 * 任一失败只标记该笔为失败，绝不用残缺数据渲染合计金额。
 */
async function loadOrders() {
  const ids = orderIds.value
  if (!ids.length) {
    loading.value = false
    return
  }
  loading.value = true
  loadError.value = ''

  const results = await Promise.allSettled(ids.map((id) => getOrderDetail(id)))

  const ok = []
  const failed = []
  results.forEach((r, i) => {
    if (r.status === 'fulfilled' && r.value?.data?.id) ok.push(r.value.data)
    else failed.push(ids[i])
  })

  failedIds.value = failed
  orders.value = ok

  // 只要有一笔失败就不渲染收银台：合计金额不可信时宁可不付
  loadError.value = failed.length ? '部分订单信息加载失败' : ''
  if (!failed.length) {
    // 倒计时以最早到期的那笔为准
    remain.value = ok.reduce((min, o) => Math.min(min, num(o.remainSeconds)), Number.MAX_SAFE_INTEGER)
    if (!Number.isFinite(remain.value)) remain.value = 0
    startTimer()
  }
  loading.value = false
}

function startTimer() {
  clearInterval(timer)
  timer = setInterval(() => {
    if (remain.value > 0) remain.value -= 1
  }, 1000)
}

const payFailedCount = computed(() => payFailed.value.length)
const failedReasonText = computed(() =>
  payFailed.value.map((f) => `#${f.orderId} ${f.reason || ''}`).join('；')
)

async function onPay() {
  if (paying.value || remain.value <= 0) return
  paying.value = true
  try {
const res = await batchPayOrders(orderIds.value, method.value)
    if (res?.code === 200) {
      payTotal.value = res.data?.totalAmount ?? totalAmount.value
      tradeNo.value = res.data?.tradeNo || ''
      paymentChannel.value = res.data?.paymentChannel || ''
      successCount.value = (res.data?.successIds || []).length
      // 后端逐笔支付，部分失败不会中断整批；这里必须把失败笔数带出来
      payFailed.value = res.data?.failed || []
      paySuccess.value = true
      clearInterval(timer)
      // 结算成功的商品已从购物车移除，重新拉一次角标数量
      window.dispatchEvent(new CustomEvent('cart:count'))
    } else {
      ElMessage.error(res?.message || '支付失败，请稍后重试')
    }
  } catch (err) {
    console.error('批量支付失败:', err)
    ElMessage.error(cartErrorMessage(err, '支付失败，请检查网络连接'))
  } finally {
    paying.value = false
  }
}

onMounted(loadOrders)
onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
.pay-page { min-height: 100vh; background: var(--bg); }

.pay-header {
  position: sticky; top: 0; z-index: 100;
  background: var(--surface); border-bottom: 1px solid var(--border);
}
.pay-header-inner {
  max-width: var(--container); margin: 0 auto;
  padding: 0 var(--space-6); height: var(--header-h);
  display: flex; align-items: center; justify-content: space-between; gap: var(--space-4);
}
.back-btn {
  display: inline-flex; align-items: center; gap: var(--space-2);
  background: none; border: none; cursor: pointer; padding: 0;
  font-size: var(--text-sm); color: var(--text-2);
}
.back-btn:hover { color: var(--text); }
.back-btn svg { width: 16px; height: 16px; }
.pay-header-title { font-size: var(--text-md); font-weight: var(--weight-semibold); color: var(--text); }
.pay-brand { display: inline-flex; align-items: center; gap: var(--space-2); text-decoration: none; }
.pay-brand-mark {
  width: 28px; height: 28px; display: inline-flex; align-items: center; justify-content: center;
  border-radius: var(--radius-sm); background: var(--accent); color: #fff;
}
.pay-brand-mark svg { width: 15px; height: 15px; }
.pay-brand-name { font-size: var(--text-sm); font-weight: var(--weight-semibold); color: var(--text); }

.pay-main { max-width: var(--container); margin: 0 auto; padding: var(--space-8) var(--space-6) var(--space-16); }

.pay-layout {
  display: grid; grid-template-columns: minmax(0, 1fr) 340px;
  gap: var(--space-5); align-items: start;
}
.pay-col-main, .pay-col-side { display: flex; flex-direction: column; gap: var(--space-5); }

.pay-hero { display: flex; align-items: center; justify-content: space-between; gap: var(--space-5); padding: var(--space-6); }
.hero-label { margin: 0 0 var(--space-1); font-size: var(--text-sm); color: var(--text-2); }
.hero-amount { margin: 0; font-size: var(--text-4xl); font-weight: var(--weight-bold); letter-spacing: -0.03em; color: var(--text); line-height: 1.1; }
.hero-cny { font-size: var(--text-xl); margin-right: 2px; }
.hero-order-no { margin: var(--space-2) 0 0; font-size: var(--text-xs); color: var(--text-3); word-break: break-all; }
.hero-right { text-align: right; flex-shrink: 0; }
.countdown-label { margin: 0 0 var(--space-1); font-size: var(--text-sm); color: var(--text-2); }
.countdown { margin: 0; font-size: var(--text-2xl); font-weight: var(--weight-semibold); color: var(--danger); }
.countdown.is-urgent { color: var(--danger); animation: pulse-urgent 1s ease-in-out infinite; }
@keyframes pulse-urgent { 0%, 100% { opacity: 1; } 50% { opacity: 0.55; } }
.countdown-hint { margin: var(--space-1) 0 0; font-size: var(--text-xs); color: var(--text-3); }

.pay-block { padding: var(--space-6); }
.block-title { margin: 0 0 var(--space-4); font-size: var(--text-md); font-weight: var(--weight-semibold); color: var(--text); }

.order-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; }
.order-item {
  display: flex; align-items: center; gap: var(--space-4);
  padding: var(--space-4) 0; border-bottom: 1px solid var(--border);
}
.order-item:last-child { border-bottom: none; padding-bottom: 0; }
.order-item:first-child { padding-top: 0; }

.goods-thumb {
  width: 64px; height: 64px; flex-shrink: 0; overflow: hidden;
  border-radius: var(--radius); background: var(--surface-3);
}
.goods-thumb img { width: 100%; height: 100%; object-fit: cover; display: block; }
.goods-thumb-fallback {
  width: 100%; height: 100%; display: inline-flex; align-items: center; justify-content: center; color: var(--text-3);
}
.goods-thumb-fallback svg { width: 18px; height: 18px; }

.order-info { flex: 1; min-width: 0; }
.order-title {
  margin: 0; font-size: var(--text-base); font-weight: var(--weight-medium); color: var(--text);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.order-no { margin: 2px 0 0; font-size: var(--text-xs); color: var(--text-3); }
.order-seller { margin: 2px 0 0; font-size: var(--text-xs); color: var(--text-2); display: flex; align-items: center; gap: var(--space-2); flex-wrap: wrap; }

.order-amount { text-align: right; flex-shrink: 0; display: flex; flex-direction: column; align-items: flex-end; gap: var(--space-1); }
.order-price { margin: 0; font-size: var(--text-base); font-weight: var(--weight-semibold); color: var(--text); }

.methods { display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--space-3); }
.method {
  position: relative; display: flex; align-items: center; gap: var(--space-2);
  padding: var(--space-3); border: 1px solid var(--border-strong);
  border-radius: var(--radius); cursor: pointer; transition: all var(--dur-fast) var(--ease);
}
.method:hover { border-color: var(--accent); background: var(--accent-soft); }
.method.is-active { border-color: var(--accent); background: var(--accent-soft); }
.method-input { position: absolute; opacity: 0; width: 0; height: 0; }
.method-icon {
  width: 28px; height: 28px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center;
  border-radius: var(--radius-sm); color: #fff; font-size: var(--text-sm); font-weight: var(--weight-semibold);
}
.method-name { font-size: var(--text-sm); color: var(--text); }
.method-check { position: absolute; top: 6px; right: 6px; width: 14px; height: 14px; color: var(--accent); }

.pay-submit { height: 46px; font-size: var(--text-md); }
.pay-terms { margin: var(--space-3) 0 0; font-size: var(--text-xs); color: var(--text-3); text-align: center; }

.escrow {
  display: flex; align-items: center; gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  background: var(--accent-soft); border: 1px solid var(--accent-soft-strong);
  border-radius: var(--radius);
}
.escrow-icon { width: 20px; height: 20px; flex-shrink: 0; color: var(--accent); }
.escrow-icon svg { width: 100%; height: 100%; }
.escrow p { margin: 0; font-size: var(--text-sm); color: var(--text-2); }

.side-card { padding: var(--space-5); }

.amount-list { margin: 0; }
.amount-row { display: flex; align-items: baseline; justify-content: space-between; gap: var(--space-3); padding: var(--space-2) 0; }
.amount-row dt { font-size: var(--text-sm); color: var(--text-2); }
.amount-row dd { margin: 0; font-size: var(--text-sm); color: var(--text); }
.amount-sub { font-size: var(--text-xs); color: var(--text-3); }
.amount-fee { color: var(--text-2); }
.divider-line { border: none; border-top: 1px solid var(--border); margin: var(--space-2) 0; }
.amount-row--total dt { font-size: var(--text-base); font-weight: var(--weight-semibold); color: var(--text); }
.amount-row--total dd { font-size: var(--text-lg); font-weight: var(--weight-semibold); color: var(--text); }

.notes { margin: 0; padding-left: var(--space-5); display: flex; flex-direction: column; gap: var(--space-2); }
.notes li { font-size: var(--text-xs); color: var(--text-2); line-height: 1.6; }

.pay-state {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: var(--space-3); padding: var(--space-16) var(--space-6); text-align: center;
}
.state-icon { width: 60px; height: 60px; display: inline-flex; align-items: center; justify-content: center; border-radius: 50%; }
.state-icon svg { width: 30px; height: 30px; }
.state-icon--success { background: var(--accent-soft); color: var(--accent); }
.state-icon--danger { background: var(--danger-soft); color: var(--danger); }
.state-icon--muted { background: var(--surface-3); color: var(--text-3); }
.state-title { margin: 0; font-size: var(--text-xl); font-weight: var(--weight-semibold); color: var(--text); }
.state-desc { margin: 0; font-size: var(--text-sm); color: var(--text-2); max-width: 460px; }
/* 部分失败用警示色，与全额成功的正常文案区分开 */
.state-desc--warn { color: var(--warning, #b07d12); font-size: var(--text-sm); margin-top: var(--space-2); }
.state-amount { color: var(--text); font-size: var(--text-lg); }
.state-trade-no { margin: 0; font-size: var(--text-xs); color: var(--text-3); }
.state-channel { color: var(--text-3); }
.state-actions { display: flex; gap: var(--space-3); margin-top: var(--space-2); flex-wrap: wrap; justify-content: center; }

.pay-skeleton { padding: var(--space-6); display: flex; flex-direction: column; gap: var(--space-3); }
.sk { background: var(--surface-3); border-radius: var(--radius-sm); animation: sk-pulse 1.4s ease-in-out infinite; }
.sk-hero { height: 56px; }
.sk-row { height: 16px; }
.sk-block { height: 120px; }
@keyframes sk-pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }

.spinner {
  width: 14px; height: 14px; border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.35); border-top-color: #fff;
  display: inline-block; animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 900px) {
  .pay-layout { grid-template-columns: 1fr; }
  .pay-hero { flex-direction: column; align-items: flex-start; }
  .hero-right { text-align: left; }
  .methods { grid-template-columns: 1fr; }
}

@media (prefers-reduced-motion: reduce) {
  .sk, .spinner, .countdown.is-urgent { animation: none; }
}
</style>

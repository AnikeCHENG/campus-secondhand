<template>
  <div class="page pay-page">
    <!-- 极简 header：支付流程中不挂主导航，避免用户跳走 -->
    <header class="pay-header">
      <div class="pay-header-inner">
        <button type="button" class="back-btn" @click="goBack">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
               aria-hidden="true"><path d="M15 18l-6-6 6-6" /></svg>
          <span>返回</span>
        </button>
        <span class="pay-header-title">收银台</span>
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
      <!-- 骨架屏：拉取详情期间占位，避免布局跳动 -->
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

      <div v-else-if="loadError" class="card pay-state">
        <div class="state-icon state-icon--danger" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="9" /><path d="M12 8v5" /><path d="M12 16h.01" />
          </svg>
        </div>
        <h2 class="state-title">{{ loadError }}</h2>
        <p class="state-desc">订单可能已被删除，或你没有查看该订单的权限。</p>
        <router-link to="/profile" class="btn btn-primary">查看我的订单</router-link>
      </div>

      <!-- 支付成功态 -->
      <div v-else-if="paySuccess" class="card pay-state">
        <div class="state-icon state-icon--success" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20 6L9 17l-5-5" />
          </svg>
        </div>
        <h2 class="state-title">支付成功</h2>
        <p class="state-desc">
          已支付 <strong class="state-amount">{{ money(order.total) }}</strong> 元，
          资金由平台托管，确认收货后打给卖家。
        </p>
        <p class="state-order-no">订单编号 {{ order.orderNo }}</p>
        <p v-if="tradeNo" class="state-trade-no">
          支付流水号 <span class="mono">{{ tradeNo }}</span>
          <span v-if="paymentChannel" class="state-channel">（{{ paymentChannel }} 渠道 · 模拟支付）</span>
        </p>
        <div class="state-actions">
          <router-link to="/profile" class="btn btn-primary">查看订单</router-link>
          <button type="button" class="btn btn-outline" @click="goBack">继续逛逛</button>
        </div>
      </div>

      <!-- 订单已取消 / 超时 -->
      <div v-else-if="order.status === 4" class="card pay-state">
        <div class="state-icon state-icon--muted" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="9" /><path d="M15 9l-6 6" /><path d="M9 9l6 6" />
          </svg>
        </div>
        <h2 class="state-title">订单已取消</h2>
        <p class="state-desc">支付超时或你已取消该订单，商品已恢复为在售状态。</p>
        <router-link to="/products" class="btn btn-primary">去看看其他商品</router-link>
      </div>

      <!-- 已支付（非本次会话）状态降级：隐藏支付区 -->
      <div v-else-if="order.status !== 0" class="card pay-state">
        <div class="state-icon state-icon--success" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20 6L9 17l-5-5" />
          </svg>
        </div>
        <h2 class="state-title">订单{{ order.statusLabel }}</h2>
        <p class="state-desc">
          订单编号 {{ order.orderNo }}，实付 <strong>{{ money(order.total) }}</strong> 元。
        </p>
        <router-link to="/profile" class="btn btn-primary">查看订单详情</router-link>
      </div>

      <!-- 正常收银台 -->
      <div v-else class="pay-layout">
        <div class="pay-col-main">
          <!-- 支付金额 + 倒计时 -->
          <section class="card pay-hero">
            <div class="hero-left">
              <p class="hero-label">支付金额</p>
              <!-- 金额为 44px 大字，--warning 在白卡上对比度 3.63:1，满足 AA 大字标准 -->
              <p class="hero-amount" :aria-label="`支付金额 ${order.total} 元`">
                <span class="hero-cny" aria-hidden="true">¥</span>{{ money(order.total) }}
              </p>
              <p class="hero-order-no">
                订单编号 <span class="mono">{{ order.orderNo }}</span>
              </p>
            </div>
            <div class="hero-right">
              <p class="countdown-label">支付剩余时间</p>
              <!-- 倒计时用 --text-2 → --danger，避免与金额的赭色语义冲突 -->
              <p class="countdown mono"
                 :class="{ 'is-urgent': remain < 300 }"
                 :aria-live="remain > 0 ? 'off' : 'assertive'"
                 :role="remain > 0 ? undefined : 'alert'">
                {{ countdownText }}
              </p>
              <p class="countdown-hint">超时订单将自动取消</p>
            </div>
          </section>

          <!-- 商品快照 -->
          <section class="card pay-block">
            <h2 class="block-title">商品信息</h2>
            <div class="goods">
              <div class="goods-thumb">
                <img v-if="order.product?.image && !imgBroken" :src="order.product.image"
                     :alt="order.product.title" @error="imgBroken = true" />
                <span v-else class="goods-thumb-fallback" aria-hidden="true">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <rect x="3" y="3" width="18" height="18" rx="2" />
                    <circle cx="8.5" cy="8.5" r="1.5" /><path d="M21 15l-5-5L5 21" />
                  </svg>
                </span>
              </div>
              <div class="goods-info">
                <p class="goods-title">{{ order.product?.title || '商品已删除' }}</p>
                <p class="goods-price mono">{{ money(order.price) }} 元</p>
                <span v-if="order.seller?.studentVerified" class="badge badge-accent">
                  认证学生 · 免服务费
                </span>
              </div>
            </div>
          </section>

          <!-- 金额明细 -->
          <section class="card pay-block">
            <h2 class="block-title">金额明细</h2>
            <dl class="amount-list">
              <div class="amount-row">
                <dt>商品金额</dt>
                <dd class="mono">{{ money(order.price) }}</dd>
              </div>
              <div class="amount-row">
                <dt>运费<span class="amount-sub">（校内自提免运费）</span></dt>
                <dd class="mono">{{ money(order.shippingFee) }}</dd>
              </div>
              <div class="amount-row">
                <dt>服务费<span class="amount-sub">（0.3%，由卖家承担）</span></dt>
                <dd class="mono amount-fee">{{ money(order.serviceFee) }}</dd>
              </div>
              <hr class="divider-line" />
              <div class="amount-row amount-row--total">
                <!-- 15px 正文达不到 4.5:1，故总计用 --text 而非赭色 -->
                <dt>实付总计</dt>
                <dd class="mono">{{ money(order.total) }}</dd>
              </div>
            </dl>
          </section>

          <!-- 支付方式 -->
          <section class="card pay-block">
            <h2 class="block-title">选择支付方式</h2>
            <div class="methods" role="radiogroup" aria-label="支付方式">
              <label v-for="m in methods" :key="m.value"
                     class="method"
                     :class="{ 'is-active': method === m.value }">
                <input type="radio" name="pay-method" :value="m.value"
                       v-model="method" class="method-input" />
                <span class="method-icon" :style="{ background: m.color }" aria-hidden="true">
                  {{ m.glyph }}
                </span>
                <span class="method-name">{{ m.label }}</span>
                <svg v-if="method === m.value" class="method-check"
                     viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"
                     aria-hidden="true"><path d="M20 6L9 17l-5-5" /></svg>
              </label>
            </div>
          </section>

          <!-- 确认支付 -->
          <button type="button"
                  class="btn btn-primary btn-block pay-submit"
                  :disabled="paying || remain <= 0"
                  @click="onPay">
            <span v-if="paying" class="spinner" aria-hidden="true"></span>
            {{ paying ? '支付处理中…' : `确认支付  ¥${money(order.total)}` }}
          </button>
          <p class="pay-terms">
            点击「确认支付」即表示你已阅读并同意平台交易担保规则
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

        <!-- 侧栏 -->
        <aside class="pay-col-side">
          <section class="card card-flat side-card">
            <h2 class="block-title">卖家</h2>
            <div class="seller">
              <span class="seller-avatar">
                <img v-if="order.seller?.avatar && !avatarBroken" :src="order.seller.avatar"
                     :alt="`${order.seller.nickname} 的头像`" @error="avatarBroken = true" />
                <span v-else aria-hidden="true">{{ (order.seller?.nickname || '?').charAt(0) }}</span>
              </span>
              <div class="seller-info">
                <p class="seller-name">{{ order.seller?.nickname || '未知卖家' }}</p>
                <span v-if="order.seller?.studentVerified" class="badge badge-success">
                  认证学生
                </span>
              </div>
            </div>
            <div class="escrow escrow--side">
              <span class="escrow-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                  <path d="M12 3l7 3v6c0 4.5-3 8.2-7 9-4-.8-7-4.5-7-9V6l7-3z" />
                  <path d="M9.5 12l1.8 1.8L15 10" />
                </svg>
              </span>
              <p>资金由平台托管，确认收货后打给卖家</p>
            </div>
          </section>

          <section class="card card-flat side-card">
            <h2 class="block-title">订单信息</h2>
            <dl class="meta-list">
              <div class="meta-row">
                <dt>订单编号</dt>
                <dd class="mono">{{ order.orderNo }}</dd>
              </div>
              <div class="meta-row">
                <dt>下单时间</dt>
                <dd>{{ formatTime(order.createdTime) }}</dd>
              </div>
              <div class="meta-row">
                <dt>支付截止</dt>
                <dd>{{ formatTime(order.expireTime) }}</dd>
              </div>
              <div class="meta-row">
                <dt>订单状态</dt>
                <dd><span class="badge badge-warning">{{ order.statusLabel }}</span></dd>
              </div>
            </dl>
          </section>

          <button type="button" class="btn btn-ghost btn-block cancel-btn"
                  :disabled="cancelling || remain <= 0" @click="onCancel">
            {{ cancelling ? '取消中…' : '取消订单' }}
          </button>
        </aside>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderDetail, payOrder, cancelOrderById } from '../api/order'

const route = useRoute()
const router = useRouter()

const order = ref({})
const loading = ref(true)
const loadError = ref('')
const paying = ref(false)
const cancelling = ref(false)
const paySuccess = ref(false)
const tradeNo = ref('')
const paymentChannel = ref('')
const remain = ref(0)
const method = ref('alipay')
const imgBroken = ref(false)
const avatarBroken = ref(false)

let timer = null

// 品牌色仅用于支付方式图标识别，不参与界面配色体系
const methods = [
  { value: 'alipay', label: '支付宝', glyph: '支', color: '#1677ff' },
  { value: 'wechat', label: '微信支付', glyph: '微', color: '#07c160' },
  { value: 'balance', label: '余额', glyph: '¥', color: '#0b6e54' }
]

const countdownText = computed(() => {
  const total = Math.max(remain.value, 0)
  const mm = String(Math.floor(total / 60)).padStart(2, '0')
  const ss = String(total % 60).padStart(2, '0')
  return `${mm}:${ss}`
})

function money(v) {
  const n = Number(v ?? 0)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

function formatTime(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 19)
}

function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/')
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await getOrderDetail(route.params.orderId)
    order.value = res.data || {}
    remain.value = Number(order.value.remainSeconds ?? 0)
    startTimer()
  } catch (e) {
    loadError.value = e?.cause?.message || e?.message || '订单加载失败'
  } finally {
    loading.value = false
  }
}

function startTimer() {
  clearInterval(timer)
  // 仅待支付订单需要倒计时
  if (order.value.status !== 0 || remain.value <= 0) return
  timer = setInterval(() => {
    remain.value -= 1
    if (remain.value <= 0) {
      clearInterval(timer)
      remain.value = 0
      onTimeout()
    }
  }, 1000)
}

/** 倒计时归零：自动取消并释放商品 */
async function onTimeout() {
  if (order.value.status !== 0) return
  try {
    await cancelOrderById(order.value.id)
    order.value = { ...order.value, status: 4, statusLabel: '已取消' }
    ElMessage.warning('订单已超时取消，商品已恢复在售')
  } catch {
    // 服务端的惰性过期检查可能已先行取消，静默降级即可
    order.value = { ...order.value, status: 4, statusLabel: '已取消' }
  }
}

async function onPay() {
  if (!method.value) {
    ElMessage.warning('请选择支付方式')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认支付 ¥${money(order.value.total)} 给卖家 ${order.value.seller?.nickname || ''}？`,
      '确认支付',
      { confirmButtonText: '确认支付', cancelButtonText: '再想想', type: 'warning' }
    )
  } catch {
    return
  }

  paying.value = true
  try {
    const res = await payOrder(order.value.id, method.value)
    order.value = res.data || order.value
    tradeNo.value = res.data?.tradeNo || order.value.transactionId || ''
    paymentChannel.value = res.data?.paymentChannel || ''
    clearInterval(timer)
    paySuccess.value = true
    ElMessage.success('支付成功')
  } catch (e) {
    ElMessage.error(e?.cause?.message || e?.message || '支付失败，请重试')
    // 状态可能已变化（如超时被服务端取消），重新拉取
    load()
  } finally {
    paying.value = false
  }
}

async function onCancel() {
  try {
    await ElMessageBox.confirm(
      '取消后商品将恢复为在售状态，你可以重新购买。确定取消该订单吗？',
      '取消订单',
      { confirmButtonText: '确定取消', cancelButtonText: '暂不取消', type: 'warning' }
    )
  } catch {
    return
  }

  cancelling.value = true
  try {
    const res = await cancelOrderById(order.value.id)
    order.value = res.data || { ...order.value, status: 4, statusLabel: '已取消' }
    clearInterval(timer)
    ElMessage.success('订单已取消，商品已恢复在售')
  } catch (e) {
    ElMessage.error(e?.cause?.message || e?.message || '取消失败，请重试')
  } finally {
    cancelling.value = false
  }
}

onMounted(load)
onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
.pay-page {
  min-height: 100vh;
  padding-bottom: var(--space-16);
}

/* ---------- 极简 header ---------- */
.pay-header {
  position: sticky;
  top: 0;
  z-index: 10;
  background: var(--surface);
  border-bottom: 1px solid var(--border);
}
.pay-header-inner {
  max-width: 920px;
  margin: 0 auto;
  padding: 0 var(--space-6);
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  height: 32px;
  padding: 0 var(--space-2);
  font-size: var(--text-sm);
  color: var(--text-2);
  background: transparent;
  border: 0;
  border-radius: var(--radius-sm);
  transition: color var(--dur-fast) var(--ease), background var(--dur-fast) var(--ease);
}
.back-btn:hover {
  color: var(--text);
  background: var(--surface-3);
}
.back-btn svg { width: 16px; height: 16px; }
.pay-header-title {
  font-size: var(--text-base);
  font-weight: var(--weight-medium);
  color: var(--text);
}
.pay-brand {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--text-2);
}
.pay-brand-mark {
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border-radius: var(--radius-full);
  background: var(--accent-soft);
  color: var(--accent);
}
.pay-brand-mark svg { width: 14px; height: 14px; }
.pay-brand-name { font-size: var(--text-sm); font-weight: var(--weight-medium); }

/* ---------- 布局 ---------- */
.pay-main {
  max-width: 920px;
  margin: 0 auto;
  padding: var(--space-8) var(--space-6) 0;
}
.pay-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: var(--space-5);
  align-items: start;
}
.pay-col-main { display: flex; flex-direction: column; gap: var(--space-4); }
.pay-col-side {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  position: sticky;
  top: calc(56px + var(--space-5));
}

.block-title {
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--text-2);
  margin-bottom: var(--space-4);
  letter-spacing: 0.01em;
}

.mono {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
}

/* ---------- 支付金额 hero ---------- */
.pay-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-6);
}
.hero-label {
  font-size: var(--text-sm);
  color: var(--text-2);
  margin-bottom: var(--space-2);
}
/* 44px 大字：#b07d12 在白卡上 3.63:1，满足 WCAG AA 大字(≥24px)标准 */
.hero-amount {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: var(--text-4xl);
  font-weight: var(--weight-semibold);
  line-height: var(--leading-tight);
  letter-spacing: -0.02em;
  color: var(--warning);
}
.hero-cny {
  font-size: var(--text-2xl);
  font-weight: var(--weight-medium);
  margin-right: 2px;
}
.hero-order-no {
  margin-top: var(--space-3);
  font-size: var(--text-sm);
  color: var(--text-2);
}
.hero-order-no .mono { color: var(--text-3); }

.hero-right { text-align: right; flex-shrink: 0; }
.countdown-label {
  font-size: var(--text-xs);
  color: var(--text-2);
}
.countdown {
  margin: var(--space-1) 0;
  font-size: var(--text-xl);
  font-weight: var(--weight-semibold);
  color: var(--text-2);
}
/* 剩余不足 5 分钟转 --danger(5.44:1)，刻意不用 --warning 以免与金额撞色 */
.countdown.is-urgent { color: var(--danger); }
.countdown-hint { font-size: var(--text-xs); color: var(--text-3); }

/* ---------- 商品快照 ---------- */
.goods { display: flex; gap: var(--space-4); }
.goods-thumb {
  width: 76px;
  height: 76px;
  flex-shrink: 0;
  border-radius: var(--radius);
  overflow: hidden;
  background: var(--surface-3);
  border: 1px solid var(--border);
  display: grid;
  place-items: center;
}
.goods-thumb img { width: 100%; height: 100%; object-fit: cover; }
.goods-thumb-fallback { color: var(--text-3); }
.goods-thumb-fallback svg { width: 28px; height: 28px; }
.goods-info { display: flex; flex-direction: column; gap: var(--space-1); align-items: flex-start; }
.goods-title {
  font-size: var(--text-base);
  font-weight: var(--weight-medium);
  color: var(--text);
}
.goods-price { font-size: var(--text-sm); color: var(--text-2); }

/* ---------- 金额明细 ---------- */
.amount-list { margin: 0; }
.amount-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-2) 0;
  font-size: var(--text-base);
  color: var(--text-2);
}
.amount-row dd { margin: 0; color: var(--text); }
.amount-sub { color: var(--text-3); font-size: var(--text-sm); }
.amount-fee { color: var(--text-2); }
.amount-row--total {
  font-weight: var(--weight-semibold);
  color: var(--text);
  font-size: var(--text-md);
}
.amount-row--total dd { font-weight: var(--weight-semibold); }
.divider-line { margin: var(--space-3) 0; }

/* ---------- 支付方式 ---------- */
.methods {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-3);
}
.method {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-4) var(--space-2);
  border: 1px solid var(--border-strong);
  border-radius: var(--radius);
  background: var(--surface);
  cursor: pointer;
  transition: border-color var(--dur-fast) var(--ease),
              background var(--dur-fast) var(--ease);
}
.method:hover { border-color: var(--text-3); }
.method.is-active {
  border-color: var(--accent);
  background: var(--accent-soft);
}
.method-input {
  position: absolute;
  opacity: 0;
  width: 1px;
  height: 1px;
}
.method:focus-within {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.method-icon {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: var(--radius-full);
  color: #fff;
  font-size: var(--text-md);
  font-weight: var(--weight-semibold);
}
.method-name { font-size: var(--text-sm); color: var(--text); }
.method-check {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 16px;
  height: 16px;
  color: var(--accent);
}

/* ---------- 确认支付 ---------- */
.pay-submit {
  height: 56px;
  font-size: var(--text-md);
  font-weight: var(--weight-semibold);
  border-radius: var(--radius-lg);
}
.pay-terms {
  margin-top: calc(var(--space-3) * -1);
  font-size: var(--text-xs);
  color: var(--text-3);
  text-align: center;
}
.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid currentColor;
  border-right-color: transparent;
  border-radius: var(--radius-full);
  animation: pay-spin 640ms linear infinite;
}
@keyframes pay-spin { to { transform: rotate(360deg); } }

/* ---------- 担保提示 ---------- */
.escrow {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4);
  background: var(--surface-2);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  font-size: var(--text-sm);
  color: var(--text-2);
}
.escrow-icon { color: var(--accent); flex-shrink: 0; }
.escrow-icon svg { width: 20px; height: 20px; }
.escrow--side { margin-top: var(--space-4); padding: var(--space-3); font-size: var(--text-xs); }

/* ---------- 侧栏 ---------- */
.side-card { padding: var(--space-5); }
.seller { display: flex; align-items: center; gap: var(--space-3); }
.seller-avatar {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: var(--radius-full);
  overflow: hidden;
  background: var(--accent-soft);
  color: var(--accent);
  font-weight: var(--weight-semibold);
}
.seller-avatar img { width: 100%; height: 100%; object-fit: cover; }
.seller-info { display: flex; flex-direction: column; gap: 2px; align-items: flex-start; }
.seller-name { font-size: var(--text-base); font-weight: var(--weight-medium); }

.meta-list { margin: 0; }
.meta-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-2) 0;
  font-size: var(--text-sm);
  color: var(--text-2);
}
.meta-row dd { margin: 0; color: var(--text); text-align: right; word-break: break-all; }
.cancel-btn { font-size: var(--text-sm); }

/* ---------- 状态页 ---------- */
.pay-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-16) var(--space-6);
  text-align: center;
}
.state-icon { display: grid; place-items: center; width: 56px; height: 56px; border-radius: var(--radius-full); }
.state-icon svg { width: 28px; height: 28px; }
.state-icon--success { background: var(--success-soft); color: var(--success); }
.state-icon--danger { background: var(--danger-soft); color: var(--danger); }
.state-icon--muted { background: var(--surface-3); color: var(--text-3); }
.state-title { font-size: var(--text-xl); font-weight: var(--weight-semibold); }
.state-desc { font-size: var(--text-sm); color: var(--text-2); max-width: 42ch; }
.state-amount { font-family: var(--font-mono); color: var(--text); }
.state-order-no { font-size: var(--text-xs); color: var(--text-3); font-family: var(--font-mono); }
.state-trade-no {
  margin-top: var(--space-3);
  padding: var(--space-2) var(--space-3);
  font-size: var(--text-xs);
  color: var(--text-2);
  background: var(--surface-2);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
}
.state-trade-no .mono { color: var(--text); }
.state-channel { color: var(--text-3); }
.state-actions { display: flex; gap: var(--space-3); margin-top: var(--space-3); }

/* ---------- 骨架屏 ---------- */
.pay-skeleton { display: flex; flex-direction: column; gap: var(--space-4); }
.sk {
  border-radius: var(--radius-sm);
  background: var(--surface-3);
  animation: pay-pulse 1.4s ease-in-out infinite;
}
.sk-hero { height: 56px; width: 60%; }
.sk-row { height: 16px; width: 80%; }
.sk-block { height: 120px; width: 100%; }
@keyframes pay-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* ---------- 响应式 ---------- */
@media (max-width: 768px) {
  .pay-layout { grid-template-columns: minmax(0, 1fr); }
  .pay-col-side { position: static; }
  .pay-hero { flex-direction: column; gap: var(--space-4); }
  .hero-right { text-align: left; }
  .methods { grid-template-columns: 1fr; }
  .method { flex-direction: row; justify-content: flex-start; gap: var(--space-3); }
  .method-check { top: 50%; transform: translateY(-50%); }
}
</style>

const BASE = import.meta.env.VITE_API_BASE || '/api'

/**
 * 购物车与批量收银台接口。
 *
 * 约定：
 * - 二手孤品，购物车无数量字段，一物一行；
 * - 商品有效性由后端以 ProductStatus 判定（1=在售为有效，0已下架/2已售出为失效），
 *   前端不自行推断 status，直接采信 list 接口返回的 valid 字段。
 */
async function request(path, options = {}) {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  try {
    const headers = { 'Content-Type': 'application/json' }
    if (token) headers['Authorization'] = `Bearer ${token}`
    const res = await fetch(`${BASE}${path}`, { headers, ...options })
    const data = await res.json().catch(() => ({ message: '服务器返回非JSON' }))
    if (!res.ok) {
      const err = new Error(data?.message || `HTTP ${res.status}`)
      err.response = data
      throw err
    }
    if (data && typeof data.code === 'number' && data.code !== 200) {
      const err = new Error(data.message || '请求未成功')
      err.response = data
      throw err
    }
    return data
  } catch (e) {
    const err = new Error('Network request failed')
    err.cause = e
    throw err
  }
}

/** 取可读的业务错误文案：优先后端 message，其次网络层 cause */
export function cartErrorMessage(err, fallback = '操作失败，请稍后重试') {
  return err?.response?.message || err?.cause?.message || err?.message || fallback
}

/** 加入购物车。400 情形由后端拦截：自己的商品 / status!=1 / 已在购物车 */
export async function addToCart(productId) {
  return await request('/cart/add', {
    method: 'POST',
    body: JSON.stringify({ productId })
  })
}

/** 移出购物车（二手孤品按 productId 定位，无数量概念） */
export async function removeFromCart(productId) {
  return await request(`/cart/${productId}`, { method: 'DELETE' })
}

/**
 * 购物车列表。
 * 返回 { items: [{ productId, title, image, price, status, sellerName, valid }] }
 */
export async function getCartList() {
  return await request('/cart/list')
}

/** 导航角标：返回 { count } */
export async function getCartCount() {
  return await request('/cart/count')
}

/**
 * 结算选中项。
 * 失效商品自动跳过不中断整单，返回
 * { orderIds: [], totalAmount, skipped: [{ productId, reason }] }
 */
export async function checkoutCart(productIds) {
  return await request('/cart/checkout', {
    method: 'POST',
    body: JSON.stringify({ productIds })
  })
}
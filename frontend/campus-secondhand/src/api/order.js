const BASE = import.meta.env.VITE_API_BASE || '/api'

async function request(path, options = {}) {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  try {
    const headers = {
      'Content-Type': 'application/json'
    }
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }
    const res = await fetch(`${BASE}${path}`, {
      headers,
      ...options,
    })
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

/** 未登录/请求失败时的演示数据，形状必须与真实分页返回一致 */
const MOCK_ORDERS = [
  { id: 'ORD20260315001', productId: 1, productTitle: '篮球', productImage: '/src/images/篮球.avif', price: '129.00', status: 0, createdAt: '2026-03-15T10:00:00', sellerId: 2, sellerName: '张三' },
  { id: 'ORD20260315002', productId: 2, productTitle: '足球', productImage: '/src/images/足球.avif', price: '99.00', status: 0, createdAt: '2026-03-15T09:30:00', sellerId: 3, sellerName: '李四' },
  { id: 'ORD20260315003', productId: 3, productTitle: '羽毛球拍', productImage: '/src/images/羽毛球拍.avif', price: '89.00', status: 0, createdAt: '2026-03-15T09:00:00', sellerId: 4, sellerName: '王五' },
  { id: 'ORD20260315004', productId: 4, productTitle: '乒乓球拍', productImage: '/src/images/乒乓球拍.avif', price: '59.00', status: 0, createdAt: '2026-03-15T08:30:00', sellerId: 5, sellerName: '赵六' },
  { id: 'ORD20260315005', productId: 5, productTitle: '小哑铃', productImage: '/src/images/小哑铃.avif', price: '159.00', status: 0, createdAt: '2026-03-15T08:00:00', sellerId: 6, sellerName: '孙七' },
  { id: 'ORD20260315006', productId: 6, productTitle: '跳绳', productImage: '/src/images/跳绳.avif', price: '29.00', status: 0, createdAt: '2026-03-15T07:30:00', sellerId: 7, sellerName: '周八' }
]

/**
 * 与我相关的订单（我买到的 + 我卖出的），服务端分页。
 *
 * <p>返回 data 结构：{ list: [], total, page, size }。</p>
 */
export async function getOrderList(page = 1, size = 10) {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  if (!token) {
    // 无 token：返回演示数据，形状必须与真实返回一致，否则调用方取 .list 会拿到 undefined
    return {
      code: 200,
      data: { list: MOCK_ORDERS, total: MOCK_ORDERS.length, page, size }
    }
  }
  try {
    return await request(`/orders/my?page=${page}&size=${size}`)
  } catch (error) {
    console.error('获取订单列表失败:', error)
    return {
      code: 200,
      data: { list: MOCK_ORDERS, total: MOCK_ORDERS.length, page, size }
    }
  }
}

// 确认收货
export async function confirmOrder(orderId) {
  return await request(`/orders/${orderId}`, {
    method: 'PUT',
    body: JSON.stringify({ status: 3 })
  })
}

// 取消订单
export async function cancelOrder(orderId) {
  return await request(`/orders/${orderId}`, {
    method: 'PUT',
    body: JSON.stringify({ status: 4 })
  })
}

// 创建订单（收银台入口）
export async function createOrder(productId) {
  try {
    return await request('/orders/create', {
      method: 'POST',
      body: JSON.stringify({ productId })
    })
  } catch (error) {
    console.error('创建订单失败:', error)
    throw error
  }
}

// 获取收银台详情（含商品快照、金额明细、卖家信息、剩余支付秒数）
export async function getOrderDetail(orderId) {
  return await request(`/orders/${orderId}`, { method: 'GET' })
}

// 模拟支付：仅待支付且未过期的订单可支付
// 参数名与后端约定为 pay_method；后端不做白名单校验，传什么记什么
export async function payOrder(orderId, payMethod) {
  return await request(`/orders/${orderId}/pay`, {
    method: 'POST',
    body: JSON.stringify({ pay_method: payMethod })
  })
}

// 取消订单（商品恢复在售）
export async function cancelOrderById(orderId) {
  return await request(`/orders/${orderId}/cancel`, { method: 'POST' })
}

/**
 * 批量支付：事务内全部支付，每笔须 status=0 且属当前用户。
 * Body { orderIds: [...] }，返回 { tradeNo, totalAmount }。
 */
export async function batchPayOrders(orderIds, payMethod = 'alipay') {
  return await request('/orders/batch-pay', {
    method: 'POST',
    body: JSON.stringify({ orderIds, pay_method: payMethod })
  })
}

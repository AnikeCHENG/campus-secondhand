// 评价与学生认证相关接口
//
// 路径归属两个不同前缀：订单评价在 /api/orders，用户信息在 /api/user，
// 因此这里各自定义 BASE，与 user.js / order.js 保持同样的写法。
// 之所以不抽公共 request 到一起，是因为现有 api 模块各自维护 BASE 已是既定风格，
// 单独抽一层反而要改动全部 8 个模块，超出本次范围。

const USER_BASE = import.meta.env.VITE_API_BASE || '/api/user'
const ORDER_BASE = import.meta.env.VITE_ORDER_BASE || '/api/orders'

async function request(base, path, options = {}) {
  let token = localStorage.getItem('token') || sessionStorage.getItem('token')
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(`${base}${path}`, { ...options, headers })
  if (!res.ok) throw new Error(`请求失败：${res.status}`)
  const text = await res.text()
  if (!text) return { code: res.status, data: null, message: '' }
  try {
    return JSON.parse(text)
  } catch {
    // 后端返回非 JSON（如反向代理的 HTML 错误页）时不要把解析异常抛给调用方
    return { code: res.status, data: null, message: '服务器返回了无法解析的内容' }
  }
}

/** 提交订单评价：仅买家本人、仅已完成订单、一单一评 */
export async function submitReview(orderId, rating, content) {
  return await request(ORDER_BASE, `/${orderId}/review`, {
    method: 'POST',
    body: JSON.stringify({ rating, content })
  })
}

/** 订单评价回显；未评价时后端返回 code=200 且 data=null */
export async function getOrderReview(orderId) {
  return await request(ORDER_BASE, `/${orderId}/review`, { method: 'GET' })
}

/**
 * 卖家评价统计。
 *
 * <p>路径与 UserController 现有风格一致（该Controller 下用户详情为
 * {@code /api/user/user/{id}}），因此这里同样是 {@code /api/user/user/{id}/review-stats}。</p>
 */
export async function getReviewStats(userId) {
  return await request(USER_BASE, `/user/${userId}/review-stats`, { method: 'GET' })
}

/** 学生认证：当前为模拟校验，接口预留，未来可对接教务系统或改为人工审核 */
export async function studentVerify(studentNo, realName) {
  return await request(USER_BASE, '/student-verify', {
    method: 'POST',
    body: JSON.stringify({ studentNo, realName })
  })
}
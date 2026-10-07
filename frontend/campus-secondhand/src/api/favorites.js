const BASE = import.meta.env.VITE_API_BASE || '/api'

/**
 * 统一的请求封装。
 *
 * 后端约定：HTTP 始终为 200，业务成败看响应体里的 code 字段。
 * 因此这里按 code 判定并 reject，调用方用 .catch 统一提示，
 * 避免出现「catch 到失败却弹成功 Toast」的情况。
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
      const err = new Error(data.message || '操作失败')
      err.response = data
      throw err
    }
    return data
  } catch (e) {
    const err = new Error('网络请求失败')
    err.cause = e
    throw err
  }
}

// ==================== 收藏 ====================

/** 我的收藏（服务端分页）：含商品图/标题/价格/状态，按收藏时间倒序；返回 { list, total, page, size } */
export async function getFavoriteList(page = 1, size = 10) {
  return await request(`/favorites/list?page=${page}&size=${size}`, { method: 'GET' })
}

/** 是否已收藏，供详情页回显；未登录时后端返回 false，不报错 */
export async function checkFavorite(productId) {
  return await request(`/favorites/check/${productId}`, { method: 'GET' })
}

/** 收藏商品；重复收藏后端返回幂等成功 */
export async function addFavorite(productId) {
  return await request(`/favorites/${productId}`, { method: 'POST' })
}

/** 取消收藏；未收藏时也返回成功（幂等） */
export async function removeFavoriteById(productId) {
  return await request(`/favorites/${productId}`, { method: 'DELETE' })
}

// ==================== 浏览历史 ====================

/** 浏览历史（服务端分页）：按最近浏览时间倒序；返回 { list, total, page, size } */
export async function getHistoryList(page = 1, size = 10) {
  return await request(`/history/list?page=${page}&size=${size}`, { method: 'GET' })
}

/** 清空浏览历史 */
export async function clearHistory() {
  return await request('/history', { method: 'DELETE' })
}

export default {
  getFavoriteList,
  checkFavorite,
  addFavorite,
  removeFavoriteById,
  getHistoryList,
  clearHistory
}

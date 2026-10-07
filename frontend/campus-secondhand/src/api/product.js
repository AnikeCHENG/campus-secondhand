const BASE = import.meta.env.VITE_API_BASE || '/api'

async function request(path, options = {}) {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  try {
    const res = await fetch(`${BASE}${path}`, {
      headers: { 
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      },
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

/**
 * 大厅商品列表（服务端分页）。
 *
 * <p>筛选与排序已下推到后端：category / keyword / minPrice / maxPrice / condition / sort
 * 都会拼进 SQL 的 WHERE 与 ORDER BY。若把筛选留在前端，就只会筛当前页的十几条记录。</p>
 *
 * <p>返回 data 结构：{ list: [], total, page, size }</p>
 */
export async function getProductList(page = 1, size = 12, filters = {}) {
  const query = new URLSearchParams({ page: String(page), size: String(size) })
  for (const [key, value] of Object.entries(filters)) {
    // 空值不传：category= 会被后端当成"分类为空字符串"而非"不筛选"
    if (value !== '' && value !== null && value !== undefined) {
      query.set(key, String(value))
    }
  }
  return await request(`/products/list?${query.toString()}`)
}

export async function getMyProducts() {
  return await request('/products/my')
}

export async function getProductsByCategory(category) {
  return await request(`/products/category/${encodeURIComponent(category)}`)
}

export async function searchProducts(keyword) {
  return await request(`/products/search?keyword=${encodeURIComponent(keyword)}`)
}

export async function getProductDetail(id) {
  return await request(`/products/${id}`)
}

export async function createProduct(params) {
  return await request('/products/create', {
    method: 'POST',
    body: JSON.stringify(params)
  })
}

export async function updateProduct(id, params) {
  return await request(`/products/update/${id}`, {
    method: 'PUT',
    body: JSON.stringify(params)
  })
}

export async function deleteProduct(id) {
  return await request(`/products/delete/${id}`, {
    method: 'DELETE'
  })
}

export async function markAsSold(id) {
  return await request(`/products/sold/${id}`, {
    method: 'PUT'
  })
}

export async function toggleLike(id) {
  return await request(`/products/${id}/like`, { method: 'POST' })
}

export async function getInteractionStats(id) {
  return await request(`/products/${id}/stats`)
}

export async function getComments(id, page = 1, size = 10) {
  return await request(`/products/${id}/comments?page=${page}&size=${size}`)
}

export async function addComment(id, content, parentId = null) {
  return await request(`/products/${id}/comment`, {
    method: 'POST',
    body: JSON.stringify({ content, parentId })
  })
}

export async function shareProduct(id) {
  return await request(`/products/${id}/share`, { method: 'POST' })
}

export default {
  getProductList,
  getMyProducts,
  getProductsByCategory,
  searchProducts,
  getProductDetail,
  createProduct,
  updateProduct,
  deleteProduct,
  markAsSold,
  toggleLike,
  getInteractionStats,
  getComments,
  addComment,
  shareProduct
}

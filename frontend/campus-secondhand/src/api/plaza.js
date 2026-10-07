const BASE = import.meta.env.VITE_API_BASE || '/api'

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
      err.status = res.status
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
    err.response = e.response
    throw err
  }
}

/** 大厅左侧名片：{ nickname, avatar, verified, grade, stats:{posts,selling,seeking} } */
export async function getHallProfile() {
  return await request('/user/hall-profile')
}

/**
 * 动态列表。type 传 null 表示全部。
 * 分页沿用项目基线：请求 page/size，返回 { list, total, page, size }。
 */
export async function getPosts({ type = null, page = 1, size = 10 } = {}) {
  const params = new URLSearchParams()
  if (type) params.set('type', type)
  params.set('page', String(page))
  params.set('size', String(size))
  return await request(`/posts?${params.toString()}`)
}

/** 类型字典：[{ value:'SELL', label:'出售' }, ...]，避免前端硬编码枚举文案 */
export async function getPostTypes() {
  return await request('/posts/types')
}

/**
 * 发布动态。
 * @param {{ type, content, tags, images, productInfo? }} payload
 */
export async function createPost(payload) {
  return await request('/posts', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}
const BASE = import.meta.env.VITE_API_BASE || '/api/user'

async function request(path, options = {}) {
  let token = localStorage.getItem('token')
  if (!token) {
    token = sessionStorage.getItem('token')
  }

  const headers = {
    'Content-Type': 'application/json'
  }

  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  try {
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
    const err = new Error(e.message || 'Network request failed')
    err.cause = e
    throw err
  }
}

export async function getProfile() {
  return await request('/profile')
}

export async function updateProfile(profileData) {
  return await request('/profile', {
    method: 'PUT',
    body: JSON.stringify(profileData)
  })
}

export async function getUserById(userId) {
  return await request(`/user/${userId}`)
}

export default { getProfile, updateProfile, getUserById }

const BASE = import.meta.env.VITE_API_BASE || '/api/auth'

async function request(path, options = {}) {
  try {
    const res = await fetch(`${BASE}${path}`, {
      headers: { 'Content-Type': 'application/json' },
      ...options,
    })
    const data = await res.json().catch(() => ({ message: '服务器返回非JSON' }))
    if (!res.ok) {
      const err = new Error(data?.message || `HTTP ${res.status}`)
      err.response = data
      throw err
    }

    if (data && typeof data.code === 'number') {
      if (data.code !== 200) {
        const err = new Error(data.message || '请求未成功')
        err.response = data
        throw err
      } else {
        // 后端返回的结构是 { code: 200, message: '成功', data: {...} }
        // 所以直接返回data.data部分
        return data.data
      }
    }

    return data
  } catch (e) {
    const err = new Error(e.message || 'Network request failed')
    err.cause = e
    throw err
  }
}

export async function login({ username, password }) {
  return await request('/login', { method: 'POST', body: JSON.stringify({ username, password }) })
}

export async function sendCode({ email }) {
  return await request('/send-code', { method: 'POST', body: JSON.stringify({ email }) })
}

export async function register({ username, email, password, confirmPassword, code }) {
  return await request('/register', { method: 'POST', body: JSON.stringify({ username, email, password, confirmPassword, code }) })
}

export async function forgotPassword({ email }) {
  const body = new URLSearchParams()
  body.append('email', email)
  return await request('/forgot-password', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body })
}

export async function logout() {
  return await request('/logout', { method: 'POST' })
}

export default { login, register, sendCode, forgotPassword, logout }

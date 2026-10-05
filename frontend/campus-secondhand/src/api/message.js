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

export async function getReceivedMessages() {
  return await request('/messages/received')
}

export async function getSentMessages() {
  return await request('/messages/sent')
}

export async function getConversations() {
  return await request('/messages/conversations')
}

export async function getConversation(otherUserId) {
  return await request(`/messages/conversation/${otherUserId}`)
}

export async function getUnreadCount() {
  return await request('/messages/unread-count')
}

export async function sendMessage(params) {
  return await request('/messages/send', {
    method: 'POST',
    body: JSON.stringify(params)
  })
}

export async function markAsRead(messageId) {
  return await request(`/messages/read/${messageId}`, {
    method: 'PUT'
  })
}

export async function markAllAsRead() {
  return await request('/messages/read-all', {
    method: 'PUT'
  })
}

export async function deleteMessage(messageId) {
  return await request(`/messages/delete/${messageId}`, {
    method: 'DELETE'
  })
}

export default { 
  getReceivedMessages, 
  getSentMessages, 
  getConversations,
  getConversation, 
  getUnreadCount,
  sendMessage,
  markAsRead,
  markAllAsRead,
  deleteMessage
}

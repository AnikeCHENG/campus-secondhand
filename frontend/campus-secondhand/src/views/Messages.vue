<template>
  <div class="page messages-page">
    <header class="site-header">
      <div class="container header-inner">
        <router-link to="/" class="brand">
          <span class="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 12a9 9 0 0 1 15-6.7L21 8" /><path d="M21 3v5h-5" />
              <path d="M21 12a9 9 0 0 1-15 6.7L3 16" /><path d="M3 21v-5h5" />
            </svg>
          </span>
          <span class="brand-name">校园二手</span>
        </router-link>
        <nav class="main-nav" aria-label="主导航">
          <router-link to="/" class="nav-link">首页</router-link>
          <router-link to="/products" class="nav-link">商品</router-link>
          <router-link to="/post" class="nav-link">发布</router-link>
          <router-link to="/messages" class="nav-link">消息</router-link>
          <router-link to="/profile" class="nav-link">我的</router-link>
        </nav>
        <div class="header-actions">
          <button class="icon-btn" type="button" aria-label="退出登录" @click="handleLogout">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" /><polyline points="16 17 21 12 16 7" /><line x1="21" y1="12" x2="9" y2="12" />
            </svg>
          </button>
          <button class="avatar" type="button" aria-label="个人中心" @click="go('/profile')">
            <img src="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 40 40'%3E%3Crect width='40' height='40' fill='%230b6e54'/%3E%3Ccircle cx='20' cy='16' r='6' fill='%23ffffff'/%3E%3Cpath d='M8 36c0-6.6 5.4-12 12-12s12 5.4 12 12' fill='%23ffffff'/%3E%3C/svg%3E" alt="用户头像" />
          </button>
        </div>
      </div>
    </header>

    <main class="msg-main">
      <div class="msg-shell">
        <!-- 会话列表 -->
        <aside class="sidebar">
          <div class="sidebar-head">
            <h2 class="sidebar-title">消息中心</h2>
            <span v-if="unreadCount > 0" class="badge badge-danger">{{ unreadCount }}</span>
          </div>
          <div class="tabs">
            <button class="tab" :class="{ active: activeTab === 'received' }" type="button" @click="activeTab = 'received'">收件箱</button>
            <button class="tab" :class="{ active: activeTab === 'sent' }" type="button" @click="activeTab = 'sent'">已发送</button>
          </div>

          <div class="conv-list">
            <div v-if="loading" class="empty"><p class="loading-text">[ LOADING… ]</p></div>
            <div v-else-if="conversations.length === 0" class="empty">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" /></svg>
              <p>暂无消息</p>
            </div>
            <button
              v-for="conv in conversations"
              :key="conv.userId"
              class="conv"
              :class="{ active: selectedUserId === conv.userId }"
              type="button"
              @click="selectConversation(conv)"
            >
              <div class="conv-avatar">
                <img :src="conv.avatar || defaultAvatar" :alt="conv.username" />
                <span v-if="conv.unreadCount > 0" class="msg-badge">{{ conv.unreadCount }}</span>
              </div>
              <div class="conv-info">
                <div class="conv-head">
                  <span class="conv-name">{{ conv.username }}</span>
                  <span class="conv-time">{{ formatTime(conv.lastMessageTime) }}</span>
                </div>
                <p class="conv-preview">{{ conv.lastMessage }}</p>
              </div>
            </button>
          </div>
        </aside>

        <!-- 聊天面板 -->
        <section class="chat">
          <div v-if="!selectedUserId" class="chat-empty">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" /></svg>
            <h3>选择对话</h3>
            <p>选择一个联系人开始聊天</p>
          </div>

          <template v-else>
            <div class="chat-head">
              <div class="chat-user">
                <div class="chat-avatar"><img :src="currentConversation?.avatar || defaultAvatar" :alt="currentConversation?.username" /></div>
                <div class="chat-user-info">
                  <span class="chat-username">{{ currentConversation?.username }}</span>
                  <span class="chat-status">在线</span>
                </div>
              </div>
            </div>

            <div class="chat-body" ref="messagesContainer">
              <div
                v-for="(msg, index) in currentMessages"
                :key="msg.id || index"
                class="message"
                :class="{ sent: msg.senderId === currentUserId, received: msg.senderId !== currentUserId }"
              >
                <div v-if="msg.senderId !== currentUserId" class="message-avatar">
                  <img :src="currentConversation?.avatar || defaultAvatar" alt="头像" />
                </div>
                <div class="message-content">
                  <div class="message-bubble"><p>{{ msg.content }}</p></div>
                  <span class="message-time">{{ formatMessageTime(msg.createdTime) }}</span>
                </div>
              </div>
            </div>

            <div class="chat-input">
              <input
                type="text"
                v-model="newMessage"
                placeholder="输入消息…"
                aria-label="消息输入"
                @keyup.enter="sendMessage"
              />
              <button class="btn btn-primary" type="button" :disabled="!newMessage.trim()" @click="sendMessage">发送</button>
            </div>
          </template>
        </section>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  getConversations,
  getConversation,
  getUnreadCount,
  sendMessage as sendMessageApi,
  markAsRead
} from '../api/message'
import { useNotificationStore } from '../stores/notification'

const router = useRouter()
const route = useRoute()
const notificationStore = useNotificationStore()
const loading = ref(true)
const activeTab = ref('received')
const conversations = ref([])
const selectedUserId = ref(null)
const currentMessages = ref([])
const newMessage = ref('')
const unreadCount = ref(0)
const messagesContainer = ref(null)
const currentUserId = ref(parseInt(localStorage.getItem('userId') || '0'))

const defaultAvatar = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 40 40'%3E%3Crect width='40' height='40' fill='%230b6e54'/%3E%3Ccircle cx='20' cy='16' r='6' fill='%23ffffff'/%3E%3Cpath d='M8 36c0-6.6 5.4-12 12-12s12 5.4 12 12' fill='%23ffffff'/%3E%3C/svg%3E"

const currentConversation = computed(() => conversations.value.find(c => c.userId === selectedUserId.value))

function go(path) { router.push(path) }

async function fetchMessages() {
  loading.value = true
  try {
    const [convRes, unreadRes] = await Promise.all([getConversations(), getUnreadCount()])
    if (unreadRes.code === 200 && unreadRes.data) {
      const count = typeof unreadRes.data === 'object' ? (unreadRes.data.count || 0) : (unreadRes.data || 0)
      unreadCount.value = count
      notificationStore.setUnreadCount(count)
    }
    if (convRes.code === 200 && convRes.data) {
      conversations.value = convRes.data.map(c => ({
        userId: c.other_user_id || c.otherUserId || c.userId,
        username: c.username || ('用户' + (c.other_user_id || c.otherUserId || c.userId)),
        avatar: c.avatar || defaultAvatar,
        lastMessage: c.lastMessage || '',
        lastMessageTime: c.lastMessageTime || c.last_message_time || '',
        unreadCount: c.unreadCount || c.unread_count || 0,
        messages: []
      }))
    }
  } catch (e) {
    console.error('加载消息失败:', e)
  } finally {
    loading.value = false
  }
}

async function selectConversation(conv) {
  selectedUserId.value = conv.userId
  try {
    const res = await getConversation(conv.userId)
    if (res.code === 200 && res.data) {
      currentMessages.value = res.data.sort((a, b) => new Date(a.createdTime) - new Date(b.createdTime))
      await nextTick()
      scrollToBottom()
      const unreadMessages = currentMessages.value.filter(m => m.senderId !== currentUserId.value && m.isRead === 0)
      for (const msg of unreadMessages) { try { await markAsRead(msg.id) } catch { console.log('标记已读失败') } }
      const newUnreadRes = await getUnreadCount()
      if (newUnreadRes.code === 200) {
        const newCount = typeof newUnreadRes.data === 'object' ? (newUnreadRes.data.count || 0) : (newUnreadRes.data || 0)
        unreadCount.value = newCount
        notificationStore.setUnreadCount(newCount)
      }
    }
  } catch (e) {
    console.error('加载对话失败:', e)
  }
}

async function sendMessage() {
  if (!newMessage.value.trim() || !selectedUserId.value) return
  const content = newMessage.value.trim()
  newMessage.value = ''
  try {
    const res = await sendMessageApi({ receiverId: selectedUserId.value, content })
    if (res.code === 200) {
      currentMessages.value.push({ id: res.data?.id || Date.now(), senderId: currentUserId.value, receiverId: selectedUserId.value, content, createdTime: new Date().toISOString(), isRead: 0 })
      await nextTick()
      scrollToBottom()
    } else {
      alert(res.message || '发送失败')
      newMessage.value = content
    }
  } catch (e) {
    alert(e.message || '发送失败')
    newMessage.value = content
  }
}

function scrollToBottom() { if (messagesContainer.value) messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight }

function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const diff = new Date() - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
  return date.toLocaleDateString('zh-CN')
}
function formatMessageTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

async function handleLogout() {
  localStorage.removeItem('token'); localStorage.removeItem('username'); sessionStorage.removeItem('justLoggedIn'); router.push('/login')
}

watch(activeTab, () => { selectedUserId.value = null; currentMessages.value = [] })

onMounted(async () => {
  await fetchMessages()
  const sellerId = route.query.sellerId
  if (sellerId) {
    const sellerConv = conversations.value.find(conv => conv.userId === parseInt(sellerId))
    if (sellerConv) selectConversation(sellerConv)
  }
})
</script>

<style scoped>
.site-header { position: sticky; top: 0; z-index: 100; background: rgba(255,255,255,0.85); backdrop-filter: saturate(180%) blur(12px); border-bottom: 1px solid var(--border); }
.header-inner { height: var(--header-h); display: flex; align-items: center; gap: var(--space-8); }
.brand { display: inline-flex; align-items: center; gap: var(--space-2); color: var(--text); flex-shrink: 0; }
.brand:hover { color: var(--text); }
.brand-mark { width: 30px; height: 30px; display: inline-flex; align-items: center; justify-content: center; border-radius: var(--radius-sm); background: var(--accent); color: ***REMOVED***fff; }
.brand-mark svg { width: 17px; height: 17px; }
.brand-name { font-size: var(--text-lg); font-weight: var(--weight-semibold); letter-spacing: -0.01em; }
.main-nav { display: flex; gap: var(--space-1); flex: 1; }
.nav-link { padding: 8px 12px; font-size: var(--text-base); color: var(--text-2); border-radius: var(--radius-sm); transition: color var(--dur-fast) var(--ease), background var(--dur-fast) var(--ease); }
.nav-link:hover { color: var(--text); background: var(--surface-3); }
.nav-link.router-link-exact-active { color: var(--accent); font-weight: var(--weight-medium); }
.header-actions { display: flex; align-items: center; gap: var(--space-2); }
.icon-btn { width: 38px; height: 38px; display: inline-flex; align-items: center; justify-content: center; border: 1px solid transparent; border-radius: var(--radius); background: transparent; color: var(--text-2); }
.icon-btn:hover { background: var(--surface-3); color: var(--text); }
.icon-btn svg { width: 20px; height: 20px; }
.avatar { width: 36px; height: 36px; padding: 0; border: 1px solid var(--border); border-radius: var(--radius-full); overflow: hidden; background: var(--surface-3); }
.avatar img { width: 100%; height: 100%; object-fit: cover; }

.messages-page { display: flex; flex-direction: column; }
.msg-main { flex: 1; max-width: var(--container); width: 100%; margin: 0 auto; padding: var(--space-6); height: calc(100vh - var(--header-h)); }
.msg-shell { display: grid; grid-template-columns: 320px 1fr; height: 100%; background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-lg); overflow: hidden; }

.sidebar { border-right: 1px solid var(--border); display: flex; flex-direction: column; min-height: 0; }
.sidebar-head { display: flex; align-items: center; justify-content: space-between; padding: var(--space-4) var(--space-5); border-bottom: 1px solid var(--border); }
.sidebar-title { font-size: var(--text-lg); font-weight: var(--weight-semibold); }
.tabs { display: flex; gap: var(--space-2); padding: var(--space-3) var(--space-5); border-bottom: 1px solid var(--border); }
.tab { flex: 1; padding: 8px; font-size: var(--text-sm); color: var(--text-2); background: var(--surface-2); border: 1px solid var(--border); border-radius: var(--radius-sm); }
.tab:hover { color: var(--text); }
.tab.active { background: var(--accent); border-color: var(--accent); color: ***REMOVED***fff; }

.conv-list { flex: 1; overflow-y: auto; padding: var(--space-2); }
.conv { display: flex; align-items: center; gap: var(--space-3); width: 100%; padding: var(--space-3); text-align: left; background: transparent; border: none; border-radius: var(--radius); transition: background var(--dur-fast) var(--ease); }
.conv:hover { background: var(--surface-2); }
.conv.active { background: var(--accent-soft); }
.conv-avatar { position: relative; width: 44px; height: 44px; border-radius: var(--radius-full); overflow: hidden; flex-shrink: 0; border: 1px solid var(--border); }
.conv-avatar img { width: 100%; height: 100%; object-fit: cover; }
.msg-badge { position: absolute; top: -2px; right: -2px; min-width: 16px; height: 16px; padding: 0 4px; background: var(--danger); color: ***REMOVED***fff; font-size: 10px; border-radius: var(--radius-full); display: flex; align-items: center; justify-content: center; }
.conv-info { flex: 1; min-width: 0; }
.conv-head { display: flex; justify-content: space-between; align-items: baseline; gap: var(--space-2); }
.conv-name { font-size: var(--text-base); font-weight: var(--weight-medium); color: var(--text); }
.conv-time { font-size: var(--text-xs); color: var(--text-3); flex-shrink: 0; }
.conv-preview { margin-top: 2px; font-size: var(--text-sm); color: var(--text-2); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.chat { display: flex; flex-direction: column; min-height: 0; }
.chat-empty { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: var(--space-2); color: var(--text-3); }
.chat-empty svg { width: 44px; height: 44px; }
.chat-empty h3 { font-size: var(--text-lg); color: var(--text); }
.chat-empty p { font-size: var(--text-sm); }
.chat-head { display: flex; align-items: center; justify-content: space-between; padding: var(--space-4) var(--space-5); border-bottom: 1px solid var(--border); }
.chat-user { display: flex; align-items: center; gap: var(--space-3); }
.chat-avatar { width: 40px; height: 40px; border-radius: var(--radius-full); overflow: hidden; border: 1px solid var(--border); }
.chat-avatar img { width: 100%; height: 100%; object-fit: cover; }
.chat-user-info { display: flex; flex-direction: column; gap: 2px; }
.chat-username { font-size: var(--text-base); font-weight: var(--weight-medium); }
.chat-status { font-size: var(--text-xs); color: var(--success); }

.chat-body { flex: 1; overflow-y: auto; padding: var(--space-5); display: flex; flex-direction: column; gap: var(--space-4); background: var(--surface-2); }
.message { display: flex; gap: var(--space-3); max-width: 72%; }
.message.sent { align-self: flex-end; flex-direction: row-reverse; }
.message.received { align-self: flex-start; }
.message-avatar { width: 32px; height: 32px; border-radius: var(--radius-full); overflow: hidden; flex-shrink: 0; border: 1px solid var(--border); }
.message-avatar img { width: 100%; height: 100%; object-fit: cover; }
.message-content { display: flex; flex-direction: column; gap: 4px; }
.message.sent .message-content { align-items: flex-end; }
.message-bubble { padding: 10px 14px; background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-lg); border-top-left-radius: 4px; }
.message.sent .message-bubble { background: var(--accent); border-color: var(--accent); border-radius: var(--radius-lg); border-top-right-radius: 4px; }
.message-bubble p { font-size: var(--text-base); color: var(--text); line-height: var(--leading-normal); }
.message.sent .message-bubble p { color: ***REMOVED***fff; }
.message-time { font-size: var(--text-xs); color: var(--text-3); padding: 0 4px; }

.chat-input { display: flex; gap: var(--space-3); padding: var(--space-4) var(--space-5); border-top: 1px solid var(--border); }
.chat-input input { flex: 1; padding: 10px 14px; font-size: var(--text-base); color: var(--text); background: var(--surface); border: 1px solid var(--border-strong); border-radius: var(--radius); outline: none; }
.chat-input input:focus { border-color: var(--accent); box-shadow: 0 0 0 3px var(--accent-soft); }

@media (max-width: 860px) {
  .main-nav { display: none; }
  .msg-shell { grid-template-columns: 1fr; }
  .sidebar { display: none; }
}
</style>

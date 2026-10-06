import { defineStore } from 'pinia'

/**
 * 当前登录用户身份。
 *
 * <p>role 只从 {@code GET /api/auth/me} 获取——该接口由服务端解析 JWT 返回，
 * 前端无法伪造。localStorage 里那份 user 仅用于首屏渲染昵称头像，
 * <b>不作为权限依据</b>。</p>
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    id: null,
    username: '',
    role: null,
    avatar: null,
    /** 是否已从服务端确认过身份。false 表示 role 尚不可信 */
    verified: false
  }),

  getters: {
    isAdmin: (state) => state.verified && state.role === 1
  },

  actions: {
    /** 从服务端拉取当前身份；仅在需要确认权限时调用一次 */
    async fetchMe() {
      const token = localStorage.getItem('token') || sessionStorage.getItem('token')
      if (!token) {
        this.reset()
        return null
      }
      try {
        const res = await fetch('/api/auth/me', {
          headers: { Authorization: `Bearer ${token}` }
        })
        const data = await res.json().catch(() => null)
        if (!data || data.code !== 200 || !data.data) {
          this.reset()
          return null
        }
        this.id = data.data.id
        this.username = data.data.username
        this.role = data.data.role
        this.avatar = data.data.avatar
        this.verified = true
        return data.data
      } catch {
        this.reset()
        return null
      }
    },

    /** 登出时清空，避免残留上一位用户的角色 */
    reset() {
      this.id = null
      this.username = ''
      this.role = null
      this.avatar = null
      this.verified = false
    },

    /**
     * 登录成功后调用：服务端已经认证过，直接写入即可，
     * 无需再发一次 /api/auth/me。
     */
    setFromLogin(user) {
      if (!user) return
      this.id = user.id ?? null
      this.username = user.username ?? ''
      this.role = user.role ?? null
      this.avatar = user.avatar ?? null
      this.verified = true
    }
  }
})
import { ref } from 'vue'
import { defineStore } from 'pinia'

/**
 * 未读消息计数。
 *
 * <p>本 store 只负责「存值」，不负责「轮询」。轮询统一由 App.vue 中的
 * {@code useMessageNotify} 负责（10 秒一次），它会调用 {@link setUnreadCount}
 * 回写这里。此前本 store 还自带一套 startPolling（15 秒一次），
 * 与前者请求同一接口、写入同一字段，导致登录后重复请求，已移除。</p>
 */
export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  const isPolling = ref(false)

  /** 直接设置未读数，由轮询方或消息页在拿到最新数据后回写 */
  function setUnreadCount(count) {
    unreadCount.value = count
  }

  /** 发消息成功后本地自减，避免等待下一次轮询 */
  function decrementUnreadCount() {
    if (unreadCount.value > 0) {
      unreadCount.value--
    }
  }

  /** 登出时清零 */
  function resetUnreadCount() {
    unreadCount.value = 0
    isPolling.value = false
  }

  return {
    unreadCount,
    isPolling,
    setUnreadCount,
    decrementUnreadCount,
    resetUnreadCount
  }
})

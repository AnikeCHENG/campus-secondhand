import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getCartCount } from '@/api/cart'

/**
 * 购物车角标数量 —— 全站单一数据源。
 *
 * <p>此前只有 Home.vue 持有这个状态（自己的 ref + 自己的 cart:add / cart:count
 * 监听），导航统一后所有页面都需要显示购物车角标，若每页各拉一次就是 N 份
 * 重复请求。现在收进 store：AppNavbar 是唯一持有者，页面只读。</p>
 *
 * <p>与 notification store 的区别：未读数由 useMessageNotify 每 10 秒轮询，
 * 而购物车没有轮询需求（只在加购、结算后变化），因此这里不建定时器，
 * 只在「首次需要时」和「收到事件时」取值。</p>
 */
export const useCartStore = defineStore('cart', () => {
  const count = ref(0)
  /** 是否已从服务端取过值。未取过就显示 0 而不显示骨架，避免闪一下 */
  const loaded = ref(false)
  /** 同一时刻只允许一个请求在飞：切页时多页同时要数量会打多次接口 */
  let inflight = null

  async function refresh() {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token')
    if (!token) {
      count.value = 0
      loaded.value = true
      return 0
    }
    if (inflight) return inflight
    inflight = (async () => {
      try {
        const res = await getCartCount()
        count.value = Number(res?.data?.count ?? 0) || 0
        loaded.value = true
      } catch {
        // 静默：角标失败不该打扰用户，也不该阻塞导航渲染
      } finally {
        inflight = null
      }
      return count.value
    })()
    return inflight
  }

  /** 加购后本地 +1 立即反馈，再由服务端计数纠正 */
  function bump() {
    count.value += 1
  }

  /**
   * 页面发出 cart:count 且带具体数字时直接采用，不额外请求。
   * 不带数字（事件只说"变了"）则回源。
   */
  function setCount(value) {
    if (typeof value === 'number') {
      count.value = value
      loaded.value = true
    } else {
      refresh()
    }
  }

  function reset() {
    count.value = 0
    loaded.value = false
  }

  return { count, loaded, refresh, bump, setCount, reset }
})

import { prefersReduced } from '../composables/useReducedMotion'

/** 尚未进入视口时的初始状态。CSS 里配 transition，指令只负责加 class。 */
const HIDDEN = 'v-reveal-pending'
const SHOWN = 'v-reveal-shown'

/**
 * v-reveal —— 元素滚入视口时淡入上浮。
 *
 * <p>用 IntersectionObserver 而不是 scroll 事件监听：后者在每个滚动帧都要
 * 读 getBoundingClientRect() 强制回流，长列表下掉帧明显。</p>
 *
 * <p>用法：</p>
 * <pre>
 *   &lt;div v-reveal&gt;默认&lt;/div&gt;
 *   &lt;div v-reveal="{ delay: 80 }"&gt;延迟 80ms，用于 stagger&lt;/div&gt;
 *   &lt;div v-reveal="{ distance: 12 }"&gt;上浮距离更小&lt;/div&gt;
 * &lt;/pre>
 *
 * <p>三条降级路径，任一情况下元素都保证可见：</p>
 * <ol>
 *   <li>reduced-motion：直接显示，不做位移；</li>
 *   <li>无 IntersectionObserver：直接显示（否则元素会永久停在 opacity:0）；</li>
 *   <li>组件卸载：disconnect + removeEventListener，防止泄漏。</li>
 * </ol>
 *
 * @type {import('vue').Directive}
 */
export const reveal = {
  mounted(el, binding) {
    const reduced = prefersReduced.value

    // 降级 1：用户要求减弱动效 → 直接可见，不给任何位移
    if (reduced) {
      el.classList.remove(HIDDEN)
      el.classList.add(SHOWN)
      return
    }

    const opts = binding.value || {}

    // 降级 2：环境不支持 IO → 直接可见。不能留成隐藏态，
    // 否则内容在这个浏览器里彻底消失，比没有动画严重得多。
    if (typeof IntersectionObserver === 'undefined') {
      el.classList.remove(HIDDEN)
      el.classList.add(SHOWN)
      return
    }

    el.classList.add(HIDDEN)
    if (opts.distance !== undefined) {
      el.style.setProperty('--reveal-distance', `${opts.distance}px`)
    }

    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (!entry.isIntersecting) continue
          // once: 只需触发一次，进入后不再回退到隐藏态
          observer.unobserve(entry.target)
          el.classList.remove(HIDDEN)
          el.classList.add(SHOWN)
          if (opts.delay) {
            el.style.transitionDelay = `${opts.delay}ms`
          }
        }
      },
      { threshold: opts.threshold ?? 0.1, rootMargin: opts.rootMargin ?? '0px 0px -8% 0px' }
    )
    observer.observe(el)

    // 卸载时断开：元素可能因 v-if 被移除，遗留的 observer 会一直持有引用
    // 降级 3：卸载时断开。元素可能因 v-if 被移除，
    // 遗留的 observer 会一直持有 el 引用直到页面关闭。
    el.__revealObserver = observer
  },

  unmounted(el) {
    el.__revealObserver?.disconnect()
    el.__revealObserver = null
  },

  updated(el) {
    // 运行中用户切换了 reduced-motion：已显示的元素保持显示即可，
    // 但若仍在隐藏态必须立即放出来，不能等下一次 IntersectionObserver 回调
    if (prefersReduced.value) {
      el.classList.remove(HIDDEN)
      el.classList.add(SHOWN)
    }
  }
}

export default reveal
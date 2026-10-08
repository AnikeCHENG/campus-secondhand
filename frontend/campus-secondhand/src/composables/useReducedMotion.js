import { ref, readonly } from 'vue'

/**
 * 系统「减弱动态效果」偏好的单一真相源。
 *
 * <p>全应用只订阅一次 matchMedia，所有调用方共享同一个 ref。若每个组件各自
 * new MediaQueryList 并 addEventListener('change')，十几个卡片就是十几份监听，
 * 且卸载时容易漏掉 removeEventListener 造成泄漏。</p>
 *
 * <p>SSR/老浏览器无 matchMedia 时降级为 false（正常动画）而非 true——
 * 判断"能不能用动画"和判断"要不要抑制动画"是相反的默认值，
 * 默认 true 会让不支持的环境彻底没有动效。</p>
 */
const supportsMq = typeof window !== 'undefined' && typeof window.matchMedia === 'function'

const mql = supportsMq
  ? window.matchMedia('(prefers-reduced-motion: reduce)')
  : null

const prefersReduced = ref(mql ? mql.matches : false)

if (mql) {
  // 用 addEventListener 而非 deprecated 的 addListener
  const onChange = (e) => { prefersReduced.value = e.matches }
  if (typeof mql.addEventListener === 'function') {
    mql.addEventListener('change', onChange)
  } else if (typeof mql.addListener === 'function') {
    mql.addListener(onChange)
  }
}

/**
 * @returns {import('vue').Readonly<import('vue').Ref<boolean>>}
 *   true 表示用户要求减弱动效，调用方应改用「静态等价物」而非直接冻结动画。
 *
 * @example
 * const reduced = useReducedMotion()
 * // 光环：正常旋转，降级为静态渐变环
 * <div class="avatar-ring" :class="{ 'is-static': reduced }" />
 */
export function useReducedMotion() {
  return readonly(prefersReduced)
}

/**
 * 原始 ref，供指令等无法走 composable 生命周期的场景读取。
 * 组件里请用 useReducedMotion() 拿 readonly 版本，避免误写。
 */
export { prefersReduced }

/**
 * 生成一个在 reduced-motion 下直接取终值的降级装饰器。
 *
 * <p>用于「动效是否有意义」的场景（数字滚动、进度推进）：这类动效只是把最终
 * 状态演一遍，降级时直接呈现结果比播一遍动画更好。</p>
 */
export function whenReduced(value) {
  return prefersReduced.value ? value : null
}
import { onBeforeUnmount, ref, watch } from 'vue'

/** easeOutCubic：起步快、收尾稳，数字滚动最常用的一条曲线 */
function easeOutCubic(t) {
  return 1 - Math.pow(1 - t, 3)
}

/**
 * 数字从 0 滚动到目标值。
 *
 * <p>三条守则：</p>
 * <ul>
 *   <li><b>不造假</b>：始终以传入的真实 target 为终点，动画中途不插值出
 *       目标之外的数字；target 变化时从当前显示值续走而不是跳回 0。</li>
 *   <li><b>reduced-motion 直接落终值</b>，不启动 rAF——这类动效只是把最终
 *       状态演一遍，降级时直接给结果更诚实。</li>
 *   <li><b>运行中切换偏好会 cancel</b> 在途帧，不留孤儿 rAF 继续跑。</li>
 * </ul>
 *
 * @param {import('vue').Ref<number>} target 目标值
 * @param {object} [options]
 * @param {number} [options.duration=900] 毫秒
 * @param {boolean} [options.startOnMount=false] 是否在 onMounted 自动启动。
 *        默认关闭，因为统计数字通常要等接口回来才启动，由调用方显式调 start()。
 * @param {import('vue').Ref<boolean>} [options.reduced] 注入的 reduced-motion ref。
 *        不传则用全局单例。传了便于测试。
 * @returns {{ display: import('vue').Ref<number>, start: () => void, stop: () => void }}
 */
export function useCountUp(target, { duration = 900, startOnMount = false, reduced } = {}) {
  const display = ref(0)
  let rafId = 0
  let startedAt = 0

  function stop() {
    if (rafId) {
      cancelAnimationFrame(rafId)
      rafId = 0
    }
  }

  function render(final) {
    display.value = final
  }

  function start() {
    stop()
    const to = Number(target.value) || 0

    // 降级为静态等价物：直接给终值，不播动画
    if (reduced?.value) {
      render(to)
      return
    }

    const from = display.value
    const delta = to - from
    if (delta === 0) {
      render(to)
      return
    }

    startedAt = 0
    const step = (ts) => {
      if (!startedAt) startedAt = ts
      const t = Math.min(1, (ts - startedAt) / duration)
      display.value = Math.round(from + delta * easeOutCubic(t))
      if (t < 1) {
        rafId = requestAnimationFrame(step)
      } else {
        // 收尾强制对齐，避免浮点误差停在 to - 1
        render(to)
        rafId = 0
      }
    }
    rafId = requestAnimationFrame(step)
  }

  // target 在动画过程中变化：从当前显示值续走
  watch(target, () => {
    if (display.value !== 0 && rafId) start()
  })

  // 用户中途打开「减弱动态效果」：立刻停下并落到终值
  if (reduced) {
    watch(reduced, (v) => { if (v) { stop(); render(Number(target.value) || 0) } })
  }

  onBeforeUnmount(stop)

  if (startOnMount) {
    // onMounted 未引入：调用方通常在数据到达后手动 start()，
    // 这里用 microtask 等首帧，避免在 setup 同步阶段就开跑
    Promise.resolve().then(() => {
      if (display.value === 0) start()
    })
  }

  return { display, start, stop }
}
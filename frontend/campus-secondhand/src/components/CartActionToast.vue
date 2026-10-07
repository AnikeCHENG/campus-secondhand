<script setup>
/**
 * 带操作按钮的轻提示。
 *
 * <p>为什么不用 {@code ElMessage}：它只支持纯文本，加按钮得靠
 * {@code dangerouslyUseHTMLString} 拼字符串——那样既无法绑定点击事件，
 * 也把用户可见文案变成了 HTML 注入点。这里自绘，行为可控。</p>
 */
import { onBeforeUnmount, ref } from 'vue'

const props = defineProps({
  message: { type: String, required: true },
  /** 按钮组：[{ label, kind: 'primary'|'ghost', onClick }] */
  actions: { type: Array, default: () => [] },
  duration: { type: Number, default: 5000 }
})

const visible = ref(true)
let timer = null

function close() {
  visible.value = false
  clearTimeout(timer)
}

function run(action) {
  close()
  action.onClick?.()
}

function start() {
  clearTimeout(timer)
  timer = setTimeout(close, props.duration)
}

start()
onBeforeUnmount(() => clearTimeout(timer))
</script>

<template>
  <Transition name="cart-toast">
    <div v-if="visible" class="cart-toast" role="status">
      <span class="cart-toast__text">{{ message }}</span>
      <div v-if="actions.length" class="cart-toast__actions">
        <button
          v-for="a in actions"
          :key="a.label"
          class="cart-toast__btn"
          :class="{ 'cart-toast__btn--primary': a.kind === 'primary' }"
          type="button"
          @click="run(a)"
        >{{ a.label }}</button>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.cart-toast {
  position: fixed;
  left: 50%;
  bottom: 96px;
  z-index: 2000;
  display: flex;
  align-items: center;
  gap: var(--space-3);
  max-width: min(92vw, 420px);
  padding: var(--space-3) var(--space-4);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--surface);
  box-shadow: var(--shadow-lg, 0 12px 32px rgba(0, 0, 0, 0.14));
  transform: translateX(-50%);
}
.cart-toast__text { font-size: var(--text-sm); color: var(--text); white-space: nowrap; }
.cart-toast__actions { display: flex; align-items: center; gap: var(--space-2); }
.cart-toast__btn {
  padding: 4px var(--space-3);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm, 4px);
  background: transparent;
  color: var(--text-2);
  font-size: var(--text-sm);
  white-space: nowrap;
  cursor: pointer;
  transition: color var(--dur) var(--ease), border-color var(--dur) var(--ease), background var(--dur) var(--ease);
}
.cart-toast__btn:hover { color: var(--accent); border-color: var(--accent); }
.cart-toast__btn--primary { border-color: var(--accent); background: var(--accent); color: #fff; }
.cart-toast__btn--primary:hover { color: #fff; filter: brightness(1.06); }

.cart-toast-enter-active, .cart-toast-leave-active { transition: opacity var(--dur) var(--ease), transform var(--dur) var(--ease); }
.cart-toast-enter-from, .cart-toast-leave-to { opacity: 0; transform: translate(-50%, 10px); }
</style>
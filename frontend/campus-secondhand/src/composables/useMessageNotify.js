import { ref, onUnmounted } from 'vue'
import { getUnreadCount } from '@/api/message'
import { useNotificationStore } from '@/stores/notification'

const SOUND_KEY = 'notifySoundEnabled'
const soundEnabled = ref(localStorage.getItem(SOUND_KEY) !== 'off')

const audio = new Audio('/sounds/notify.mp3')
audio.preload = 'auto'
audio.addEventListener('error', () => {
  if (!audio.src.endsWith('.wav')) audio.src = '/sounds/notify.wav'
}, { once: true })

let unlocked = false
function unlockAudio() {
  if (unlocked) return
  unlocked = true
  audio.muted = true
  audio.play().finally(() => { audio.muted = false })
}
window.addEventListener('pointerdown', unlockAudio, { once: true })

function onWindowFocus(originalTitle, stopFlash) {
  document.title = originalTitle
  stopFlash()
}

export function useMessageNotify() {
  const store = useNotificationStore()
  let lastCount = -1
  let timer = null
  const originalTitle = document.title
  let titleTimer = null

  function playSound() {
    if (!soundEnabled.value) return
    audio.currentTime = 0
    audio.play().catch(() => {})
  }

  function stopFlash() {
    clearInterval(titleTimer)
    titleTimer = null
  }

  function flashTitle() {
    if (!document.hidden) return
    stopFlash()
    titleTimer = setInterval(() => {
      document.title = document.title === originalTitle
        ? '【新消息】' + originalTitle
        : originalTitle
    }, 1000)
  }

  function notify() {
    if (typeof Notification !== 'undefined' && Notification.permission === 'granted') {
      new Notification('校园二手交易', { body: '你收到一条新私信' })
    }
  }

  async function poll() {
    if (!localStorage.getItem('token') && !sessionStorage.getItem('token')) return
    try {
      const res = await getUnreadCount()
      const count = typeof res.data === 'object' ? (res.data.count || 0) : (res.data || 0)
      if (lastCount >= 0 && count > lastCount) {
        playSound()
        flashTitle()
        notify()
      }
      lastCount = count
      store.setUnreadCount(count)
    } catch {
      // 网络异常忽略
    }
  }

  function startNotify(interval = 10000) {
    if (timer) return
    if (typeof Notification !== 'undefined' && Notification.permission === 'default') {
      Notification.requestPermission()
    }
    poll()
    timer = setInterval(poll, interval)
  }

  function stopNotify() {
    clearInterval(timer); timer = null
    stopFlash()
    document.title = originalTitle
  }

  function toggleSound() {
    soundEnabled.value = !soundEnabled.value
    localStorage.setItem(SOUND_KEY, soundEnabled.value ? 'on' : 'off')
  }

  const focusHandler = () => onWindowFocus(originalTitle, stopFlash)
  window.addEventListener('focus', focusHandler)

  onUnmounted(() => {
    stopNotify()
    window.removeEventListener('focus', focusHandler)
  })
  return { playSound, startNotify, stopNotify, soundEnabled, toggleSound }
}

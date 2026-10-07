<template>
  <AuthShell>
    <div class="glass-card">
      <header class="auth-head">
        <BrandLogo :size="44" class="auth-head__logo" />
        <h1 class="auth-title">创建账号</h1>
        <p class="auth-sub">加入校园二手交易平台</p>
      </header>

      <el-form :model="form" :rules="rules" ref="regForm" class="auth-form stagger">
        <div class="field">
          <label class="field-label" for="reg-username">用户名</label>
          <div class="field-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <circle cx="12" cy="8" r="4" />
              <path d="M4 21v-1a6 6 0 0 1 6-6h4a6 6 0 0 1 6 6v1" />
            </svg>
          </div>
          <input id="reg-username" v-model="form.username" class="input" type="text" placeholder="3-20 个字符" />
        </div>

        <div class="field">
          <label class="field-label" for="reg-email">邮箱</label>
          <div class="email-row">
            <div class="field-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <rect x="2" y="4" width="20" height="16" rx="2" />
                <path d="m2 7 10 6 10-6" />
              </svg>
            </div>
            <input id="reg-email" v-model="form.email" class="input" type="email" placeholder="name@example.com" />
            <button
              type="button"
              class="btn btn-outline code-btn"
              :disabled="sendingCode || countdown > 0"
              @click="onSendCode"
            >
              {{ sendingCode ? '发送中…' : countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
            </button>
          </div>
        </div>

        <div class="field">
          <label class="field-label" for="reg-code">邮箱验证码</label>
          <div class="field-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path d="M4 4h16v16H4z" />
              <path d="M8 9h8M8 13h5" />
            </svg>
          </div>
          <input id="reg-code" v-model="form.code" class="input" type="text" inputmode="numeric" maxlength="6" placeholder="6 位验证码" />
        </div>

        <div class="field">
          <label class="field-label" for="reg-password">密码</label>
          <div class="field-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <rect x="3" y="11" width="18" height="11" rx="2" />
              <path d="M7 11V7a5 5 0 0 1 10 0v4" />
            </svg>
          </div>
          <div class="input-affix">
            <input
              id="reg-password"
              v-model="form.password"
              class="input"
              :type="showPassword ? 'text' : 'password'"
              placeholder="至少 6 位"
            />
            <button type="button" class="affix-btn" :aria-label="showPassword ? '隐藏密码' : '显示密码'" @click="showPassword = !showPassword">
              <svg v-if="!showPassword" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
                <line x1="1" y1="1" x2="23" y2="23" />
              </svg>
            </button>
          </div>
          <div v-if="form.password" class="strength" :class="strengthClass">
            <span class="strength-track"><span class="strength-fill" :style="{ width: strengthWidth }"></span></span>
            <span class="strength-text">{{ strengthText }}</span>
          </div>
        </div>

        <div class="field">
          <label class="field-label" for="reg-confirm">确认密码</label>
          <div class="field-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path d="M12 2 4 6v6c0 5 3.4 8.6 8 10 4.6-1.4 8-5 8-10V6z" />
              <path d="m9 12 2 2 4-4" />
            </svg>
          </div>
          <div class="input-affix">
            <input
              id="reg-confirm"
              v-model="form.confirm"
              class="input"
              :type="showConfirmPassword ? 'text' : 'password'"
              placeholder="再次输入密码"
            />
            <button type="button" class="affix-btn" :aria-label="showConfirmPassword ? '隐藏密码' : '显示密码'" @click="showConfirmPassword = !showConfirmPassword">
              <svg v-if="!showConfirmPassword" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
                <line x1="1" y1="1" x2="23" y2="23" />
              </svg>
            </button>
          </div>
        </div>

        <button type="button" class="btn btn-primary btn-block" :disabled="loading" @click="onRegister">
          {{ loading ? '创建中…' : '创建账号' }}
        </button>
      </el-form>

      <p class="auth-foot">
        已有账号？
        <router-link to="/login" class="text-link">立即登录</router-link>
      </p>
    </div>
  </AuthShell>
</template>

<script setup>
import { reactive, ref, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import auth from '../api/auth'
import AuthShell from '../components/AuthShell.vue'
import BrandLogo from '../components/BrandLogo.vue'

const router = useRouter()
const regForm = ref(null)
const loading = ref(false)
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)
let countdownTimer = null

const form = reactive({ username: '', email: '', password: '', confirm: '', code: '' })

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度3-20字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20字符', trigger: 'blur' }
  ],
  confirm: [{ required: true, message: '请确认密码', trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

const strengthScore = computed(() => {
  const password = form.password
  if (!password) return 0
  let score = 0
  if (password.length >= 6) score += 1
  if (password.length >= 10) score += 1
  if (/[A-Z]/.test(password)) score += 1
  if (/[a-z]/.test(password)) score += 1
  if (/[0-9]/.test(password)) score += 1
  if (/[^A-Za-z0-9]/.test(password)) score += 1
  return Math.min(score, 5)
})

const strengthClass = computed(() => {
  if (strengthScore.value === 0) return 'none'
  if (strengthScore.value <= 2) return 'weak'
  if (strengthScore.value <= 3) return 'medium'
  return 'strong'
})

const strengthWidth = computed(() => `${(strengthScore.value / 5) * 100}%`)

const strengthText = computed(() => {
  switch (strengthClass.value) {
    case 'weak': return '弱'
    case 'medium': return '中'
    case 'strong': return '强'
    default: return ''
  }
})

const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function startCountdown(seconds = 60) {
  countdown.value = seconds
  clearInterval(countdownTimer)
  countdownTimer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

async function onSendCode() {
  if (sendingCode.value || countdown.value > 0) return
  if (!emailRegex.test(form.email)) {
    ElMessage.error('请输入正确的邮箱格式')
    return
  }

  sendingCode.value = true
  try {
    await auth.sendCode({ email: form.email })
    ElMessage.success('验证码已发送')
    startCountdown(60)
  } catch (err) {
    ElMessage.error(err.message || err.cause?.message || '验证码发送失败')
  } finally {
    sendingCode.value = false
  }
}

onUnmounted(() => {
  clearInterval(countdownTimer)
})

function onRegister() {
  regForm.value.validate((valid) => {
    if (valid) {
      if (form.password !== form.confirm) {
        ElMessage.error('两次输入的密码不一致')
        return
      }
      if (!form.code) {
        ElMessage.error('请输入邮箱验证码')
        return
      }
      loading.value = true
      auth
        .register({
          username: form.username,
          email: form.email,
          password: form.password,
          confirmPassword: form.confirm,
          code: form.code
        })
        .then(() => {
          ElMessage.success({ message: '注册成功！', type: 'success', duration: 2000 })
          setTimeout(() => router.push({ path: '/login' }), 1500)
        })
        .catch((err) => {
          ElMessage.error(err.message || '注册失败，请重试')
        })
        .finally(() => {
          loading.value = false
        })
    }
  })
}
</script>

<style scoped>
.auth-head__logo {
  margin-bottom: var(--space-4);
}

/* 邮箱行：图标 + 输入框 + 获取验证码按钮 */
.email-row {
  position: relative;
  display: flex;
  gap: var(--space-3);
  align-items: stretch;
}
.email-row .input { flex: 1; min-width: 0; }
.email-row .code-btn {
  flex-shrink: 0;
  min-width: 118px;
  border-radius: var(--radius);
  white-space: nowrap;
  font-size: var(--text-sm);
}
.email-row .code-btn:disabled { opacity: 0.55; cursor: not-allowed; }

/* 密码强度条 */
.strength {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-top: var(--space-2);
}
.strength-track {
  flex: 1;
  height: 4px;
  border-radius: var(--radius-full);
  background: rgba(255, 255, 255, 0.14);
  overflow: hidden;
}
.strength-fill {
  display: block;
  height: 100%;
  border-radius: var(--radius-full);
  transition: width var(--dur) var(--ease);
}
.strength-text {
  font-size: var(--text-xs);
  font-weight: var(--weight-medium);
}
.strength.weak .strength-fill { background: var(--danger); }
.strength.weak .strength-text { color: var(--danger); }
.strength.medium .strength-fill { background: var(--warning); }
.strength.medium .strength-text { color: var(--warning); }
.strength.strong .strength-fill { background: var(--success); }
.strength.strong .strength-text { color: var(--success); }
</style>
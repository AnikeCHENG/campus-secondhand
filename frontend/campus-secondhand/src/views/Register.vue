<template>
  <div class="auth-page">
    <VideoBackground />
    <div class="auth-card">
      <header class="auth-head">
        <span class="brand-mark" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 12a9 9 0 0 1 15-6.7L21 8" />
            <path d="M21 3v5h-5" />
            <path d="M21 12a9 9 0 0 1-15 6.7L3 16" />
            <path d="M3 21v-5h5" />
          </svg>
        </span>
        <h1 class="auth-title">创建账号</h1>
        <p class="auth-sub">加入校园二手交易平台</p>
      </header>

      <el-form :model="form" :rules="rules" ref="regForm" class="auth-form">
        <div class="field">
          <label class="field-label" for="reg-username">用户名</label>
          <input id="reg-username" v-model="form.username" class="input" type="text" placeholder="3-20 个字符" />
        </div>

        <div class="field">
          <label class="field-label" for="reg-email">邮箱</label>
          <input id="reg-email" v-model="form.email" class="input" type="email" placeholder="name@example.com" />
        </div>

        <div class="field">
          <label class="field-label" for="reg-password">密码</label>
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
  </div>
</template>

<script setup>
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import auth from '../api/auth'
import VideoBackground from '../components/VideoBackground.vue'

const router = useRouter()
const regForm = ref(null)
const loading = ref(false)
const showPassword = ref(false)
const showConfirmPassword = ref(false)

const form = reactive({ username: '', email: '', password: '', confirm: '' })

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

function onRegister() {
  regForm.value.validate((valid) => {
    if (valid) {
      if (form.password !== form.confirm) {
        ElMessage.error('两次输入的密码不一致')
        return
      }
      loading.value = true
      auth
        .register({
          username: form.username,
          email: form.email,
          password: form.password,
          confirmPassword: form.confirm
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
.auth-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-6);
  background: transparent;
}

.auth-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 400px;
  background: rgba(255, 255, 255, 0.82);
  -webkit-backdrop-filter: blur(18px) saturate(180%);
  backdrop-filter: blur(18px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: var(--radius-xl);
  padding: var(--space-10) var(--space-8);
  box-shadow: var(--shadow-md);
}

.auth-head { text-align: center; margin-bottom: var(--space-8); }

.brand-mark {
  width: 44px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius);
  background: var(--accent);
  color: ***REMOVED***fff;
  margin-bottom: var(--space-4);
}
.brand-mark svg { width: 24px; height: 24px; }

.auth-title { font-size: var(--text-2xl); font-weight: var(--weight-semibold); letter-spacing: -0.02em; color: var(--text); }
.auth-sub { margin-top: var(--space-1); font-size: var(--text-sm); color: var(--text-2); }

.auth-form { margin-bottom: var(--space-6); }

.input-affix { position: relative; }
.input-affix .input { padding-right: 44px; }
.affix-btn {
  position: absolute; top: 50%; right: 8px; transform: translateY(-50%);
  width: 30px; height: 30px; display: inline-flex; align-items: center; justify-content: center;
  border: none; background: transparent; color: var(--text-3); border-radius: var(--radius-sm);
}
.affix-btn:hover { color: var(--text); }
.affix-btn svg { width: 18px; height: 18px; }

.strength { display: flex; align-items: center; gap: var(--space-3); margin-top: var(--space-2); }
.strength-track { flex: 1; height: 4px; border-radius: var(--radius-full); background: var(--surface-3); overflow: hidden; }
.strength-fill { display: block; height: 100%; border-radius: var(--radius-full); transition: width var(--dur) var(--ease); }
.strength-text { font-size: var(--text-xs); font-weight: var(--weight-medium); }
.strength.weak .strength-fill { background: var(--danger); }
.strength.weak .strength-text { color: var(--danger); }
.strength.medium .strength-fill { background: var(--warning); }
.strength.medium .strength-text { color: var(--warning); }
.strength.strong .strength-fill { background: var(--success); }
.strength.strong .strength-text { color: var(--success); }

.auth-foot { text-align: center; font-size: var(--text-sm); color: var(--text-2); }
.text-link { font-size: var(--text-sm); color: var(--accent); }
.text-link:hover { color: var(--accent-hover); }

:deep(.el-form-item) { margin-bottom: 0; }

@media (max-width: 480px) {
  .auth-card { padding: var(--space-8) var(--space-6); }
}
</style>

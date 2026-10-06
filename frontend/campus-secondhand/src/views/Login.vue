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
        <h1 class="auth-title">登录</h1>
        <p class="auth-sub">欢迎回到校园二手交易平台</p>
      </header>

      <el-form :model="form" :rules="rules" ref="loginForm" class="auth-form">
        <div class="field">
          <label class="field-label" for="login-username">用户名 / 学号</label>
          <input
            id="login-username"
            v-model="form.username"
            class="input"
            type="text"
            autocomplete="username"
            placeholder="请输入用户名或学号"
          />
        </div>

        <div class="field">
          <label class="field-label" for="login-password">密码</label>
          <div class="input-affix">
            <input
              id="login-password"
              v-model="form.password"
              class="input"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="current-password"
              placeholder="请输入密码"
            />
            <button
              type="button"
              class="affix-btn"
              :aria-label="showPassword ? '隐藏密码' : '显示密码'"
              @click="showPassword = !showPassword"
            >
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
        </div>

        <div class="form-options">
          <label class="checkbox">
            <input type="checkbox" v-model="form.remember" />
            <span>记住我</span>
          </label>
          <router-link to="/forgot" class="text-link">忘记密码？</router-link>
        </div>

        <button
          type="button"
          class="btn btn-primary btn-block"
          :disabled="loading"
          @click="onSubmit"
        >
          {{ loading ? '登录中…' : '登录' }}
        </button>
      </el-form>

      <p class="auth-foot">
        还没有账号？
        <router-link to="/register" class="text-link">立即注册</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import auth from '../api/auth'
import VideoBackground from '../components/VideoBackground.vue'

const router = useRouter()
const route = useRoute()
const loginForm = ref(null)
const loading = ref(false)
const showPassword = ref(false)

const form = reactive({
  username: '',
  password: '',
  remember: false,
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

function onSubmit() {
  loginForm.value.validate((valid) => {
    if (valid) {
      loading.value = true
      const username = form.username?.trim()
      const password = form.password

      auth.login({ username, password })
        .then((res) => {
          const token = res?.token
          const user = res?.user
          if (token && user) {
            // 单独存一份 userId：部分页面（如 Messages.vue）需要直接取数值 ID，
            // 每次解析 user JSON 既冗余又容易在解析失败时静默退化为 0
            const uid = String(user.id ?? '')
            if (form.remember) {
              localStorage.setItem('token', token)
              localStorage.setItem('user', JSON.stringify(user))
              if (uid) localStorage.setItem('userId', uid)
            } else {
              sessionStorage.setItem('token', token)
              sessionStorage.setItem('user', JSON.stringify(user))
              if (uid) sessionStorage.setItem('userId', uid)
            }
            ElMessage.success({ message: '登录成功，欢迎回来！', type: 'success', duration: 2000 })
            const redirect = route.query.redirect || '/'
            router.push(redirect)
          } else {
            ElMessage.error('登录返回缺少 token 或用户信息')
          }
        })
        .catch((err) => {
          ElMessage.error(err.message || '登录失败，请检查用户名和密码')
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

.auth-head {
  text-align: center;
  margin-bottom: var(--space-8);
}

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

.auth-title {
  font-size: var(--text-2xl);
  font-weight: var(--weight-semibold);
  letter-spacing: -0.02em;
  color: var(--text);
}

.auth-sub {
  margin-top: var(--space-1);
  font-size: var(--text-sm);
  color: var(--text-2);
}

.auth-form { margin-bottom: var(--space-6); }

.input-affix { position: relative; }
.input-affix .input { padding-right: 44px; }
.affix-btn {
  position: absolute;
  top: 50%;
  right: 8px;
  transform: translateY(-50%);
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: var(--text-3);
  border-radius: var(--radius-sm);
}
.affix-btn:hover { color: var(--text); }
.affix-btn svg { width: 18px; height: 18px; }

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: var(--space-2) 0 var(--space-6);
}

.checkbox {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  color: var(--text-2);
  cursor: pointer;
}
.checkbox input { width: 15px; height: 15px; accent-color: var(--accent); }

.text-link { font-size: var(--text-sm); color: var(--accent); }
.text-link:hover { color: var(--accent-hover); }

.auth-foot {
  text-align: center;
  font-size: var(--text-sm);
  color: var(--text-2);
}

/* Element Plus 表单项去默认 margin，交由 .field 控制 */
:deep(.el-form-item) { margin-bottom: 0; }

@media (max-width: 480px) {
  .auth-card { padding: var(--space-8) var(--space-6); }
}
</style>

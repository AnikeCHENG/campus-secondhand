<template>
  <AuthShell variant="split" show-brand>
    <!-- 品牌区 -->
    <template #brand>
      <div class="auth-brand__inner">
        <header class="auth-brand__top">
          <BrandLogo :size="44" />
          <span class="auth-brand__name">校园二手交易</span>
        </header>

        <div class="auth-brand__copy">
          <h1 class="auth-brand__slogan">让闲置流转 · 让温暖传递</h1>
          <p class="auth-brand__sub">你的闲置，是别人的刚需</p>
        </div>

        <ul class="auth-brand__badges">
          <li class="auth-badge">🎓 学生实名认证</li>
          <li class="auth-badge">🛡 平台担保交易</li>
          <li class="auth-badge">🤝 校内面交</li>
        </ul>

        <div class="auth-deal-ticker" role="status" aria-live="polite">
          <transition name="auth-ticker">
            <p :key="dealIndex" class="auth-deal-text">{{ deals[dealIndex] }}</p>
          </transition>
        </div>
      </div>
    </template>

    <!-- 表单区 -->
    <div class="glass-card">
      <header class="auth-head">
        <BrandLogo :size="44" class="auth-head__logo" />
        <h1 class="auth-title">登录</h1>
        <p class="auth-sub">欢迎回到校园二手交易平台</p>
      </header>

      <el-form :model="form" :rules="rules" ref="loginForm" class="auth-form stagger">
        <div class="field">
          <label class="field-label" for="login-username">用户名 / 学号</label>
          <div class="field-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <circle cx="12" cy="8" r="4" />
              <path d="M4 21v-1a6 6 0 0 1 6-6h4a6 6 0 0 1 6 6v1" />
            </svg>
          </div>
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
          <div class="field-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <rect x="3" y="11" width="18" height="11" rx="2" />
              <path d="M7 11V7a5 5 0 0 1 10 0v4" />
            </svg>
          </div>
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
  </AuthShell>
</template>

<script setup>
import { reactive, ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import auth from '../api/auth'
import AuthShell from '../components/AuthShell.vue'
import BrandLogo from '../components/BrandLogo.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
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

// 成交滚动条 mock 文案（每 5 秒轮播）
const deals = [
  '刚刚 · 学长的《高数教材》找到了新主人',
  '1 分钟前 · 一辆闲置自行车被学妹骑走了',
  '3 分钟前 · 毕业师兄的耳机已安全交付',
]
const dealIndex = ref(0)
let dealTimer = null

function prefersReducedMotion() {
  return window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

onMounted(() => {
  if (prefersReducedMotion()) return
  dealTimer = setInterval(() => {
    dealIndex.value = (dealIndex.value + 1) % deals.length
  }, 5000)
})

onBeforeUnmount(() => {
  if (dealTimer) {
    clearInterval(dealTimer)
    dealTimer = null
  }
})

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
            // 登录响应已由服务端认证，直接写入 store，守卫无需再请求 /api/auth/me
            userStore.setFromLogin(user)
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
/* 头部 Logo 与标题的间距（组件其余样式内聚在 BrandLogo.vue） */
.auth-head__logo {
  margin-bottom: var(--space-4);
}
</style>
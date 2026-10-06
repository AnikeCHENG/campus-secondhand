<template>
  <div class="auth-page">
    <div class="auth-card">
      <header class="auth-head">
        <span class="brand-mark" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="11" width="18" height="11" rx="2" />
            <path d="M7 11V7a5 5 0 0 1 10 0v4" />
          </svg>
        </span>
        <h1 class="auth-title">重置密码</h1>
        <p class="auth-sub">输入注册邮箱，我们将发送重置链接</p>
      </header>

      <el-form :model="form" :rules="rules" ref="resetForm" class="auth-form">
        <div class="field">
          <label class="field-label" for="reset-email">邮箱</label>
          <input id="reset-email" v-model="form.email" class="input" type="email" placeholder="name@example.com" />
        </div>

        <button type="button" class="btn btn-primary btn-block" :disabled="loading" @click="onReset">
          {{ loading ? '发送中…' : '发送重置链接' }}
        </button>
      </el-form>

      <p class="auth-foot">
        <router-link to="/login" class="text-link">返回登录</router-link>
        <span class="sep">·</span>
        <router-link to="/register" class="text-link">立即注册</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import auth from '../api/auth'

const router = useRouter()
const resetForm = ref(null)
const loading = ref(false)
const form = reactive({ email: '' })

const rules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ]
}

function onReset() {
  if (!form.email) {
    ElMessage.error('请输入邮箱')
    return
  }
  loading.value = true
  auth
    .forgotPassword({ email: form.email })
    .then((res) => {
      ElMessage.success(res.message || '重置邮件已发送，请查收')
      setTimeout(() => router.push({ path: '/login' }), 2000)
    })
    .catch((err) => {
      ElMessage.error(
        err.message ||
          (err.response && err.response.data && err.response.data.message) ||
          '发送失败，请稍后重试'
      )
    })
    .finally(() => {
      loading.value = false
    })
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-6);
  background: var(--bg);
}
.auth-card {
  width: 100%;
  max-width: 400px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  padding: var(--space-10) var(--space-8);
  box-shadow: var(--shadow-sm);
}
.auth-head { text-align: center; margin-bottom: var(--space-8); }
.brand-mark {
  width: 44px; height: 44px;
  display: inline-flex; align-items: center; justify-content: center;
  border-radius: var(--radius); background: var(--accent); color: #fff;
  margin-bottom: var(--space-4);
}
.brand-mark svg { width: 24px; height: 24px; }
.auth-title { font-size: var(--text-2xl); font-weight: var(--weight-semibold); letter-spacing: -0.02em; color: var(--text); }
.auth-sub { margin-top: var(--space-1); font-size: var(--text-sm); color: var(--text-2); }
.auth-form { margin-bottom: var(--space-6); }
.auth-foot { text-align: center; font-size: var(--text-sm); color: var(--text-2); }
.text-link { color: var(--accent); }
.text-link:hover { color: var(--accent-hover); }
.sep { margin: 0 var(--space-2); color: var(--text-3); }
:deep(.el-form-item) { margin-bottom: 0; }
@media (max-width: 480px) { .auth-card { padding: var(--space-8) var(--space-6); } }
</style>

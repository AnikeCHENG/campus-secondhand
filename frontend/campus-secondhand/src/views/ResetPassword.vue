<template>
  <AuthShell>
    <div class="glass-card">
      <header class="auth-head">
        <BrandLogo :size="44" class="auth-head__logo" />
        <h1 class="auth-title">重置密码</h1>
        <p class="auth-sub">输入注册邮箱，我们将发送重置链接</p>
      </header>

      <el-form :model="form" :rules="rules" ref="resetForm" class="auth-form stagger">
        <div class="field">
          <label class="field-label" for="reset-email">邮箱</label>
          <div class="field-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <rect x="2" y="4" width="20" height="16" rx="2" />
              <path d="m2 7 10 6 10-6" />
            </svg>
          </div>
          <input id="reset-email" v-model="form.email" class="input" type="email" placeholder="name@example.com" />
        </div>

        <button type="button" class="btn btn-primary btn-block" :disabled="loading" @click="onReset">
          {{ loading ? '发送中…' : '发送重置链接' }}
        </button>
      </el-form>

      <p class="auth-foot">
        <router-link to="/login" class="text-link">返回登录</router-link>
        <span class="auth-sep">·</span>
        <router-link to="/register" class="text-link">立即注册</router-link>
      </p>
    </div>
  </AuthShell>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import auth from '../api/auth'
import AuthShell from '../components/AuthShell.vue'
import BrandLogo from '../components/BrandLogo.vue'

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
.auth-head__logo {
  margin-bottom: var(--space-4);
}
</style>
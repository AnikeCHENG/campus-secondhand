<template>
  <div class="page">
    <header class="site-header">
      <div class="container header-inner">
        <router-link to="/" class="brand">
          <span class="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 12a9 9 0 0 1 15-6.7L21 8" /><path d="M21 3v5h-5" />
              <path d="M21 12a9 9 0 0 1-15 6.7L3 16" /><path d="M3 21v-5h5" />
            </svg>
          </span>
          <span class="brand-name">校园二手</span>
        </router-link>
        <nav class="main-nav" aria-label="主导航">
          <router-link to="/" class="nav-link">首页</router-link>
          <router-link to="/products" class="nav-link">商品</router-link>
          <router-link to="/post" class="nav-link">发布</router-link>
          <router-link to="/messages" class="nav-link">消息</router-link>
          <router-link to="/profile" class="nav-link">我的</router-link>
        </nav>
        <div class="header-actions">
          <button class="icon-btn" type="button" aria-label="退出登录" @click="handleLogout">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" /><polyline points="16 17 21 12 16 7" /><line x1="21" y1="12" x2="9" y2="12" />
            </svg>
          </button>
          <button class="avatar" type="button" aria-label="个人中心" @click="go('/profile')">
            <img src="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 40 40'%3E%3Crect width='40' height='40' fill='%230b6e54'/%3E%3Ccircle cx='20' cy='16' r='6' fill='%23ffffff'/%3E%3Cpath d='M8 36c0-6.6 5.4-12 12-12s12 5.4 12 12' fill='%23ffffff'/%3E%3C/svg%3E" alt="用户头像" />
          </button>
        </div>
      </div>
    </header>

    <main class="page-main post-main">
      <div class="page-head">
        <h1 class="page-title">发布闲置</h1>
        <p class="page-sub">让闲置物品找到新主人</p>
      </div>

      <form @submit.prevent="handleSubmit" class="post-form">
        <section class="card">
          <h2 class="section-title">基本信息</h2>

          <div class="field">
            <label class="field-label" for="p-title">商品标题 <span class="required">*</span></label>
            <input id="p-title" type="text" v-model="form.title" class="input" placeholder="请输入商品标题，最多50个字符" maxlength="50" required />
            <span class="char-count">{{ form.title.length }}/50</span>
          </div>

          <div class="form-row">
            <div class="field">
              <label class="field-label" for="p-cat">商品分类 <span class="required">*</span></label>
              <select id="p-cat" v-model="form.category" class="select" required>
                <option value="">请选择分类</option>
                <option value="books">教材书籍</option>
                <option value="electronics">电子产品</option>
                <option value="transport">出行工具</option>
                <option value="gaming">游戏数码</option>
                <option value="clothing">服饰穿搭</option>
                <option value="living">生活用品</option>
                <option value="other">其他</option>
              </select>
            </div>
            <div class="field">
              <label class="field-label" for="p-cond">商品成色 <span class="required">*</span></label>
              <select id="p-cond" v-model="form.condition" class="select" required>
                <option value="">请选择成色</option>
                <option value="全新">全新未使用</option>
                <option value="几乎全新">几乎全新</option>
                <option value="轻微使用痕迹">轻微使用痕迹</option>
                <option value="明显使用痕迹">明显使用痕迹</option>
                <option value="有磨损">有磨损/瑕疵</option>
              </select>
            </div>
          </div>

          <div class="form-row">
            <div class="field">
              <label class="field-label" for="p-price">商品价格 <span class="required">*</span></label>
              <div class="price-wrap">
                <span class="price-symbol">¥</span>
                <input id="p-price" type="number" v-model="form.price" class="input price-input" placeholder="0.00" min="0" step="0.01" required />
              </div>
            </div>
            <div class="field">
              <label class="field-label" for="p-oprice">原价（可选）</label>
              <div class="price-wrap">
                <span class="price-symbol">¥</span>
                <input id="p-oprice" type="number" v-model="form.originalPrice" class="input price-input" placeholder="0.00" min="0" step="0.01" />
              </div>
            </div>
          </div>
          <p class="form-hint">填写原价可以显示折扣信息</p>
        </section>

        <section class="card">
          <h2 class="section-title">商品描述</h2>
          <div class="field">
            <label class="field-label" for="p-desc">详细描述 <span class="required">*</span></label>
            <textarea id="p-desc" v-model="form.description" class="textarea" placeholder="请详细描述商品的状态、使用时间、转手原因等…" rows="6" maxlength="500" required></textarea>
            <span class="char-count">{{ form.description.length }}/500</span>
          </div>
        </section>

        <section class="card">
          <h2 class="section-title">商品图片</h2>
          <div class="image-upload">
            <div v-for="(img, index) in form.images" :key="index" class="uploaded-image">
              <img :src="img" :alt="'商品图片' + (index + 1)" />
              <button type="button" class="remove-image" aria-label="移除图片" @click="removeImage(index)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            </div>
            <label v-if="form.images.length < 9" class="upload-button">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" /><polyline points="17 8 12 3 7 8" /><line x1="12" y1="3" x2="12" y2="15" />
              </svg>
              <span>上传图片</span>
              <input type="file" accept="image/*" @change="handleImageUpload" hidden />
            </label>
          </div>
          <p class="form-hint">最多可上传 9 张图片，支持 JPG、PNG 格式</p>
        </section>

        <section class="card tips">
          <h2 class="section-title">发布须知</h2>
          <ul class="tips-list">
            <li>商品信息需真实准确，不得虚假宣传</li>
            <li>建议上传实物图片，展示商品真实状态</li>
            <li>定价合理、描述详细，更容易成交</li>
            <li>遵守平台规则，文明交易</li>
          </ul>
        </section>

        <div class="form-actions">
          <button type="button" class="btn btn-outline" @click="saveDraft">保存草稿</button>
          <button type="submit" class="btn btn-primary" :disabled="submitting">
            {{ submitting ? '发布中…' : '发布商品' }}
          </button>
        </div>
      </form>
    </main>

    <div v-if="showSuccess" class="overlay" @click.self="showSuccess = false">
      <div class="modal">
        <div class="success-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" /><polyline points="22 4 12 14.01 9 11.01" />
          </svg>
        </div>
        <h2>发布成功！</h2>
        <p>您的商品已成功发布，等待买家咨询</p>
        <div class="modal-actions">
          <button class="btn btn-outline" @click="go('/products')">返回商品列表</button>
          <button class="btn btn-primary" @click="continuePost">继续发布</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { createProduct } from '../api/product'

const router = useRouter()
const submitting = ref(false)
const showSuccess = ref(false)

const form = reactive({
  title: '',
  category: '',
  condition: '',
  price: '',
  originalPrice: '',
  description: '',
  images: []
})

function go(path) { router.push(path) }

function handleImageUpload(event) {
  const file = event.target.files[0]
  if (!file) return
  if (form.images.length >= 9) { alert('最多只能上传9张图片'); return }
  const reader = new FileReader()
  reader.onload = (e) => {
    const img = new Image()
    img.onload = () => {
      const canvas = document.createElement('canvas')
      const ctx = canvas.getContext('2d')
      let width = img.width
      let height = img.height
      const maxSize = 800
      if (width > height) {
        if (width > maxSize) { height = Math.round(height * (maxSize / width)); width = maxSize }
      } else {
        if (height > maxSize) { width = Math.round(width * (maxSize / height)); height = maxSize }
      }
      canvas.width = width
      canvas.height = height
      ctx.drawImage(img, 0, 0, width, height)
      form.images.push(canvas.toDataURL('image/jpeg', 0.7))
    }
    img.src = e.target.result
  }
  reader.readAsDataURL(file)
  event.target.value = ''
}

function removeImage(index) { form.images.splice(index, 1) }

async function handleSubmit() {
  if (!form.title || !form.category || !form.condition || !form.price || !form.description) {
    alert('请填写所有必填项')
    return
  }
  submitting.value = true
  try {
    const res = await createProduct({
      title: form.title,
      category: form.category,
      condition: form.condition,
      price: parseFloat(form.price),
      originalPrice: form.originalPrice ? parseFloat(form.originalPrice) : null,
      description: form.description,
      images: form.images.join(',')
    })
    if (res.code === 200) {
      showSuccess.value = true
      resetForm()
      setTimeout(() => router.push('/products'), 1500)
    } else {
      alert(res.message || '发布失败，请重试')
    }
  } catch (e) {
    alert(e.message || '发布失败，请重试')
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  form.title = ''; form.category = ''; form.condition = ''; form.price = ''
  form.originalPrice = ''; form.description = ''; form.images = []
}

function saveDraft() {
  const draft = { ...form, savedAt: Date.now() }
  const drafts = JSON.parse(localStorage.getItem('product_drafts') || '[]')
  drafts.unshift(draft)
  localStorage.setItem('product_drafts', JSON.stringify(drafts))
  localStorage.removeItem('product_draft')
  alert('草稿已保存到草稿箱')
}

function continuePost() { showSuccess.value = false; resetForm() }

async function handleLogout() {
  localStorage.removeItem('token'); localStorage.removeItem('username'); localStorage.removeItem('product_draft'); sessionStorage.removeItem('justLoggedIn'); router.push('/login')
}

onMounted(() => {
  const draft = localStorage.getItem('product_draft')
  if (draft) { try { Object.assign(form, JSON.parse(draft)) } catch { console.log('读取草稿失败') } }
})
</script>

<style scoped>
.site-header { position: sticky; top: 0; z-index: 100; background: rgba(255,255,255,0.85); backdrop-filter: saturate(180%) blur(12px); border-bottom: 1px solid var(--border); }
.header-inner { height: var(--header-h); display: flex; align-items: center; gap: var(--space-8); }
.brand { display: inline-flex; align-items: center; gap: var(--space-2); color: var(--text); flex-shrink: 0; }
.brand:hover { color: var(--text); }
.brand-mark { width: 30px; height: 30px; display: inline-flex; align-items: center; justify-content: center; border-radius: var(--radius-sm); background: var(--accent); color: ***REMOVED***fff; }
.brand-mark svg { width: 17px; height: 17px; }
.brand-name { font-size: var(--text-lg); font-weight: var(--weight-semibold); letter-spacing: -0.01em; }
.main-nav { display: flex; gap: var(--space-1); flex: 1; }
.nav-link { padding: 8px 12px; font-size: var(--text-base); color: var(--text-2); border-radius: var(--radius-sm); transition: color var(--dur-fast) var(--ease), background var(--dur-fast) var(--ease); }
.nav-link:hover { color: var(--text); background: var(--surface-3); }
.nav-link.router-link-exact-active { color: var(--accent); font-weight: var(--weight-medium); }
.header-actions { display: flex; align-items: center; gap: var(--space-2); }
.icon-btn { width: 38px; height: 38px; display: inline-flex; align-items: center; justify-content: center; border: 1px solid transparent; border-radius: var(--radius); background: transparent; color: var(--text-2); }
.icon-btn:hover { background: var(--surface-3); color: var(--text); }
.icon-btn svg { width: 20px; height: 20px; }
.avatar { width: 36px; height: 36px; padding: 0; border: 1px solid var(--border); border-radius: var(--radius-full); overflow: hidden; background: var(--surface-3); }
.avatar img { width: 100%; height: 100%; object-fit: cover; }

.post-main { max-width: 760px; }
.page-head { margin-bottom: var(--space-6); }
.page-title { font-size: var(--text-2xl); font-weight: var(--weight-semibold); letter-spacing: -0.02em; }
.page-sub { margin-top: var(--space-1); color: var(--text-2); font-size: var(--text-sm); }
.post-form { display: flex; flex-direction: column; gap: var(--space-5); }
.section-title { font-size: var(--text-base); font-weight: var(--weight-semibold); margin-bottom: var(--space-5); }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-4); }
.required { color: var(--danger); }
.char-count { position: absolute; right: 12px; bottom: 10px; font-size: var(--text-xs); color: var(--text-3); }
.field { position: relative; }
.form-hint { font-size: var(--text-xs); color: var(--text-3); }

.price-wrap { position: relative; }
.price-symbol { position: absolute; left: 14px; top: 50%; transform: translateY(-50%); font-size: var(--text-base); color: var(--text-2); }
.price-input { padding-left: 34px; }

.image-upload { display: flex; flex-wrap: wrap; gap: var(--space-3); }
.uploaded-image { position: relative; width: 96px; height: 96px; border-radius: var(--radius); overflow: hidden; border: 1px solid var(--border); }
.uploaded-image img { width: 100%; height: 100%; object-fit: cover; }
.remove-image { position: absolute; top: 4px; right: 4px; width: 22px; height: 22px; display: inline-flex; align-items: center; justify-content: center; background: rgba(0,0,0,0.55); border: none; border-radius: var(--radius-full); color: ***REMOVED***fff; }
.remove-image svg { width: 13px; height: 13px; }
.remove-image:hover { background: var(--danger); }
.upload-button { width: 96px; height: 96px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px; border: 1px dashed var(--border-strong); border-radius: var(--radius); cursor: pointer; color: var(--text-3); transition: border-color var(--dur-fast) var(--ease), color var(--dur-fast) var(--ease), background var(--dur-fast) var(--ease); }
.upload-button svg { width: 24px; height: 24px; }
.upload-button span { font-size: var(--text-xs); }
.upload-button:hover { border-color: var(--accent); color: var(--accent); background: var(--accent-soft); }

.tips { background: var(--surface-2); }
.tips-list { list-style: none; padding: 0; display: flex; flex-direction: column; gap: var(--space-2); }
.tips-list li { position: relative; padding-left: var(--space-5); font-size: var(--text-sm); color: var(--text-2); }
.tips-list li::before { content: ''; position: absolute; left: 0; top: 8px; width: 6px; height: 6px; border-radius: 50%; background: var(--accent); }

.form-actions { display: flex; justify-content: flex-end; gap: var(--space-3); }

.overlay { position: fixed; inset: 0; z-index: 200; background: rgba(20,20,18,0.35); backdrop-filter: blur(3px); display: flex; align-items: center; justify-content: center; padding: var(--space-4); }
.modal { width: 100%; max-width: 400px; background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-lg); padding: var(--space-8); text-align: center; box-shadow: var(--shadow-md); }
.success-icon { width: 56px; height: 56px; margin: 0 auto var(--space-5); display: flex; align-items: center; justify-content: center; border-radius: var(--radius-full); background: var(--accent-soft); color: var(--accent); }
.success-icon svg { width: 28px; height: 28px; }
.modal h2 { font-size: var(--text-xl); font-weight: var(--weight-semibold); margin-bottom: var(--space-2); }
.modal p { color: var(--text-2); font-size: var(--text-sm); margin-bottom: var(--space-6); }
.modal-actions { display: flex; gap: var(--space-3); justify-content: center; }

@media (max-width: 720px) {
  .main-nav { display: none; }
  .form-row { grid-template-columns: 1fr; }
}
</style>

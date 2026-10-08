<template>
  <div class="page">

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
          <h2 class="section-title">成色与瑕疵</h2>
          <p class="section-hint">如实填写能显著降低买卖纠纷，买家最关心的就是用了多久、有没有毛病。</p>

          <div class="field">
            <label class="field-label" for="p-level">商品成色 <span class="required">*</span></label>
            <div class="chip-row" role="radiogroup" aria-labelledby="p-level">
              <button
                v-for="lv in conditionLevels"
                :key="lv.value"
                class="chip"
                :class="{ active: form.conditionLevel === lv.value }"
                type="button"
                role="radio"
                :aria-checked="form.conditionLevel === lv.value"
                @click="form.conditionLevel = lv.value"
              >
                {{ lv.label }}
              </button>
            </div>
            <p class="form-hint">{{ selectedConditionHint }}</p>
          </div>

          <div class="field">
            <label class="field-label" for="p-flaw">瑕疵说明 <span class="optional">（选填）</span></label>
            <textarea
              id="p-flaw"
              v-model="form.flawDescription"
              class="textarea"
              placeholder="请如实描述划痕、维修史等，无瑕疵可留空"
              rows="4"
              maxlength="255"
            ></textarea>
            <span class="char-count">{{ form.flawDescription.length }}/255</span>
            <p class="form-hint">留空将展示为「卖家承诺无明显瑕疵」；填写后会公开展示在商品详情页。</p>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { createProduct } from '../api/product'

const router = useRouter()
const submitting = ref(false)
const showSuccess = ref(false)

/**
 * 成色等级选项，value 与后端 {@code ConditionLevel} 的 code 一一对应。
 *
 * <p>不要在这里改 value：后端按 0~4 校验，传别的值会直接被 400 拒绝。
 * 数值越大＝越旧。</p>
 */
const conditionLevels = [
  { value: 0, label: '全新', hint: '未使用或仅试机' },
  { value: 1, label: '99新', hint: '几乎全新，无明显使用痕迹' },
  { value: 2, label: '95新', hint: '轻微使用痕迹，功能完好' },
  { value: 3, label: '9成新', hint: '有正常使用痕迹' },
  { value: 4, label: '8成新以下', hint: '明显磨损或存在瑕疵' }
]

const form = reactive({
  title: '',
  category: '',
  /** 成色等级 0~4，null 表示未选（提交前会被拦下） */
  conditionLevel: null,
  /** 瑕疵说明，留空即视为无明显瑕疵 */
  flawDescription: '',
  price: '',
  originalPrice: '',
  description: '',
  images: []
})

const selectedConditionHint = computed(() => {
  const hit = conditionLevels.find(lv => lv.value === form.conditionLevel)
  return hit ? hit.hint : '请选择一个成色等级'
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
  // 成色单独提示：它比标题更容易被漏掉，混在"请填写完整信息"里看不出缺了什么
  if (form.conditionLevel === null) {
    alert('请选择商品成色')
    return
  }
  if (!form.title || !form.category || !form.price || !form.description) {
    alert('请填写完整信息')
    return
  }
  submitting.value = true
  try {
    const res = await createProduct({
      title: form.title,
      category: form.category,
      conditionLevel: form.conditionLevel,
      // 空串传 null 而非 ''，后端据此把"未填写"与"填了空"区分开
      flawDescription: form.flawDescription.trim() || null,
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
  form.title = ''; form.category = ''; form.conditionLevel = null; form.flawDescription = ''; form.price = ''
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

onMounted(() => {
  const draft = localStorage.getItem('product_draft')
  if (draft) { try { Object.assign(form, JSON.parse(draft)) } catch { console.log('读取草稿失败') } }
})
</script>

<style scoped>

/* ---- 成色与瑕疵 ---- */
.section-hint { margin: 0 0 var(--space-4); font-size: var(--text-sm); color: var(--text-2); }
.chip-row { display: flex; flex-wrap: wrap; gap: var(--space-2); }
.chip {
  padding: var(--space-2) var(--space-4);
  border: 1px solid var(--border);
  border-radius: 999px;
  background: var(--surface);
  color: var(--text-2);
  font-size: var(--text-sm);
  cursor: pointer;
  transition: border-color var(--dur) var(--ease), background var(--dur) var(--ease), color var(--dur) var(--ease);
}
.chip:hover { border-color: var(--accent); color: var(--text); }
.chip:focus-visible { outline: 2px solid var(--accent); outline-offset: 2px; }
.chip.active { border-color: var(--accent); background: var(--accent); color: #fff; }
.optional { color: var(--text-3); font-weight: var(--weight-normal); }
.form-hint { margin-top: var(--space-2); font-size: var(--text-xs); color: var(--text-3); }
.brand-mark svg { width: 17px; height: 17px; }
.icon-btn svg { width: 20px; height: 20px; }
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
.remove-image { position: absolute; top: 4px; right: 4px; width: 22px; height: 22px; display: inline-flex; align-items: center; justify-content: center; background: rgba(0,0,0,0.55); border: none; border-radius: var(--radius-full); color: #fff; }
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
  .form-row { grid-template-columns: 1fr; }
}
</style>

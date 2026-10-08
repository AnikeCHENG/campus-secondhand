<template>
  <div class="page">

    <main class="page-main">
      <div class="page-head">
        <div>
          <h1 class="page-title">草稿箱</h1>
          <p class="page-sub">管理您保存的商品草稿</p>
        </div>
        <router-link to="/post" class="btn btn-primary">去发布商品</router-link>
      </div>

      <div v-if="drafts.length === 0" class="empty">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z" /><polyline points="17 21 17 13 7 13 7 21" /><polyline points="7 3 7 8 15 8" />
        </svg>
        <p>暂无草稿</p>
        <p class="empty-sub">您还没有保存任何商品草稿</p>
      </div>

      <div v-else class="drafts-grid">
        <article v-for="(draft, index) in drafts" :key="index" class="draft-card card">
          <div v-if="draft.images && draft.images.length > 0" class="draft-image">
            <img :src="draft.images[0]" :alt="draft.title" />
            <span v-if="draft.images.length > 1" class="image-count">{{ draft.images.length }}</span>
          </div>
          <div v-else class="draft-image placeholder">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <rect x="3" y="3" width="18" height="18" rx="2" /><circle cx="8.5" cy="8.5" r="1.5" /><polyline points="21 15 16 10 5 21" />
            </svg>
          </div>
          <div class="draft-info">
            <h3 class="draft-title">{{ draft.title || '未命名商品' }}</h3>
            <div class="draft-meta">
              <span class="tag">{{ getCategoryName(draft.category) }}</span>
              <span v-if="draft.condition" class="tag">{{ draft.condition }}</span>
            </div>
            <p v-if="draft.price" class="draft-price">¥{{ draft.price }}</p>
            <p class="draft-date">保存于 {{ formatDate(draft.savedAt) }}</p>
          </div>
          <div class="draft-actions">
            <button class="btn btn-outline btn-sm" @click="editDraft(draft)">编辑</button>
            <button class="btn btn-ghost btn-sm danger" @click="deleteDraft(index)">删除</button>
          </div>
        </article>
      </div>

      <div v-if="drafts.length > 0" class="clear-all">
        <button class="btn btn-ghost" @click="clearAllDrafts">清空所有草稿</button>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const drafts = ref([])

const categoryNames = {
  books: '教材书籍', electronics: '电子产品', transport: '出行工具',
  gaming: '游戏数码', clothing: '服饰穿搭', living: '生活用品', other: '其他'
}

function getCategoryName(category) { return categoryNames[category] || '其他' }
function formatDate(timestamp) {
  if (!timestamp) return '未知时间'
  const date = new Date(timestamp)
  return `${date.getMonth() + 1}月${date.getDate()}日 ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
}
function editDraft(draft) { localStorage.setItem('editing_draft', JSON.stringify(draft)); router.push('/post') }
function deleteDraft(index) {
  if (confirm('确定要删除这个草稿吗？')) {
    const savedDrafts = JSON.parse(localStorage.getItem('product_drafts') || '[]')
    savedDrafts.splice(index, 1)
    localStorage.setItem('product_drafts', JSON.stringify(savedDrafts))
    drafts.value = savedDrafts
  }
}
function clearAllDrafts() {
  if (confirm('确定要清空所有草稿吗？此操作不可恢复。')) {
    localStorage.removeItem('product_drafts')
    drafts.value = []
  }
}
onMounted(() => { drafts.value = JSON.parse(localStorage.getItem('product_drafts') || '[]') })
</script>

<style scoped>
.brand-mark svg { width: 17px; height: 17px; }
.icon-btn svg { width: 20px; height: 20px; }
.avatar img { width: 100%; height: 100%; object-fit: cover; }

.page-head { display: flex; align-items: flex-end; justify-content: space-between; gap: var(--space-4); margin-bottom: var(--space-6); }
.page-title { font-size: var(--text-2xl); font-weight: var(--weight-semibold); letter-spacing: -0.02em; }
.page-sub { margin-top: var(--space-1); color: var(--text-2); font-size: var(--text-sm); }
.empty-sub { font-size: var(--text-sm); }

.drafts-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: var(--space-5); }
.draft-card { padding: 0; overflow: hidden; }
.draft-image { position: relative; aspect-ratio: 16 / 10; background: var(--surface-3); }
.draft-image img { width: 100%; height: 100%; object-fit: cover; }
.draft-image.placeholder { display: flex; align-items: center; justify-content: center; color: var(--text-3); }
.draft-image.placeholder svg { width: 40px; height: 40px; }
.image-count { position: absolute; bottom: 8px; right: 8px; padding: 2px 8px; background: rgba(0,0,0,0.6); color: #fff; border-radius: var(--radius-full); font-size: var(--text-xs); }
.draft-info { padding: var(--space-4); }
.draft-title { font-size: var(--text-base); font-weight: var(--weight-medium); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.draft-meta { display: flex; gap: var(--space-2); margin: var(--space-3) 0; }
.tag { padding: 2px 10px; font-size: var(--text-xs); color: var(--text-2); background: var(--surface-3); border-radius: var(--radius-full); }
.draft-price { font-size: var(--text-lg); font-weight: var(--weight-semibold); color: var(--accent); }
.draft-date { margin-top: 4px; font-size: var(--text-xs); color: var(--text-3); }
.draft-actions { display: flex; gap: var(--space-2); padding: var(--space-3) var(--space-4); border-top: 1px solid var(--border); }
.draft-actions .btn { flex: 1; }
.danger { color: var(--danger); }
.danger:hover { background: var(--danger-soft); color: var(--danger); }
.clear-all { text-align: center; margin-top: var(--space-8); }

@media (max-width: 720px) {
  .page-head { flex-direction: column; align-items: flex-start; }
}
</style>

<template>
  <div class="page plaza">
    <header class="plaza-header">
      <div class="container plaza-header-inner">
        <router-link to="/" class="brand">校园二手</router-link>
        <nav class="plaza-nav" aria-label="主导航">
          <router-link to="/" class="nav-link">首页</router-link>
          <router-link to="/plaza" class="nav-link is-active">大厅</router-link>
          <router-link to="/products" class="nav-link">商品</router-link>
          <router-link to="/messages" class="nav-link">消息</router-link>
        </nav>
      </div>
    </header>

    <main class="plaza-shell">
      <aside class="plaza-left">
        <!-- 用户名片：字段取不到就隐藏对应区域，不显示占位文案 -->
        <section class="card plaza-user">
          <div class="avatar avatar-lg">
            <img v-if="profile?.avatar" :src="profile.avatar" :alt="`${profile.nickname} 的头像`" />
            <template v-else>{{ (profile?.nickname || '?').charAt(0) }}</template>
          </div>
          <h2>
            {{ profile?.nickname }}
            <VerifiedBadge v-if="profile?.verified" :verified="true" size="sm" />
          </h2>
          <p v-if="profile?.grade">{{ profile.grade }}</p>
          <div v-if="profile?.stats" class="user-stats">
            <span><b>{{ profile.stats.posts }}</b>动态</span>
            <span><b>{{ profile.stats.selling }}</b>在售</span>
            <span><b>{{ profile.stats.seeking }}</b>求购</span>
          </div>
          <p v-else-if="profileError" class="user-hint">{{ profileError }}</p>
        </section>

        <button class="plaza-post-btn" type="button" @click="openComposer">+ 发布动态/商品</button>

        <section class="card plaza-filter">
          <h3>分类筛选</h3>
          <button
            v-for="item in filters"
            :key="item.value"
            type="button"
            :class="{ active: activeFilter === item.value }"
            @click="setFilter(item.value)"
          >
            {{ item.label }}
          </button>
        </section>
      </aside>

      <section class="plaza-feed">
        <!-- 发布器 -->
        <div v-if="composerOpen" class="card feed-composer">
          <div class="composer-types">
            <button
              v-for="t in typeOptions"
              :key="t.value"
              type="button"
              :class="{ active: draft.type === t.value }"
              @click="draft.type = t.value"
            >
              {{ t.label }}
            </button>
          </div>

          <textarea
            v-model="draft.content"
            class="composer-textarea"
            rows="3"
            :placeholder="composerPlaceholder"
          ></textarea>

          <div class="composer-images">
            <div v-for="(img, i) in draft.images" :key="i" class="uploaded-image">
              <img :src="img" :alt="`已上传第${i + 1}张图片`" />
              <button class="remove-image" type="button" @click="removeImage(i)">×</button>
            </div>
            <label v-if="draft.images.length < 9" class="upload-button">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" /><polyline points="17 8 12 3 7 8" /><line x1="12" y1="3" x2="12" y2="15" />
              </svg>
              <span>上传图片</span>
              <input type="file" accept="image/*" multiple hidden @change="handleImageUpload" />
            </label>
          </div>

          <!-- 仅 SELL/FREE 显示商品信息 -->
          <div v-if="needsProductInfo" class="composer-product">
            <label class="cp-field">
              <span>商品标题 <em>*</em></span>
              <input v-model="draft.productTitle" type="text" placeholder="例：九成新自行车，附送车锁" />
            </label>
            <label class="cp-field cp-field--price">
              <span>价格（元）<em v-if="draft.type === 'SELL'">*</em></span>
              <input
                v-model="draft.productPrice"
                type="number"
                min="0"
                step="0.01"
                :placeholder="draft.type === 'FREE' ? '免费送可留空' : '必填'"
              />
            </label>
          </div>
          <p v-if="draftError" class="composer-error">{{ draftError }}</p>

          <div class="composer-actions">
            <button type="button" class="cp-cancel" @click="composerOpen = false">取消</button>
            <button type="button" class="cp-submit" :disabled="submitting" @click="submitPost">
              {{ submitting ? '发布中…' : '发布' }}
            </button>
          </div>
        </div>
        <div v-else class="card feed-composer">
          <button type="button" @click="openComposer">分享动态、发起求购，或把闲置挂上大厅…</button>
        </div>

        <!-- 加载失败 -->
        <div v-if="feedError" class="card feed-state">
          <p>{{ feedError }}</p>
          <button type="button" @click="loadFeed">重试</button>
        </div>

        <!-- 骨架屏 -->
        <div v-else-if="loading" class="card feed-skeleton">
          <div class="sk sk-row"></div>
          <div class="sk sk-line"></div>
          <div class="sk sk-line sk-line--short"></div>
        </div>

        <!-- 空状态 -->
        <div v-else-if="posts.length === 0" class="card feed-state">
          <p>{{ activeFilter === 'all' ? '大厅还没有动态，来发第一条吧' : '该分类下暂无动态' }}</p>
          <button type="button" @click="activeFilter = 'all'; loadFeed()">查看全部动态</button>
        </div>

        <article v-for="post in posts" :key="post.id" class="card feed-card">
          <header class="feed-head">
            <div class="avatar">
              <img v-if="post.author?.avatar" :src="post.author.avatar" :alt="`${post.author.username} 的头像`" />
              <template v-else>{{ (post.author?.username || '?').charAt(0) }}</template>
            </div>
            <div class="feed-meta">
              <b>{{ post.author?.username }}</b>
              <span>
                {{ formatTime(post.createdTime) }}
                <template v-if="post.author?.location"> · {{ post.author.location }}</template>
              </span>
            </div>
            <em class="badge" :data-type="post.type">{{ typeIcon(post.type) }} {{ post.typeLabel }}</em>
          </header>

          <p class="feed-content" :class="{ expanded: expanded[post.id] }">{{ post.content }}</p>
          <button
            v-if="post.content.length > 80"
            class="expand-btn"
            type="button"
            @click="expanded[post.id] = !expanded[post.id]"
          >
            {{ expanded[post.id] ? '收起' : '展开全文' }}
          </button>

          <div v-if="post.tagList?.length" class="tags">
            <span v-for="tag in post.tagList" :key="tag">#{{ tag }}</span>
          </div>

          <div v-if="post.imageList?.length" class="feed-images" :class="imageGridClass(post.imageList.length)">
            <div v-for="(img, i) in post.imageList.slice(0, 9)" :key="i" class="image-item">
              <img :src="img" alt="动态图片" />
              <span>查看详情</span>
            </div>
          </div>

          <!-- 商品卡片：仅 productId 非空时有值 -->
          <router-link
            v-if="post.product"
            :to="`/products/${post.product.id}`"
            class="feed-product"
          >
            <span class="fp-title">{{ post.product.title }}</span>
            <span class="fp-price">¥{{ post.product.price }}</span>
            <span class="fp-status">{{ productStatusText(post.product.status) }}</span>
          </router-link>

          <div v-else-if="post.type === 'SEEK'" class="price-row">求带价</div>

          <!--
            互动按钮：仅商品类动态显示。
            现有互动接口挂在 /api/products 下，且 like_record/comments 表没有
            target_type 列，post 与 product 的 ID 会碰撞，故非商品类动态不提供互动入口。
          -->
          <footer v-if="post.productId" class="feed-actions">
            <button type="button" :class="{ 'like-pop': likeAnimating[post.id] }" @click="onLike(post)">
              {{ likedMap[post.id] ? '❤️' : '🤍' }} 点赞 {{ post.likeCount ?? 0 }}
            </button>
            <button type="button" @click="toggleComments(post)">
              💬 评论 {{ post.commentCount ?? 0 }}
            </button>
            <button type="button" @click="onShare(post)">✉️ 转发 {{ post.shareCount ?? 0 }}</button>
          </footer>

          <section v-if="activeCommentId === post.id" class="comment-area">
            <div v-if="commentLoadingMap[post.id]" class="comment-loading">加载评论…</div>
            <div v-for="comment in commentMap[post.id] || []" :key="comment.id" class="comment-item">
              <b>{{ comment.username || comment.userId }}</b>
              <span>{{ comment.content }}</span>
              <small>{{ formatTime(comment.createdTime) }}</small>
            </div>
            <div class="comment-input">
              <input
                v-model="commentDrafts[post.id]"
                type="text"
                placeholder="写下你的评论…"
                @keyup.enter="submitComment(post)"
              />
              <button type="button" @click="submitComment(post)">发送</button>
            </div>
          </section>
        </article>

        <!-- 分页 -->
        <div v-if="!loading && !feedError && total > 0" class="feed-pager">
          <button type="button" :disabled="page === 1" @click="changePage(page - 1)">上一页</button>
          <span>第 {{ page }} / {{ totalPages }} 页，共 {{ total }} 条</span>
          <button type="button" :disabled="page >= totalPages" @click="changePage(page + 1)">下一页</button>
        </div>
      </section>

      <aside class="plaza-right">
        <section class="card">
          <h3>热门话题</h3>
          <a v-for="topic in hotTopics" :key="topic" href="javascript:void 0">#{{ topic }}</a>
        </section>

        <section class="card">
          <h3>活跃用户</h3>
          <div v-for="user in activeUsers" :key="user.name" class="active-user">
            <div class="avatar">{{ user.name[0] }}</div>
            <span>{{ user.name }}</span>
            <small>{{ user.note }}</small>
          </div>
        </section>

        <section class="card">
          <h3>平台公告</h3>
          <p class="announcement">本周开启“毕业季清仓”主题陈列，欢迎学生上架低价闲置。</p>
        </section>
      </aside>
    </main>

    <div v-if="toast" class="plaza-toast">{{ toast }}</div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import VerifiedBadge from '../components/VerifiedBadge.vue'
import { getHallProfile, getPosts, getPostTypes, createPost } from '../api/plaza'
import { toggleLike, getComments, addComment, shareProduct } from '../api/product'

const router = useRouter()

/* ---------- 状态 ---------- */
const posts = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(true)
const feedError = ref('')
const profile = ref(null)
const profileError = ref('')
const typeOptions = ref([])
const activeFilter = ref('all')
const likedMap = ref({})
const likeAnimating = ref({})
const activeCommentId = ref(null)
const commentMap = ref({})
const commentDrafts = ref({})
const commentLoadingMap = ref({})
const composerOpen = ref(false)
const submitting = ref(false)
const draftError = ref('')
const draft = reactive({ type: 'SELL', content: '', productTitle: '', productPrice: '', images: [] })
const toast = ref('')
const expanded = ref({})

let toastTimer = null

/** 后端枚举兜底；正常由 /posts/types 提供，取不到时用这一份静态文案 */
const FALLBACK_TYPES = [
  { value: 'SELL', label: '出售' },
  { value: 'SEEK', label: '求购' },
  { value: 'FREE', label: '免费送' },
  { value: 'WARN', label: '避雷' },
  { value: 'CHAT', label: '闲聊' },
]

const TYPE_ICONS = { SELL: '🔴', SEEK: '🔵', FREE: '🟢', WARN: '🟡', CHAT: '⚪' }

const filters = computed(() => [{ value: 'all', label: '全部' }, ...typeOptions.value])

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))
const needsProductInfo = computed(() => draft.type === 'SELL' || draft.type === 'FREE')
const composerPlaceholder = computed(() => {
  switch (draft.type) {
    case 'SEEK': return '想求购什么？写清版本、要求与预算更容易被找到…'
    case 'WARN': return '提醒同学们注意什么？让更多人少踩坑…'
    case 'CHAT': return '随便聊聊，抛个话题给大伙儿…'
    default: return '描述一下这件闲置的成色、入手渠道与可交易时间…'
  }
})

function typeIcon(type) { return TYPE_ICONS[type] || '⚪' }

function productStatusText(status) {
  if (status === 1) return '在售'
  if (status === 2) return '已售出'
  if (status === 0) return '已下架'
  return ''
}

function imageGridClass(count) {
  if (count === 1) return 'grid-1'
  if (count <= 4) return 'grid-2'
  return 'grid-3'
}

function handleImageUpload(event) {
  const files = event.target.files ? Array.from(event.target.files) : []
  if (!files.length) return
  if (draft.images.length + files.length > 9) {
    alert('最多只能上传 9 张图片')
    return
  }
  files.forEach(file => {
    if (!file.type.startsWith('image/')) return
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
        draft.images.push(canvas.toDataURL('image/jpeg', 0.7))
      }
      img.src = e.target.result
    }
    reader.readAsDataURL(file)
  })
  event.target.value = ''
}

function removeImage(index) {
  draft.images.splice(index, 1)
}

function showToast(message) {
  toast.value = message
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.value = '' }, 1800)
}

function formatTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

/* ---------- 数据加载 ---------- */
async function loadProfile() {
  try {
    const res = await getHallProfile()
    profile.value = res?.data || null
  } catch (err) {
    profile.value = null
    profileError.value = err?.response?.message || '名片加载失败'
  }
}

async function loadTypes() {
  try {
    const res = await getPostTypes()
    const list = res?.data
    if (Array.isArray(list) && list.length) {
      typeOptions.value = list
      return
    }
  } catch { /* 忽略，走兜底 */ }
  typeOptions.value = FALLBACK_TYPES
}

async function loadFeed() {
  try {
    loading.value = true
    feedError.value = ''
    const res = await getPosts({
      type: activeFilter.value === 'all' ? null : activeFilter.value,
      page: page.value,
      size: pageSize
    })
    const data = res?.data
    posts.value = data?.list || []
    total.value = data?.total || 0
  } catch (err) {
    console.error('获取大厅动态失败:', err)
    feedError.value = err?.response?.message || '动态加载失败，请检查网络'
    posts.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function setFilter(value) {
  activeFilter.value = value
  page.value = 1
  loadFeed()
}

function changePage(next) {
  if (next < 1 || next > totalPages.value) return
  page.value = next
  loadFeed()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

/* ---------- 发布 ---------- */
function openComposer() {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录后再发布动态')
    router.push({ path: '/login', query: { redirect: '/plaza' } })
    return
  }
  draftError.value = ''
  composerOpen.value = true
}

async function submitPost() {
  draftError.value = ''

  const content = draft.content.trim()
  if (!content && draft.images.length === 0) {
    draftError.value = '动态内容不能为空，请至少上传一张图片'
    return
  }

  const payload = {
    type: draft.type,
    content,
    tags: [],
    images: draft.images
  }

  if (needsProductInfo.value && draft.productTitle.trim()) {
    payload.productInfo = {
      title: draft.productTitle.trim(),
      price: draft.productPrice === '' ? null : Number(draft.productPrice)
    }
    if (draft.type === 'SELL' && (payload.productInfo.price === null || payload.productInfo.price <= 0)) {
      draftError.value = '出售动态必须填写大于 0 的价格'
      return
    }
  } else if (draft.type === 'SELL') {
    draftError.value = '出售动态需要填写商品标题'
    return
  }

  submitting.value = true
  try {
    const res = await createPost(payload)
    if (res?.code === 200) {
      ElMessage.success(res.message || '发布成功')
      composerOpen.value = false
      draft.type = 'SELL'
      draft.content = ''
      draft.productTitle = ''
      draft.productPrice = ''
      draft.images = []
      page.value = 1
      await Promise.all([loadFeed(), loadProfile()])
    } else {
      draftError.value = res?.message || '发布失败'
    }
  } catch (err) {
    draftError.value = err?.response?.message || err?.cause?.message || '发布失败，请重试'
  } finally {
    submitting.value = false
  }
}

/* ---------- 互动（仅商品类动态） ---------- */
async function onLike(post) {
  likeAnimating.value[post.id] = true
  setTimeout(() => { likeAnimating.value[post.id] = false }, 300)
  try {
    const res = await toggleLike(post.productId)
    post.likeCount = res.data.likeCount
    likedMap.value[post.id] = res.data.liked
  } catch {
    showToast('点赞失败')
  }
}

async function toggleComments(post) {
  activeCommentId.value = activeCommentId.value === post.id ? null : post.id
  if (activeCommentId.value === post.id && !commentMap.value[post.id]) {
    commentLoadingMap.value[post.id] = true
    try {
      const res = await getComments(post.productId, 1, 20)
      commentMap.value[post.id] = res.data
      post.commentCount = res.data.length
    } catch {
      showToast('加载评论失败')
    } finally {
      commentLoadingMap.value[post.id] = false
    }
  }
}

async function submitComment(post) {
  const content = (commentDrafts.value[post.id] || '').trim()
  if (!content) return
  try {
    const res = await addComment(post.productId, content)
    if (!commentMap.value[post.id]) commentMap.value[post.id] = []
    commentMap.value[post.id].push(res.data)
    post.commentCount += 1
    commentDrafts.value[post.id] = ''
  } catch {
    showToast('评论失败')
  }
}

async function onShare(post) {
  try {
    const res = await shareProduct(post.productId)
    post.shareCount = res.data.shareCount
    showToast('已转发/分享成功')
  } catch {
    showToast('分享失败')
  }
}

onMounted(async () => {
  await loadTypes()
  await Promise.all([loadProfile(), loadFeed()])
})
</script>

<style scoped>
.plaza { background: var(--bg); min-height: 100vh; }

.plaza-header {
  position: sticky; top: 0; z-index: 100;
  background: rgba(15, 15, 16, 0.72);
  border-bottom: 1px solid var(--border);
  backdrop-filter: blur(16px) saturate(160%);
}
.plaza-header-inner {
  position: relative;
  display: flex; align-items: center; justify-content: space-between;
  height: var(--header-h);
}
.brand { color: var(--text); text-decoration: none; font-weight: 700; }

/* 导航水平居中于整个 header（与首页间距对齐） */
.plaza-nav {
  position: absolute; left: 50%; transform: translateX(-50%);
  display: flex; gap: var(--space-1);
}
.nav-link {
  padding: 8px 12px; font-size: var(--text-base);
  border-radius: var(--radius-sm); color: var(--text-2);
  text-decoration: none; white-space: nowrap;
  transition: color var(--dur-fast) var(--ease);
}
.nav-link:hover { color: var(--text); }
.nav-link.is-active { color: var(--text); font-weight: 600; }

.plaza-shell {
  max-width: 1440px; margin: 0 auto;
  display: grid; grid-template-columns: 280px minmax(0, 1fr) 320px;
  gap: 20px; padding: 24px;
}

.plaza-user { text-align: center; }
.plaza-user h2, .card h3 { margin: 10px 0 4px; }
.plaza-user h2 { display: inline-flex; align-items: center; gap: 6px; }
.plaza-user p, .feed-meta span, .announcement, .active-user small { color: var(--text-2); }
.plaza-user .user-hint { font-size: var(--text-xs); margin: var(--space-2) 0 0; }

.user-stats {
  display: flex; justify-content: space-around;
  gap: var(--space-2); margin-top: var(--space-4);
  padding-top: var(--space-4); border-top: 1px solid var(--border);
}
.user-stats b { display: block; font-size: var(--text-lg); color: var(--text); }
.user-stats span { font-size: var(--text-xs); color: var(--text-2); }

.avatar {
  width: 36px; height: 36px; border-radius: 50%;
  display: inline-flex; align-items: center; justify-content: center;
  background: var(--surface-3); color: var(--text-2);
  overflow: hidden; flex-shrink: 0;
}
.avatar img { width: 100%; height: 100%; object-fit: cover; }
.avatar-lg { width: 64px; height: 64px; margin: 0 auto; font-size: var(--text-xl); }

.plaza-post-btn, .feed-composer button, .feed-actions button, .plaza-filter button, .expand-btn {
  border: 1px solid var(--border-strong); background: var(--surface);
  color: var(--text); padding: 8px 14px; border-radius: var(--radius);
  font-size: var(--text-sm); cursor: pointer;
  transition: all 0.3s cubic-bezier(.25,.8,.25,1);
}
.plaza-post-btn {
  width: 100%; padding: var(--space-3);
  background: var(--accent); color: #fff; border: none; font-weight: var(--weight-medium);
}
.plaza-post-btn:hover { background: var(--accent-hover); }
.plaza-filter button.active { background: var(--accent-soft); border-color: var(--accent); color: var(--accent); }

.plaza-feed { display: grid; gap: 16px; }
.feed-composer { padding: 16px; }
.feed-composer > button { width: 100%; text-align: left; color: var(--text-3); background: var(--surface-2); border-style: dashed; }

.composer-types { display: flex; flex-wrap: wrap; gap: var(--space-2); margin-bottom: var(--space-3); }
.composer-types button { padding: 4px 12px; font-size: var(--text-xs); }
.composer-types button.active { background: var(--accent-soft); border-color: var(--accent); color: var(--accent); }

.composer-textarea {
  width: 100%; padding: var(--space-3); font-size: var(--text-sm); font-family: inherit;
  border: 1px solid var(--border-strong); border-radius: var(--radius);
  background: var(--surface); color: var(--text); resize: vertical;
}
.composer-textarea:focus { outline: none; border-color: var(--accent); box-shadow: 0 0 0 3px var(--accent-soft); }

.composer-images { display: flex; flex-wrap: wrap; gap: var(--space-3); margin-top: var(--space-3); }
.uploaded-image { position: relative; width: 96px; height: 96px; border-radius: var(--radius); overflow: hidden; border: 1px solid var(--border); }
.uploaded-image img { width: 100%; height: 100%; object-fit: cover; }
.remove-image { position: absolute; top: 4px; right: 4px; width: 22px; height: 22px; display: inline-flex; align-items: center; justify-content: center; background: rgba(0,0,0,0.55); border: none; border-radius: var(--radius-full); color: #fff; cursor: pointer; }
.upload-button { width: 96px; height: 96px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px; border: 1px dashed var(--border-strong); border-radius: var(--radius); cursor: pointer; color: var(--text-3); transition: border-color var(--dur-fast) var(--ease), color var(--dur-fast) var(--ease), background var(--dur-fast) var(--ease); }
.upload-button svg { width: 24px; height: 24px; }
.upload-button span { font-size: var(--text-xs); }
.upload-button:hover { border-color: var(--accent); color: var(--accent); background: var(--accent-soft); }

.composer-product {
  display: flex; gap: var(--space-3); margin-top: var(--space-3); flex-wrap: wrap;
}
.cp-field { flex: 1; min-width: 200px; display: flex; flex-direction: column; gap: var(--space-1); }
.cp-field--price { flex: 0 0 160px; }
.cp-field span { font-size: var(--text-xs); color: var(--text-2); }
.cp-field em { color: var(--danger); font-style: normal; }
.cp-field input {
  padding: 8px 12px; font-size: var(--text-sm);
  border: 1px solid var(--border-strong); border-radius: var(--radius);
  background: var(--surface); color: var(--text);
}
.cp-field input:focus { outline: none; border-color: var(--accent); box-shadow: 0 0 0 3px var(--accent-soft); }

.composer-error { margin: var(--space-2) 0 0; font-size: var(--text-xs); color: var(--danger); }
.composer-actions { display: flex; justify-content: flex-end; gap: var(--space-2); margin-top: var(--space-3); }
.cp-cancel, .cp-submit { padding: 6px 16px; font-size: var(--text-sm); }
.cp-submit { background: var(--accent); color: #fff; border-color: var(--accent); }
.cp-submit:hover:not(:disabled) { background: var(--accent-hover); }
.cp-submit:disabled { opacity: 0.6; cursor: not-allowed; }

.feed-card { padding: 16px; transition: transform .3s cubic-bezier(.25,.8,.25,1), box-shadow .3s ease; }
.feed-card:hover { transform: translateY(-8px); box-shadow: 0 16px 40px rgba(0,0,0,.28); }
.feed-head { display: flex; align-items: center; gap: 12px; }
.feed-meta { display: grid; }
.feed-meta b { font-size: var(--text-sm); color: var(--text); }
.feed-meta span { font-size: var(--text-xs); }

.badge { margin-left: auto; font-size: var(--text-xs); padding: 3px 10px; border-radius: var(--radius-full); background: var(--surface-3); color: var(--text-2); font-style: normal; }

.feed-content {
  margin: var(--space-3) 0 0; color: var(--text);
  line-height: 1.7; display: -webkit-box; -webkit-line-clamp: 3;
  -webkit-box-orient: vertical; overflow: hidden;
}
.feed-content.expanded { display: block; }
.expand-btn { margin-top: var(--space-2); font-size: var(--text-xs); padding: 2px 0; border: none; background: none; color: var(--accent); }

.tags { display: flex; flex-wrap: wrap; gap: var(--space-2); margin-top: var(--space-3); }
.tags span { font-size: var(--text-xs); color: var(--accent); }

.feed-images { display: grid; gap: 8px; margin: 12px 0; }
.image-item { position: relative; overflow: hidden; border-radius: var(--radius); background: var(--surface-3); }
.image-item img { width: 100%; height: 100%; object-fit: cover; display: block; }
.image-item span {
  position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;
  background: rgba(0,0,0,.45); color: #fff; font-size: var(--text-sm);
  opacity: 0; transition: opacity var(--dur) var(--ease);
}
.image-item:hover span { opacity: 1; }
.feed-images.grid-1 .image-item { height: 320px; }
.feed-images.grid-2 { grid-template-columns: repeat(2, 1fr); }
.feed-images.grid-2 .image-item { aspect-ratio: 1 / 1; }
.feed-images.grid-3 { grid-template-columns: repeat(3, 1fr); }
.feed-images.grid-3 .image-item { aspect-ratio: 1 / 1; }

.feed-product {
  display: flex; align-items: center; gap: var(--space-3);
  margin-top: var(--space-3); padding: var(--space-3);
  background: var(--surface-2); border: 1px solid var(--border);
  border-radius: var(--radius); text-decoration: none;
}
.feed-product:hover { border-color: var(--accent); }
.fp-title { flex: 1; font-size: var(--text-sm); color: var(--text); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.fp-price { font-size: var(--text-md); font-weight: var(--weight-semibold); color: var(--accent); }
.fp-status { font-size: var(--text-xs); color: var(--text-3); }

.price-row { margin-top: var(--space-3); font-size: var(--text-sm); color: var(--text-2); }

.feed-actions { display: flex; gap: var(--space-4); margin-top: var(--space-4); }
.feed-actions button { background: transparent; color: var(--text-2); padding: 0; border: none; font-size: var(--text-sm); }
.feed-actions button:hover { color: var(--text); }

.comment-area { margin-top: var(--space-4); padding-top: var(--space-3); border-top: 1px solid var(--border); }
.comment-loading { font-size: var(--text-xs); color: var(--text-3); }
.comment-item { display: flex; gap: var(--space-2); padding: 6px 0; font-size: var(--text-sm); }
.comment-item small { color: var(--text-3); font-size: var(--text-xs); }
.comment-input { display: flex; gap: var(--space-2); margin-top: var(--space-2); }
.comment-input input {
  flex: 1; padding: 6px 12px; font-size: var(--text-sm);
  border: 1px solid var(--border-strong); border-radius: var(--radius);
  background: var(--surface); color: var(--text);
}
.comment-input button { font-size: var(--text-xs); padding: 6px 14px; }

.feed-state {
  display: flex; flex-direction: column; align-items: center; gap: var(--space-3);
  padding: var(--space-12) var(--space-4); text-align: center;
}
.feed-state p { margin: 0; font-size: var(--text-sm); color: var(--text-2); }
.feed-state button { font-size: var(--text-sm); padding: 6px 16px; }

.feed-pager {
  display: flex; align-items: center; justify-content: center; gap: var(--space-4);
  padding: var(--space-3); font-size: var(--text-sm); color: var(--text-2);
}
.feed-pager button {
  padding: 4px 14px; font-size: var(--text-xs);
  border: 1px solid var(--border-strong); border-radius: var(--radius-sm);
  background: var(--surface); color: var(--text); cursor: pointer;
}
.feed-pager button:disabled { opacity: 0.45; cursor: not-allowed; }

.feed-skeleton { padding: var(--space-4); display: flex; flex-direction: column; gap: var(--space-3); }
.sk { background: var(--surface-3); border-radius: var(--radius-sm); animation: sk-pulse 1.4s ease-in-out infinite; }
.sk-row { height: 36px; }
.sk-line { height: 14px; }
.sk-line--short { width: 40%; }
@keyframes sk-pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }

.plaza-toast {
  position: fixed; bottom: 32px; left: 50%; transform: translateX(-50%);
  z-index: 1000; padding: 10px 20px; border-radius: var(--radius);
  background: rgba(20,20,18,.9); color: #fff; font-size: var(--text-sm);
}

@media (max-width: 1100px) {
  .plaza-shell { grid-template-columns: 240px minmax(0, 1fr); }
  .plaza-right { display: none; }
}
@media (max-width: 760px) {
  .plaza-shell { grid-template-columns: 1fr; padding: 16px; }
  .plaza-left, .plaza-right { position: static; }
}
@media (max-width: 768px) {
  .plaza-nav {
    position: static; transform: none;
    flex: 1; justify-content: flex-end;
    overflow-x: auto; flex-wrap: nowrap; scrollbar-width: none;
  }
  .plaza-nav::-webkit-scrollbar { display: none; }
}
@media (prefers-reduced-motion: reduce) {
  .sk { animation: none; }
  .feed-card:hover { transform: none; }
}
</style>
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
        <section class="card plaza-user">
          <div class="avatar avatar-lg">我</div>
          <h2>同学昵称</h2>
          <p>计算机学院 · 2023级</p>
          <div class="user-stats">
            <span><b>12</b>动态</span>
            <span><b>8</b>在售</span>
            <span><b>3</b>求购</span>
          </div>
        </section>

        <button class="plaza-post-btn" type="button">+ 发布动态/商品</button>

        <section class="card plaza-filter">
          <h3>分类筛选</h3>
          <button v-for="item in filters" :key="item.value" type="button" :class="{ active: activeFilter === item.value }" @click="activeFilter = item.value">
            {{ item.label }}
          </button>
        </section>
      </aside>

      <section class="plaza-feed">
        <div class="card feed-composer">
          <button type="button">分享动态、发起求购，或把閒置挂上大厅…</button>
        </div>

        <article v-for="post in filteredPosts" :key="post.id" class="card feed-card">
          <header class="feed-head">
            <div class="avatar">{{ post.user[0] }}</div>
            <div class="feed-meta">
              <b>{{ post.user }}</b>
              <span>{{ post.time }} · {{ post.location }}</span>
            </div>
            <em class="badge" :data-type="post.type">{{ post.typeIcon }} {{ post.typeLabel }}</em>
          </header>

          <p class="feed-content" :class="{ expanded: expanded[post.id] }">{{ post.content }}</p>
          <button v-if="post.content.length > 80" class="expand-btn" type="button" @click="expanded[post.id] = !expanded[post.id]">
            {{ expanded[post.id] ? '收起' : '展开全文' }}
          </button>

          <div v-if="post.tags.length" class="tags">
            <span v-for="tag in post.tags" :key="tag">***REMOVED***{{ tag }}</span>
          </div>

          <div v-if="post.images.length" class="feed-images" :class="imageGridClass(post.images.length)">
            <div v-for="img in post.images.slice(0, 9)" :key="img" class="image-item">
              <img :src="img" alt="动态图片" />
              <span>查看详情</span>
            </div>
          </div>

          <div v-if="post.type === 'sell'" class="price-row">¥{{ post.price }}</div>
          <div v-else-if="post.type === 'buy'" class="price-row">求带价</div>

          <footer class="feed-actions">
            <button type="button" :class="{ 'like-pop': likeAnimating[post.id] }" @click="onLike(post)">
              {{ likedMap[post.id] ? '❤️' : '🤍' }} 点赞 {{ post.likes }}
            </button>
            <button type="button" @click="toggleComments(post)">💬 评论 {{ post.comments }}</button>
            <button type="button" @click="onShare(post)">✉️ 转发 {{ post.shares || 0 }}</button>
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
      </section>

      <aside class="plaza-right">
        <section class="card">
          <h3>热门话题</h3>
          <a v-for="topic in hotTopics" :key="topic" href="javascript:void 0">***REMOVED***{{ topic }}</a>
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

    <div v-if="toast" class="toast">{{ toast }}</div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { toggleLike, getComments, addComment, shareProduct } from '../api/product'

const filters = [
  { value: 'all', label: '全部' },
  { value: 'sell', label: '出售' },
  { value: 'buy', label: '求购' },
  { value: 'free', label: '免费送' },
  { value: 'warning', label: '避雷' },
  { value: 'chat', label: '闲聊' },
]
const activeFilter = ref('all')
const likedMap = ref({})
const likeAnimating = ref({})
const activeCommentId = ref(null)
const commentMap = ref({})
const commentDrafts = ref({})
const commentLoadingMap = ref({})
const toast = ref('')
let toastTimer = null

const posts = ref([
  {
    id: 1, userId: 12, user: '宿舍王姐', avatar: '', time: '刚刚', location: '东校区',
    type: 'sell', typeLabel: '出售', typeIcon: '🔴',
    content: '护眼台灯和高数教材一起打包，宿舍自提。这个台灯陪伴了我两年，亮度可以调，晚上写作业很舒服；高数教材笔记比较完整，适合下学期预习复习。价格可以小刀，但打包送走优先。',
    images: ['/sample/book.svg', '/sample/hero.svg'],
    price: 20, status: 'selling', tags: ['教材', '宿舍神器'], likes: 6, comments: 2,
  },
  {
    id: 2, userId: 18, user: '数学系的P', avatar: '', time: '10 分钟前', location: '西校区',
    type: 'buy', typeLabel: '求购', typeIcon: '🔵',
    content: '求购 2024 年线性代数真题，任意版本都可。最好有解析，考研复试用。',
    images: ['/sample/book.svg'],
    price: null, status: 'wanted', tags: ['教材', '考研'], likes: 3, comments: 4,
  },
  {
    id: 3, userId: 24, user: '环保先锋', avatar: '', time: '1 小时前', location: '南区',
    type: 'free', typeLabel: '免费送', typeIcon: '🟢',
    content: '宿舍盆栽搬家剩几盆绿萝，带盆自提。',
    images: ['/sample/hero.svg', '/sample/bike.svg', '/sample/phone.svg', '/sample/book.svg'],
    price: null, status: 'selling', tags: ['闲置', '自提'], likes: 9, comments: 1,
  },
  {
    id: 4, userId: 31, user: '避坑小分队', avatar: '', time: '昨天', location: '北区',
    type: 'warning', typeLabel: '避雷', typeIcon: '🟡',
    content: '提醒：这周有人虚标二手平板，请线下验货。特别是平板和耳机，最好见面验机，不要只看截图。',
    images: ['/sample/phone.svg', '/sample/hero.svg', '/sample/bike.svg', '/sample/book.svg', '/sample/phone.svg'],
    price: null, status: 'resolved', tags: ['校园避雷榜'], likes: 18, comments: 7,
  },
  {
    id: 5, userId: 37, user: '路过的栗子', avatar: '', time: '前天', location: '中区',
    type: 'chat', typeLabel: '闲聊', typeIcon: '🟢',
    content: '毕业季谁来组个清仓群？宿舍好多还能用的东西，直接扔有点可惜。顺便问下大家平时会把闲置挂到这里吗？',
    images: [],
    price: null, status: 'selling', tags: ['毕业季清仓'], likes: 5, comments: 8,
  },
])

const hotTopics = ['毕业季清仓', '宿舍搬家', '二手教材互助', '校园避雷榜']
const activeUsers = [
  { name: '阿明同学', note: '在售 9 件' },
  { name: '小林', note: '本月 12 单' },
  { name: '毕业倒计时', note: '全场低价' },
]

const filteredPosts = computed(() => {
  if (activeFilter.value === 'all') return posts.value
  return posts.value.filter((post) => post.type === activeFilter.value)
})

const expanded = ref({})
function imageGridClass(count) {
  if (count === 1) return 'grid-1'
  if (count <= 4) return 'grid-2'
  return 'grid-3'
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

async function onLike(post) {
  likeAnimating.value[post.id] = true
  setTimeout(() => { likeAnimating.value[post.id] = false }, 300)
  try {
    const res = await toggleLike(post.id)
    post.likes = res.data.likeCount
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
      const res = await getComments(post.id, 1, 20)
      commentMap.value[post.id] = res.data
      post.comments = res.data.length
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
    const res = await addComment(post.id, content)
    if (!commentMap.value[post.id]) commentMap.value[post.id] = []
    commentMap.value[post.id].push(res.data)
    post.comments += 1
    commentDrafts.value[post.id] = ''
  } catch {
    showToast('评论失败')
  }
}

async function onShare(post) {
  try {
    const res = await shareProduct(post.id)
    post.shares = res.data.shareCount
    showToast('已转发/分享成功')
  } catch {
    showToast('分享失败')
  }
}
</script>

<style scoped>
.plaza {
  --surface: rgba(18, 18, 20, 0.78);
  --surface-2: rgba(255, 255, 255, 0.06);
  --border: rgba(255, 255, 255, 0.09);
  --text: ***REMOVED***f5f5f4;
  --text-2: ***REMOVED***a8a29e;
  min-height: 100vh;
  color: var(--text);
}

.plaza-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(15, 15, 16, 0.72);
  border-bottom: 1px solid var(--border);
  backdrop-filter: blur(16px) saturate(160%);
}

.plaza-header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
}

.brand { color: var(--text); text-decoration: none; font-weight: 700; }
.plaza-nav { display: flex; gap: 18px; }
.nav-link { color: var(--text-2); text-decoration: none; }
.nav-link.is-active { color: var(--text); font-weight: 600; }

.plaza-shell {
  max-width: 1440px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr) 320px;
  gap: 20px;
  padding: 24px;
}

.plaza-left,
.plaza-right {
  position: sticky;
  top: 84px;
  align-self: start;
  display: grid;
  gap: 16px;
}

.plaza-feed { display: grid; gap: 16px; }

.plaza .card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 14px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.18);
  backdrop-filter: blur(16px) saturate(160%);
}

.plaza-user { text-align: center; }
.plaza-user h2, .card h3 { margin: 10px 0 4px; }
.plaza-user p, .feed-meta span, .announcement, .active-user small { color: var(--text-2); }
.user-stats { display: flex; justify-content: center; gap: 16px; margin-top: 14px; }
.user-stats span { display: grid; color: var(--text-2); font-size: 12px; }
.user-stats b { color: var(--text); font-size: 18px; }

.avatar {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: 999px;
  background: linear-gradient(135deg, ***REMOVED***2dd4bf, ***REMOVED***64748b);
  color: ***REMOVED***111;
  font-weight: 700;
}
.avatar-lg { width: 72px; height: 72px; margin: 0 auto; font-size: 24px; }

.plaza-post-btn, .feed-composer button, .feed-actions button, .plaza-filter button, .expand-btn {
  border: 0;
  border-radius: 999px;
  cursor: pointer;
  transition: transform .18s ease, background-color .2s ease, color .2s ease, box-shadow .2s ease;
}
.feed-actions button:hover,
.plaza-filter button:hover,
.expand-btn:hover {
  transform: scale(1.05);
  background-color: rgba(255, 255, 255, 0.12);
}
.plaza-post-btn:hover,
.feed-composer button:hover {
  transform: scale(1.05);
  filter: brightness(0.95);
}
.feed-actions button:active,
.plaza-filter button:active,
.plaza-post-btn:active,
.feed-composer button:active,
.expand-btn:active {
  transform: scale(0.95);
}
.plaza-post-btn, .feed-composer button {
  width: 100%;
  padding: 12px 16px;
  background: ***REMOVED***f5f5f4;
  color: ***REMOVED***111;
  font-weight: 700;
}

.plaza-filter { display: grid; gap: 8px; }
.plaza-filter button {
  padding: 10px 12px;
  text-align: left;
  background: var(--surface-2);
  color: var(--text);
}
.plaza-filter button.active { background: ***REMOVED***f5f5f4; color: ***REMOVED***111; }

.feed-composer { padding: 16px; }
.feed-card { padding: 16px; transition: transform .3s cubic-bezier(.25,.8,.25,1), box-shadow .3s ease; }
.feed-card:hover { transform: translateY(-8px); box-shadow: 0 16px 40px rgba(0, 0, 0, 0.28); }
.feed-head { display: flex; align-items: center; gap: 12px; }
.feed-meta { display: grid; }
.badge {
  margin-left: auto;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255,255,255,.08);
  color: var(--text);
  font-size: 12px;
  font-style: normal;
}
.feed-content {
  display: -webkit-box;
  -webkit-line-clamp: 4;
  line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin: 14px 0 0;
  line-height: 1.7;
}
.feed-content.expanded { display: block; }
.expand-btn {
  border: 0;
  background: transparent;
  color: ***REMOVED***7dd3fc;
  cursor: pointer;
  padding: 4px 0;
}
.tags { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 12px; }
.tags span {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255,255,255,.08);
  color: var(--text);
  font-size: 12px;
}
.feed-images { display: grid; gap: 8px; margin: 12px 0; }
.image-item { position: relative; overflow: hidden; border-radius: 12px; }
.image-item img { width: 100%; height: 100%; object-fit: cover; border-radius: 12px; display: block; transition: transform .4s ease; }
.image-item span { position: absolute; inset: 0; display: grid; place-items: center; background: rgba(0, 0, 0, 0.38); color: ***REMOVED***fff; opacity: 0; transition: opacity .25s ease; }
.image-item:hover img { transform: scale(1.08); }
.image-item:hover span { opacity: 1; }
.feed-images.grid-1 .image-item { height: 320px; }
.feed-images.grid-2 { grid-template-columns: repeat(2, 1fr); }
.feed-images.grid-2 .image-item { aspect-ratio: 1 / 1; }
.feed-images.grid-3 { grid-template-columns: repeat(3, 1fr); }
.feed-images.grid-3 .image-item { aspect-ratio: 1 / 1; }
.price-row { color: ***REMOVED***fca5a5; font-weight: 700; margin: 12px 0; }
.feed-actions {
  display: flex;
  gap: 16px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid var(--border);
  color: var(--text-2);
}
.feed-actions button { background: transparent; color: var(--text-2); padding: 0; }
.comment-area { margin-top: 12px; display: grid; gap: 10px; }
.comment-loading { color: var(--text-2); font-size: var(--text-sm); }
.comment-item { display: grid; gap: 2px; padding: 8px 10px; border-radius: 10px; background: rgba(255,255,255,.05); }
.comment-item b { font-size: 13px; }
.comment-item small { color: var(--text-2); font-size: 12px; }
.comment-input { display: flex; gap: 8px; }
.comment-input input { flex: 1; padding: 8px 12px; border-radius: 999px; border: 1px solid var(--border); background: rgba(255,255,255,.08); color: var(--text); outline: none; }
.comment-input button { border: 0; border-radius: 999px; padding: 8px 14px; background: ***REMOVED***f5f5f4; color: ***REMOVED***111; cursor: pointer; }
.toast { position: fixed; left: 50%; bottom: 24px; transform: translateX(-50%); padding: 10px 18px; border-radius: 999px; background: rgba(0,0,0,.75); color: ***REMOVED***fff; z-index: 10000; }
.like-pop { animation: pop .3s ease; }
@keyframes pop { 50% { transform: scale(1.25); } }
.card h3 { margin-top: 0; }
.card a { display: block; color: var(--text); text-decoration: none; margin: 10px 0; }
.active-user { display: grid; grid-template-columns: 40px 1fr; gap: 10px; align-items: center; margin-top: 12px; }
.active-user small { grid-column: 2; }

@media (max-width: 1100px) {
  .plaza-shell { grid-template-columns: 240px minmax(0, 1fr); }
  .plaza-right { display: none; }
}

@media (max-width: 760px) {
  .plaza-shell { grid-template-columns: 1fr; padding: 16px; }
  .plaza-left, .plaza-right { position: static; }
}
</style>

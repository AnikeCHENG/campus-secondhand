<template>
  <!--
    data-plaza-dark 是暗色玻璃的隔离舱：
    它只重定义 CSS 自定义属性，靠变量沿 DOM 树继承流入所有子组件
    （含 PostCard），因此组件内部 CSS 一行都不用改，Home/Products 等
    其他页面也完全不受影响——全局 theme.css 零字符改动。
  -->
  <div class="page plaza" data-plaza-dark>
    <header class="plaza-header" :class="{ 'is-scrolled': headerScrolled }">
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
        <section class="glass plaza-user">
          <div class="avatar avatar-lg avatar-ring">
            <img v-if="profile?.avatar" :src="profile.avatar" :alt="`${profile.nickname} 的头像`" />
            <template v-else>{{ (profile?.nickname || '?').charAt(0) }}</template>
          </div>
          <h2>
            {{ profile?.nickname }}
            <VerifiedBadge v-if="profile?.verified" :verified="true" size="sm" />
          </h2>
          <p v-if="profile?.grade">{{ profile.grade }}</p>
          <!--
            统计数字用 count-up 滚动到真实值。
            首次数据到达后 start 一次即可：useCountUp 内部会在 target 变化时
            从当前显示值续走，不需要每次都重启动画。
          -->
          <div v-if="profile?.stats" class="user-stats">
            <span><b>{{ postsCount }}</b>动态</span>
            <span><b>{{ sellingCount }}</b>在售</span>
            <span><b>{{ seekingCount }}</b>求购</span>
          </div>
          <p v-else-if="profileError" class="user-hint">{{ profileError }}</p>
        </section>

        <button class="plaza-post-btn" type="button" @click="openComposer">
          <span>+ 发布动态/商品</span>
        </button>

        <section class="glass plaza-filter">
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
        <div v-if="composerOpen" class="glass feed-composer">
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
        <div v-else class="glass feed-composer">
          <button type="button" @click="openComposer">分享动态、发起求购，或把闲置挂上大厅…</button>
        </div>

        <!-- 加载失败 -->
        <div v-if="feedError" class="glass feed-state">
          <p>{{ feedError }}</p>
          <button type="button" @click="loadFeed">重试</button>
        </div>

        <!-- 骨架屏 -->
        <div v-else-if="loading" class="glass feed-skeleton">
          <div class="sk sk-row"></div>
          <div class="sk sk-line"></div>
          <div class="sk sk-line sk-line--short"></div>
        </div>

        <!-- 空状态 -->
        <div v-else-if="posts.length === 0" class="glass feed-state">
          <p>{{ activeFilter === 'all' ? '大厅还没有动态，来发第一条吧' : '该分类下暂无动态' }}</p>
          <button type="button" @click="activeFilter = 'all'; loadFeed()">查看全部动态</button>
        </div>

        <!--
          卡片拆成 PostCard：父组件持有全部 Map，v-for 时查出派生状态传入。
          v-reveal 做滚动入场 stagger：delay 按索引递增 80ms，
          超过 5 张后封顶，否则长列表末尾要等近 1 秒才出现。
        -->
        <!--
          class="glass" 走 Vue 3 的 class fallthrough：
          PostCard 是单根节点且未设 inheritAttrs:false，父级传入的 class
          会自动合并到其根元素。PostCard 内部 CSS 零改动，别处复用时
          各自传自己的表面类即可。
        -->
        <PostCard
          v-for="(post, i) in posts"
          :key="post.id"
          class="glass"
          v-reveal="{ delay: Math.min(i, 5) * 80 }"
          :post="post"
          :is-expanded="!!expanded[post.id]"
          :is-liked="!!likedMap[post.id]"
          :is-like-animating="!!likeAnimating[post.id]"
          :is-comments-visible="activeCommentId === post.id"
          :comments="commentMap[post.id] || []"
          :is-comment-loading="!!commentLoadingMap[post.id]"
          @toggle-expand="expanded[post.id] = !expanded[post.id]"
          @like="onLike(post)"
          @toggle-comments="toggleComments(post)"
          @submit-comment="submitComment(post)"
          @share="onShare(post)"
        />

        <!-- 分页 -->
        <div v-if="!loading && !feedError && total > 0" class="feed-pager">
          <button type="button" :disabled="page === 1" @click="changePage(page - 1)">上一页</button>
          <span>第 {{ page }} / {{ totalPages }} 页，共 {{ total }} 条</span>
          <button type="button" :disabled="page >= totalPages" @click="changePage(page + 1)">下一页</button>
        </div>
      </section>

      <aside class="plaza-right">
        <section class="glass">
          <h3>热门话题</h3>
          <div v-if="hotTopicsLoading" class="side-skeleton">
            <span class="sk sk-line"></span>
            <span class="sk sk-line"></span>
            <span class="sk sk-line sk-line--short"></span>
          </div>
          <p v-else-if="hotTopicsError" class="side-hint">
            {{ hotTopicsError }}
            <button type="button" class="side-retry" @click="loadSidebar">重试</button>
          </p>
          <p v-else-if="hotTopics.length === 0" class="side-hint">还没有人用过话题标签</p>
          <!--
            Top3 用渐变大字序号（排名即视觉权重），其余用小号中性序号。
            序号单独成元素而非 ::before 伪元素：伪元素里的文字读屏软件不读。
          -->
          <ol v-else class="topic-list">
            <li
              v-for="(topic, i) in hotTopics"
              :key="topic.tag"
              class="topic-item"
              :class="{ 'is-top': i < 3 }"
            >
              <span class="topic-rank" aria-hidden="true">{{ i + 1 }}</span>
              <span class="topic-name">#{{ topic.tag }}</span>
              <em class="topic-count">{{ topic.count }}</em>
            </li>
          </ol>
        </section>

        <section class="glass">
          <h3>活跃用户</h3>
          <div v-if="activeUsersLoading" class="side-skeleton">
            <span class="sk sk-line"></span>
            <span class="sk sk-line"></span>
            <span class="sk sk-line sk-line--short"></span>
          </div>
          <p v-else-if="activeUsersError" class="side-hint">
            {{ activeUsersError }}
            <button type="button" class="side-retry" @click="loadSidebar">重试</button>
          </p>
          <p v-else-if="activeUsers.length === 0" class="side-hint">暂无活跃用户</p>
          <ul v-else class="active-user-list">
            <li v-for="user in activeUsers" :key="user.userId" class="active-user">
              <span class="avatar avatar-ring">
                <img v-if="user.avatar" :src="user.avatar" :alt="`${user.username} 的头像`" />
                <template v-else>{{ (user.username || '?').charAt(0) }}</template>
              </span>
              <span class="au-body">
                <b>{{ user.username }}</b>
                <small>{{ user.subtitle }}</small>
              </span>
            </li>
          </ul>
        </section>

        <section class="glass">
          <h3>平台公告</h3>
          <p class="announcement">本周开启“毕业季清仓”主题陈列，欢迎学生上架低价闲置。</p>
        </section>
      </aside>
    </main>

    <div v-if="toast" class="plaza-toast">{{ toast }}</div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PostCard from '../components/PostCard.vue'
import VerifiedBadge from '../components/VerifiedBadge.vue'
import { useCountUp } from '../composables/useCountUp'
import { useReducedMotion } from '../composables/useReducedMotion'
import { getHallProfile, getPosts, getPostTypes, createPost, getHotTopics, getActiveUsers } from '../api/plaza'
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
const commentLoadingMap = ref({})
const composerOpen = ref(false)
const submitting = ref(false)
const draftError = ref('')
const draft = reactive({ type: 'SELL', content: '', productTitle: '', productPrice: '', images: [] })
const toast = ref('')
const expanded = ref({})

let toastTimer = null

/* ---------- 右栏：热门话题 / 活跃用户 ----------
 *
 * 这两个接口由后端并行实现，未就绪时按 404 处理。各自持有独立的
 * loading/error 三态：右栏挂了不影响主 feed，也不向上冒泡。
 */
const hotTopics = ref([])
const hotTopicsLoading = ref(true)
const hotTopicsError = ref('')
const activeUsers = ref([])
const activeUsersLoading = ref(true)
const activeUsersError = ref('')

/** 后端枚举兜底；正常由 /posts/types 提供，取不到时用这一份静态文案 */
const FALLBACK_TYPES = [
  { value: 'SELL', label: '出售' },
  { value: 'SEEK', label: '求购' },
  { value: 'FREE', label: '免费送' },
  { value: 'WARN', label: '避雷' },
  { value: 'CHAT', label: '闲聊' },
]

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

/** 上传图片在浏览器端压到 800px / JPEG 0.7 再入库，避免 base64 把 posts.images 撑爆 */
function handleImageUpload(event) {
  const files = Array.from(event.target.files || [])
  if (!files.length) return

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


/* ---------- 名片统计数字：count-up 滚动到真实值 ----------
 * 不用 computed 直接渲染 profile.stats：那样数字是"啪"地出现，
 * 用户注意不到数字变了。useCountUp 内部监听 target 变化续走，
 * 这里只需在数据到达后启动一次。
 */
const reducedMotion = useReducedMotion()
const postsStat = computed(() => Number(profile.value?.stats?.posts) || 0)
const sellingStat = computed(() => Number(profile.value?.stats?.selling) || 0)
const seekingStat = computed(() => Number(profile.value?.stats?.seeking) || 0)

const { display: postsCount, start: startPostsCount } = useCountUp(postsStat, { reduced: reducedMotion, duration: 800 })
const { display: sellingCount, start: startSellingCount } = useCountUp(sellingStat, { reduced: reducedMotion, duration: 800 })
const { display: seekingCount, start: startSeekingCount } = useCountUp(seekingStat, { reduced: reducedMotion, duration: 800 })

/* ---------- 数据加载 ---------- */
async function loadProfile() {
  try {
    const res = await getHallProfile()
    profile.value = res?.data || null
    // 名片拿到后才启动数字滚动，且三者错开 90ms 形成层次
    if (profile.value?.stats) {
      startPostsCount()
      setTimeout(startSellingCount, 90)
      setTimeout(startSeekingCount, 180)
    }
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

/**
 * 拉右栏两个公开接口。
 *
 * <p>用 allSettled 而非 all：这两个接口目前可能还没上线，若用 all 会在
 * 任何一个失败时把另一个的成功结果一起丢掉，且 rejection 未处理直接冒泡。
 * allSettled 让两者互不影响，各自进自己的 error 分支。</p>
 */
async function loadSidebar() {
  const [topicsRes, usersRes] = await Promise.allSettled([
    getHotTopics(),
    getActiveUsers()
  ])

  if (topicsRes.status === 'fulfilled') {
    hotTopics.value = topicsRes.value?.data?.topics || []
    hotTopicsError.value = ''
  } else {
    hotTopics.value = []
    hotTopicsError.value = '暂时无法加载热门话题'
  }
  hotTopicsLoading.value = false

  if (usersRes.status === 'fulfilled') {
    activeUsers.value = usersRes.value?.data?.users || []
    activeUsersError.value = ''
  } else {
    activeUsers.value = []
    activeUsersError.value = '暂时无法加载活跃用户'
  }
  activeUsersLoading.value = false
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

/**
 * 提交评论。draft 由卡片内部持有并随事件回传。
 *
 * <p>成功后不主动清空：卡片内的草稿随评论列表渲染完成而重置，
 * 父组件这里只负责把评论塞进 Map。</p>
 */
async function submitComment(post, draft) {
  const content = (draft || '').trim()
  if (!content) return
  try {
    const res = await addComment(post.productId, content)
    if (!commentMap.value[post.id]) commentMap.value[post.id] = []
    commentMap.value[post.id].push(res.data)
    post.commentCount += 1
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

/**
 * 导航滚动状态：滚过 8px 给 header 加 is-scrolled。
 *
 * <p>用 scroll 监听而非 IntersectionObserver：header 是 sticky 元素，
 * 它的位置由布局决定而非进入视口，IO 在这里没有语义。8px 阈值避免
 * 页面刚开始轻微抖动就切换背景。</p>
 */
const headerScrolled = ref(false)
function onScroll() {
  headerScrolled.value = window.scrollY > 8
}

onMounted(async () => {
  onScroll()
  window.addEventListener('scroll', onScroll, { passive: true })
  await loadTypes()
  // 右栏不 await：它失败也要先把主 feed 渲染出来
  loadSidebar()
  await Promise.all([loadProfile(), loadFeed()])
})

// 必须解绑：Plaza 挂了缓存的返回/前进会重新挂载，残留监听会累积
onBeforeUnmount(() => window.removeEventListener('scroll', onScroll))
</script>

<style scoped>
/* ============================================================================
 * 暗色玻璃隔离舱
 *
 * 只重定义自定义属性，不写任何组件样式规则。原因：Vue 的 scoped 会给组件
 * 根元素打上各自组件的 data-v 属性，写在 Plaza 的 <style> 里的
 * [data-plaza-dark] .feed-card 匹配不到 PostCard 内部的元素。
 * 而自定义属性沿 DOM 树继承、不受 scoped 影响，能自然流到所有子组件。
 *
 * 这是「不改一行组件内部 CSS 就能整体换肤」的唯一可靠做法。
 * ==========================================================================*/
[data-plaza-dark] {
  /* 背景保持透明：让 WallpaperBackground 的星空/灯笼透出来，
     .glass 的 blur(18px) 才有真实内容可模糊。
     theme.css 的 body.has-wallpaper 已经把 --bg 设为 transparent，
     这里显式重申是为了不依赖那层间接。 */
  --bg: transparent;
  --bg-subtle: transparent;

  /* 深色底上的三级表面：不再是白，而是白色低透明叠加。
     数值比 --bg 更克制，因为背后已经有壁纸，再叠白会发灰。 */
  --surface: rgba(255, 255, 255, 0.06);
  --surface-2: rgba(255, 255, 255, 0.04);
  --surface-3: rgba(255, 255, 255, 0.03);

  --border: rgba(255, 255, 255, 0.10);
  --border-strong: rgba(255, 255, 255, 0.18);

  /* 三级文字：主 0.92 / 次 0.60 / 弱 0.38。
     0.38 是「弱提示」的下限——用在时间戳、计数这类可略过的信息上。
     需要读正文时用 0.92，正文实测对比度见下方 .plaza 的 background 叠加层。 */
  --text: rgba(255, 255, 255, 0.92);
  --text-2: rgba(255, 255, 255, 0.60);
  --text-3: rgba(255, 255, 255, 0.38);

  /* 深绿 #0b6e54 在深色底上几乎看不见（对比度约 1.4:1）。
     换成同色系提亮的青绿，与 --grad-brand 的 #10b981/#2dd4bf 同一家族。 */
  --accent: #2dd4bf;
  --accent-hover: #5eead4;
  --accent-active: #14b8a6;
  --accent-soft: rgba(45, 212, 191, 0.14);
  --accent-soft-strong: rgba(45, 212, 191, 0.22);
}

.plaza {
  background: var(--bg);
  min-height: 100vh;
  position: relative;
  z-index: 1;
}

/* ---------------------------------------------------------------------------
 * .feed-card 的边框修正
 *
 * PostCard.vue 的 .feed-card 写了 border: 1px solid transparent —— 那是它在
 * 浅色主题下的「预留位」（hover 时换成绿色）。加上 class="glass" 后，两个类
 * 都命中同一条 border，且 .feed-card 的作用域属性使其后注入，transparant
 * 赢了 .glass 的 rgba(255,255,255,.08) → 玻璃的细白边整个消失。
 *
 * 这里把玻璃边框重新提回来，同时保留 hover 的绿色提亮：
 * 不改 PostCard 一行代码，由外层覆写。
 * -------------------------------------------------------------------------*/
.plaza-shell :deep(.feed-card) {
  border-color: rgba(255, 255, 255, 0.08);
}
.plaza-shell :deep(.feed-card:hover) {
  border-color: rgba(45, 212, 191, 0.45);
}

.plaza-header {
  position: sticky; top: 0; z-index: 100;
  background: rgba(10, 16, 19, 0.55);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  -webkit-backdrop-filter: blur(18px) saturate(140%);
  backdrop-filter: blur(18px) saturate(140%);
  transition: background var(--dur-slow) var(--ease),
    box-shadow var(--dur-slow) var(--ease);
}
/* 滚动后加深并浮起：未滚动时半透明让壁纸多露一点，滚动后需要把下面的
   卡片内容压住，否则文字会与滑过的卡片叠在一起。 */
.plaza-header.is-scrolled {
  background: rgba(8, 13, 16, 0.82);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.35);
}
.plaza-header-inner {
  position: relative;
  display: flex; align-items: center; justify-content: space-between;
  height: var(--header-h);
}

/* Logo 渐变字：background-clip:text 必须保留 color 兜底，
   不支持时文字是深色 --text 而非透明消失。 */
.brand {
  color: var(--text);
  text-decoration: none;
  font-weight: 700; font-size: var(--text-lg);
  background: linear-gradient(135deg, #2dd4bf, #38bdf8);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

/* 导航水平居中于整个 header（与首页间距对齐） */
.plaza-nav {
  position: absolute; left: 50%; transform: translateX(-50%);
  display: flex; gap: var(--space-1);
}
.nav-link {
  position: relative;
  padding: 8px 12px; font-size: var(--text-base);
  border-radius: var(--radius-sm); color: var(--text-2);
  text-decoration: none; white-space: nowrap;
  transition: color var(--dur-fast) var(--ease);
}
.nav-link:hover { color: var(--text); }
/* 当前项渐变下划线：用 ::after 而不是 border-bottom，
   border 会占掉盒模型高度导致按下时整行跳动。 */
.nav-link.is-active { color: var(--text); font-weight: 600; }
.nav-link.is-active::after {
  content: "";
  position: absolute; left: 12px; right: 12px; bottom: 2px;
  height: 2px; border-radius: var(--radius-full);
  background: linear-gradient(90deg, #2dd4bf, #38bdf8);
}

.plaza-shell {
  max-width: 1440px; margin: 0 auto;
  display: grid; grid-template-columns: 280px minmax(0, 1fr) 320px;
  gap: 20px; padding: 24px;
}

.plaza-user { text-align: center; }
.plaza-user h2 { margin: 10px 0 4px; }
.plaza-user h2 { display: inline-flex; align-items: center; gap: 6px; }
.plaza-user p, .announcement, .active-user small { color: var(--text-2); }
.plaza-user .user-hint { font-size: var(--text-xs); margin: var(--space-2) 0 0; }

/* 区块标题：渐变字。用 background-clip:text 并保留 color 作为降级/兜底色，
   浏览器不支持 background-clip:text 时文字仍是可读的深色而非透明。 */
.plaza-left h3, .plaza-right h3 {
  margin: 0 0 var(--space-3);
  font-size: var(--text-sm); font-weight: var(--weight-semibold);
  letter-spacing: 0.02em;
  color: var(--text);
  background: var(--grad-brand);
  -webkit-background-clip: text;
  background-clip: text;
}

.user-stats {
  display: flex; justify-content: space-around;
  gap: var(--space-2); margin-top: var(--space-4);
  padding-top: var(--space-4); border-top: 1px solid var(--border);
}
/* 数字用等宽数位，滚动过程中宽度不跳 */
.user-stats b {
  display: block; font-size: var(--text-lg); color: var(--text);
  font-variant-numeric: tabular-nums;
}
.user-stats span { font-size: var(--text-xs); color: var(--text-2); }

.avatar {
  width: 36px; height: 36px; border-radius: 50%;
  display: inline-flex; align-items: center; justify-content: center;
  background: var(--surface-3); color: var(--text-2);
  overflow: hidden; flex-shrink: 0;
}
.avatar img { width: 100%; height: 100%; object-fit: cover; }
.avatar-lg { width: 64px; height: 64px; margin: 0 auto; font-size: var(--text-xl); }

/* conic 光环：用 ::before 画在头像外圈。
   刻意不加 overflow:hidden 到 .avatar-ring 上——那会把外扩的环裁掉。
   降级时只去 animation，background 保留 → 静态渐变环。 */
.avatar-ring { position: relative; isolation: isolate; }
.avatar-ring::before {
  content: "";
  position: absolute;
  inset: calc(-1 * var(--ring-size));
  z-index: -1;
  border-radius: 50%;
  background: var(--ring-gradient);
  animation: ring-spin var(--ring-spin) linear infinite;
}
@keyframes ring-spin { to { transform: rotate(1turn); } }

.plaza-post-btn, .feed-composer button, .plaza-filter button, .expand-btn {
  border: 1px solid var(--border-strong); background: var(--surface);
  color: var(--text); padding: 8px 14px; border-radius: var(--radius);
  font-size: var(--text-sm); cursor: pointer;
  transition: background var(--dur) var(--ease), border-color var(--dur) var(--ease),
    color var(--dur) var(--ease), transform var(--dur) var(--ease),
    box-shadow var(--dur) var(--ease);
}

/* 发布按钮：渐变底 + hover 扫光 + 轻微上浮。
   扫光用 ::after 而非 background 渐变动画：这样文字颜色不用跟着动，
   且扫光不会顶掉渐变底色。 */
.plaza-post-btn {
  position: relative; overflow: hidden;
  width: 100%; padding: var(--space-3);
  background: var(--grad-brand); color: #fff; border: none;
  font-weight: var(--weight-medium);
}
.plaza-post-btn span { position: relative; z-index: 1; }
.plaza-post-btn::after {
  content: "";
  position: absolute; inset: 0;
  background: var(--grad-sheen);
  opacity: 0;
  transform: translateX(-100%);
  pointer-events: none;
}
.plaza-post-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(16, 185, 129, 0.28);
}
.plaza-post-btn:hover::after { opacity: 1; animation: sheen-sweep 850ms var(--ease) forwards; }
@keyframes sheen-sweep {
  from { transform: translateX(-100%); }
  to { transform: translateX(100%); }
}

/* 分类筛选：选中项渐变胶囊 + 光晕；未选中=幽灵描边，hover 平移 */
.plaza-filter button {
  border-color: rgba(255, 255, 255, 0.14);
  background: transparent;
  color: var(--text-2);
}
.plaza-filter button:hover {
  transform: translateX(3px);
  border-color: rgba(45, 212, 191, 0.45);
  color: var(--text);
  background: rgba(45, 212, 191, 0.08);
}
.plaza-filter button.active {
  background: var(--grad-brand);
  border-color: transparent;
  color: #04211d;
  font-weight: var(--weight-semibold);
  box-shadow: 0 4px 14px rgba(45, 212, 191, 0.32);
}
.plaza-filter button.active:hover { transform: translateX(3px); }

.plaza-feed { display: grid; gap: 16px; }
.feed-composer { padding: 16px; }
.feed-composer > button { width: 100%; text-align: left; color: var(--text-3); background: var(--surface-2); border-style: dashed; }

.composer-types { display: flex; flex-wrap: wrap; gap: var(--space-2); margin-bottom: var(--space-3); }
.composer-types button { padding: 4px 12px; font-size: var(--text-xs); }
.composer-types button.active { background: var(--accent-soft); border-color: var(--accent); color: var(--accent); }

/* 输入框：内层暗玻璃 —— 比 .glass 更暗一档且不 blur。
   输入区需要「凹进去」的暗示：外层卡片是浮起的玻璃，输入框若是同样材质会
   失去层级差。这里用 surface-2 叠加一层内阴影，视觉上比背景深。 */
.composer-textarea {
  width: 100%; padding: var(--space-3); font-size: var(--text-sm); font-family: inherit;
  border: 1px solid rgba(255, 255, 255, 0.12); border-radius: var(--radius);
  background: rgba(0, 0, 0, 0.22); color: var(--text); resize: vertical;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.3);
  transition: border-color var(--dur) var(--ease), box-shadow var(--dur) var(--ease);
}
.composer-textarea::placeholder { color: var(--text-3); }
/* 聚焦：渐变描边 + 光晕。用 border-color 渐变需要 border-image，
   但 border-image 会圆角失效，所以这里用两层 box-shadow 模拟渐变描边。 */
.composer-textarea:focus {
  outline: none;
  border-color: rgba(45, 212, 191, 0.6);
  box-shadow:
    inset 0 1px 2px rgba(0, 0, 0, 0.3),
    0 0 0 3px rgba(45, 212, 191, 0.16),
    0 0 12px rgba(45, 212, 191, 0.2);
}

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
  border: 1px solid rgba(255, 255, 255, 0.12); border-radius: var(--radius);
  background: rgba(0, 0, 0, 0.22); color: var(--text);
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.3);
  transition: border-color var(--dur) var(--ease), box-shadow var(--dur) var(--ease);
}
.cp-field input::placeholder { color: var(--text-3); }
.cp-field input:focus {
  outline: none;
  border-color: rgba(45, 212, 191, 0.6);
  box-shadow:
    inset 0 1px 2px rgba(0, 0, 0, 0.3),
    0 0 0 3px rgba(45, 212, 191, 0.16),
    0 0 12px rgba(45, 212, 191, 0.2);
}

.composer-error { margin: var(--space-2) 0 0; font-size: var(--text-xs); color: var(--danger); }
.composer-actions { display: flex; justify-content: flex-end; gap: var(--space-2); margin-top: var(--space-3); }
.cp-cancel, .cp-submit { padding: 6px 16px; font-size: var(--text-sm); }
.cp-submit { background: var(--accent); color: #fff; border-color: var(--accent); }
.cp-submit:hover:not(:disabled) { background: var(--accent-hover); }
.cp-submit:disabled { opacity: 0.6; cursor: not-allowed; }


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

/* ---------- 右栏 ---------- */
.side-skeleton { display: flex; flex-direction: column; gap: var(--space-2); }
/* 骨架块要撑满一行，否则宽度塌成 0 看不见 */
.side-skeleton .sk-line { width: 100%; }
.side-hint {
  margin: 0; font-size: var(--text-xs); color: var(--text-3);
  display: flex; align-items: center; gap: var(--space-2);
}
/* 幽灵按钮：透明底 + 细描边，hover 才浮现填充与光晕。
   降级态也要降得好看——这是右栏接口未就绪时用户唯一能点的出口，
   做成实心按钮会让「出错」看起来像「成功」。 */
.side-retry {
  padding: 2px 10px; font-size: var(--text-xs);
  border: 1px solid rgba(255, 255, 255, 0.18); border-radius: var(--radius-full);
  background: transparent; color: var(--text-2); cursor: pointer;
  transition: border-color var(--dur) var(--ease), color var(--dur) var(--ease),
    background var(--dur) var(--ease), box-shadow var(--dur) var(--ease);
}
.side-retry:hover {
  border-color: rgba(45, 212, 191, 0.55);
  color: var(--accent);
  background: rgba(45, 212, 191, 0.1);
  box-shadow: 0 0 12px rgba(45, 212, 191, 0.22);
}

/* 热门话题：Top3 用渐变大字序号，其余用小号中性序号。
   序号是排名的视觉权重，不是装饰——第 1 和第 6 必须一眼能分辨。 */
.topic-list { list-style: none; margin: 0; padding: 0; display: grid; gap: 6px; }
.topic-item {
  display: flex; align-items: center; gap: var(--space-2);
  padding: 4px 8px; font-size: var(--text-xs); color: var(--text-2);
  border-radius: var(--radius-sm);
  transition: background var(--dur) var(--ease), transform var(--dur) var(--ease);
}
.topic-item:hover { background: rgba(255, 255, 255, 0.05); transform: translateX(3px); }

.topic-rank {
  flex: none; width: 18px; text-align: center;
  font-size: var(--text-xs); font-weight: var(--weight-semibold);
  color: var(--text-3); font-variant-numeric: tabular-nums;
}
.topic-item.is-top .topic-rank {
  width: 22px; font-size: var(--text-lg); line-height: 1;
  background: var(--grad-brand);
  -webkit-background-clip: text; background-clip: text;
  color: transparent;
}
.topic-item.is-top:nth-child(2) .topic-rank { background: var(--grad-cool); }
.topic-item.is-top:nth-child(3) .topic-rank { background: var(--grad-warm); }

.topic-name {
  flex: 1; min-width: 0;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.topic-count { font-style: normal; color: var(--text-3); font-size: var(--text-xs); }

/* 活跃用户：hover 整行上浮 + 渐变光环 */
.active-user-list { list-style: none; margin: 0; padding: 0; display: grid; gap: var(--space-2); }
.active-user {
  display: flex; align-items: center; gap: var(--space-2);
  padding: var(--space-2); border-radius: var(--radius);
  border: 1px solid rgba(255, 255, 255, 0.06);
  transition: transform var(--dur) var(--ease), box-shadow var(--dur) var(--ease),
    border-color var(--dur) var(--ease), background var(--dur) var(--ease);
}
.active-user:hover {
  transform: translateY(-3px);
  border-color: rgba(45, 212, 191, 0.4);
  background: rgba(255, 255, 255, 0.05);
  box-shadow: 0 8px 20px rgba(45, 212, 191, 0.14);
}
.au-body { display: grid; min-width: 0; }
.au-body b {
  font-size: var(--text-xs); color: var(--text);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.au-body small { font-size: var(--text-xs); color: var(--text-3); }

/* shimmer 骨架屏：一道斜向高光扫过。
   降级时改为固定灰阶（见文件末尾媒体查询），而不是停在首帧。 */
.feed-skeleton { padding: var(--space-4); display: flex; flex-direction: column; gap: var(--space-3); }
.sk {
  position: relative; overflow: hidden;
  /* 暗色下的骨架块要比浅色时更亮一点，否则在深色玻璃上完全看不见 */
  background: rgba(255, 255, 255, 0.07);
  border-radius: var(--radius-sm);
  --sk-rest-opacity: 0.7;
}
.sk::after {
  content: "";
  position: absolute; inset: 0;
  background: var(--grad-sheen);
  opacity: 0.35;
  transform: translateX(-100%);
  animation: shimmer 1.4s ease-in-out infinite;
}
.sk-row { height: 36px; }
.sk-line { height: 14px; }
.sk-line--short { width: 40%; }
@keyframes shimmer {
  0% { transform: translateX(-100%); }
  60%, 100% { transform: translateX(100%); }
}

.plaza-toast {
  position: fixed; bottom: 32px; left: 50%; transform: translateX(-50%);
  z-index: 1000; padding: 10px 20px; border-radius: var(--radius);
  background: rgba(10, 16, 19, 0.9); color: var(--text); font-size: var(--text-sm);
  border: 1px solid rgba(255, 255, 255, 0.1);
  -webkit-backdrop-filter: blur(14px);
  backdrop-filter: blur(14px);
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
/* =========================================================
   降级为静态等价物，而非冻住动画

   逐条对应（每条都保留"这件事发生了"的视觉暗示）：
   - 光环 conic 渐变 → 保留静态渐变环，只停旋转
   - 骨架 shimmer     → 固定灰阶块 + opacity .7（停动画会停在首帧 opacity:1，
                        看起来像内容已加载，比不降级更糟）
   - hover 位移/扫光  → 去 transform，保留阴影、边框、背景变化
   - 数字 count-up    → 由 useCountUp 直接落终值（见 composables/useCountUp.js）

   注意：.feed-card 的降级规则在 PostCard.vue 里，不在这里——
   卡片是子组件，scoped 属性不同，这里写匹配不到。 */
@media (prefers-reduced-motion: reduce) {
  .sk::after { animation: none; opacity: 0; }
  .sk { opacity: var(--sk-rest-opacity, 0.7); }

  /* 光环：只停旋转，渐变背景保留 —— 这就是静态等价物 */
  .avatar-ring::before { animation: none; }

  /* 上浮全部取消，改为「仅边框提亮」：
     边框变化不依赖运动，仍能告诉用户「指向了这一项」。 */
  .plaza-post-btn:hover { transform: none; }
  .plaza-post-btn::after { animation: none; opacity: 0; }
  .plaza-filter button:hover,
  .plaza-filter button.active:hover,
  .topic-item:hover { transform: none; }
  .active-user:hover {
    transform: none;
    border-color: rgba(45, 212, 191, 0.4);
    box-shadow: none;
  }
  /* 幽灵按钮 hover 只换填充与描边，不做位移，故 transition 本身也要关 */
  .side-retry:hover { transform: none; }
  .side-retry,
  .plaza-filter button,
  .active-user,
  .topic-item { transition: none; }

  .plaza-nav,
  .plaza-header { transition: none; }
}
</style>

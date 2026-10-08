<script setup>
/**
 * 单条大厅动态卡片（瘦子展示组件）。
 *
 * <p>刻意不感知父级的任何 Map——父组件在 v-for 里查出派生状态（布尔/数组/字符串）
 * 再传进来。这样卡片内部只剩"展示 + 抛事件"，动效作用域完全局限在单卡里，
 * 不会和父组件的列表编排互相干扰。</p>
 */
import { computed, ref, watch } from 'vue'
import VerifiedBadge from './VerifiedBadge.vue'

const props = defineProps({
  /** 后端返回的动态对象，唯一必填项 */
  post: { type: Object, required: true },
  /** 正文是否展开全文（由父组件的 expanded[id] 派生） */
  isExpanded: { type: Boolean, default: false },
  isLiked: { type: Boolean, default: false },
  isLikeAnimating: { type: Boolean, default: false },
  isCommentsVisible: { type: Boolean, default: false },
  comments: { type: Array, default: () => [] },
  isCommentLoading: { type: Boolean, default: false }
})

const emit = defineEmits([
  'toggle-expand',
  'like',
  'toggle-comments',
  'submit-comment',
  'share'
])

/**
 * 评论输入的草稿只在卡片内持有。
 *
 * <p>不走上双向绑定：那样父组件就得为每个 id 存一份草稿，且提交成功后的清理
 * 时机要跨组件协调。留在卡片内则天然随 v-if 卸载而丢弃。</p>
 */
const commentDraft = ref('')

// 切换到另一条动态时清空草稿，避免把 A 的半句话发到 B 下
watch(() => props.post.id, () => { commentDraft.value = '' })

// 彩色圆点表示类型，不用 emoji 以免和正文里的话题标签抢注意力
const TYPE_ICONS = { SELL: '🔴', SEEK: '🔵', FREE: '🟢', WARN: '🟡', CHAT: '⚪' }

function typeIcon(type) {
  return TYPE_ICONS[type] || '⚪'
}

/**
 * 图片网格：1 张大图 / 2-3 张并排 / 4 张及以上九宫格。
 *
 * <p>容器只渲染前 9 张——再多就不是九宫格了。</p>
 */
function imageGridClass(count) {
  if (count === 1) return 'grid-1'
  if (count <= 3) return 'grid-2'
  return 'grid-3'
}

function productStatusText(status) {
  if (status === 1) return '在售'
  if (status === 2) return '已售出'
  if (status === 0) return '已下架'
  return ''
}

function formatTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

const showExpandToggle = computed(() => props.post.content.length > 80)

/** 互动栏仅商品类动态显示：like_record/comments 无 target_type 列，
 *  post 与 product 的 ID 会碰撞，非商品动态挂互动会串数据。 */
const isProductPost = computed(() => !!props.post.productId)
</script>

<template>
  <article class="card feed-card">
    <header class="feed-head">
      <!-- 头像：conic 渐变光环。降级时保留静态渐变环，只是不再旋转。 -->
      <div class="avatar avatar-ring">
        <img v-if="post.author?.avatar" :src="post.author.avatar" :alt="`${post.author.username} 的头像`" />
        <template v-else>{{ (post.author?.username || '?').charAt(0) }}</template>
      </div>
      <div class="feed-meta">
        <b>
          {{ post.author?.username }}
          <VerifiedBadge v-if="post.author?.studentVerifiedUser" :verified="true" size="sm" />
        </b>
        <span>
          {{ formatTime(post.createdTime) }}
          <template v-if="post.author?.location"> · {{ post.author.location }}</template>
        </span>
      </div>
      <em class="badge" :data-type="post.type">{{ typeIcon(post.type) }} {{ post.typeLabel }}</em>
    </header>

    <p class="feed-content" :class="{ expanded: isExpanded }">{{ post.content }}</p>
    <button
      v-if="showExpandToggle"
      class="expand-btn"
      type="button"
      @click="emit('toggle-expand')"
    >
      {{ isExpanded ? '收起' : '展开全文' }}
    </button>

    <div v-if="post.tagList?.length" class="tags">
      <span v-for="tag in post.tagList" :key="tag">#{{ tag }}</span>
    </div>

    <div v-if="post.imageList?.length" class="feed-images" :class="imageGridClass(post.imageList.length)">
      <figure v-for="(img, i) in post.imageList.slice(0, 9)" :key="i" class="image-item">
        <img :src="img" alt="动态图片" />
      </figure>
    </div>

    <!-- 关联商品内嵌小卡 -->
    <router-link
      v-if="post.product"
      :to="`/products/${post.product.id}`"
      class="feed-product"
    >
      <span class="fp-title">{{ post.product.title }}</span>
      <span class="fp-price">¥{{ post.product.price }}</span>
      <span class="fp-status" :data-status="post.product.status">{{ productStatusText(post.product.status) }}</span>
    </router-link>

    <div v-else-if="post.type === 'SEEK'" class="price-row">求带价</div>

    <!-- 互动栏仅商品类动态显示，计数取服务端真实值 -->
    <footer v-if="isProductPost" class="feed-actions">
      <button
        type="button"
        class="like-btn"
        :class="{ 'like-pop': isLikeAnimating }"
        :aria-pressed="isLiked"
        @click="emit('like')"
      >
        <span class="like-heart" :class="{ 'is-liked': isLiked }" aria-hidden="true">{{ isLiked ? '❤️' : '🤍' }}</span>
        <span>点赞</span>
        <span class="like-count">{{ post.likeCount ?? 0 }}</span>
      </button>
      <button type="button" @click="emit('toggle-comments')">
        💬 评论 <span class="act-count">{{ post.commentCount ?? 0 }}</span>
      </button>
      <button type="button" @click="emit('share')">✉️ 转发 <span class="act-count">{{ post.shareCount ?? 0 }}</span></button>
    </footer>

    <section v-if="isCommentsVisible" class="comment-area">
      <div v-if="isCommentLoading" class="comment-loading">加载评论…</div>
      <div v-for="c in comments" :key="c.id" class="comment-item">
        <b>{{ c.username || c.userId }}</b>
        <span>{{ c.content }}</span>
        <small>{{ formatTime(c.createdTime) }}</small>
      </div>
      <div class="comment-input">
        <input
          v-model="commentDraft"
          type="text"
          placeholder="写下你的评论…"
          @keyup.enter="emit('submit-comment', commentDraft)"
        />
        <button type="button" @click="emit('submit-comment', commentDraft)">发送</button>
      </div>
    </section>
  </article>
</template>

<style scoped>
.feed-card {
  padding: 16px;
  border: 1px solid transparent;
  transition:
    transform var(--dur-slow) var(--ease),
    box-shadow var(--dur-slow) var(--ease),
    border-color var(--dur) var(--ease);
}
.feed-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(16, 185, 129, 0.15);
  border-color: rgba(16, 185, 129, 0.4);
}

/* ---------- 卡片头 ---------- */
.feed-head { display: flex; align-items: center; gap: 12px; }

.avatar {
  position: relative;
  width: 40px; height: 40px; flex: none;
  display: flex; align-items: center; justify-content: center;
  border-radius: 50%;
  background: var(--surface-3); color: var(--text-2);
  font-size: var(--text-sm); font-weight: var(--weight-semibold);
  isolation: isolate;
}
.avatar img { width: 100%; height: 100%; object-fit: cover; border-radius: 50%; }

/* 光环用 ::before 画在头像外圈，不侵入图片本身。
   animation:none 时 conic-gradient 依然生效 → 静态渐变环，即降级等价物。 */
.avatar-ring::before {
  content: "";
  position: absolute;
  inset: calc(-1 * var(--ring-size));
  z-index: -1;
  border-radius: 50%;
  background: var(--ring-gradient);
  animation: ring-spin var(--ring-spin) linear infinite;
}

.feed-meta { display: grid; }
.feed-meta b {
  display: flex; align-items: center; gap: 4px;
  font-size: var(--text-sm); color: var(--text);
}
.feed-meta span { font-size: var(--text-xs); }

/* 类型徽章：渐变底 */
.badge {
  margin-left: auto;
  font-size: var(--text-xs);
  padding: 3px 10px;
  border-radius: var(--radius-full);
  font-style: normal;
  color: #fff;
  background: var(--surface-3);
}
.badge[data-type='SELL'] { background: var(--grad-brand); }
.badge[data-type='SEEK'] { background: var(--grad-cool); }
.badge[data-type='FREE'] { background: var(--grad-warm); }
.badge[data-type='WARN'] { background: linear-gradient(135deg, #ef4444 0%, #b91c1c 100%); }
.badge[data-type='CHAT'] { background: linear-gradient(135deg, #64748b 0%, #475569 100%); }

/* ---------- 正文 ---------- */
.feed-content {
  margin: var(--space-3) 0 0; color: var(--text);
  line-height: 1.7; display: -webkit-box; -webkit-line-clamp: 3;
  -webkit-box-orient: vertical; overflow: hidden;
}
.feed-content.expanded { display: block; }
.expand-btn {
  margin-top: var(--space-2); font-size: var(--text-xs); padding: 2px 0;
  border: none; background: none; color: var(--accent);
}

.tags { display: flex; flex-wrap: wrap; gap: var(--space-2); margin-top: var(--space-3); }
.tags span { font-size: var(--text-xs); color: var(--accent); }

/* ---------- 图片网格 ---------- */
.feed-images { display: grid; gap: 8px; margin: 12px 0; }
.image-item {
  position: relative; overflow: hidden;
  margin: 0;
  border-radius: var(--radius);
  background: var(--surface-3);
}
.image-item img {
  width: 100%; height: 100%; object-fit: cover; display: block;
  transition: transform 520ms var(--ease);
}
.image-item:hover img { transform: scale(1.05); }

/* 斜向扫光：静态时 opacity:0 完全不可见，不靠 display 切换，
   免得 hover 时出现一帧闪烁。 */
.image-item::after {
  content: "";
  position: absolute; inset: 0;
  background: var(--grad-sheen);
  opacity: 0;
  transform: translateX(-100%);
  transition: opacity var(--dur) var(--ease);
  pointer-events: none;
}
.image-item:hover::after {
  opacity: 1;
  animation: sheen-sweep 900ms var(--ease) forwards;
}

.feed-images.grid-1 .image-item { height: 320px; }
.feed-images.grid-2 { grid-template-columns: repeat(2, 1fr); }
.feed-images.grid-2 .image-item { aspect-ratio: 1 / 1; }
.feed-images.grid-3 { grid-template-columns: repeat(3, 1fr); }
.feed-images.grid-3 .image-item { aspect-ratio: 1 / 1; }

/* ---------- 关联商品小卡 ---------- */
.feed-product {
  display: flex; align-items: center; gap: var(--space-3);
  margin-top: var(--space-3); padding: var(--space-3);
  background: var(--surface-2);
  border: 1px solid var(--border);
  border-radius: var(--radius); text-decoration: none;
  transition: border-color var(--dur) var(--ease), background var(--dur) var(--ease);
}
.feed-product:hover { border-color: var(--accent); background: var(--surface); }
.fp-title {
  flex: 1; font-size: var(--text-sm); color: var(--text);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
/* 价格：渐变文字。用 background-clip 而非直接给 color，
   否则降级时无从下手，且渐变文字对比度不可控，只用于装饰性数字。 */
.fp-price {
  font-size: var(--text-md); font-weight: var(--weight-semibold);
  background: var(--grad-brand);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.fp-status {
  font-size: var(--text-xs); color: var(--text-3);
  padding: 2px 8px; border-radius: var(--radius-full);
  background: var(--surface-3);
}
.fp-status[data-status='2'] { background: var(--surface-3); color: var(--text-3); }
.fp-status[data-status='0'] { background: var(--danger-soft); color: var(--danger); }

.price-row { margin-top: var(--space-3); font-size: var(--text-sm); color: var(--text-2); }

/* ---------- 互动栏 ---------- */
.feed-actions { display: flex; gap: var(--space-4); margin-top: var(--space-4); }
.feed-actions button {
  display: inline-flex; align-items: center; gap: 5px;
  background: transparent; color: var(--text-2);
  padding: 0; border: none; font-size: var(--text-sm);
  transition: color var(--dur) var(--ease);
}
.feed-actions button:hover { color: var(--text); }

.like-heart { display: inline-block; line-height: 1; }
.like-heart.is-liked { filter: saturate(1.2); }
.like-pop .like-heart { animation: heart-pop 420ms var(--ease-out-back); }
.like-pop .like-count { display: inline-block; animation: count-bounce 420ms var(--ease-out-back); }
.like-count, .act-count { font-variant-numeric: tabular-nums; }

/* ---------- 评论 ---------- */
.comment-area {
  margin-top: var(--space-4); padding-top: var(--space-3);
  border-top: 1px solid var(--border);
}
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

@keyframes ring-spin {
  to { transform: rotate(1turn); }
}
@keyframes heart-pop {
  0% { transform: scale(1); }
  40% { transform: scale(1.45); }
  100% { transform: scale(1); }
}
@keyframes count-bounce {
  0% { transform: translateY(0) scale(1); }
  40% { transform: translateY(-3px) scale(1.18); }
  100% { transform: translateY(0) scale(1); }
}
@keyframes sheen-sweep {
  from { transform: translateX(-100%); }
  to { transform: translateX(100%); }
}

/* =========================================================
   降级：静态等价物
   ========================================================= */
@media (prefers-reduced-motion: reduce) {
  /* 光环保留为静态渐变环——只停旋转，不移除 background */
  .avatar-ring::before { animation: none; }
  /* hover 去掉位移，保留阴影与边框变化（不依赖运动） */
  .feed-card:hover { transform: none; }
  /* 图片只做轻微提亮，不放大、不扫光 */
  .image-item img { transition: none; }
  .image-item:hover img { transform: none; }
  .image-item::after { animation: none; opacity: 0; }
  /* 爆点与弹跳取消，仅保留已变更的数字本身 */
  .like-pop .like-heart,
  .like-pop .like-count { animation: none; }
}
</style>
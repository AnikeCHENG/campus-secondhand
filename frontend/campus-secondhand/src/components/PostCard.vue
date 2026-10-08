<script setup>
/**
 * 单条大厅动态卡片（瘦子展示组件）。
 *
 * <p>刻意不感知父级的任何 Map——父组件在 v-for 里查出派生状态（布尔/数组/字符串）
 * 再传进来。这样卡片内部只剩"展示 + 抛事件"，阶段 4 的动效作用域完全局限在
 * 单卡里，不会和父组件的列表编排互相干扰。</p>
 */
import { computed, ref, watch } from 'vue'

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
 * 时机要跨组件协调。留在卡片内则天然随 v-if 卸载而丢弃，父组件的 commentDrafts
 * 仍以 Map 为准，两者不会打架。</p>
 */
const commentDraft = ref('')

// 切换到另一条动态时清空草稿，避免把 A 的半句话发到 B 下
watch(() => props.post.id, () => { commentDraft.value = '' })

// 与重构前 Plaza.vue 中的 TYPE_ICONS 逐字一致：彩色圆点表示类型，
// 不用 emoji 以免和正文里的话题标签抢注意力。
const TYPE_ICONS = { SELL: '🔴', SEEK: '🔵', FREE: '🟢', WARN: '🟡', CHAT: '⚪' }

function typeIcon(type) {
  return TYPE_ICONS[type] || '💬'
}

/**
 * 图片网格布局：1 张大图、2-4 张两列、5 张以上三列九宫格。
 *
 * <p>容器只渲染前 9 张，多出的由后端限制而非前端静默丢弃——slice 在模板里，
 * 用户能看到"还有更多"的截断语义。</p>
 */
function imageGridClass(count) {
  if (count === 1) return 'grid-1'
  if (count <= 4) return 'grid-2'
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
      <div v-for="(img, i) in post.imageList.slice(0, 9)" :key="i" class="image-item">
        <img :src="img" alt="动态图片" />
        <span>查看详情</span>
      </div>
    </div>

    <!-- 关联商品内嵌小卡 -->
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

    <!-- 互动栏仅商品类动态显示 -->
    <footer v-if="isProductPost" class="feed-actions">
      <button
        type="button"
        :class="{ 'like-pop': isLikeAnimating }"
        :aria-pressed="isLiked"
        @click="emit('like')"
      >
        {{ isLiked ? '❤️' : '🤍' }} 点赞 {{ post.likeCount ?? 0 }}
      </button>
      <button type="button" @click="emit('toggle-comments')">
        💬 评论 {{ post.commentCount ?? 0 }}
      </button>
      <button type="button" @click="emit('share')">✉️ 转发 {{ post.shareCount ?? 0 }}</button>
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
.feed-card { padding: 16px; transition: transform .3s cubic-bezier(.25,.8,.25,1), box-shadow .3s ease; }
.feed-card:hover { transform: translateY(-8px); box-shadow: 0 16px 40px rgba(0,0,0,.28); }
.feed-head { display: flex; align-items: center; gap: 12px; }
.feed-meta { display: grid; }
.feed-meta b { font-size: var(--text-sm); color: var(--text); }
.feed-meta span { font-size: var(--text-xs); }

.avatar {
  position: relative;
  width: 40px; height: 40px; flex: none;
  display: flex; align-items: center; justify-content: center;
  border-radius: 50%; overflow: hidden;
  background: var(--surface-3); color: var(--text-2);
  font-size: var(--text-sm); font-weight: var(--weight-semibold);
}
.avatar img { width: 100%; height: 100%; object-fit: cover; }

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

/* 降级为静态等价物：去掉位移，但保留阴影——阴影变化不依赖运动，
   仍能传达"这张卡片被指向了"。这条必须写在 PostCard 里：
   父组件的 scoped 属性匹配不到子组件渲染出的 .feed-card。 */
@media (prefers-reduced-motion: reduce) {
  .feed-card:hover {
    transform: none;
    box-shadow: 0 16px 40px rgba(0, 0, 0, .28);
  }
}
</style>
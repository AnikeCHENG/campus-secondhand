import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/plaza',
    name: 'Plaza',
    component: () => import('../views/Plaza.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
  },
  {
    path: '/forgot',
    name: 'ResetPassword',
    component: () => import('../views/ResetPassword.vue'),
  },
  {
    path: '/products',
    name: 'Products',
    component: () => import('../views/Products.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/browse',
    name: 'Browse',
    component: () => import('../views/Products.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/post',
    name: 'PostProduct',
    component: () => import('../views/PostProduct.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/drafts',
    name: 'Drafts',
    component: () => import('../views/Drafts.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/messages',
    name: 'Messages',
    component: () => import('../views/Messages.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/products/:id',
    name: 'ProductDetail',
    component: () => import('../views/ProductDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/payment/:orderId',
    name: 'Payment',
    component: () => import('../views/Payment.vue'),
    meta: { requiresAuth: true }
  },
  // 404 兜底：任何未匹配的路径都渲染提示页而不是空白页
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFound.vue')
  },
  // 管理员路由
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('../views/Admin/AdminDashboard.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/users',
    name: 'AdminUsers',
    component: () => import('../views/Admin/AdminUsers.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/products',
    name: 'AdminProducts',
    component: () => import('../views/Admin/AdminProducts.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/orders',
    name: 'AdminOrders',
    component: () => import('../views/Admin/AdminOrders.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/categories',
    name: 'AdminCategories',
    component: () => import('../views/Admin/AdminCategories.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/messages',
    name: 'AdminMessages',
    component: () => import('../views/Admin/AdminMessages.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

// ============================================================================
// 全局路由守卫
//
// 【重要】前端守卫仅用于体验优化与拦截误入，任何人都可以通过改 localStorage
// 或直接调用接口绕过它——它不是安全边界。
// 真实授权由后端 AdminAuthInterceptor 强制保证：
// 所有 /api/admin/** 在进入 Controller 之前都会校验 JWT 中的 role，
// 非管理员一律 403。因此即使前端被绕过，也拿不到任何管理数据。
// ============================================================================
router.beforeEach(async (to, from, next) => {
  const requiresAuth = to.meta && to.meta.requiresAuth
  const requiresAdmin = to.meta && to.meta.requiresAdmin
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')

  if (requiresAuth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  if (requiresAdmin) {
    const store = useUserStore()
    // role 未从服务端确认过时，先查一次 /api/auth/me 并缓存，后续导航不再请求
    if (!store.verified) {
      const me = await store.fetchMe()
      if (!me) {
        // token 失效
        next({ path: '/login', query: { redirect: to.fullPath } })
        return
      }
    }
    if (store.role !== 1) {
      ElMessage.error('无权限：需要管理员身份')
      next({ path: '/' })
      return
    }
  }

  next()
})

export default router

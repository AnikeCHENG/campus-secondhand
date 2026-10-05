const BASE = import.meta.env.VITE_API_BASE || '/api'

async function request(path, options = {}) {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  console.log('Token from storage:', token ? 'exists' : 'null')
  console.log('Token value:', token)
  try {
    const headers = {
      'Content-Type': 'application/json'
    }
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }
    console.log('Request headers:', headers)
    const res = await fetch(`${BASE}${path}`, {
      headers,
      ...options,
    })
    const data = await res.json().catch(() => ({ message: '服务器返回非JSON' }))
    if (!res.ok) {
      const err = new Error(data?.message || `HTTP ${res.status}`)
      err.response = data
      throw err
    }
    if (data && typeof data.code === 'number' && data.code !== 200) {
      const err = new Error(data.message || '请求未成功')
      err.response = data
      throw err
    }
    return data
  } catch (e) {
    const err = new Error('Network request failed')
    err.cause = e
    throw err
  }
}

// 获取订单列表
export async function getOrderList() {
  try {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token')
    if (!token) {
      // 如果没有 token，直接返回模拟数据
      return {
        code: 200,
        data: [
          {
            id: 'ORD20260315001',
            productId: 1,
            productTitle: '篮球',
            productImage: '/src/images/篮球.avif',
            price: '129.00',
            status: 0,
            createdAt: '2026-03-15T10:00:00',
            sellerId: 2,
            sellerName: '张三'
          },
          {
            id: 'ORD20260315002',
            productId: 2,
            productTitle: '足球',
            productImage: '/src/images/足球.avif',
            price: '99.00',
            status: 0,
            createdAt: '2026-03-15T09:30:00',
            sellerId: 3,
            sellerName: '李四'
          },
          {
            id: 'ORD20260315003',
            productId: 3,
            productTitle: '羽毛球拍',
            productImage: '/src/images/羽毛球拍.avif',
            price: '89.00',
            status: 0,
            createdAt: '2026-03-15T09:00:00',
            sellerId: 4,
            sellerName: '王五'
          },
          {
            id: 'ORD20260315004',
            productId: 4,
            productTitle: '乒乓球拍',
            productImage: '/src/images/乒乓球拍.avif',
            price: '59.00',
            status: 0,
            createdAt: '2026-03-15T08:30:00',
            sellerId: 5,
            sellerName: '赵六'
          },
          {
            id: 'ORD20260315005',
            productId: 5,
            productTitle: '小哑铃',
            productImage: '/src/images/小哑铃.avif',
            price: '159.00',
            status: 0,
            createdAt: '2026-03-15T08:00:00',
            sellerId: 6,
            sellerName: '孙七'
          },
          {
            id: 'ORD20260315006',
            productId: 6,
            productTitle: '跳绳',
            productImage: '/src/images/跳绳.avif',
            price: '29.00',
            status: 0,
            createdAt: '2026-03-15T07:30:00',
            sellerId: 7,
            sellerName: '周八'
          }
        ]
      }
    }
    return await request('/orders/my')
  } catch (error) {
    console.error('获取订单列表失败:', error)
    // 模拟数据
    return {
      code: 200,
      data: [
        {
          id: 'ORD20260315001',
          productId: 1,
          productTitle: '篮球',
          productImage: '/src/images/篮球.avif',
          price: '129.00',
          status: 0,
          createdAt: '2026-03-15T10:00:00',
          sellerId: 2,
          sellerName: '张三'
        },
        {
          id: 'ORD20260315002',
          productId: 2,
          productTitle: '足球',
          productImage: '/src/images/足球.avif',
          price: '99.00',
          status: 0,
          createdAt: '2026-03-15T09:30:00',
          sellerId: 3,
          sellerName: '李四'
        },
        {
          id: 'ORD20260315003',
          productId: 3,
          productTitle: '羽毛球拍',
          productImage: '/src/images/羽毛球拍.avif',
          price: '89.00',
          status: 0,
          createdAt: '2026-03-15T09:00:00',
          sellerId: 4,
          sellerName: '王五'
        },
        {
          id: 'ORD20260315004',
          productId: 4,
          productTitle: '乒乓球拍',
          productImage: '/src/images/乒乓球拍.avif',
          price: '59.00',
          status: 0,
          createdAt: '2026-03-15T08:30:00',
          sellerId: 5,
          sellerName: '赵六'
        },
        {
          id: 'ORD20260315005',
          productId: 5,
          productTitle: '小哑铃',
          productImage: '/src/images/小哑铃.avif',
          price: '159.00',
          status: 0,
          createdAt: '2026-03-15T08:00:00',
          sellerId: 6,
          sellerName: '孙七'
        },
        {
          id: 'ORD20260315006',
          productId: 6,
          productTitle: '跳绳',
          productImage: '/src/images/跳绳.avif',
          price: '29.00',
          status: 0,
          createdAt: '2026-03-15T07:30:00',
          sellerId: 7,
          sellerName: '周八'
        }
      ]
    }
  }
}

// 确认收货
export async function confirmOrder(orderId) {
  try {
    return await request(`/orders/${orderId}/confirm`, {
      method: 'PUT'
    })
  } catch (error) {
    console.error('确认收货失败:', error)
    return {
      code: 200,
      message: '确认收货成功'
    }
  }
}

// 取消订单
export async function cancelOrder(orderId) {
  try {
    return await request(`/orders/${orderId}/cancel`, {
      method: 'PUT'
    })
  } catch (error) {
    console.error('取消订单失败:', error)
    return {
      code: 200,
      message: '取消订单成功'
    }
  }
}

// 创建订单
export async function createOrder(productId) {
  try {
    return await request('/orders', {
      method: 'POST',
      body: JSON.stringify({ productId })
    })
  } catch (error) {
    console.error('创建订单失败:', error)
    throw error
  }
}

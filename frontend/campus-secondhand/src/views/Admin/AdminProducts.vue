<template>
  <div class="admin-products">
    <div class="admin-header">
      <h1>商品管理</h1>
      <router-link to="/admin" class="back-link">返回控制面板</router-link>
    </div>
    
    <!-- 搜索和筛选 -->
    <div class="filter-section">
      <div class="filter-form">
        <input 
          v-model="searchQuery" 
          type="text" 
          placeholder="搜索商品标题或描述" 
          class="search-input"
          @keyup.enter="fetchProducts"
        />
        <select v-model="categoryFilter" class="filter-select" @change="fetchProducts">
          <option value="">所有分类</option>
          <option value="电子产品">电子产品</option>
          <option value="服装鞋帽">服装鞋帽</option>
          <option value="家居用品">家居用品</option>
          <option value="运动户外">运动户外</option>
          <option value="图书音像">图书音像</option>
          <option value="其他">其他</option>
        </select>
        <select v-model="statusFilter" class="filter-select" @change="fetchProducts">
          <option value="">所有状态</option>
          <option value="0">在售</option>
          <option value="1">已售出</option>
          <option value="2">已下架</option>
        </select>
        <button class="search-button" @click="fetchProducts">搜索</button>
      </div>
    </div>
    
    <!-- 商品列表 -->
    <div class="products-table-container">
      <div v-if="loading" class="loading-container">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>
      <div v-else-if="error" class="error-container">
        <p>{{ error }}</p>
        <button class="retry-button" @click="fetchProducts">重试</button>
      </div>
      <table v-else class="products-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>标题</th>
            <th>卖家</th>
            <th>价格</th>
            <th>分类</th>
            <th>状态</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="product in products" :key="product.id">
            <td>{{ product.id }}</td>
            <td class="product-title">{{ product.title }}</td>
            <td>{{ product.userId }}</td>
            <td>¥{{ product.price }}</td>
            <td>{{ product.category || '未分类' }}</td>
            <td>
              <span class="status-badge" :class="getStatusClass(product.status)">
                {{ getStatusText(product.status) }}
              </span>
            </td>
            <td>{{ formatDate(product.createdTime) }}</td>
            <td>
              <div class="action-buttons">
                <button 
                  class="action-button view-button"
                  @click="viewProductDetail(product)"
                  title="查看详情"
                >
                  详情
                </button>
                <button 
                  class="action-button" 
                  :class="getActionClass(product.status)"
                  @click="toggleProductStatus(product.id, product.status)"
                >
                  {{ getStatusAction(product.status) }}
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    
    <!-- 分页 -->
    <div v-if="!loading && !error && total > 0" class="pagination">
      <button 
        class="pagination-button" 
        @click="changePage(1)" 
        :disabled="page === 1"
      >
        首页
      </button>
      <button 
        class="pagination-button" 
        @click="changePage(page - 1)" 
        :disabled="page === 1"
      >
        上一页
      </button>
      <span class="pagination-info">
        第 {{ page }} 页，共 {{ totalPages }} 页，总计 {{ total }} 条
      </span>
      <button 
        class="pagination-button" 
        @click="changePage(page + 1)" 
        :disabled="page === totalPages"
      >
        下一页
      </button>
      <button 
        class="pagination-button" 
        @click="changePage(totalPages)" 
        :disabled="page === totalPages"
      >
        末页
      </button>
    </div>
    
    <!-- 商品详情模态框 -->
    <div v-if="showDetailModal" class="modal-overlay" @click.self="closeDetailModal">
      <div class="modal-content">
        <div class="modal-header">
          <h2>商品详情</h2>
          <button class="close-button" @click="closeDetailModal">&times;</button>
        </div>
        <div class="modal-body">
          <div v-if="selectedProduct" class="product-detail">
            <div class="detail-row">
              <span class="detail-label">ID:</span>
              <span class="detail-value">{{ selectedProduct.id }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">标题:</span>
              <span class="detail-value">{{ selectedProduct.title }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">描述:</span>
              <span class="detail-value">{{ selectedProduct.description || '无' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">价格:</span>
              <span class="detail-value">¥{{ selectedProduct.price }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">原价:</span>
              <span class="detail-value">¥{{ selectedProduct.originalPrice || '未设置' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">分类:</span>
              <span class="detail-value">{{ selectedProduct.category || '未分类' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">成色:</span>
              <span class="detail-value">{{ selectedProduct.condition || '未设置' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">卖家ID:</span>
              <span class="detail-value">{{ selectedProduct.userId }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">状态:</span>
              <span class="detail-value">{{ getStatusText(selectedProduct.status) }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">浏览次数:</span>
              <span class="detail-value">{{ selectedProduct.viewCount || 0 }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">创建时间:</span>
              <span class="detail-value">{{ formatDate(selectedProduct.createdTime) }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">更新时间:</span>
              <span class="detail-value">{{ formatDate(selectedProduct.updatedTime) }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">售出时间:</span>
              <span class="detail-value">{{ formatDate(selectedProduct.soldTime) || '未售出' }}</span>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="close-modal-button" @click="closeDetailModal">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import axios from 'axios'

const products = ref([])
const loading = ref(false)
const error = ref('')
const searchQuery = ref('')
const categoryFilter = ref('')
const statusFilter = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const showDetailModal = ref(false)
const selectedProduct = ref(null)

const totalPages = computed(() => {
  return Math.ceil(total.value / pageSize.value)
})

const fetchProducts = async () => {
  try {
    loading.value = true
    error.value = ''
    const token = localStorage.getItem('token') || sessionStorage.getItem('token')
    const response = await axios.get('/api/admin/products', {
      headers: {
        Authorization: `Bearer ${token}`
      },
      params: {
        search: searchQuery.value,
        category: categoryFilter.value,
        status: statusFilter.value,
        page: page.value,
        pageSize: pageSize.value
      }
    })
    if (response.data.code === 200) {
      products.value = response.data.data.items
      total.value = response.data.data.total
    } else {
      error.value = response.data.message || '获取商品列表失败'
    }
  } catch (err) {
    console.error('获取商品列表失败:', err)
    error.value = '网络错误，请检查网络连接'
  } finally {
    loading.value = false
  }
}

const toggleProductStatus = async (productId, currentStatus) => {
  try {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token')
    let newStatus
    switch (currentStatus) {
      case 0: // 在售 -> 已下架
        newStatus = 2
        break
      case 1: // 已售出 -> 在售
        newStatus = 0
        break
      case 2: // 已下架 -> 在售
        newStatus = 0
        break
      default:
        newStatus = 0
    }
    const response = await axios.put(`/api/admin/products/${productId}/status`, newStatus, {
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    })
    if (response.data.code === 200) {
      // 更新本地商品列表
      const product = products.value.find(p => p.id === productId)
      if (product) {
        product.status = newStatus
      }
      // 显示成功提示
      alert('状态更新成功')
    } else {
      alert('状态更新失败: ' + (response.data.message || '未知错误'))
    }
  } catch (err) {
    console.error('更新商品状态失败:', err)
    alert('网络错误，请检查网络连接')
  }
}

const viewProductDetail = async (product) => {
  try {
    loading.value = true
    const token = localStorage.getItem('token') || sessionStorage.getItem('token')
    const response = await axios.get(`/api/admin/products/${product.id}`, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })
    if (response.data.code === 200) {
      selectedProduct.value = response.data.data
      showDetailModal.value = true
    } else {
      alert('获取商品详情失败: ' + (response.data.message || '未知错误'))
    }
  } catch (err) {
    console.error('获取商品详情失败:', err)
    alert('网络错误，请检查网络连接')
  } finally {
    loading.value = false
  }
}

const closeDetailModal = () => {
  showDetailModal.value = false
  selectedProduct.value = null
}

const changePage = (newPage) => {
  if (newPage >= 1 && newPage <= totalPages.value) {
    page.value = newPage
    fetchProducts()
  }
}

const getStatusText = (status) => {
  switch (status) {
    case 0:
      return '在售'
    case 1:
      return '已售出'
    case 2:
      return '已下架'
    default:
      return '未知'
  }
}

const getStatusClass = (status) => {
  switch (status) {
    case 0:
      return 'status-available'
    case 1:
      return 'status-sold'
    case 2:
      return 'status-offline'
    default:
      return ''
  }
}

const getStatusAction = (status) => {
  switch (status) {
    case 0:
      return '下架'
    case 1:
      return '重新上架'
    case 2:
      return '上架'
    default:
      return '操作'
  }
}

const getActionClass = (status) => {
  switch (status) {
    case 0:
      return 'action下架'
    case 1:
      return 'action上架'
    case 2:
      return 'action上架'
    default:
      return ''
  }
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleString()
}

onMounted(() => {
  fetchProducts()
})
</script>

<style scoped>
.admin-products {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  padding-bottom: 16px;
  border-bottom: 1px solid ***REMOVED***e0e0e0;
}

.admin-header h1 {
  font-size: 24px;
  font-weight: 600;
  color: ***REMOVED***333;
  margin: 0;
}

.back-link {
  text-decoration: none;
  color: ***REMOVED***333;
  padding: 10px 16px;
  background-color: ***REMOVED***f5f5f5;
  border-radius: 8px;
  transition: all 0.3s ease;
  font-size: 14px;
  font-weight: 500;
}

.back-link:hover {
  background-color: ***REMOVED***e0e0e0;
  transform: translateY(-1px);
}

/* 筛选部分 */
.filter-section {
  margin-bottom: 24px;
}

.filter-form {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.search-input {
  flex: 1;
  min-width: 200px;
  padding: 10px 16px;
  border: 1px solid ***REMOVED***e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s ease;
}

.search-input:focus {
  outline: none;
  border-color: ***REMOVED***10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}

.filter-select {
  padding: 10px 16px;
  border: 1px solid ***REMOVED***e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  background-color: white;
  transition: all 0.3s ease;
}

.filter-select:focus {
  outline: none;
  border-color: ***REMOVED***10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}

.search-button {
  padding: 10px 20px;
  background-color: ***REMOVED***10b981;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.search-button:hover {
  background-color: ***REMOVED***059669;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}

/* 加载和错误状态 */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid ***REMOVED***f3f3f3;
  border-top: 4px solid ***REMOVED***10b981;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: ***REMOVED***ef4444;
}

.retry-button {
  margin-top: 16px;
  padding: 8px 16px;
  background-color: ***REMOVED***f5f5f5;
  color: ***REMOVED***333;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.retry-button:hover {
  background-color: ***REMOVED***e0e0e0;
}

/* 商品表格 */
.products-table-container {
  overflow-x: auto;
  margin-bottom: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.products-table {
  width: 100%;
  border-collapse: collapse;
  background-color: white;
}

.products-table th,
.products-table td {
  padding: 12px 16px;
  text-align: left;
  border-bottom: 1px solid ***REMOVED***f0f0f0;
}

.products-table th {
  background-color: ***REMOVED***f9fafb;
  font-weight: 600;
  color: ***REMOVED***333;
  font-size: 14px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.products-table tr:hover {
  background-color: ***REMOVED***f9f9f9;
}

.product-title {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 状态标签 */
.status-badge {
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.status-available {
  background-color: rgba(16, 185, 129, 0.1);
  color: ***REMOVED***059669;
}

.status-sold {
  background-color: rgba(59, 130, 246, 0.1);
  color: ***REMOVED***1d4ed8;
}

.status-offline {
  background-color: rgba(107, 114, 128, 0.1);
  color: ***REMOVED***4b5563;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  gap: 8px;
}

.action-button {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 12px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.view-button {
  background-color: ***REMOVED***f5f5f5;
  color: ***REMOVED***333;
}

.view-button:hover {
  background-color: ***REMOVED***e0e0e0;
}

.action上架 {
  background-color: ***REMOVED***10b981;
  color: white;
}

.action上架:hover {
  background-color: ***REMOVED***059669;
}

.action下架 {
  background-color: ***REMOVED***ef4444;
  color: white;
}

.action下架:hover {
  background-color: ***REMOVED***dc2626;
}

/* 分页 */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid ***REMOVED***e0e0e0;
}

.pagination-button {
  padding: 8px 16px;
  background-color: ***REMOVED***f5f5f5;
  color: ***REMOVED***333;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
  font-weight: 500;
}

.pagination-button:hover:not(:disabled) {
  background-color: ***REMOVED***e0e0e0;
  transform: translateY(-1px);
}

.pagination-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination-info {
  font-size: 14px;
  color: ***REMOVED***666;
  font-weight: 500;
}

/* 模态框 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background-color: white;
  border-radius: 12px;
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid ***REMOVED***e0e0e0;
}

.modal-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: ***REMOVED***333;
  margin: 0;
}

.close-button {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: ***REMOVED***666;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.close-button:hover {
  background-color: ***REMOVED***f5f5f5;
  color: ***REMOVED***333;
}

.modal-body {
  padding: 24px;
}

.product-detail {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.detail-label {
  font-weight: 600;
  color: ***REMOVED***333;
  min-width: 100px;
  flex-shrink: 0;
}

.detail-value {
  flex: 1;
  color: ***REMOVED***666;
  line-height: 1.5;
}

.modal-footer {
  padding: 20px 24px;
  border-top: 1px solid ***REMOVED***e0e0e0;
  display: flex;
  justify-content: flex-end;
}

.close-modal-button {
  padding: 10px 20px;
  background-color: ***REMOVED***f5f5f5;
  color: ***REMOVED***333;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
  font-weight: 500;
}

.close-modal-button:hover {
  background-color: ***REMOVED***e0e0e0;
  transform: translateY(-1px);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .admin-products {
    padding: 16px;
  }
  
  .filter-form {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-input {
    min-width: auto;
  }
  
  .products-table-container {
    font-size: 12px;
  }
  
  .products-table th,
  .products-table td {
    padding: 8px 12px;
  }
  
  .action-buttons {
    flex-direction: column;
    gap: 4px;
  }
  
  .action-button {
    padding: 4px 8px;
    font-size: 10px;
  }
  
  .pagination {
    flex-wrap: wrap;
  }
  
  .pagination-button {
    padding: 6px 12px;
    font-size: 12px;
  }
}
</style>

<!-- ===== 简约浅色主题覆盖 ===== -->
<style scoped>
.admin-products { max-width: var(--container); padding: var(--space-6); font-family: var(--font-sans); }
.admin-header { border-bottom: 1px solid var(--border); padding-bottom: var(--space-4); margin-bottom: var(--space-5); }
.admin-header h1 { font-size: var(--text-2xl); font-weight: var(--weight-semibold); letter-spacing: -0.02em; color: var(--text); }
.back-link { color: var(--text); background: var(--surface); border: 1px solid var(--border-strong); border-radius: var(--radius); padding: 8px 14px; font-size: var(--text-sm); }
.back-link:hover { background: var(--surface-2); transform: none; }

.filter-form { gap: var(--space-3); }
.search-input, .filter-select { padding: 10px 14px; border: 1px solid var(--border-strong); border-radius: var(--radius); font-size: var(--text-base); color: var(--text); background: var(--surface); }
.search-input:focus, .filter-select:focus { border-color: var(--accent); box-shadow: 0 0 0 3px var(--accent-soft); }
.search-button { padding: 10px 20px; background: var(--accent); color: ***REMOVED***fff; border: none; border-radius: var(--radius); font-size: var(--text-base); font-weight: var(--weight-medium); }
.search-button:hover { background: var(--accent-hover); transform: none; box-shadow: none; }

.products-table-container { background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-lg); box-shadow: none; }
.products-table { background: var(--surface); }
.products-table th, .products-table td { padding: var(--space-3) var(--space-4); border-bottom: 1px solid var(--border); font-size: var(--text-sm); }
.products-table th { background: var(--surface-2); color: var(--text-2); font-weight: var(--weight-medium); text-transform: none; letter-spacing: 0; }
.products-table td { color: var(--text); }
.products-table tr:last-child td { border-bottom: none; }
.products-table tr:hover { background: var(--surface-2); }

.status-badge { padding: 3px 12px; border-radius: var(--radius-full); font-size: var(--text-xs); font-weight: var(--weight-medium); text-transform: none; letter-spacing: 0; }
.status-available { background: var(--accent-soft); color: var(--accent); }
.status-sold { background: var(--info-soft); color: var(--info); }
.status-offline { background: var(--surface-3); color: var(--text-2); }

.action-button { padding: 5px 12px; border-radius: var(--radius-sm); font-size: var(--text-xs); font-weight: var(--weight-medium); text-transform: none; letter-spacing: 0; border: 1px solid transparent; background: var(--surface-3); color: var(--text); }
.view-button { background: var(--surface-3); color: var(--text); }
.view-button:hover { background: var(--surface-2); }
.action上架 { background: var(--accent-soft); color: var(--accent); }
.action上架:hover { background: var(--accent); color: ***REMOVED***fff; }
.action下架 { background: var(--danger-soft); color: var(--danger); }
.action下架:hover { background: var(--danger); color: ***REMOVED***fff; }

.pagination { border-top: 1px solid var(--border); }
.pagination-button { padding: 6px 14px; background: var(--surface); color: var(--text); border: 1px solid var(--border-strong); border-radius: var(--radius-sm); font-size: var(--text-sm); }
.pagination-button:hover:not(:disabled) { background: var(--surface-2); transform: none; }
.pagination-info { font-size: var(--text-sm); color: var(--text-2); }

.loading-spinner { border: 3px solid var(--surface-3); border-top-color: var(--accent); }
.error-container { color: var(--danger); }
.retry-button { background: var(--surface); color: var(--text); border: 1px solid var(--border-strong); border-radius: var(--radius-sm); }
.retry-button:hover { background: var(--surface-2); }

.modal-overlay { background: rgba(20,20,18,0.35); }
.modal-content { background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-lg); box-shadow: var(--shadow-md); }
.modal-header { border-bottom: 1px solid var(--border); }
.modal-header h2 { color: var(--text); font-weight: var(--weight-semibold); }
.close-button { color: var(--text-2); border-radius: var(--radius-sm); }
.close-button:hover { background: var(--surface-3); color: var(--text); }
.detail-label { color: var(--text); font-weight: var(--weight-medium); }
.detail-value { color: var(--text-2); }
.modal-footer { border-top: 1px solid var(--border); }
.close-modal-button { background: var(--surface); color: var(--text); border: 1px solid var(--border-strong); border-radius: var(--radius-sm); }
.close-modal-button:hover { background: var(--surface-2); transform: none; }
</style>

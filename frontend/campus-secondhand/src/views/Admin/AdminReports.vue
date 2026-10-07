<template>
  <div class="admin-reports">
    <div class="admin-header">
      <h1>举报管理</h1>
      <router-link to="/admin" class="back-link">返回控制面板</router-link>
    </div>

    <!-- 筛选 -->
    <div class="filter-section">
      <div class="filter-form">
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜索举报原因或处理备注"
          class="search-input"
          @keyup.enter="fetchReports"
        />
        <select v-model="statusFilter" class="filter-select" @change="handleFilterChange">
          <option value="">所有状态</option>
          <option value="0">待处理</option>
          <option value="1">已处理并处罚</option>
          <option value="2">已驳回</option>
        </select>
        <select v-model="typeFilter" class="filter-select" @change="handleFilterChange">
          <option value="">所有类型</option>
          <option value="PRODUCT">商品</option>
          <option value="USER">用户</option>
        </select>
        <button class="search-button" @click="fetchReports">搜索</button>
      </div>
    </div>

    <!-- 举报列表 -->
    <div class="reports-table-container">
      <div v-if="loading" class="loading-container">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>
      <div v-else-if="error" class="error-container">
        <p>{{ error }}</p>
        <button class="retry-button" @click="fetchReports">重试</button>
      </div>
      <div v-else-if="reports.length === 0" class="no-reports">暂无举报记录</div>
      <table v-else class="reports-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>举报人</th>
            <th>被举报对象</th>
            <th>举报原因</th>
            <th>举报时间</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="report in reports" :key="report.id">
            <td>{{ report.id }}</td>
            <td>{{ report.reporterName || `用户${report.reporterId}` }}</td>
            <td>
              <div class="target-cell">
                <span class="type-badge" :class="targetTypeClass(report.targetType)">
                  {{ targetTypeText(report.targetType) }}
                </span>
                <span class="target-name" :title="targetDisplay(report)">
                  {{ targetDisplay(report) }}
                </span>
              </div>
            </td>
            <td>
              <span class="reason-text" :title="report.reason">{{ report.reason }}</span>
            </td>
            <td>{{ formatDate(report.createdTime) }}</td>
            <td>
              <span class="status-badge" :class="getStatusClass(report.status)">
                {{ getStatusText(report.status) }}
              </span>
              <p v-if="report.adminRemark" class="remark-line" :title="report.adminRemark">
                备注：{{ report.adminRemark }}
              </p>
            </td>
            <td>
              <button
                v-if="report.status === 0"
                class="action-button handle-button"
                @click="openHandleModal(report)"
              >
                处理
              </button>
              <span v-else class="done-text">已处理</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 分页 -->
    <div v-if="!loading && !error && total > 0" class="pagination">
      <button class="pagination-button" @click="changePage(1)" :disabled="page === 1">首页</button>
      <button class="pagination-button" @click="changePage(page - 1)" :disabled="page === 1">上一页</button>
      <span class="pagination-info">
        第 {{ page }} 页，共 {{ totalPages }} 页，总计 {{ total }} 条
      </span>
      <button class="pagination-button" @click="changePage(page + 1)" :disabled="page === totalPages">下一页</button>
      <button class="pagination-button" @click="changePage(totalPages)" :disabled="page === totalPages">末页</button>
    </div>

    <!-- 处理弹窗 -->
    <div v-if="showHandleModal" class="modal-overlay" @click.self="closeHandleModal">
      <div class="modal-content">
        <div class="modal-header">
          <h2>处理举报 #{{ currentReport?.id }}</h2>
          <button class="close-button" @click="closeHandleModal" aria-label="关闭">&times;</button>
        </div>
        <div class="modal-body">
          <div class="handle-summary">
            <div class="detail-row">
              <span class="detail-label">举报人:</span>
              <span class="detail-value">{{ currentReport?.reporterName || `用户${currentReport?.reporterId}` }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">被举报:</span>
              <span class="detail-value">
                {{ targetTypeText(currentReport?.targetType) }} · {{ targetDisplay(currentReport) }}
              </span>
            </div>
            <div class="detail-row">
              <span class="detail-label">举报原因:</span>
              <span class="detail-value">{{ currentReport?.reason }}</span>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">处理动作 <span class="required-mark">*</span></label>
            <div class="radio-list">
              <label
                v-for="opt in availableActions"
                :key="opt.value"
                class="radio-item"
                :class="{ 'is-danger': opt.tone === 'danger' }"
              >
                <input type="radio" :value="opt.value" v-model="handleForm.action" />
                <span>{{ opt.label }}</span>
              </label>
            </div>
            <p class="form-hint">{{ actionHint }}</p>
          </div>

          <div class="form-group">
            <label class="form-label" for="handle-remark">
              处理备注 <span class="required-mark">*</span>
            </label>
            <textarea
              id="handle-remark"
              v-model="handleForm.remark"
              class="remark-input"
              rows="3"
              placeholder="请填写处理依据，将展示在举报记录中"
            ></textarea>
            <p v-if="remarkError" class="field-error">{{ remarkError }}</p>
          </div>
        </div>
        <div class="modal-footer">
          <button class="close-modal-button" @click="closeHandleModal" :disabled="submitting">取消</button>
          <button class="submit-button" @click="submitHandle" :disabled="submitting">
            {{ submitting ? '提交中…' : '确认处理' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const reports = ref([])
const loading = ref(false)
const error = ref('')
const searchQuery = ref('')
const statusFilter = ref('')
const typeFilter = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const showHandleModal = ref(false)
const currentReport = ref(null)
const submitting = ref(false)
const remarkError = ref('')
const handleForm = reactive({ action: '', remark: '' })

const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

/**
 * 可选处理动作按 targetType 动态过滤。
 * 前端过滤只是体验优化，真正的权威校验在后端：
 * PUT /api/admin/reports/{id}/handle 会校验 action 与 targetType 是否匹配，不匹配返回 400。
 */
const availableActions = computed(() => {
  const type = currentReport.value?.targetType
  const all = [
    { value: 'DELETE_PRODUCT', label: '下架该商品', tone: 'danger' },
    { value: 'BAN_USER', label: '封禁该用户', tone: 'danger' },
    { value: 'REJECT', label: '驳回举报（不予处理）', tone: 'normal' },
  ]
  const allowed =
    type === 'PRODUCT'
      ? ['DELETE_PRODUCT', 'REJECT']
      : type === 'USER'
        ? ['BAN_USER', 'REJECT']
        : []
  return all.filter((o) => allowed.includes(o.value))
})

const actionHint = computed(() => {
  const type = currentReport.value?.targetType
  if (type === 'PRODUCT') return '该举报对象为商品，仅可下架商品或驳回举报。'
  if (type === 'USER') return '该举报对象为用户，仅可封禁用户或驳回举报。'
  return ''
})

/**
 * 分页契约与项目基线一致（common/PageResult）：
 * 请求发 size，响应读 res.data.list / res.data.total。
 * 与 AdminUsers.vue / AdminProducts.vue / AdminMessages.vue 同一套解析。
 */
const fetchReports = async () => {
  try {
    loading.value = true
    error.value = ''
    const token = localStorage.getItem('token') || sessionStorage.getItem('token')
    const response = await axios.get('/api/admin/reports', {
      headers: { Authorization: `Bearer ${token}` },
      params: {
        search: searchQuery.value || undefined,
        status: statusFilter.value === '' ? undefined : statusFilter.value,
        targetType: typeFilter.value || undefined,
        page: page.value,
        size: pageSize.value
      }
    })
    if (response.data && response.data.data) {
      reports.value = response.data.data.list || []
      total.value = response.data.data.total || 0
    } else {
      reports.value = []
      total.value = 0
    }
  } catch (err) {
    console.error('获取举报列表失败:', err)
    error.value = err?.response?.data?.message || '网络错误，请检查网络连接'
  } finally {
    loading.value = false
  }
}

const handleFilterChange = () => {
  page.value = 1
  fetchReports()
}

const changePage = (newPage) => {
  if (newPage >= 1 && newPage <= totalPages.value) {
    page.value = newPage
    fetchReports()
  }
}

const openHandleModal = (report) => {
  currentReport.value = report
  handleForm.action = availableActions.value[0]?.value || ''
  handleForm.remark = ''
  remarkError.value = ''
  showHandleModal.value = true
}

const closeHandleModal = () => {
  if (submitting.value) return
  showHandleModal.value = false
  currentReport.value = null
  remarkError.value = ''
}

const submitHandle = async () => {
  if (!handleForm.action) {
    remarkError.value = ''
    ElMessage.warning('请选择处理动作')
    return
  }
  if (!handleForm.remark.trim()) {
    remarkError.value = '处理备注为必填项'
    return
  }

  submitting.value = true
  try {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token')
    const response = await axios.put(
      `/api/admin/reports/${currentReport.value.id}/handle`,
      { action: handleForm.action, remark: handleForm.remark.trim() },
      {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      }
    )
    if (response.data && response.data.code === 200) {
      ElMessage.success(response.data.message || '处理成功')
      showHandleModal.value = false
      currentReport.value = null
      await fetchReports()
    } else {
      ElMessage.error(response.data?.message || '处理失败')
    }
  } catch (err) {
    console.error('处理举报失败:', err)
    ElMessage.error(err?.response?.data?.message || '网络错误，请检查网络连接')
  } finally {
    submitting.value = false
  }
}

const targetTypeText = (type) => (type === 'PRODUCT' ? '商品' : type === 'USER' ? '用户' : '未知')

const targetTypeClass = (type) =>
  type === 'PRODUCT' ? 'type-product' : type === 'USER' ? 'type-user' : 'type-unknown'

/** 商品显示标题，用户显示昵称；后端未返回展示名时回退到 ID */
const targetDisplay = (report) => {
  if (!report) return ''
  if (report.targetType === 'PRODUCT') {
    return report.targetTitle || report.targetName || `商品#${report.targetId}`
  }
  return report.targetName || report.targetTitle || `用户#${report.targetId}`
}

const getStatusText = (status) => {
  switch (status) {
    case 0: return '待处理'
    case 1: return '已处理并处罚'
    case 2: return '已驳回'
    default: return '未知'
  }
}

const getStatusClass = (status) => {
  switch (status) {
    case 0: return 'status-pending'
    case 1: return 'status-handled'
    case 2: return 'status-rejected'
    default: return ''
  }
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  if (Number.isNaN(date.getTime())) return ''
  return date.toLocaleString()
}

onMounted(() => {
  fetchReports()
})
</script>

<style scoped>
.admin-reports {
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
  border-bottom: 1px solid #e0e0e0;
}

.admin-header h1 {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.back-link {
  text-decoration: none;
  color: #333;
  padding: 10px 16px;
  background-color: #f5f5f5;
  border-radius: 8px;
  transition: all 0.3s ease;
  font-size: 14px;
  font-weight: 500;
}

.back-link:hover {
  background-color: #e0e0e0;
  transform: translateY(-1px);
}

.filter-section { margin-bottom: 24px; }

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
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s ease;
}

.search-input:focus {
  outline: none;
  border-color: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}

.filter-select {
  padding: 10px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  background-color: white;
  transition: all 0.3s ease;
}

.filter-select:focus {
  outline: none;
  border-color: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}

.search-button {
  padding: 10px 20px;
  background-color: #10b981;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.search-button:hover {
  background-color: #059669;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}

.loading-container,
.error-container,
.no-reports {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #10b981;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-container { color: #ef4444; }

.no-reports { color: #9a9a92; }

.retry-button {
  margin-top: 16px;
  padding: 8px 16px;
  background-color: #f5f5f5;
  color: #333;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.retry-button:hover { background-color: #e0e0e0; }

.reports-table-container {
  overflow-x: auto;
  margin-bottom: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.reports-table {
  width: 100%;
  border-collapse: collapse;
  background-color: white;
}

.reports-table th,
.reports-table td {
  padding: 12px 16px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
}

.reports-table th {
  background-color: #f9fafb;
  font-weight: 600;
  color: #333;
  font-size: 14px;
}

.reports-table tr:hover { background-color: #f9f9f9; }

.target-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.target-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 220px;
}

.type-badge {
  flex-shrink: 0;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
}

.type-product { background-color: rgba(16, 185, 129, 0.1); color: #059669; }
.type-user { background-color: rgba(59, 130, 246, 0.1); color: #1d4ed8; }
.type-unknown { background-color: rgba(107, 114, 128, 0.1); color: #4b5563; }

.reason-text {
  display: inline-block;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
}

.status-pending { background-color: rgba(251, 191, 36, 0.1); color: #b45309; }
.status-handled { background-color: rgba(16, 185, 129, 0.1); color: #059669; }
.status-rejected { background-color: rgba(107, 114, 128, 0.1); color: #4b5563; }

.remark-line {
  margin: 6px 0 0;
  font-size: 12px;
  color: #9a9a92;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.handle-button {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  background-color: #10b981;
  color: white;
  transition: all 0.3s ease;
}

.handle-button:hover { background-color: #059669; }

.done-text { font-size: 12px; color: #9a9a92; }

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #e0e0e0;
}

.pagination-button {
  padding: 8px 16px;
  background-color: #f5f5f5;
  color: #333;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
  font-weight: 500;
}

.pagination-button:hover:not(:disabled) {
  background-color: #e0e0e0;
  transform: translateY(-1px);
}

.pagination-button:disabled { opacity: 0.5; cursor: not-allowed; }

.pagination-info { font-size: 14px; color: #666; font-weight: 500; }

/* 模态框 */
.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
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
  border-bottom: 1px solid #e0e0e0;
}

.modal-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.close-button {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #666;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.close-button:hover { background-color: #f5f5f5; color: #333; }

.modal-body { padding: 24px; }

.handle-summary {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 16px;
  margin-bottom: 20px;
  background-color: #f9fafb;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
}

.detail-row { display: flex; align-items: flex-start; gap: 16px; }

.detail-label {
  font-weight: 600;
  color: #333;
  min-width: 76px;
  flex-shrink: 0;
}

.detail-value { flex: 1; color: #666; line-height: 1.5; }

.form-group { margin-bottom: 20px; }

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 10px;
}

.required-mark { color: #ef4444; }

.radio-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.radio-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #333;
  transition: all 0.3s ease;
}

.radio-item:hover { background-color: #f9fafb; border-color: #10b981; }
.radio-item input { accent-color: #10b981; }
.radio-item.is-danger input { accent-color: #ef4444; }

.form-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #9a9a92;
}

.remark-input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
  transition: all 0.3s ease;
}

.remark-input:focus {
  outline: none;
  border-color: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}

.field-error { margin: 8px 0 0; font-size: 12px; color: #ef4444; }

.modal-footer {
  padding: 20px 24px;
  border-top: 1px solid #e0e0e0;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.close-modal-button {
  padding: 10px 20px;
  background-color: #f5f5f5;
  color: #333;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.close-modal-button:hover { background-color: #e0e0e0; }

.submit-button {
  padding: 10px 20px;
  background-color: #10b981;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.submit-button:hover:not(:disabled) { background-color: #059669; }
.submit-button:disabled { opacity: 0.6; cursor: not-allowed; }

@media (max-width: 768px) {
  .admin-reports { padding: 16px; }
  .filter-form { flex-direction: column; align-items: stretch; }
  .search-input { min-width: auto; }
  .reports-table th, .reports-table td { padding: 8px 12px; font-size: 12px; }
  .reason-text, .target-name { max-width: 120px; }
  .pagination { flex-wrap: wrap; }
  .pagination-button { padding: 6px 12px; font-size: 12px; }
}
</style>

<!-- ===== 简约浅色主题覆盖（与 AdminProducts.vue 同款做法） ===== -->
<style scoped>
.admin-reports { max-width: var(--container); padding: var(--space-6); font-family: var(--font-sans); }
.admin-header { border-bottom: 1px solid var(--border); padding-bottom: var(--space-4); margin-bottom: var(--space-5); }
.admin-header h1 { font-size: var(--text-2xl); font-weight: var(--weight-semibold); letter-spacing: -0.02em; color: var(--text); }
.back-link { color: var(--text); background: var(--surface); border: 1px solid var(--border-strong); border-radius: var(--radius); padding: 8px 14px; font-size: var(--text-sm); }
.back-link:hover { background: var(--surface-2); transform: none; }

.filter-form { gap: var(--space-3); }
.search-input, .filter-select { padding: 10px 14px; border: 1px solid var(--border-strong); border-radius: var(--radius); font-size: var(--text-base); color: var(--text); background: var(--surface); }
.search-input:focus, .filter-select:focus { border-color: var(--accent); box-shadow: 0 0 0 3px var(--accent-soft); }
.search-button { padding: 10px 20px; background: var(--accent); color: #fff; border: none; border-radius: var(--radius); font-size: var(--text-base); font-weight: var(--weight-medium); }
.search-button:hover { background: var(--accent-hover); transform: none; box-shadow: none; }

.reports-table-container { background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-lg); box-shadow: none; }
.reports-table { background: var(--surface); }
.reports-table th, .reports-table td { padding: var(--space-3) var(--space-4); border-bottom: 1px solid var(--border); font-size: var(--text-sm); }
.reports-table th { background: var(--surface-2); color: var(--text-2); font-weight: var(--weight-medium); }
.reports-table td { color: var(--text); }
.reports-table tr:last-child td { border-bottom: none; }
.reports-table tr:hover { background: var(--surface-2); }

.type-product { background: var(--accent-soft); color: var(--accent); }
.type-user { background: var(--info-soft); color: var(--info); }
.type-unknown { background: var(--surface-3); color: var(--text-3); }

.status-badge { padding: 3px 12px; border-radius: var(--radius-full); font-size: var(--text-xs); font-weight: var(--weight-medium); }
.status-pending { background: var(--warning-soft); color: var(--warning); }
.status-handled { background: var(--accent-soft); color: var(--accent); }
.status-rejected { background: var(--surface-3); color: var(--text-3); }
.remark-line { color: var(--text-3); }

.handle-button { background: var(--accent); color: #fff; }
.handle-button:hover { background: var(--accent-hover); }
.done-text { color: var(--text-3); }

.pagination { border-top: 1px solid var(--border); }
.pagination-button { padding: 6px 14px; background: var(--surface); color: var(--text); border: 1px solid var(--border-strong); border-radius: var(--radius-sm); font-size: var(--text-sm); }
.pagination-button:hover:not(:disabled) { background: var(--surface-2); transform: none; }
.pagination-info { font-size: var(--text-sm); color: var(--text-2); }

.loading-spinner { border: 3px solid var(--surface-3); border-top-color: var(--accent); }
.error-container { color: var(--danger); }
.no-reports { color: var(--text-3); }
.retry-button { background: var(--surface); color: var(--text); border: 1px solid var(--border-strong); border-radius: var(--radius-sm); }
.retry-button:hover { background: var(--surface-2); }

.modal-overlay { background: rgba(20, 20, 18, 0.35); }
.modal-content { background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-lg); box-shadow: var(--shadow-md); }
.modal-header { border-bottom: 1px solid var(--border); }
.modal-header h2 { color: var(--text); font-weight: var(--weight-semibold); }
.close-button { color: var(--text-2); border-radius: var(--radius-sm); }
.close-button:hover { background: var(--surface-3); color: var(--text); }
.handle-summary { background: var(--surface-2); border: 1px solid var(--border); }
.detail-label { color: var(--text); font-weight: var(--weight-medium); }
.detail-value { color: var(--text-2); }
.form-label { color: var(--text); }
.required-mark { color: var(--danger); }
.radio-item { border-color: var(--border-strong); color: var(--text); }
.radio-item:hover { background: var(--surface-2); border-color: var(--accent); }
.radio-item input { accent-color: var(--accent); }
.radio-item.is-danger input { accent-color: var(--danger); }
.form-hint { color: var(--text-3); }
.remark-input { border: 1px solid var(--border-strong); color: var(--text); background: var(--surface); }
.remark-input:focus { border-color: var(--accent); box-shadow: 0 0 0 3px var(--accent-soft); }
.field-error { color: var(--danger); }
.modal-footer { border-top: 1px solid var(--border); }
.close-modal-button { background: var(--surface); color: var(--text); border: 1px solid var(--border-strong); border-radius: var(--radius-sm); }
.close-modal-button:hover { background: var(--surface-2); }
.submit-button { background: var(--accent); color: #fff; border-radius: var(--radius-sm); }
.submit-button:hover:not(:disabled) { background: var(--accent-hover); }
</style>
<template>
  <div class="profile-page">
    <div class="animated-bg">
      <div class="gradient-sphere sphere-1"></div>
      <div class="gradient-sphere sphere-2"></div>
      <div class="gradient-sphere sphere-3"></div>
    </div>

    <header class="main-header">
      <div class="header-content">
        <div class="logo-section" @click="go('/')">
          <svg class="header-logo" viewBox="0 0 40 40">
            <defs>
              <linearGradient id="headerLogoGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" style="stop-color:#10b981"/>
                <stop offset="100%" style="stop-color:#3b82f6"/>
              </linearGradient>
            </defs>
            <circle cx="20" cy="20" r="18" fill="url(#headerLogoGrad)"/>
            <path d="M13 20 L18 24 L27 16" stroke="white" stroke-width="3" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span class="brand-name">废旧物品再利用平台</span>
        </div>
        
        <nav class="main-nav">
          <router-link to="/" class="nav-link">首页</router-link>
          <router-link to="/products" class="nav-link">商品</router-link>
          <router-link to="/post" class="nav-link">发布</router-link>
          <router-link to="/messages" class="nav-link">消息</router-link>
          <span class="nav-link active">我的</span>
        </nav>
        
        <div class="header-actions">
          <button class="action-btn search-btn" @click="go('/products')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/>
              <line x1="21" y1="21" x2="16.65" y2="16.65"/>
            </svg>
          </button>
          <button class="action-btn logout-btn" @click="handleLogout" title="退出登录">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
              <polyline points="16 17 21 12 16 7"/>
              <line x1="21" y1="12" x2="9" y2="12"/>
            </svg>
          </button>
        </div>
      </div>
    </header>

    <main class="main-content">
      <div class="profile-header">
        <div class="profile-card">
          <div class="avatar-section">
            <div class="avatar-wrapper">
              <img :src="userInfo.avatar" alt="用户头像" class="avatar" />
              <div class="avatar-ring"></div>
            </div>
            <div class="user-badges">
              <span class="badge verified">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                  <polyline points="22 4 12 14.01 9 11.01"/>
                </svg>
                已认证
              </span>
              <span class="badge level">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                </svg>
                {{ userInfo.level }}
              </span>
            </div>
          </div>
          
          <div class="user-info">
            <h1 class="user-name">{{ userInfo.username }}</h1>
            <p class="user-bio">{{ userInfo.bio || '这个人很懒，什么都没写~' }}</p>
            <div class="user-meta">
              <div class="meta-item">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
                  <circle cx="12" cy="10" r="3"/>
                </svg>
                <span>{{ userInfo.location || '未设置' }}</span>
              </div>
              <div class="meta-item">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
                  <line x1="16" y1="2" x2="16" y2="6"/>
                  <line x1="8" y1="2" x2="8" y2="6"/>
                  <line x1="3" y1="10" x2="21" y2="10"/>
                </svg>
                <span>加入于 {{ userInfo.joinDate || '2025年' }}</span>
              </div>
            </div>
          </div>
          
          <div class="user-stats">
            <div class="stat-item">
              <div class="stat-value">{{ stats.listings }}</div>
              <div class="stat-label">在售</div>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <div class="stat-value">{{ stats.sold }}</div>
              <div class="stat-label">已售</div>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <div class="stat-value">{{ stats.favorites }}</div>
              <div class="stat-label">收藏</div>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <div class="stat-value">{{ stats.views }}</div>
              <div class="stat-label">浏览</div>
            </div>
          </div>
          
          <div class="profile-actions">
            <button class="action-btn-primary" @click="go('/post')">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="5" x2="12" y2="19"/>
                <line x1="5" y1="12" x2="19" y2="12"/>
              </svg>
              发布商品
            </button>
            <button class="action-btn-secondary" @click="editProfile">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
              </svg>
              编辑资料
            </button>
          </div>
        </div>
      </div>

      <div class="profile-tabs">
        <button 
          v-for="tab in tabs" 
          :key="tab.id"
          class="tab-btn"
          :class="{ active: activeTab === tab.id }"
          @click="activeTab = tab.id"
        >
          <component :is="tab.icon" />
          <span>{{ tab.name }}</span>
          <span class="tab-count">{{ tab.count }}</span>
        </button>
      </div>

      <div class="tab-content">
        <div v-if="activeTab === 'listings'" class="listings-grid">
          <div 
            v-for="(item, idx) in myListings" 
            :key="idx" 
            class="listing-card"
            @click="viewProduct(item)"
          >
            <div class="listing-image">
              <img :src="item.image" :alt="item.title" />
              <div class="listing-status" :class="item.status">{{ item.statusText }}</div>
            </div>
            <div class="listing-info">
              <h3 class="listing-title">{{ item.title }}</h3>
              <div class="listing-meta">
                <span class="listing-price">{{ item.price }}</span>
                <span class="listing-views">{{ item.views }} 次浏览</span>
              </div>
              <div class="listing-actions">
                <button class="listing-btn edit" @click.stop="editProduct(item)">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                  </svg>
                </button>
                <button class="listing-btn delete" @click.stop="deleteProduct(item)">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="3 6 5 6 21 6"/>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                  </svg>
                </button>
              </div>
            </div>
          </div>
          <div v-if="myListings.length === 0" class="empty-state">
            <div class="empty-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="9" cy="21" r="1"/>
                <circle cx="20" cy="21" r="1"/>
                <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/>
              </svg>
            </div>
            <h3>还没有发布商品</h3>
            <p>发布你的第一个闲置物品，开启校园交易之旅</p>
            <button class="empty-btn" @click="go('/post')">去发布</button>
          </div>
        </div>

        <div v-if="activeTab === 'favorites'" class="favorites-grid">
          <div 
            v-for="(item, idx) in myFavorites" 
            :key="idx" 
            class="favorite-card"
            :class="{ 'is-unavailable': item.isUnavailable }"
            @click="viewProduct(item)"
          >
            <div class="favorite-image">
              <img v-if="item.image" :src="item.image" :alt="item.title" />
              <span v-else class="favorite-noimg" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <rect x="3" y="3" width="18" height="18" rx="2" />
                  <circle cx="8.5" cy="8.5" r="1.5" /><path d="M21 15l-5-5L5 21" />
                </svg>
              </span>
              <span v-if="item.isDeleted" class="corner-badge">已删除</span>
              <span v-else-if="item.isSold" class="corner-badge">已售出</span>
            </div>
            <div class="favorite-info">
              <h3 class="favorite-title">{{ item.title }}</h3>
              <div class="favorite-price">{{ item.price }}</div>
              <div class="favorite-meta">
                <span class="favorite-seller">卖家 {{ item.seller }}</span>
                <span class="favorite-time">{{ item.time }}</span>
              </div>
            </div>
            <button class="favorite-remove" :aria-label="`取消收藏 ${item.title}`" @click.stop="removeFavorite(item)">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>
          <div v-if="myFavorites.length === 0" class="empty-state">
            <div class="empty-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78l1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
              </svg>
            </div>
            <h3>还没有收藏商品</h3>
            <p>浏览商品时点击收藏，随时关注心仪物品</p>
            <button class="empty-btn" @click="go('/products')">去逛逛</button>
          </div>
        </div>

        <div v-if="activeTab === 'orders'" class="orders-list">
          <div 
            v-for="(order, idx) in myOrders" 
            :key="idx" 
            class="order-card"
          >
            <div class="order-header">
              <div class="order-info">
                <span class="order-id">订单号：{{ order.id }}</span>
                <span class="order-date">{{ order.date }}</span>
              </div>
              <div class="order-status" :class="order.status">{{ order.statusText }}</div>
            </div>
            <div class="order-content">
              <div class="order-image">
                <img :src="order.image" :alt="order.title" />
              </div>
              <div class="order-details">
                <h3 class="order-title">{{ order.title }}</h3>
                <div class="order-meta">
                  <span class="order-seller">卖家：{{ order.seller }}</span>
                </div>
              </div>
              <div class="order-price-section">
                <div class="order-price">{{ order.price }}</div>
                <div class="order-actions">
                  <button v-if="order.statusCode === 0" class="order-btn primary" @click="goPay(order)">去支付</button>
                  <button v-if="order.statusCode === 2" class="order-btn primary" @click="confirmReceive(order)">确认收货</button>
                  <button v-if="order.statusCode === 0" class="order-btn danger" @click="cancelOrderItem(order)">取消订单</button>
                  <button class="order-btn secondary" @click="contactSeller(order)">{{ order.isBuyer ? '联系卖家' : '联系买家' }}</button>
                </div>
              </div>
            </div>
          </div>
          <div v-if="myOrders.length === 0" class="empty-state">
            <div class="empty-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
                <line x1="16" y1="13" x2="8" y2="13"/>
                <line x1="16" y1="17" x2="8" y2="17"/>
              </svg>
            </div>
            <h3>还没有订单</h3>
            <p>购买商品后，订单会显示在这里</p>
            <button class="empty-btn" @click="go('/products')">去购物</button>
          </div>
        </div>

        <div v-if="activeTab === 'history'" class="history-list">
          <div v-if="browseHistory.length > 0" class="history-toolbar">
            <span class="history-count">共 {{ browseHistory.length }} 条，最多保留 50 条</span>
            <button type="button" class="history-clear" @click="clearBrowseHistory">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                   aria-hidden="true"><path d="M3 6h18M8 6V4h8v2M6 6l1 14h10l1-14" /></svg>
              清空历史
            </button>
          </div>
          <div 
            v-for="(item, idx) in browseHistory" 
            :key="idx" 
            class="history-card"
            @click="viewProduct(item)"
          >
            <div class="history-image">
              <img v-if="item.image" :src="item.image" :alt="item.title" />
              <span v-else class="history-noimg" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <rect x="3" y="3" width="18" height="18" rx="2" />
                  <circle cx="8.5" cy="8.5" r="1.5" /><path d="M21 15l-5-5L5 21" />
                </svg>
              </span>
            </div>
            <div class="history-info">
              <h3 class="history-title">{{ item.title }}</h3>
              <div class="history-meta">
                <span class="history-price">{{ item.price }}</span>
                <span v-if="item.isSold" class="history-tag">已售出</span>
                <span v-if="item.isDeleted" class="history-tag">已删除</span>
                <span class="history-time">{{ item.time }}</span>
              </div>
            </div>
          </div>
          <div v-if="browseHistory.length === 0" class="empty-state">
            <div class="empty-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <polyline points="12 6 12 12 16 14"/>
              </svg>
            </div>
            <h3>浏览记录为空</h3>
            <p>浏览商品时会自动记录</p>
            <button class="empty-btn" @click="go('/products')">去逛逛</button>
          </div>
        </div>

        <div v-if="activeTab === 'drafts'" class="drafts-list">
          <div 
            v-for="(draft, idx) in myDrafts" 
            :key="idx" 
            class="draft-card"
            @click="editDraft(draft)"
          >
            <div class="draft-image">
              <img v-if="draft.images && draft.images.length > 0" :src="draft.images[0]" :alt="draft.title" />
              <div v-else class="draft-placeholder">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                  <circle cx="8.5" cy="8.5" r="1.5"/>
                  <polyline points="21 15 16 10 5 21"/>
                </svg>
              </div>
              <span v-if="draft.images && draft.images.length > 1" class="draft-image-count">{{ draft.images.length }}</span>
            </div>
            <div class="draft-info">
              <h3 class="draft-title">{{ draft.title || '未命名商品' }}</h3>
              <div class="draft-meta">
                <span class="draft-category">{{ getCategoryName(draft.category) }}</span>
                <span class="draft-condition">{{ draft.condition }}</span>
              </div>
              <div class="draft-price" v-if="draft.price">¥{{ draft.price }}</div>
              <div class="draft-date">保存于 {{ formatDate(draft.savedAt) }}</div>
            </div>
            <div class="draft-actions">
              <button class="draft-btn edit" @click.stop="editDraft(draft)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                </svg>
                编辑
              </button>
              <button class="draft-btn delete" @click.stop="deleteDraft(idx)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="3 6 5 6 21 6"/>
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                </svg>
                删除
              </button>
            </div>
          </div>
          <div v-if="myDrafts.length === 0" class="empty-state">
            <div class="empty-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
                <line x1="16" y1="13" x2="8" y2="13"/>
                <line x1="16" y1="17" x2="8" y2="17"/>
              </svg>
            </div>
            <h3>草稿箱为空</h3>
            <p>发布商品时可以选择保存草稿，稍后继续编辑</p>
            <button class="empty-btn" @click="go('/post')">去发布</button>
          </div>
        </div>
      </div>
    </main>

    <footer class="main-footer">
      <div class="footer-content">
        <div class="footer-section">
          <div class="footer-logo">
            <svg viewBox="0 0 40 40">
              <circle cx="20" cy="20" r="18" fill="rgba(255,255,255,0.2)"/>
              <path d="M13 20 L18 24 L27 16" stroke="white" stroke-width="3" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span>废旧物品再利用平台</span>
          </div>
          <p class="footer-desc">废旧物品再利用平台，让闲置物品重新发挥价值</p>
        </div>
        <div class="footer-section">
          <h4>快速链接</h4>
          <div class="footer-links">
            <a href="#" @click.prevent="go('/')">首页</a>
            <a href="#" @click.prevent="go('/products')">商品列表</a>
            <a href="#" @click.prevent="go('/post')">发布商品</a>
          </div>
        </div>
        <div class="footer-section">
          <h4>帮助中心</h4>
          <div class="footer-links">
            <a href="#">常见问题</a>
            <a href="#">交易指南</a>
            <a href="#">联系我们</a>
          </div>
        </div>
      </div>
      <div class="footer-bottom">
        <p>© 2025 废旧物品再利用平台</p>
      </div>
    </footer>

    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showEditModal" class="modal-overlay" @click.self="closeEditModal">
          <div class="edit-modal">
            <div class="modal-header">
              <h2>编辑资料</h2>
              <button class="close-btn" @click="closeEditModal">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"/>
                  <line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
              </button>
            </div>
            
            <div class="modal-body">
              <div class="avatar-upload-section">
                <div class="avatar-upload">
                  <img :src="editForm.avatar" alt="头像" class="avatar-preview" />
                  <div class="avatar-edit-overlay">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>
                      <circle cx="12" cy="13" r="4"/>
                    </svg>
                    <span>更换头像</span>
                  </div>
                  <input type="file" accept="image/*" class="avatar-input" @change="handleAvatarChange" />
                </div>
                <p class="avatar-tip">支持 JPG、PNG 格式，大小不超过 2MB</p>
              </div>
              
              <div class="form-section">
                <div class="form-group">
                  <label>用户名</label>
                  <input 
                    type="text" 
                    v-model="editForm.username" 
                    placeholder="请输入用户名"
                    maxlength="20"
                  />
                  <span class="char-count">{{ editForm.username.length }}/20</span>
                </div>
                
                <div class="form-group">
                  <label>个人简介</label>
                  <textarea 
                    v-model="editForm.bio" 
                    placeholder="介绍一下自己吧..."
                    maxlength="200"
                    rows="3"
                  ></textarea>
                  <span class="char-count">{{ editForm.bio.length }}/200</span>
                </div>
                
                <div class="form-group">
                  <label>所在位置</label>
                  <input 
                    type="text" 
                    v-model="editForm.location" 
                    placeholder="如：XX大学XX校区"
                    maxlength="50"
                  />
                </div>
                
                <div class="form-group">
                  <label>联系QQ</label>
                  <input 
                    type="text" 
                    v-model="editForm.qq" 
                    placeholder="请输入QQ号"
                    maxlength="15"
                  />
                </div>
                
                <div class="form-group">
                  <label>联系微信</label>
                  <input 
                    type="text" 
                    v-model="editForm.wechat" 
                    placeholder="请输入微信号"
                    maxlength="30"
                  />
                </div>
              </div>
            </div>
            
            <div class="modal-footer">
              <button class="btn-cancel" @click="closeEditModal">取消</button>
              <button class="btn-save" @click="saveProfile" :disabled="isSaving">
                <svg v-if="isSaving" class="spinner" viewBox="0 0 24 24">
                  <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" fill="none" stroke-dasharray="31.4 31.4" stroke-linecap="round"/>
                </svg>
                <span v-if="!isSaving">保存</span>
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, h, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { logout } from '../api/auth'
import { updateProfile, getProfile } from '../api/user'
import { getMyProducts, deleteProduct as deleteProductAPI } from '../api/product'
import { getOrderList, confirmOrder, cancelOrder } from '../api/order'
import { getFavoriteList, removeFavoriteById, getHistoryList, clearHistory } from '../api/favorites'

const router = useRouter()
function go(path) { router.push(path) }

const activeTab = ref('listings')
const loading = ref(true)

const userInfo = ref({
  username: localStorage.getItem('username') || '用户',
  avatar: 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"%3E%3Ccircle cx="20" cy="20" r="20" fill="%23333"/%3E%3Ccircle cx="20" cy="16" r="6" fill="%23666"/%3E%3Cpath d="M8 36c0-6.6 5.4-12 12-12s12 5.4 12 12" fill="%23666"/%3E%3C/svg%3E',
  bio: '',
  location: '校园',
  qq: '',
  wechat: '',
  joinDate: '2025年1月',
  level: '青铜用户'
})

const stats = ref({
  listings: 0,
  sold: 0,
  favorites: 0,
  views: 0
})

const ShoppingBag = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('path', { d: 'M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z' }),
      h('line', { x1: '3', y1: '6', x2: '21', y2: '6' }),
      h('path', { d: 'M16 10a4 4 0 0 1-8 0' })
    ])
  }
}

const Heart = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('path', { d: 'M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78l1.06-1.06a5.5 5.5 0 0 0 0-7.78z' })
    ])
  }
}

const List = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('line', { x1: '8', y1: '6', x2: '21', y2: '6' }),
      h('line', { x1: '8', y1: '12', x2: '21', y2: '12' }),
      h('line', { x1: '8', y1: '18', x2: '21', y2: '18' }),
      h('line', { x1: '3', y1: '6', x2: '3.01', y2: '6' }),
      h('line', { x1: '3', y1: '12', x2: '3.01', y2: '12' }),
      h('line', { x1: '3', y1: '18', x2: '3.01', y2: '18' })
    ])
  }
}

const Clock = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('circle', { cx: '12', cy: '12', r: '10' }),
      h('polyline', { points: '12 6 12 12 16 14' })
    ])
  }
}

const File = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('path', { d: 'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z' }),
      h('polyline', { points: '14 2 14 8 20 8' }),
      h('line', { x1: '16', y1: '13', x2: '8', y2: '13' }),
      h('line', { x1: '16', y1: '17', x2: '8', y2: '17' })
    ])
  }
}

const tabs = ref([
  { id: 'listings', name: '我的发布', count: 0, icon: ShoppingBag },
  { id: 'favorites', name: '我的收藏', count: 0, icon: Heart },
  { id: 'orders', name: '我的订单', count: 0, icon: List },
  { id: 'history', name: '浏览历史', count: 0, icon: Clock },
  { id: 'drafts', name: '我的草稿', count: 0, icon: File }
])

const myListings = ref([])
const myFavorites = ref([])
const myOrders = ref([])
const browseHistory = ref([])
const myDrafts = ref([])

/**
 * 当前登录用户 ID。
 *
 * 独立的 userId 键此前全项目从未写入，只能从 user JSON 里取；
 * Login.vue 现已补写 userId，这里仍保留 JSON 兜底以兼容旧会话。
 */
function resolveCurrentUserId() {
  for (const store of [localStorage, sessionStorage]) {
    const direct = parseInt(store.getItem('userId') || '', 10)
    if (Number.isFinite(direct) && direct > 0) return direct
  }
  for (const store of [localStorage, sessionStorage]) {
    try {
      const raw = store.getItem('user')
      if (!raw) continue
      const id = parseInt(JSON.parse(raw)?.id, 10)
      if (Number.isFinite(id) && id > 0) return id
    } catch { /* 忽略解析失败 */ }
  }
  return 0
}

const currentUserId = ref(resolveCurrentUserId())

/**
 * 订单状态映射（后端 orders.status 为 0-4 五态）。
 * cls 复用既有 .order-status 的三个样式变体，避免新增 CSS。
 */
const ORDER_STATUS_MAP = {
  0: { cls: 'pending', text: '待支付' },
  1: { cls: 'pending', text: '待发货' },
  2: { cls: 'pending', text: '待收货' },
  3: { cls: 'completed', text: '已完成' },
  4: { cls: 'cancelled', text: '已取消' }
}

async function loadUserProfile() {
  try {
    const res = await getProfile()
    if (res.code === 200 && res.data) {
      const data = res.data
      userInfo.value.username = data.username || userInfo.value.username
      userInfo.value.avatar = data.avatar || userInfo.value.avatar
      userInfo.value.bio = data.bio || ''
      userInfo.value.location = data.location || '校园'
      userInfo.value.qq = data.qq || ''
      userInfo.value.wechat = data.wechat || ''
    }
  } catch (e) {
    console.error('Failed to load user profile:', e)
  }
}

async function loadMyProducts() {
  try {
    const res = await getMyProducts()
    if (res.code === 200 && res.data) {
      const products = res.data
      myListings.value = products.map(p => ({
        id: p.id,
        title: p.title,
        price: `¥${p.price}`,
        image: p.images ? p.images.split(',')[0] : '/sample/phone.svg',
        // 商品状态 0=下架 1=在售 2=已售出
        status: p.status === 1 ? 'selling' : p.status === 2 ? 'sold' : 'offline',
        statusText: p.status === 1 ? '在售' : p.status === 2 ? '已售' : '已下架',
        views: p.viewCount || 0
      }))
      
      const listingsCount = products.filter(p => p.status === 1).length
      const soldCount = products.filter(p => p.status === 2).length
      const totalViews = products.reduce((sum, p) => sum + (p.viewCount || 0), 0)
      
      stats.value.listings = listingsCount
      stats.value.sold = soldCount
      stats.value.views = totalViews
      
      tabs.value.find(t => t.id === 'listings').count = myListings.value.length
    }
  } catch (e) {
    console.error('Failed to load my products:', e)
  }
}

function loadDrafts() {
  const drafts = JSON.parse(localStorage.getItem('product_drafts') || '[]')
  myDrafts.value = drafts
  const draftsTab = tabs.value.find(t => t.id === 'drafts')
  if (draftsTab) draftsTab.count = drafts.length
}

async function loadOrders() {
  try {
    const res = await getOrderList()
    console.log('订单数据响应:', res)
    if (res.code === 200 && res.data) {
      myOrders.value = res.data.map(order => ({
        id: order.id,
        // statusCode 保留后端原始数字状态，供按钮可见性判断；
        // status 仅是 CSS 类名（pending/completed/cancelled），
        // 0/1/2 三个状态共用 pending，用它判断会导致按钮出现在错误的阶段
statusCode: order.status,
        title: order.productTitle,
        price: `¥${order.price}`,
        image: order.productImage,
        status: ORDER_STATUS_MAP[order.status]?.cls || 'cancelled',
        statusText: ORDER_STATUS_MAP[order.status]?.text || '未知状态',
        seller: order.sellerName,
        // 保留买卖双方 ID：聊天页按 userId 开会话，缺了它就无从发起
        sellerId: order.sellerId,
        buyerId: order.buyerId,
        productId: order.productId,
        // 我是买家则联系卖家，我是卖家则联系买家
        isBuyer: Number(order.buyerId) === Number(currentUserId.value),
        date: new Date(order.createdAt).toLocaleString('zh-CN')
      }))
      
      const ordersTab = tabs.value.find(t => t.id === 'orders')
      if (ordersTab) ordersTab.count = myOrders.value.length
    } else {
      console.error('订单数据格式错误:', res)
    }
  } catch (e) {
    console.error('加载订单失败:', e)
  }
}

async function loadAllData() {
  loading.value = true
  await Promise.all([
    loadUserProfile(),
    loadMyProducts(),
    loadDrafts(),
    loadOrders(),
    loadFavorites(),
    loadBrowseHistory()
  ])
  loading.value = false
}

/** 我的收藏（真实数据） */
async function loadFavorites() {
  try {
    const res = await getFavoriteList()
    if (res.code === 200 && res.data) {
      myFavorites.value = res.data.map(mapFavoriteItem)
      stats.value.favorites = myFavorites.value.length
      const tab = tabs.value.find(t => t.id === 'favorites')
      if (tab) tab.count = myFavorites.value.length
    }
  } catch (error) {
    console.error('加载收藏失败:', error)
  }
}

/** 收藏项字段映射为模板所需结构 */
function mapFavoriteItem(raw) {
  return {
    id: raw.id,
    productId: raw.productId,
    title: raw.title,
    image: raw.image,
    price: typeof raw.price === 'number' ? `¥${raw.price.toFixed(2)}` : `¥${raw.price ?? '0.00'}`,
    seller: raw.sellerName || '',
    time: formatTimeAgo(raw.createTime),
    // 已售出/已下架置灰加角标，但依然展示——收藏时商品在售，
    // 卖出后应看到「已售出」而不是凭空消失
    isSold: raw.status === 2,
    isUnavailable: raw.status !== 1 || raw.deleted,
    isDeleted: !!raw.deleted
  }
}

/** 浏览历史（真实数据） */
async function loadBrowseHistory() {
  try {
    const res = await getHistoryList()
    if (res.code === 200 && res.data) {
      browseHistory.value = res.data.map(raw => ({
        id: raw.id,
        productId: raw.productId,
        title: raw.title,
        image: raw.image,
        price: typeof raw.price === 'number' ? `¥${raw.price.toFixed(2)}` : `¥${raw.price ?? '0.00'}`,
        time: formatTimeAgo(raw.lastViewTime),
        isSold: raw.status === 2,
        isDeleted: !!raw.deleted
      }))
      const tab = tabs.value.find(t => t.id === 'history')
      if (tab) tab.count = browseHistory.value.length
    }
  } catch (error) {
    console.error('加载浏览历史失败:', error)
  }
}

/** 相对时间：刚刚 / x分钟前 / x小时前 / x天前 / 具体日期 */
function formatTimeAgo(value) {
  if (!value) return ''
  const time = new Date(String(value).replace(' ', 'T'))
  if (Number.isNaN(time.getTime())) return String(value).slice(0, 16)
  const diff = Date.now() - time.getTime()
  if (diff < 60_000) return '刚刚'
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)} 分钟前`
  if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)} 小时前`
  if (diff < 2_592_000_000) return `${Math.floor(diff / 86_400_000)} 天前`
  return time.toLocaleDateString('zh-CN')
}

/** 清空浏览历史，需二次确认 */
async function clearBrowseHistory() {
  try {
    await ElMessageBox.confirm('确定要清空全部浏览记录吗？此操作不可恢复。', '清空浏览历史', {
      confirmButtonText: '确定清空',
      cancelButtonText: '再想想',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await clearHistory()
    browseHistory.value = []
    const tab = tabs.value.find(t => t.id === 'history')
    if (tab) tab.count = 0
    ElMessage.success('已清空浏览记录')
  } catch (error) {
    ElMessage.error(error?.cause?.message || error?.message || '清空失败，请稍后重试')
  }
}

function editDraft(draft) {
  localStorage.setItem('product_draft', JSON.stringify(draft))
  router.push('/post')
}

function deleteDraft(index) {
  if (confirm('确定要删除这个草稿吗？')) {
    const drafts = JSON.parse(localStorage.getItem('product_drafts') || '[]')
    drafts.splice(index, 1)
    localStorage.setItem('product_drafts', JSON.stringify(drafts))
    myDrafts.value = drafts
    const draftsTab = tabs.value.find(t => t.id === 'drafts')
    if (draftsTab) draftsTab.count = drafts.length
  }
}

function getCategoryName(category) {
  const categories = {
    'textbooks': '教材',
    'electronics': '电子产品',
    'clothing': '服装',
    'daily': '日用品',
    'sports': '运动器材',
    'books': '书籍',
    'other': '其他'
  }
  return categories[category] || category || '未分类'
}

function formatDate(timestamp) {
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
}

async function handleLogout() {
  try { 
    await logout() 
  } catch {
    console.log('Logout API call failed')
  }
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  router.push('/login')
}

function editProfile() {
  openEditModal()
}

function viewProduct(item) {
  router.push(`/products/${item.id}`)
}

function editProduct(item) {
  router.push(`/post?id=${item.id}`)
}

async function deleteProduct(item) {
  try {
    await deleteProductAPI(item.id)
    const idx = myListings.value.findIndex(p => p.id === item.id)
    if (idx !== -1) myListings.value.splice(idx, 1)
    stats.value.listings--
    alert('删除成功！')
  } catch (error) {
    console.error('删除失败:', error)
    alert('删除失败，请稍后重试')
  }
}

/** 取消收藏（发真实请求，失败不改动本地状态） */
async function removeFavorite(item) {
  try {
    await ElMessageBox.confirm(`确定取消收藏「${item.title}」吗？`, '取消收藏', {
      confirmButtonText: '确定取消',
      cancelButtonText: '再想想',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await removeFavoriteById(item.productId ?? item.id)
    const idx = myFavorites.value.findIndex(p => (p.productId ?? p.id) === (item.productId ?? item.id))
    if (idx !== -1) myFavorites.value.splice(idx, 1)
    stats.value.favorites = Math.max(stats.value.favorites - 1, 0)
    const tab = tabs.value.find(t => t.id === 'favorites')
    if (tab) tab.count = myFavorites.value.length
    ElMessage.success('已取消收藏')
  } catch (error) {
    ElMessage.error(error?.cause?.message || error?.message || '取消收藏失败，请稍后重试')
  }
}

/** 待支付订单跳转收银台 */
function goPay(order) {
  router.push(`/payment/${order.id}`)
}

async function confirmReceive(order) {
  try {
    const res = await confirmOrder(order.id)
    if (res.code === 200) {
      // 同时更新 statusCode，否则按钮可见性判断会停留在旧状态
      order.statusCode = 3
      order.status = ORDER_STATUS_MAP[3].cls
      order.statusText = ORDER_STATUS_MAP[3].text
      alert('确认收货成功！')
    } else {
      alert(res.message || '确认收货失败')
    }
  } catch (error) {
    console.error('确认收货失败:', error)
    alert('确认收货失败，请稍后重试')
  }
}

async function cancelOrderItem(order) {
  if (confirm('确定要取消这个订单吗？')) {
    try {
      const res = await cancelOrder(order.id)
      if (res.code === 200) {
        order.statusCode = 4
        order.status = ORDER_STATUS_MAP[4].cls
        order.statusText = ORDER_STATUS_MAP[4].text
        alert('取消订单成功！')
      } else {
        alert(res.message || '取消订单失败')
      }
    } catch (error) {
      console.error('取消订单失败:', error)
      alert('取消订单失败，请稍后重试')
    }
  }
}

/**
 * 联系订单对方。
 *
 * 此前只传了 `seller`（用户名），而聊天页读的是 `sellerId`，
 * 收到 undefined → parseInt 得 NaN → 查找失败 → 静默无反应。
 * 同时要区分买卖视角：买家联系卖家，卖家联系买家。
 */
function contactSeller(order) {
  const targetId = order.isBuyer ? order.sellerId : order.buyerId
  const targetName = order.isBuyer ? order.seller : (order.buyerName || '买家')
  if (!targetId) {
    ElMessage.warning('无法获取对方信息')
    return
  }
  router.push({
    path: '/messages',
    query: { sellerId: String(targetId), productId: String(order.productId ?? ''), seller: targetName }
  })
}

const showEditModal = ref(false)
const isSaving = ref(false)

const editForm = ref({
  username: '',
  avatar: '',
  bio: '',
  location: '',
  qq: '',
  wechat: ''
})

function openEditModal() {
  editForm.value = {
    username: userInfo.value.username,
    avatar: userInfo.value.avatar,
    bio: userInfo.value.bio || '',
    location: userInfo.value.location || '',
    qq: userInfo.value.qq || '',
    wechat: userInfo.value.wechat || ''
  }
  showEditModal.value = true
}

function closeEditModal() {
  showEditModal.value = false
}

function handleAvatarChange(event) {
  const file = event.target.files[0]
  if (!file) return
  
  if (!file.type.match(/image\/(jpeg|png)/)) {
    alert('请选择 JPG 或 PNG 格式的图片')
    return
  }
  
  if (file.size > 2 * 1024 * 1024) {
    alert('图片大小不能超过 2MB')
    return
  }
  
  const reader = new FileReader()
  reader.onload = (e) => {
    compressImage(e.target.result, 200, 200, 0.8, (compressedDataUrl) => {
      editForm.value.avatar = compressedDataUrl
    })
  }
  reader.readAsDataURL(file)
}

function compressImage(dataUrl, maxWidth, maxHeight, quality, callback) {
  const img = new Image()
  img.onload = () => {
    const canvas = document.createElement('canvas')
    let width = img.width
    let height = img.height
    
    if (width > maxWidth || height > maxHeight) {
      if (width / height > maxWidth / maxHeight) {
        height = Math.round(height * maxWidth / width)
        width = maxWidth
      } else {
        width = Math.round(width * maxHeight / height)
        height = maxHeight
      }
    }
    
    canvas.width = width
    canvas.height = height
    
    const ctx = canvas.getContext('2d')
    ctx.drawImage(img, 0, 0, width, height)
    
    callback(canvas.toDataURL('image/jpeg', quality))
  }
  img.src = dataUrl
}

async function saveProfile() {
  if (!editForm.value.username.trim()) {
    alert('用户名不能为空')
    return
  }
  
  if (editForm.value.username.length < 2) {
    alert('用户名至少2个字符')
    return
  }
  
  isSaving.value = true
  
  try {
    const result = await updateProfile({
      username: editForm.value.username,
      avatar: editForm.value.avatar,
      bio: editForm.value.bio,
      location: editForm.value.location,
      qq: editForm.value.qq,
      wechat: editForm.value.wechat
    })
    
    if (result.code === 200) {
      const data = result.data
      userInfo.value.username = data.username
      userInfo.value.avatar = data.avatar
      userInfo.value.bio = data.bio
      userInfo.value.location = data.location
      userInfo.value.qq = data.qq
      userInfo.value.wechat = data.wechat
      
      localStorage.setItem('username', data.username)
      localStorage.setItem('avatar', data.avatar || '')
      localStorage.setItem('bio', data.bio || '')
      localStorage.setItem('location', data.location || '')
      localStorage.setItem('qq', data.qq || '')
      localStorage.setItem('wechat', data.wechat || '')
      
      showEditModal.value = false
      alert('保存成功！')
    } else {
      alert(result.message || '保存失败')
    }
  } catch (error) {
    console.error('保存失败:', error)
    alert(error.message || '保存失败，请稍后重试')
  } finally {
    isSaving.value = false
  }
}

onMounted(() => {
  loadAllData()
})
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #0f0f23 0%, #1a1a3e 50%, #2d1b4e 100%);
  position: relative;
  overflow-x: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.animated-bg {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}

.gradient-sphere {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.3;
  animation: floatBg 25s ease-in-out infinite;
}

.sphere-1 {
  width: 600px;
  height: 600px;
  background: linear-gradient(135deg, #10b981, #3b82f6);
  top: -300px;
  right: -200px;
}

.sphere-2 {
  width: 500px;
  height: 500px;
  background: linear-gradient(135deg, #8b5cf6, #ec4899);
  bottom: -200px;
  left: -200px;
  animation-delay: -8s;
}

.sphere-3 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #f59e0b, #ef4444);
  top: 40%;
  left: 30%;
  animation-delay: -16s;
}

@keyframes floatBg {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(50px, -50px) scale(1.05); }
  50% { transform: translate(-30px, 30px) scale(0.95); }
  75% { transform: translate(30px, 50px) scale(1.02); }
}

.main-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: rgba(15, 15, 35, 0.8);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 16px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.header-logo {
  width: 40px;
  height: 40px;
}

.brand-name {
  font-size: 22px;
  font-weight: 700;
  background: linear-gradient(135deg, #10b981, #3b82f6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.main-nav {
  display: flex;
  gap: 8px;
}

.nav-link {
  padding: 10px 20px;
  color: rgba(255, 255, 255, 0.7);
  text-decoration: none;
  font-size: 15px;
  font-weight: 500;
  border-radius: 10px;
  transition: all 0.3s ease;
  cursor: pointer;
}

.nav-link:hover {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.1);
}

.nav-link.active {
  color: #10b981;
  background: rgba(16, 185, 129, 0.15);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.action-btn {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  transform: translateY(-2px);
}

.action-btn svg {
  width: 20px;
  height: 20px;
}

.main-content {
  padding-top: 100px;
  max-width: 1200px;
  margin: 0 auto;
  padding-left: 24px;
  padding-right: 24px;
  padding-bottom: 60px;
  position: relative;
  z-index: 1;
}

.profile-header {
  margin-bottom: 40px;
}

.profile-card {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 40px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  display: grid;
  grid-template-columns: 150px 1fr auto auto;
  gap: 40px;
  align-items: center;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.avatar-wrapper {
  position: relative;
  width: 120px;
  height: 120px;
}

.avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
  position: relative;
  z-index: 1;
}

.avatar-ring {
  position: absolute;
  inset: -6px;
  border-radius: 50%;
  background: linear-gradient(135deg, #10b981, #3b82f6, #8b5cf6);
  animation: rotate 4s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.user-badges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
}

.badge {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
}

.badge svg {
  width: 14px;
  height: 14px;
}

.badge.verified {
  background: rgba(16, 185, 129, 0.2);
  color: #10b981;
}

.badge.level {
  background: rgba(245, 158, 11, 0.2);
  color: #f59e0b;
}

.user-info {
  flex: 1;
}

.user-name {
  font-size: 28px;
  font-weight: 700;
  color: #ffffff;
  margin: 0 0 8px 0;
}

.user-bio {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
  margin: 0 0 16px 0;
}

.user-meta {
  display: flex;
  gap: 24px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
}

.meta-item svg {
  width: 16px;
  height: 16px;
}

.user-stats {
  display: flex;
  align-items: center;
  gap: 24px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #ffffff;
}

.stat-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 4px;
}

.stat-divider {
  width: 1px;
  height: 50px;
  background: rgba(255, 255, 255, 0.2);
}

.profile-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-btn-primary, .action-btn-secondary {
  padding: 14px 24px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.3s ease;
}

.action-btn-primary {
  background: linear-gradient(135deg, #10b981, #059669);
  color: white;
  border: none;
}

.action-btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 30px -10px rgba(16, 185, 129, 0.5);
}

.action-btn-secondary {
  background: rgba(255, 255, 255, 0.1);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.action-btn-secondary:hover {
  background: rgba(255, 255, 255, 0.15);
}

.action-btn-primary svg, .action-btn-secondary svg {
  width: 18px;
  height: 18px;
}

.profile-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  padding: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.tab-btn {
  flex: 1;
  padding: 14px 20px;
  border-radius: 12px;
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.3s ease;
}

.tab-btn svg {
  width: 18px;
  height: 18px;
}

.tab-btn:hover {
  color: rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.05);
}

.tab-btn.active {
  background: linear-gradient(135deg, #10b981, #059669);
  color: white;
}

.tab-count {
  background: rgba(255, 255, 255, 0.2);
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}

.tab-btn.active .tab-count {
  background: rgba(255, 255, 255, 0.3);
}

.tab-content {
  min-height: 400px;
}

.listings-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.listing-card {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;
}

.listing-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 20px 40px -10px rgba(0, 0, 0, 0.3);
}

.listing-image {
  position: relative;
  height: 180px;
}

.listing-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.listing-status {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
}

.listing-status.selling {
  background: rgba(16, 185, 129, 0.9);
  color: white;
}

.listing-status.sold {
  background: rgba(107, 114, 128, 0.9);
  color: white;
}

.listing-info {
  padding: 16px;
}

.listing-title {
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
  margin: 0 0 8px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.listing-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.listing-price {
  font-size: 20px;
  font-weight: 700;
  color: #10b981;
}

.listing-views {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.listing-actions {
  display: flex;
  gap: 8px;
}

.listing-btn {
  flex: 1;
  padding: 10px;
  border-radius: 10px;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.listing-btn svg {
  width: 18px;
  height: 18px;
}

.listing-btn.edit {
  background: rgba(59, 130, 246, 0.2);
  color: #3b82f6;
}

.listing-btn.delete {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.listing-btn:hover {
  transform: scale(1.05);
}

.favorites-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.favorite-card {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.favorite-card:hover {
  background: rgba(255, 255, 255, 0.08);
  transform: translateX(5px);
}

.favorite-image {
  width: 80px;
  height: 80px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
}

.favorite-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.favorite-info {
  flex: 1;
  min-width: 0;
}

.favorite-title {
  font-size: 15px;
  font-weight: 600;
  color: #ffffff;
  margin: 0 0 6px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.favorite-price {
  font-size: 18px;
  font-weight: 700;
  color: #10b981;
  margin-bottom: 6px;
}

.favorite-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.favorite-remove {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.3s ease;
}

.favorite-card:hover .favorite-remove {
  opacity: 1;
}

.favorite-remove svg {
  width: 16px;
  height: 16px;
}

.orders-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.order-info {
  display: flex;
  gap: 16px;
}

.order-id {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

.order-date {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
}

.order-status {
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
}

.order-status.pending {
  background: rgba(245, 158, 11, 0.2);
  color: #f59e0b;
}

.order-status.completed {
  background: rgba(16, 185, 129, 0.2);
  color: #10b981;
}

.order-content {
  display: flex;
  gap: 16px;
  align-items: center;
}

.order-image {
  width: 80px;
  height: 80px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
}

.order-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.order-details {
  flex: 1;
  min-width: 0;
}

.order-title {
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
  margin: 0 0 8px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.order-meta {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

.order-price-section {
  text-align: right;
}

.order-price {
  font-size: 22px;
  font-weight: 700;
  color: #10b981;
  margin-bottom: 12px;
}

.order-actions {
  display: flex;
  gap: 8px;
}

.order-btn {
  padding: 8px 16px;
  border-radius: 8px;
  border: none;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.order-btn.primary {
  background: linear-gradient(135deg, #10b981, #059669);
  color: white;
}

.order-btn.secondary {
  background: rgba(255, 255, 255, 0.1);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.order-btn.danger {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: white;
}

.order-btn:hover {
  transform: translateY(-2px);
}

.history-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.history-card {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;
}

.history-card:hover {
  background: rgba(255, 255, 255, 0.08);
  transform: translateY(-2px);
}

.history-image {
  width: 80px;
  height: 80px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
}

.history-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.history-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.history-title {
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  margin: 0 0 8px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.history-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.history-price {
  font-size: 16px;
  font-weight: 700;
  color: #10b981;
}

.history-time {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.drafts-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.draft-card {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;
}

.draft-card:hover {
  background: rgba(255, 255, 255, 0.08);
  transform: translateY(-2px);
}

.draft-image {
  width: 100px;
  height: 100px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
  position: relative;
  background: rgba(255, 255, 255, 0.05);
}

.draft-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.draft-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.3);
}

.draft-placeholder svg {
  width: 40px;
  height: 40px;
}

.draft-image-count {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}

.draft-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.draft-title {
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
  margin: 0 0 8px 0;
}

.draft-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.draft-category, .draft-condition {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.7);
}

.draft-price {
  font-size: 18px;
  font-weight: 700;
  color: #10b981;
  margin-bottom: 4px;
}

.draft-date {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.draft-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  justify-content: center;
}

.draft-btn {
  padding: 8px 16px;
  border-radius: 8px;
  border: none;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.3s ease;
}

.draft-btn.edit {
  background: rgba(59, 130, 246, 0.2);
  color: #3b82f6;
}

.draft-btn.delete {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.draft-btn:hover {
  transform: scale(1.05);
}

.draft-btn svg {
  width: 16px;
  height: 16px;
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
}

.empty-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 24px;
  color: rgba(255, 255, 255, 0.2);
}

.empty-icon svg {
  width: 100%;
  height: 100%;
}

.empty-state h3 {
  font-size: 20px;
  font-weight: 600;
  color: #ffffff;
  margin: 0 0 8px 0;
}

.empty-state p {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0 0 24px 0;
}

.empty-btn {
  padding: 12px 28px;
  background: linear-gradient(135deg, #10b981, #059669);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.empty-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 30px -10px rgba(16, 185, 129, 0.5);
}

.main-footer {
  background: rgba(0, 0, 0, 0.3);
  padding: 40px 0 20px;
  position: relative;
  z-index: 1;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  gap: 40px;
}

.footer-section h4 {
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
  margin: 0 0 16px 0;
}

.footer-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.footer-logo svg {
  width: 40px;
  height: 40px;
}

.footer-logo span {
  font-size: 20px;
  font-weight: 700;
  color: #ffffff;
}

.footer-desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
  line-height: 1.6;
}

.footer-links {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.footer-links a {
  color: rgba(255, 255, 255, 0.6);
  text-decoration: none;
  font-size: 14px;
  transition: all 0.3s ease;
}

.footer-links a:hover {
  color: #10b981;
}

.footer-bottom {
  max-width: 1200px;
  margin: 40px auto 0;
  padding: 20px 24px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  text-align: center;
}

.footer-bottom p {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.4);
  margin: 0;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(10px);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.edit-modal {
  background: rgba(26, 26, 62, 0.95);
  backdrop-filter: blur(30px);
  border-radius: 24px;
  width: 100%;
  max-width: 520px;
  max-height: 90vh;
  overflow-y: auto;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.modal-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #ffffff;
  margin: 0;
}

.close-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  color: #ffffff;
}

.close-btn svg {
  width: 20px;
  height: 20px;
}

.modal-body {
  padding: 24px;
}

.avatar-upload-section {
  text-align: center;
  margin-bottom: 32px;
}

.avatar-upload {
  position: relative;
  width: 120px;
  height: 120px;
  margin: 0 auto 12px;
  cursor: pointer;
}

.avatar-preview {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-edit-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.3s ease;
  color: white;
}

.avatar-upload:hover .avatar-edit-overlay {
  opacity: 1;
}

.avatar-edit-overlay svg {
  width: 32px;
  height: 32px;
  margin-bottom: 4px;
}

.avatar-edit-overlay span {
  font-size: 12px;
}

.avatar-input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.avatar-tip {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
}

.form-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  position: relative;
}

.form-group label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 8px;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  color: #ffffff;
  font-size: 15px;
  outline: none;
  transition: all 0.3s ease;
  box-sizing: border-box;
}

.form-group input:focus,
.form-group textarea:focus {
  border-color: #10b981;
  background: rgba(16, 185, 129, 0.05);
}

.form-group textarea {
  resize: none;
}

.char-count {
  position: absolute;
  right: 12px;
  bottom: 10px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
}

.modal-footer {
  display: flex;
  gap: 12px;
  padding: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.btn-cancel,
.btn-save {
  flex: 1;
  padding: 14px 24px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-cancel {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.btn-cancel:hover {
  background: rgba(255, 255, 255, 0.15);
}

.btn-save {
  background: linear-gradient(135deg, #10b981, #059669);
  color: white;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.btn-save:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 30px -10px rgba(16, 185, 129, 0.5);
}

.btn-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.spinner {
  width: 20px;
  height: 20px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .edit-modal,
.modal-leave-to .edit-modal {
  transform: scale(0.95);
  opacity: 0;
}

@media (max-width: 1024px) {
  .profile-card {
    grid-template-columns: 120px 1fr;
    gap: 24px;
  }

  .user-stats {
    grid-column: 1 / -1;
    justify-content: space-around;
  }

  .profile-actions {
    grid-column: 1 / -1;
    flex-direction: row;
    justify-content: center;
  }

  .footer-content {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 768px) {
  .header-content {
    padding: 12px 16px;
  }

  .main-nav {
    display: none;
  }

  .main-content {
    padding-top: 80px;
    padding-left: 16px;
    padding-right: 16px;
  }

  .profile-card {
    padding: 24px;
    grid-template-columns: 1fr;
    text-align: center;
  }

  .avatar-section {
    align-items: center;
  }

  .user-meta {
    justify-content: center;
    flex-wrap: wrap;
  }

  .user-stats {
    flex-wrap: wrap;
    justify-content: center;
    gap: 16px;
  }

  .stat-divider {
    display: none;
  }

  .profile-tabs {
    flex-wrap: wrap;
  }

  .tab-btn {
    padding: 10px 16px;
    font-size: 13px;
  }

  .listings-grid,
  .favorites-grid,
  .history-list,
  .drafts-list {
    grid-template-columns: 1fr;
  }

  .footer-content {
    grid-template-columns: 1fr;
    text-align: center;
  }

  .footer-logo {
    justify-content: center;
  }

  .order-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .order-price-section {
    width: 100%;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .draft-card {
    flex-direction: column;
  }

  .draft-image {
    width: 100%;
    height: 160px;
  }

  .draft-actions {
    flex-direction: row;
  }
}
</style>

<!-- ===== 简约浅色主题覆盖（Design Tokens） ===== -->
<style scoped>
.profile-page { background: var(--bg); font-family: var(--font-sans); }
.animated-bg { display: none; }

.main-header { position: sticky; background: rgba(255,255,255,0.85); backdrop-filter: saturate(180%) blur(12px); border-bottom: 1px solid var(--border); }
.header-content { max-width: var(--container); height: var(--header-h); padding: 0 var(--space-6); }
.header-logo { width: 30px; height: 30px; }
.brand-name { font-size: var(--text-lg); font-weight: var(--weight-semibold); background: none; -webkit-text-fill-color: currentColor; color: var(--text); letter-spacing: -0.01em; }
.main-nav { gap: var(--space-1); }
.nav-link { padding: 8px 12px; color: var(--text-2); border-radius: var(--radius-sm); }
.nav-link:hover { color: var(--text); background: var(--surface-3); }
.nav-link.active { color: var(--accent); background: var(--accent-soft); }
.action-btn { width: 38px; height: 38px; border-radius: var(--radius); background: transparent; color: var(--text-2); border: 1px solid transparent; }
.action-btn:hover { background: var(--surface-3); color: var(--text); transform: none; }

.main-content { max-width: var(--container); padding: calc(var(--header-h) + var(--space-6)) var(--space-6) var(--space-16); }

.profile-card { background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-lg); padding: var(--space-8); gap: var(--space-8); }
.avatar { width: 96px; height: 96px; border: 1px solid var(--border); }
.avatar-wrapper { width: 96px; height: 96px; }
.avatar-ring { inset: -4px; background: none; border: 2px solid var(--accent-soft-strong); animation: none; }
.badge { border-radius: var(--radius-full); }
.badge.verified { background: var(--accent-soft); color: var(--accent); }
.badge.level { background: var(--warning-soft); color: var(--warning); }
.user-name { font-size: var(--text-2xl); color: var(--text); font-weight: var(--weight-semibold); letter-spacing: -0.02em; }
.user-bio { color: var(--text-2); }
.meta-item { color: var(--text-2); }
.meta-item svg { color: var(--text-3); }
.stat-value { color: var(--text); font-size: var(--text-2xl); font-weight: var(--weight-semibold); }
.stat-label { color: var(--text-2); }
.stat-divider { background: var(--border); }

.action-btn-primary { background: var(--accent); color: #fff; border-radius: var(--radius); font-weight: var(--weight-medium); }
.action-btn-primary:hover { background: var(--accent-hover); box-shadow: none; transform: none; }
.action-btn-secondary { background: var(--surface); color: var(--text); border: 1px solid var(--border-strong); border-radius: var(--radius); font-weight: var(--weight-medium); }
.action-btn-secondary:hover { background: var(--surface-2); }

.profile-tabs { background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius); padding: var(--space-2); gap: var(--space-2); }
.tab-btn { color: var(--text-2); border-radius: var(--radius-sm); font-weight: var(--weight-medium); }
.tab-btn:hover { color: var(--text); background: var(--surface-2); }
.tab-btn.active { background: var(--accent); color: #fff; }
.tab-count { background: var(--surface-3); color: var(--text-2); }
.tab-btn.active .tab-count { background: rgba(255,255,255,0.25); color: #fff; }

.listing-card, .favorite-card, .order-card, .history-card, .draft-card { background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-lg); box-shadow: none; }
.listing-card:hover, .history-card:hover, .draft-card:hover { border-color: var(--border-strong); box-shadow: var(--shadow-sm); transform: none; }
.listing-title, .favorite-title, .order-title, .history-title, .draft-title, .empty-state h3 { color: var(--text); }
.listing-price, .favorite-price, .order-price, .history-price, .draft-price { color: var(--accent); }
.listing-views, .favorite-meta, .order-meta, .order-id, .order-date, .history-time, .draft-date, .empty-state p { color: var(--text-2); }
.listing-status.selling { background: var(--accent-soft); color: var(--accent); }
.listing-status.sold { background: var(--surface-3); color: var(--text-2); }
.listing-status.offline { background: var(--warning-soft); color: var(--warning); }
.listing-btn.edit { background: var(--info-soft); color: var(--info); }
.listing-btn.delete { background: var(--danger-soft); color: var(--danger); }
.listing-btn:hover { transform: none; }
.favorite-remove { background: var(--danger-soft); color: var(--danger); }
.order-status.pending { background: var(--warning-soft); color: var(--warning); }
.order-status.completed { background: var(--success-soft); color: var(--success); }
.order-status.cancelled { background: var(--danger-soft); color: var(--danger); }
.order-btn.primary, .empty-btn { background: var(--accent); color: #fff; border-radius: var(--radius-sm); }
.order-btn.primary:hover, .empty-btn:hover { background: var(--accent-hover); box-shadow: none; transform: none; }
.order-btn.secondary { background: var(--surface); color: var(--text); border: 1px solid var(--border-strong); }
.order-btn.danger { background: var(--danger); color: #fff; }
.order-btn:hover { transform: none; }
.draft-category, .draft-condition { background: var(--surface-3); color: var(--text-2); }
.draft-btn.edit { background: var(--info-soft); color: var(--info); }
.draft-btn.delete { background: var(--danger-soft); color: var(--danger); }
.draft-btn:hover { transform: none; }
.empty-icon { color: var(--text-3); opacity: 0.5; }

.main-footer { background: var(--surface); border-top: 1px solid var(--border); }
.footer-content { max-width: var(--container); }
.footer-section h4, .footer-logo span { color: var(--text); }
.footer-desc, .footer-links a, .footer-bottom p { color: var(--text-2); }
.footer-links a:hover { color: var(--accent); }
.footer-bottom { border-top: 1px solid var(--border); }

.modal-overlay { background: rgba(20,20,18,0.35); }
.edit-modal { background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-lg); backdrop-filter: none; }
.modal-header { border-bottom: 1px solid var(--border); }
.modal-header h2 { color: var(--text); }
.close-btn { background: var(--surface-3); color: var(--text-2); border-radius: var(--radius-sm); }
.close-btn:hover { background: var(--surface-2); color: var(--text); }
.form-group label { color: var(--text-2); }
.form-group input, .form-group textarea { background: var(--surface); border: 1px solid var(--border-strong); color: var(--text); border-radius: var(--radius); }
.modal-footer { border-top: 1px solid var(--border); }
.btn-cancel { background: var(--surface); border: 1px solid var(--border-strong); color: var(--text); }
.btn-save { background: var(--accent); color: #fff; }

/* ============ 收藏 / 浏览历史 真实数据样式 ============ */
/* 已售出/已下架/已删除：置灰但保留可见，让用户知道商品发生过什么 */
.favorite-card.is-unavailable .favorite-image,
.favorite-card.is-unavailable .favorite-info {
  opacity: 0.55;
  filter: grayscale(0.7);
}
.favorite-noimg,
.history-noimg {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  min-height: 96px;
  color: #9a9a92;
}
.favorite-noimg svg,
.history-noimg svg { width: 32px; height: 32px; }
.corner-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 2px 8px;
  font-size: 12px;
  font-weight: 500;
  color: #fff;
  background: #6b6b64;
  border-radius: 999px;
}
.corner-badge.sold { background: #c0392b; }
.history-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.history-count { font-size: 13px; color: #9a9a92; }
.history-clear {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  font-size: 13px;
  color: #6b6b64;
  background: transparent;
  border: 1px solid #d8d8d2;
  border-radius: 8px;
  cursor: pointer;
  transition: color 150ms ease, border-color 150ms ease, background 150ms ease;
}
.history-clear svg { width: 14px; height: 14px; }
.history-clear:hover {
  color: #c0392b;
  border-color: #c0392b;
  background: rgba(192, 57, 43, 0.06);
}
.history-tag {
  padding: 1px 6px;
  font-size: 12px;
  color: #c0392b;
  background: rgba(192, 57, 43, 0.1);
  border-radius: 4px;
}</style>
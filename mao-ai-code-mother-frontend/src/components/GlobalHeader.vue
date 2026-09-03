<template>
  <div class="app-layout">
    <aside class="sider" :class="{ collapsed }">
      <!-- 顶部 Logo 区域 -->
      <div class="sider-header-bar">
        <div class="header-left-wrap">
          <img class="logo-image" src="/logo.png" alt="Logo" @click="expandSider" />
          <span v-if="!collapsed" class="logo-text">元知AI</span>
        </div>
        <button v-if="!collapsed" class="layout-toggle-btn" @click="collapsed = true">
          <svg class="columns-icon" viewBox="0 0 24 24" fill="none" stroke="#444" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="3" width="18" height="18" rx="2" />
            <line x1="12" y1="3" x2="12" y2="21" />
          </svg>
        </button>
      </div>

      <!-- 菜单区域 -->
      <nav class="sider-menu">
        <!-- 菜单列表 -->
        <ul>
          <li
            v-for="item in filteredMenuItems"
            :key="item.key"
            :class="{ active: selectedKeys.includes(item.key) }"
            @click="handleMenuClick(item.key)"
          >
            <span class="icon">{{ item.icon }}</span>
            <span v-if="!collapsed">{{ item.label }}</span>
          </li>
        </ul>
      </nav>

      <!-- 底部区域 -->
      <div class="sider-footer">
        <template v-if="!collapsed">
          <div v-if="loginUserStore.loginUser.id" class="user-info">
            <div class="user-dropdown-wrap">
              <div class="user-dropdown-trigger" @click="toggleUserDropdown">
                <div class="user-avatar-circle">
                  {{ loginUserStore.loginUser.userName?.charAt(0) || 'U' }}
                </div>
                <span class="username">{{ loginUserStore.loginUser.userName ?? '无名' }}</span>
              </div>
              <div v-show="userDropdownOpen" class="user-dropdown-panel" @click.stop>
                <!-- 用户信息区 -->
                <div class="user-info-section">
                  <div class="user-avatar-large">
                    {{ loginUserStore.loginUser.userName?.charAt(0) || 'U' }}
                  </div>
                  <div class="user-detail">
                    <div class="user-phone-row">
                      <span class="user-phone">{{ maskPhone(loginUserStore.loginUser.userPhone || loginUserStore.loginUser.userAccount) }}</span>
                      <span class="edit-icon" title="编辑" @click.stop="editProfile">✎</span>
                    </div>
                    <div class="user-id-row">
                      <span class="user-id-label">ID：</span>
                      <span class="user-id">user-{{ loginUserStore.loginUser.id }}</span>
                      <span class="copy-icon" title="复制" @click.stop="copyUserId">⧉</span>
                    </div>
                  </div>
                </div>
                <div class="menu-divider"></div>
                <!-- 菜单项 -->
                <div class="menu-list">
                  <div class="menu-item" @click.stop="handleUserMenuClick('profile')">
                    <span class="menu-icon">👤</span>
                    <span class="menu-text">个人主页</span>
                  </div>
                  <div class="menu-item" @click.stop="handleUserMenuClick('theme')">
                    <span class="menu-icon">🎨</span>
                    <span class="menu-text">主题设置</span>
                    <span class="menu-arrow">›</span>
                  </div>
                </div>
                <div class="menu-divider"></div>
                <div class="menu-list">
                  <div class="menu-item" @click.stop="handleUserMenuClick('help')">
                    <span class="menu-icon">📖</span>
                    <span class="menu-text">帮助文档</span>
                  </div>
                  <div class="menu-item" @click.stop="handleUserMenuClick('tutorial')">
                    <span class="menu-icon">▶️</span>
                    <span class="menu-text">教学视频</span>
                  </div>
                  <div class="menu-item" @click.stop="handleUserMenuClick('training')">
                    <span class="menu-icon">🎓</span>
                    <span class="menu-text">秒哒实训营</span>
                  </div>
                </div>
                <div class="menu-divider"></div>
                <div class="menu-list">
                  <div class="menu-item" @click.stop="handleUserMenuClick('skill')">
                    <span class="menu-icon">🤖</span>
                    <span class="menu-text">秒哒 Skill</span>
                  </div>
                  <div class="menu-item" @click.stop="handleUserMenuClick('invite')">
                    <span class="menu-icon">🎟️</span>
                    <span class="menu-text">绑定邀请码</span>
                  </div>
                  <div class="menu-item">
                    <span class="menu-icon">🎧</span>
                    <span class="menu-text">联系我们</span>
                    <div class="contact-icons">
                      <span class="contact-icon xhs" title="小红书">📕</span>
                      <span class="contact-icon wechat" title="微信">💬</span>
                      <span class="contact-icon other" title="其他">👤</span>
                    </div>
                  </div>
                </div>
                <div class="menu-divider"></div>
                <div class="menu-list">
                  <div class="menu-item logout-item" @click.stop="handleUserMenuClick('logout')">
                    <span class="menu-icon">🚪</span>
                    <span class="menu-text">退出登录</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <button v-else class="footer-login-btn" @click="router.push('/user/login')">登录</button>
        </template>
        <button class="footer-bell-btn">
          <svg class="bell-icon-svg" viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
            <path d="M13.73 21a2 2 0 0 1-3.46 0" />
          </svg>
        </button>
      </div>
    </aside>

    <!-- 右侧主内容区 -->
    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { userLogout } from '@/api/userController.ts'
import { adminMenuItems } from '@/config/adminMenu'

const loginUserStore = useLoginUserStore()
const router = useRouter()

const collapsed = ref(false)
const selectedKeys = ref<string[]>(['/'])
const userDropdownOpen = ref(false)

// 监听路由变化，更新当前选中菜单
router.afterEach((to) => {
  selectedKeys.value = [to.path]
})

// 菜单配置项
const menuItems = [
  { key: '/', icon: '👤', label: '首页' },
  { key: '/app', icon: '📱', label: '项目' },
  { key: '/skills', icon: '🏠', label: '技能中心' },
  { key: '/materials', icon: '👤', label: '素材库' },
  { key: '/community', icon: '📱', label: '社区' },
  // 后台入口统一从 config/adminMenu.ts 读取（排除仪表盘，那是后台首页由后台侧边栏承载）
  ...adminMenuItems
    .filter((item) => item.path !== '/admin/dashboard')
    .map((item) => ({ ...item, key: item.path, adminOnly: true })),
]

// 过滤菜单项（管理员菜单仅管理员可见）
const filteredMenuItems = menuItems.filter((item) => {
  if (item.adminOnly) {
    const loginUser = loginUserStore.loginUser
    if (!loginUser || loginUser.userRole !== 'admin') {
      return false
    }
  }
  return true
})

// 处理菜单点击
const handleMenuClick = (key: string) => {
  selectedKeys.value = [key]
  if (key.startsWith('/')) {
    router.push(key)
  }
}

// 展开侧边栏
const expandSider = () => {
  if (collapsed.value) {
    collapsed.value = false
  }
}

// 切换用户下拉菜单
const toggleUserDropdown = () => {
  userDropdownOpen.value = !userDropdownOpen.value
}

// 关闭用户下拉
const closeUserDropdown = () => {
  userDropdownOpen.value = false
}

// 处理用户下拉菜单项点击
const handleUserMenuClick = async (type: string) => {
  closeUserDropdown()
  switch (type) {
    case 'profile':
      message.info('个人主页功能开发中')
      break
    case 'theme':
      message.info('主题设置功能开发中')
      break
    case 'help':
      message.info('帮助文档功能开发中')
      break
    case 'tutorial':
      message.info('教学视频功能开发中')
      break
    case 'training':
      message.info('秒哒实训营功能开发中')
      break
    case 'skill':
      router.push('/skills')
      break
    case 'invite':
      message.info('绑定邀请码功能开发中')
      break
    case 'logout':
      await doLogout()
      break
  }
}

// 编辑资料
const editProfile = () => {
  message.info('编辑资料功能开发中')
}

// 退出登录
const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}

// 账号脱敏：手机号 / 邮箱 / 用户名分别处理，避免把邮箱脱敏成乱码
const maskPhone = (val?: string) => {
  if (!val) return '****'
  // 手机号：138****8888
  if (/^1[3-9]\d{9}$/.test(val)) {
    return val.slice(0, 3) + '****' + val.slice(-4)
  }
  // 邮箱：ab***@domain.com
  if (val.includes('@')) {
    const [name, domain] = val.split('@')
    const masked = name.length <= 2 ? name[0] + '***' : name.slice(0, 2) + '***' + name.slice(-1)
    return masked + '@' + domain
  }
  // 用户名：a***z
  if (val.length <= 1) return val
  return val.slice(0, 1) + '***' + val.slice(-1)
}

// 复制用户ID
const copyUserId = () => {
  const id = `user-${loginUserStore.loginUser.id}`
  navigator.clipboard.writeText(id).then(() => {
    message.success('ID已复制')
  }).catch(() => {
    message.error('复制失败')
  })
}

// 监听 collapsed 变化，关闭下拉面板
watch(collapsed, (val) => {
  if (val) {
    userDropdownOpen.value = false
  }
})

// 点击外部关闭下拉
const handleClickOutside = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  const dropdownWrap = document.querySelector('.user-dropdown-wrap')
  if (dropdownWrap && !dropdownWrap.contains(target)) {
    userDropdownOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.app-layout {
  display: flex;
  height: 100vh;
}

/* ---------- 左侧边栏 ---------- */
.sider {
  width: 240px;
  background: #ffffff;
  border-right: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  height: 100vh;
  position: sticky;
  top: 0;
  flex-shrink: 0;
  transition: width 0.3s ease;
}

.sider.collapsed {
  width: 64px;
}

/* ---------- 顶部 Logo 栏 ---------- */
.sider-header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  height: 64px;
  padding: 0 16px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
  transition: all 0.3s ease;
}

.sider.collapsed .sider-header-bar {
  justify-content: center;
  padding: 0;
}

.header-left-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logo-image {
  width: 32px;
  height: 32px;
  object-fit: contain;
  border-radius: 4px;
  flex-shrink: 0;
  cursor: pointer;
}

.sider.collapsed .logo-image {
  cursor: pointer;
}

.logo-text {
  font-size: 18px;
  font-weight: bold;
  color: #001529;
  white-space: nowrap;
  transition: opacity 0.3s ease, width 0.3s ease;
}

.sider.collapsed .logo-text {
  opacity: 0;
  width: 0;
  overflow: hidden;
}

.layout-toggle-btn {
  width: 32px;
  height: 32px;
  border-radius: 4px;
  background: transparent;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.layout-toggle-btn:hover {
  background: #f5f5f5;
}

.columns-icon {
  width: 20px;
  height: 20px;
}

/* ---------- 菜单区域 ---------- */
.sider-menu {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.sider.collapsed .sider-menu {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.sider-menu ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.sider-menu li {
  padding: 0 16px;
  line-height: 40px;
  cursor: pointer;
  transition: background 0.2s;
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.sider-menu li:hover {
  background: #f5f5f5;
}

.sider-menu li.active {
  background: #e6f7ff;
  color: #1890ff;
}

.sider-menu li .icon {
  font-size: 16px;
  width: 20px;
  text-align: center;
  flex-shrink: 0;
}

.sider.collapsed .sider-menu li {
  padding: 0;
  justify-content: center;
  width: 48px;
  height: 40px;
}

/* ---------- 底部区域 ---------- */
.sider-footer {
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-shrink: 0;
}

.sider.collapsed .sider-footer {
  justify-content: center;
  padding: 12px 0;
}

.footer-login-btn {
  background: #000;
  color: #fff;
  border: none;
  border-radius: 999px;
  padding: 5px 16px;
  font-size: 13px;
  cursor: pointer;
}

.footer-login-btn:hover {
  background: #222;
}

.footer-bell-btn {
  width: 36px;
  height: 36px;
  border-radius: 6px;
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.footer-bell-btn:hover {
  background: #f5f5f5;
}

.bell-icon-svg {
  width: 20px;
  height: 20px;
  stroke: #444;
}

/* ---------- 用户信息 ---------- */
.user-info {
  flex: 1;
  display: flex;
  justify-content: center;
}

.user-dropdown-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.2s;
  width: 100%;
  justify-content: center;
}

.user-dropdown-trigger:hover {
  background: #f5f5f5;
}

.username {
  font-size: 14px;
  color: #333;
}

/* 侧边栏头像圆圈 */
.user-avatar-circle {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #000;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 500;
  flex-shrink: 0;
}

/* 用户下拉容器 */
.user-dropdown-wrap {
  position: relative;
  width: 100%;
}

/* ========== 用户下拉面板 ========== */
.user-dropdown-panel {
  position: absolute;
  bottom: calc(100% + 12px);
  left: 50%;
  transform: translateX(-50%);
  width: 280px;
  background: #fff;
  border-radius: 12px;
  padding: 16px 0;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.12);
  z-index: 1000;
}

.user-info-section {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 16px 12px;
}

.user-avatar-large {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #000;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 500;
  flex-shrink: 0;
}

.user-detail {
  flex: 1;
  min-width: 0;
}

.user-phone-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.user-phone {
  font-size: 18px;
  font-weight: 600;
  color: #1f2329;
}

.edit-icon {
  font-size: 16px;
  color: #868c96;
  cursor: pointer;
  transition: color 0.2s;
}

.edit-icon:hover {
  color: #4056d8;
}

.user-id-row {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 14px;
  color: #868c96;
}

.user-id-label {
  color: #868c96;
}

.user-id {
  color: #4e5969;
  font-family: monospace;
}

.copy-icon {
  font-size: 14px;
  color: #868c96;
  cursor: pointer;
  margin-left: 4px;
  transition: color 0.2s;
}

.copy-icon:hover {
  color: #4056d8;
}

.menu-divider {
  height: 1px;
  background: #f0f0f0;
  margin: 6px 0;
}

.menu-list {
  padding: 4px 0;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.2s;
  font-size: 15px;
  color: #1f2329;
}

.menu-item:hover {
  background: #f7f8fa;
}

.menu-icon {
  font-size: 18px;
  width: 22px;
  text-align: center;
}

.menu-text {
  flex: 1;
}

.menu-arrow {
  font-size: 16px;
  color: #868c96;
}

.logout-item {
  color: #f53f3f;
}

.logout-item:hover {
  background: #fff0f0;
}

/* 联系我们图标 */
.contact-icons {
  display: flex;
  align-items: center;
  gap: 8px;
}

.contact-icon {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  cursor: pointer;
  transition: transform 0.2s;
}

.contact-icon:hover {
  transform: scale(1.1);
}

.contact-icon.xhs {
  background: #ff2442;
  color: #fff;
}

.contact-icon.wechat {
  background: #07c160;
  color: #fff;
}

.contact-icon.other {
  background: #07c160;
  color: #fff;
}

/* ---------- 右侧内容区 ---------- */
.content {
  flex: 1;
  padding: 24px;
  background: #f0f2f5;
  overflow: auto;
}
</style>

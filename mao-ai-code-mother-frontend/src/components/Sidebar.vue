<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter, useRoute } from 'vue-router'
import { useRecentProjectsStore } from '@/stores/recentProjects'
import { userLogout } from '@/api/userController'
import { useLoginUserStore } from '@/stores/loginUser'
import { IconHome } from '@tabler/icons-vue'
import { IconTabs } from '@tabler/icons-vue'
import { IconUserCircle } from '@tabler/icons-vue'
import { IconSparkles } from '@tabler/icons-vue'
import { IconPhoto } from '@tabler/icons-vue'
import { IconUsers } from '@tabler/icons-vue'
import { IconSearch } from '@tabler/icons-vue'
import { IconPlus } from '@tabler/icons-vue'
import { IconChevronDown } from '@tabler/icons-vue'
import { IconStarFilled } from '@tabler/icons-vue'
import { IconBell } from '@tabler/icons-vue'
import { IconLayoutSidebarLeftCollapse } from '@tabler/icons-vue'
import { IconDownload } from '@tabler/icons-vue'
import { IconDeviceMobile } from '@tabler/icons-vue'
import { IconPencil } from '@tabler/icons-vue'
import { IconCopy } from '@tabler/icons-vue'
import { IconUser } from '@tabler/icons-vue'
import { IconPalette } from '@tabler/icons-vue'
import { IconLogout } from '@tabler/icons-vue'
import { IconChevronRight } from '@tabler/icons-vue'
import { IconCirclePlus } from '@tabler/icons-vue'
import { IconUserFilled } from '@tabler/icons-vue'
import { IconGift } from '@tabler/icons-vue'
import { message } from 'ant-design-vue'
import { checkinPoints, getCheckinStatus } from '@/api/pointsController'

const COLLAPSE_KEY = 'miaoda_sidebar_collapsed'

const collapsed = ref(localStorage.getItem(COLLAPSE_KEY) === '1')

// 折叠宽度写到根节点，用户中心弹窗用 var(--sidebar-w) 定位 left
watch(
  collapsed,
  (v) => {
    localStorage.setItem(COLLAPSE_KEY, v ? '1' : '0')
    document.documentElement.style.setProperty('--sidebar-w', v ? '60px' : '240px')
  },
  { immediate: true },
)

const recentStore = useRecentProjectsStore()
const keyword = ref('')
const userMenuOpen = ref(false)
const userPopupOpen = ref(false)

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return [...recentStore.projects]
    .filter((p) => p.name.toLowerCase().includes(kw))
    .sort((a, b) => (b.ts || 0) - (a.ts || 0))
})

function fmtTime(ts?: number) {
  if (!ts) return ''
  const diff = (Date.now() - ts) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + ' 小时前'
  return Math.floor(diff / 86400) + ' 天前'
}

// 进入对话页即视为"打开应用"，记录动作统一由 AppChatPage 挂载时触发（POST /app/recent/{appId} + 刷新 store），
// 这里只负责跳转，避免重复打点。
function openProject(id: string | number) {
  router.push(`/app/chat/${id}`)
}

function addProject() {
  router.push('/')
}

function toggleUserMenu() {
  userMenuOpen.value = !userMenuOpen.value
}

function toggleUserPopup() {
  // 互斥：打开底部弹窗时收起顶部下拉
  userMenuOpen.value = false
  userPopupOpen.value = !userPopupOpen.value
}

function copyId() {
  // TODO: 接入真实用户 ID；此处仅做演示复制
  try {
    navigator.clipboard?.writeText('user-det8eqs1gxs0')
  } catch (e) {
    /* noop */
  }
}

function goProfile() {
  userPopupOpen.value = false
  router.push('/user/profile')
}

// 退出登录：先弹确认，确认后再真正调用接口
const loggingOut = ref(false)
const showLogoutConfirm = ref(false)

function confirmLogout() {
  // 互斥：收起顶部下拉，弹出确认
  userMenuOpen.value = false
  userPopupOpen.value = true
  showLogoutConfirm.value = true
}

function cancelLogout() {
  showLogoutConfirm.value = false
}

async function doLogout() {
  if (loggingOut.value) return
  loggingOut.value = true
  try {
    await userLogout()
  } catch (e) {
    // 即使后端报「用户未登录」也继续清理本地登录态
  } finally {
    // 重置本地登录态
    useLoginUserStore().setLoginUser({ userName: '未登录' })
    showLogoutConfirm.value = false
    userPopupOpen.value = false
    userMenuOpen.value = false
    loggingOut.value = false
    router.push('/user/login')
  }
}

function toggleCollapse() {
  collapsed.value = !collapsed.value
}

function openNotify() {
  alert('打开通知面板')
}
function openDownload() {
  // TODO: 接下载页/二维码
  alert('下载秒哒 App')
}

const searchOpen = ref(false)
const modalKeyword = ref('')
const modalInputRef = ref<HTMLInputElement | null>(null)

function openSearch() {
  modalKeyword.value = ''
  searchOpen.value = true
  nextTick(() => modalInputRef.value?.focus())
}
function closeSearch() {
  searchOpen.value = false
}
const modalList = computed(() => {
  const kw = modalKeyword.value.trim().toLowerCase()
  return [...recentStore.projects]
    .filter((p) => p.name.toLowerCase().includes(kw))
    .sort((a, b) => (b.ts || 0) - (a.ts || 0))
})
function selectProject(item: { id: string | number }) {
  searchOpen.value = false
  openProject(item.id)
}

function openMember() {
  router.push('/membership')
}

// 登录态：未登录时显示「登录」按钮而不是头像
const { loginUser } = storeToRefs(useLoginUserStore())
const isLoggedIn = computed(() => !!loginUser.value?.id)
function goLogin() {
  router.push('/user/login')
}
function goRegister() {
  router.push('/user/register')
}
function goCreateOrg() {
  // TODO: 接后端「创建企业组织」接口；此处先给个占位提示
  userMenuOpen.value = false
  alert('创建企业组织功能开发中')
}

// 每日签到：领取积分（复用积分记账体系，赠送计入 giftSecondsBalance）
const checkedToday = ref(false)
const checkinReward = ref(200)

async function fetchCheckinStatus() {
  if (!isLoggedIn.value) return
  try {
    const res = await getCheckinStatus()
    if (res.data?.code === 0 && res.data.data) {
      checkedToday.value = !!res.data.data.checkedToday
      if (res.data.data.reward != null) checkinReward.value = res.data.data.reward
    }
  } catch (e) {
    // 忽略：签到状态非关键路径
  }
}

async function doCheckin() {
  if (checkedToday.value) return
  try {
    const res = await checkinPoints()
    if (res.data?.code === 0 && res.data.data) {
      checkedToday.value = true
      if (res.data.data.reward != null) checkinReward.value = res.data.data.reward
      message.success(`签到成功，+${res.data.data.reward ?? checkinReward.value} 积分`)
      // 刷新登录态（积分余额实时更新）
      await useLoginUserStore().fetchLoginUser()
    } else {
      message.error(res.data?.message || '签到失败')
    }
  } catch (e: any) {
    // 后端抛「今日已签到」等也提示出来
    message.error(e?.response?.data?.message || e?.message || '签到失败')
  }
}

onMounted(async () => {
  // 拉取最近项目（GET /app/recent）；之后由 AppChatPage 打开应用时通过 store 实时刷新
  await recentStore.fetchRecent()
  // 拉取签到状态（已登录才需要）
  await fetchCheckinStatus()
})

const navItems = [
  { label: '首页', icon: IconHome, path: '/' },
  { label: '项目', icon: IconTabs, path: '/app', requireAuth: true },
  { label: '技能中心', icon: IconSparkles, path: '/skills', requireAuth: true },
  { label: '素材库', icon: IconPhoto, path: '/materials', requireAuth: true },
  { label: '社区', icon: IconUsers, path: '/community' },
]

const router = useRouter()
const route = useRoute()
const activePath = computed(() => route.path)

function handleNav(item: { path: string; requireAuth?: boolean }) {
  // 需要登录的页面：未登录直接跳转到登录页
  if (item.requireAuth && !isLoggedIn.value) {
    router.push('/user/login')
    return
  }
  if (item.path !== route.path) router.push(item.path)
}
</script>

<template>
  <aside class="sidebar" :class="{ collapsed }">
    <!-- 顶栏：logo + 折叠按钮 -->
    <div class="header-row" :class="{ 'is-collapsed': collapsed }">
      <div
        class="logo-wrap"
        :class="{ 'logo-collapsed': collapsed }"
        :title="collapsed ? '展开侧边栏' : ''"
        @click="collapsed && toggleCollapse()"
      >
        <div class="logo-icon"></div>
        <span v-show="!collapsed">妙想</span>
      </div>
      <button
        v-show="!collapsed"
        class="collapse-btn"
        @click="toggleCollapse"
        title="收起侧边栏"
      >
        <IconLayoutSidebarLeftCollapse :size="20" />
      </button>
    </div>

    <!-- 个人空间下拉 -->
    <div class="user-select" @click="toggleUserMenu" v-show="!collapsed">
      <div class="user-left">
        <div class="user-avatar"><IconUserCircle :size="22" /></div>
        <span>个人空间</span>
      </div>
      <IconChevronDown class="arrow-icon" :class="{ open: userMenuOpen }" :size="18" />
      <div class="user-menu" :class="{ open: userMenuOpen }" v-if="isLoggedIn">
        <div class="workspace-header">个人空间</div>
        <div class="workspace-user">
          <div class="workspace-avatar">
            <IconUserFilled :size="20" />
          </div>
          <span class="workspace-name">个人空间</span>
        </div>
        <div class="workspace-divider"></div>
        <div class="workspace-new" @click="goCreateOrg">
          <span class="workspace-new-icon"><IconCirclePlus :size="20" /></span>
          <span>新建企业组织</span>
        </div>
      </div>
      <div class="user-menu" :class="{ open: userMenuOpen }" v-else>
        <div class="user-menu-item" @click="goLogin">登录</div>
        <div class="user-menu-item" @click="goRegister">注册</div>
      </div>
    </div>
    <!-- 折叠态：单独显示头像 -->
    <div class="user-avatar user-avatar-collapsed" v-show="collapsed" @click="toggleUserMenu">
      <IconUserCircle :size="22" />
    </div>

    <!-- 中间可滚动区域 -->
    <div class="sidebar-scroll" v-show="!collapsed">
      <div class="nav-list">
        <div
          class="nav-item"
          v-for="item in navItems"
          :key="item.label"
          :class="{ active: activePath === item.path }"
          @click="handleNav(item)"
        >
          <div class="nav-icon"><component :is="item.icon" :size="20" /></div>
          <span>{{ item.label }}</span>
        </div>
      </div>

      <template v-if="isLoggedIn">
        <div class="recent-header">
          <span>最近项目</span>
          <button class="search-trigger" @click="openSearch" title="搜索项目">
            <IconSearch :size="18" />
          </button>
        </div>
        <div class="recent-list">
          <div
            class="recent-item"
            v-for="p in filtered"
            :key="p.id"
            @click="openProject(p.id)"
          >
            <span class="recent-name">{{ p.name }}</span>
            <span class="recent-time">{{ fmtTime(p.ts) }}</span>
          </div>
          <div v-if="filtered.length === 0" class="recent-empty">暂无最近项目</div>
        </div>
        <div class="recent-new" @click="addProject">
          <IconPlus :size="18" /><span>新建项目</span>
        </div>
      </template>
    </div>

    <!-- 折叠态：仅图标导航 -->
    <div class="nav-list-collapsed" v-show="collapsed">
      <div
        class="nav-item-collapsed"
        v-for="item in navItems"
        :key="item.label"
        :class="{ active: activePath === item.path }"
        @click="handleNav(item)"
        :title="item.label"
      >
        <component :is="item.icon" :size="18" />
      </div>
    </div>

    <!-- 底部：每日签到 + 额度 + 头像/通知 -->
    <div class="sidebar-footer" v-show="!collapsed">
      <div
        class="checkin-card"
        v-if="isLoggedIn"
        :class="{ done: checkedToday }"
        @click="doCheckin"
        title="每日签到领积分"
      >
        <div class="ck-icon"><IconGift :size="22" /></div>
        <div class="ck-text">
          <div class="ck-sub">领 {{ checkinReward }} 积分</div>
        </div>
        <div class="ck-btn">{{ checkedToday ? '今日已签' : '去签到' }}</div>
      </div>

      <div class="credit-card" v-if="isLoggedIn">
        <IconStarFilled class="star-icon" :size="20" />
        <span class="credit-num">{{ Number(loginUser?.secondsBalance ?? 0) + Number(loginUser?.giftSecondsBalance ?? 0) }}</span>
        <span class="version-text" @click="openMember" title="查看会员中心">个人免费版</span>
      </div>

      <div class="action-bar">
        <div
          v-if="isLoggedIn"
          class="bottom-module"
          @click="toggleUserPopup"
          title="个人中心"
        >
          <div class="avatar-circle">1</div>
        </div>
        <div
          v-else
          class="login-button"
          @click="goLogin"
          title="登录账号"
        >
          登录
        </div>
        <button
          class="notify-btn"
          @click="openNotify"
          title="消息通知"
          aria-label="消息通知"
        >
          <IconBell :size="22" />
          <span class="badge" v-if="isLoggedIn">3</span>
        </button>
      </div>
    </div>

    <!-- 折叠态：底部签到入口 + 头像/登录 + 通知 -->
    <div class="sidebar-footer-collapsed" v-show="collapsed">
      <div
        class="footer-icon-btn"
        v-if="isLoggedIn"
        @click="doCheckin"
        :title="checkedToday ? '今日已签到' : '每日签到领积分'"
      >
        <IconGift :size="22" />
      </div>
      <div class="footer-icon-btn" @click="openNotify" title="消息通知">
        <IconBell class="bell-icon" :size="22" />
        <span class="badge" v-if="isLoggedIn">3</span>
      </div>
      <div
        v-if="isLoggedIn"
        class="footer-avatar"
        @click="toggleUserPopup"
        title="个人中心"
      >1</div>
      <div
        v-else
        class="footer-login"
        @click="goLogin"
        title="登录"
      >登录</div>
    </div>
  </aside>

  <!-- 用户中心弹窗（点击底部头像触发，常驻侧边栏锚定，无遮罩） -->
  <teleport to="body">
    <div v-if="userPopupOpen" class="user-popup" @click.stop>
      <div class="popup-header">
        <div class="popup-avatar">1</div>
        <div class="popup-user">
          <div class="popup-name">
            <span>181****18</span>
            <button class="icon-btn" title="修改昵称"><IconPencil :size="14" /></button>
          </div>
          <div class="popup-id">
            <span>ID: user-det8eqs1gxs0</span>
            <button class="icon-btn" title="复制 ID" @click="copyId"><IconCopy :size="14" /></button>
          </div>
        </div>
      </div>
      <div class="popup-divider"></div>

      <div class="popup-row" @click="goProfile">
        <div class="popup-icon"><IconUser :size="18" /></div>
        <span>个人主页</span>
      </div>
      <div class="popup-row">
        <div class="popup-icon"><IconPalette :size="18" /></div>
        <span>主题设置</span>
        <IconChevronRight class="row-arrow" :size="16" />
      </div>
      <div class="popup-divider"></div>

      <div class="popup-row logout" @click="confirmLogout" v-if="!showLogoutConfirm">
        <div class="popup-icon"><IconLogout :size="18" /></div>
        <span>退出登录</span>
      </div>

      <!-- 退出确认 -->
      <div class="logout-confirm" v-if="showLogoutConfirm">
        <div class="logout-confirm-title">确定退出登录吗？</div>
        <div class="logout-confirm-desc">退出后将返回登录页，需重新登录才能使用</div>
        <div class="logout-confirm-actions">
          <button class="btn-cancel" :disabled="loggingOut" @click="cancelLogout">取消</button>
          <button class="btn-ok" :disabled="loggingOut" @click="doLogout">
            {{ loggingOut ? '退出中…' : '确定退出' }}
          </button>
        </div>
      </div>
    </div>
  </teleport>
  <!-- 搜索弹窗 -->
  <teleport to="body">
    <div class="search-modal-mask" v-if="searchOpen" @click.self="closeSearch">
      <div class="search-modal">
        <div class="search-header">
          <IconSearch :size="20" class="search-icon" />
          <input
            ref="modalInputRef"
            class="search-input"
            v-model="modalKeyword"
            placeholder="请搜索"
          />
          <span class="close-btn" @click="closeSearch">✕</span>
        </div>
        <div class="search-list">
          <div
            class="search-item"
            v-for="item in modalList"
            :key="item.id"
            @click="selectProject(item)"
          >
            <span>{{ item.name }}</span>
            <span class="item-time">{{ item.ts ? fmtTime(item.ts) : '' }}</span>
          </div>
        </div>
      </div>
    </div>
  </teleport>
</template>

<style scoped>
.sidebar {
  width: 240px;
  background: #ffffff;
  border-right: 1px solid #eef0f4;
  padding: 20px 16px 16px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  transition: width 0.22s ease;
  /* 重构：嵌入文档流，常驻左侧；sticky 让主体滚动时侧边栏保持可见 */
  position: sticky;
  top: 0;
  height: 100vh;
  align-self: flex-start;
  z-index: 50;
}
.sidebar.collapsed {
  width: 60px;
  padding: 20px 7px 16px;
}
.sidebar-scroll {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

/* 顶栏：logo + 折叠按钮 */
.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  min-height: 40px;
}
.header-row.is-collapsed {
  justify-content: center;
}
.logo-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
}
.logo-collapsed {
  cursor: pointer;
}
.logo-icon {
  width: 26px;
  height: 26px;
  background: linear-gradient(135deg, #6c5cff, #5b8cff);
  border-radius: 8px;
}
.collapse-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  color: #4b5563;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.2s;
}
.collapse-btn:hover {
  background: #f2f4f8;
  color: #111827;
}
.collapse-btn.collapsed {
  transform: rotate(180deg);
}

/* 个人空间下拉 */
.user-select {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 7px 10px;
  margin-bottom: 16px;
  cursor: pointer;
  user-select: none;
}
.user-select:hover {
  background: #f8fafc;
}
.user-left {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  font-weight: 500;
}
.user-avatar {
  width: 32px;
  height: 32px;
  background: #ffffff;
  border: 1px solid #e8ebf0;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #4080ff;
  flex-shrink: 0;
}
.user-avatar-collapsed {
  margin: 0 auto 18px;
  cursor: pointer;
  padding-bottom: 18px;
  border-bottom: 1px solid #eef0f4;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #e8f0fe;
  color: #4080ff;
  display: flex;
  align-items: center;
  justify-content: center;
}
.arrow-icon {
  transition: transform 0.2s;
}
.arrow-icon.open {
  transform: rotate(180deg);
}
.user-menu {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  right: 0;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.1);
  padding: 6px;
  display: none;
  z-index: 20;
}
.user-menu.open {
  display: block;
}
.user-menu-item {
  padding: 10px 12px;
  border-radius: 6px;
  font-size: 15px;
  cursor: pointer;
}
.user-menu-item:hover {
  background: #f2f4f8;
}
.user-menu-item.logout-item {
  color: #e23b3b;
}
.user-menu-item.logout-item:hover {
  background: #fef2f2;
}

/* 工作区切换器 */
.workspace-header {
  font-size: 12px;
  color: #8a8f99;
  padding: 6px 4px 4px;
}
.workspace-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid #e8ebf0;
  background: #ffffff;
  cursor: pointer;
}
.workspace-user:hover {
  background: #f2f4f8;
  border-color: #d1d5db;
}
.workspace-avatar {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background: #f0f5ff;
  color: #4080ff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.workspace-name {
  font-size: 13px;
  color: #1f2329;
  font-weight: 600;
}
.workspace-divider {
  height: 1px;
  background: #eef0f4;
  margin: 6px 2px;
}
.workspace-new {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 10px;
  border-radius: 6px;
  font-size: 12px;
  color: #4b5563;
  cursor: pointer;
}
.workspace-new:hover {
  background: #f2f4f8;
}
.workspace-new-icon {
  color: #6b7280;
  display: flex;
  align-items: center;
}

/* 导航列表 */
.nav-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: stretch;
  padding: 4px 4px 0;
}
.nav-list-collapsed {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
  padding-top: 4px;
  flex: 1 1 auto;
  min-height: 0;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 8px 10px;
  border-radius: 10px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  color: #1f2329;
}
.nav-item:hover {
  background: #f2f4f8;
}
/* 激活态：深色填充（对齐截图） */
.nav-item.active {
  background: #2c2c2c;
  color: #ffffff;
}
.nav-item.active .nav-icon {
  color: #ffffff;
}
.nav-icon {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #4b5563;
}
.nav-item-collapsed {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  cursor: pointer;
  color: #4b5563;
  transition: background 0.15s, color 0.15s;
}
.nav-item-collapsed:hover {
  background: #f2f4f8;
  color: #111827;
}
.nav-item-collapsed.active {
  background: #2c2c2c;
  color: #ffffff;
}

/* 最近项目 */
.recent-header {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  color: #9ca3af;
  font-size: 13px;
  padding: 0 4px;
}
.recent-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-top: 4px;
}
.recent-item {
  display: flex;
  align-items: center;
  padding: 9px 12px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: #1f2329;
}
.recent-item:hover {
  background: #f2f4f8;
}
.recent-name {
  flex: 1 1 auto;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.recent-time {
  flex-shrink: 0;
  margin-left: 8px;
  font-size: 11px;
  color: #b0b6c2;
  white-space: nowrap;
}
.recent-empty {
  padding: 8px 12px;
  font-size: 12px;
  color: #9ca3af;
}
.recent-new {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 12px;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  color: #4096ff;
  font-size: 12px;
  cursor: pointer;
  margin-top: 6px;
}
.recent-new:hover {
  background: #f0f7ff;
}
.search-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  color: #666;
  cursor: pointer;
  flex-shrink: 0;
}
.search-trigger:hover {
  background: #f2f4f8;
}

/* 底部 */
.sidebar-footer,
.sidebar-footer-collapsed {
  flex-shrink: 0;
  padding-top: 16px;
  border-top: 1px solid #eef0f4;
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 12px;
}
/* 折叠态底部图标需要居中、且不等宽（图标固定宽、登录占满） */
.sidebar-footer-collapsed {
  align-items: center;
}
.sidebar-footer-collapsed .footer-icon-btn {
  width: 36px;
  height: 36px;
}
/* 折叠态：头像与消息通知按钮(36px)保持一致 */
.sidebar-footer-collapsed .footer-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  font-size: 15px;
}
.sidebar-footer-collapsed .footer-login {
  width: 100%;
  height: 40px;
  margin-top: 4px;
  border-radius: 10px;
  font-size: 13px;
}

/* 下载提示卡 */
.checkin-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: linear-gradient(135deg, #fff4e6 0%, #ffe9f3 100%);
  border-radius: 12px;
  border: 1px solid #ffd9c2;
  cursor: pointer;
  transition: background 0.15s, opacity 0.15s;
}
.checkin-card:hover {
  background: linear-gradient(135deg, #ffe9cf 0%, #ffd9ec 100%);
}
.checkin-card.done {
  background: #f3f4f6;
  border-color: #e6e7ea;
  cursor: default;
}
.ck-icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ff8a3d;
  flex-shrink: 0;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}
.ck-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.ck-title {
  font-size: 13px;
  font-weight: 600;
  color: #1f2329;
}
.ck-sub {
  font-size: 12px;
  color: #ff8a3d;
  margin-top: 2px;
  font-weight: 600;
}
.ck-btn {
  margin-left: auto;
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 600;
  color: #ffffff;
  background: #ff8a3d;
  padding: 5px 12px;
  border-radius: 999px;
}
.checkin-card.done .ck-btn {
  color: #8b8f9a;
  background: #e6e7ea;
}

/* 额度卡 */
.credit-card {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #ffffff;
  padding: 8px 14px;
  border-radius: 12px;
  border: 1px solid #e8ebf0;
}
.star-icon {
  color: #8040ff;
  flex-shrink: 0;
}
.credit-num {
  font-size: 16px;
  font-weight: 700;
  color: #4444ff;
}
.version-text {
  margin-left: auto;
  font-size: 13px;
  color: #9498a2;
  cursor: pointer;
}
.version-text:hover {
  color: #6c7280;
}

.action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
/* 最底部两个等宽模块：左头像、右消息 */
.bottom-module {
  flex: 1;
  height: 48px;
  border-radius: 12px;
  border: 1px solid #e8ebf0;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  transition: background 0.15s, border-color 0.15s;
}
.bottom-module:hover {
  background: #f2f4f8;
  border-color: #d1d5db;
}
.bottom-module .avatar-circle {
  width: 32px;
  height: 32px;
  font-size: 14px;
}
.login-button {
  width: 96px;
  flex-shrink: 0;
  height: 48px;
  border-radius: 12px;
  background: #1f1f1f;
  color: #ffffff;
  font-size: 12px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  user-select: none;
  transition: background 0.15s;
}
.login-button:hover {
  background: #2c2c2c;
}
/* 消息按钮：单独一个图标按钮，与登录按钮视觉上明显区分 */
.notify-btn {
  width: 48px;
  height: 48px;
  flex-shrink: 0;
  border: none;
  background: transparent;
  color: #444955;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 12px;
  position: relative;
  transition: background 0.15s, color 0.15s;
}
.notify-btn:hover {
  background: #f2f4f8;
  color: #1f2329;
}
.notify-btn .badge {
  position: absolute;
  top: 6px;
  right: 8px;
}
.login-circle {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #1f1f1f;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
}
.login-circle:hover {
  background: #2c2c2c;
}

/* 折叠态底部元素 */
.footer-icon-btn {
  width: 44px;
  height: 44px;
  border: 1px solid #e8ebf0;
  border-radius: 12px;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #4b5563;
  cursor: pointer;
  position: relative;
  transition: background 0.15s, border-color 0.15s;
}
.footer-icon-btn:hover {
  background: #f2f4f8;
  border-color: #d1d5db;
  color: #1f2329;
}
.footer-icon-btn .badge {
  position: absolute;
  top: 4px;
  right: 4px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: #e23b3b;
  color: #fff;
  font-size: 11px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1.5px solid #f3f4f8;
  box-sizing: border-box;
}
.footer-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #1f1f1f;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  flex-shrink: 0;
}
.footer-avatar:hover {
  background: #2c2c2c;
}
.footer-login {
  width: 100%;
  height: 40px;
  border-radius: 10px;
  background: #1f1f1f;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  user-select: none;
  transition: background 0.15s;
}
.footer-login:hover {
  background: #2c2c2c;
}
.bottom-module .bell-icon {
  color: #444955;
}
.bottom-module .badge {
  top: 5px;
  right: 10px;
}
.avatar-circle {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #1f1f1f;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
  flex-shrink: 0;
}
.notification-wrap {
  position: relative;
  cursor: pointer;
  display: flex;
  align-items: center;
}
.bell-icon {
  color: #444955;
}
.badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #ff3333;
  color: white;
  font-size: 11px;
  font-weight: 600;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 搜索弹窗 */
.search-modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.search-modal {
  width: 420px;
  background: #ffffff;
  border-radius: 14px;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.12);
  overflow: hidden;
  transform: translateY(-12%);
}
.search-header {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
  gap: 10px;
}
.search-icon {
  font-size: 20px;
  color: #666;
  flex-shrink: 0;
}
.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 18px;
  color: #333;
}
.search-input::placeholder {
  color: #b0b6c2;
}
.close-btn {
  font-size: 22px;
  cursor: pointer;
  color: #333;
  user-select: none;
  flex-shrink: 0;
}
.search-list {
  max-height: 520px;
  overflow-y: auto;
}
.search-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  font-size: 17px;
  cursor: pointer;
}
.search-item:hover {
  background: #f3f6fc;
}
.item-time {
  font-size: 16px;
  color: #989ca8;
}

/* ========== 用户中心弹窗 ========== */
.user-popup {
  position: fixed;
  left: calc(var(--sidebar-w, 240px) + 16px);
  bottom: 16px;
  width: 304px;
  max-height: calc(100vh - 32px);
  overflow-y: auto;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.14);
  z-index: 200;
  padding: 8px;
  color: #1f2329;
  font-size: 15px;
  animation: popup-in 0.16s ease;
}
@keyframes popup-in {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
.popup-header {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 10px 8px 12px;
}
.popup-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #1f1f1f;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 600;
  flex-shrink: 0;
}
.popup-user {
  flex: 1;
  min-width: 0;
}
.popup-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 600;
}
.popup-id {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #8b8f9a;
  margin-top: 3px;
}
.icon-btn {
  border: none;
  background: transparent;
  color: #9ca3af;
  cursor: pointer;
  padding: 2px;
  display: flex;
  align-items: center;
  border-radius: 4px;
  transition: color 0.15s, background 0.15s;
}
.icon-btn:hover {
  color: #1f2329;
  background: #f2f4f8;
}
.popup-divider {
  height: 1px;
  background: #eef0f4;
  margin: 4px 4px;
}
.popup-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 15px;
  transition: background 0.12s;
}
.popup-row:hover {
  background: #f2f4f8;
}
.popup-row.highlighted {
  background: #eef2ff;
}
.popup-row.highlighted:hover {
  background: #e1e9ff;
}
.popup-row .row-arrow {
  margin-left: auto;
  color: #9ca3af;
}
.popup-icon {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #4b5563;
  flex-shrink: 0;
}
.popup-row.logout {
  color: #e23b3b;
}
.popup-row.logout .popup-icon {
  color: #e23b3b;
}
.popup-row.logout:hover {
  background: #fef2f2;
}

/* 退出确认 */
.logout-confirm {
  padding: 4px 4px 8px;
}
.logout-confirm-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
  text-align: center;
  padding: 8px 0 2px;
}
.logout-confirm-desc {
  font-size: 12px;
  color: #8a8f99;
  text-align: center;
  line-height: 1.5;
  padding: 0 8px 10px;
}
.logout-confirm-actions {
  display: flex;
  gap: 10px;
}
.logout-confirm-actions button {
  flex: 1;
  height: 36px;
  border-radius: 8px;
  border: none;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s, opacity 0.15s;
}
.logout-confirm-actions button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.btn-cancel {
  background: #f2f3f5;
  color: #4b5563;
}
.btn-cancel:hover:not(:disabled) {
  background: #e9eaec;
}
.btn-ok {
  background: #e23b3b;
  color: #ffffff;
}
.btn-ok:hover:not(:disabled) {
  background: #cc2f2f;
}
.social-icons {
  margin-left: auto;
  display: flex;
  gap: 6px;
}
.social-chip {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  color: #ffffff;
  font-size: 11px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.12s;
}
.social-chip:hover {
  transform: scale(1.1);
}
</style>

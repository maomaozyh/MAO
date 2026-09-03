<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <aside class="admin-sidebar">
      <div class="sidebar-logo">
        <div class="logo-icon">⚡</div>
        <span class="logo-text">Admin Pro</span>
      </div>

      <div class="sidebar-menu">
        <template v-for="group in menuGroups" :key="group.title">
          <div class="menu-group-title">{{ group.title }}</div>
          <div
            v-for="item in group.items"
            :key="item.path"
            class="menu-item"
            :class="{ active: isActive(item.path) }"
            @click="navigateTo(item.path)"
          >
            <span class="menu-icon">{{ item.icon }}</span>
            <span class="menu-label">{{ item.label }}</span>
          </div>
        </template>
      </div>

      <div class="sidebar-footer">
        <div class="admin-user-info">
          <a-avatar :size="36" style="background: #6366f1;">
            {{ loginUser?.userName?.charAt(0) || 'A' }}
          </a-avatar>
          <div class="admin-user-detail">
            <div class="admin-user-name">{{ loginUser?.userName || '管理员' }}</div>
            <div class="admin-user-role">超级管理员</div>
          </div>
        </div>
      </div>
    </aside>

    <!-- 主内容区 -->
    <div class="admin-main">
      <!-- 顶部导航 -->
      <header class="admin-header">
        <div class="header-left">
          <a-breadcrumb>
            <a-breadcrumb-item v-for="(item, index) in breadcrumbs" :key="index">
              {{ item }}
            </a-breadcrumb-item>
          </a-breadcrumb>
        </div>
        <div class="header-right">
          <a-input-search
            v-model:value="searchText"
            placeholder="搜索..."
            style="width: 240px"
            @search="handleSearch"
          />
          <a-badge :count="3" :offset="[-4, 4]">
            <a-button type="text" class="header-btn">
              <BellOutlined style="font-size: 18px; color: #64748b" />
            </a-button>
          </a-badge>
          <a-dropdown>
            <a-button type="text" class="header-btn">
              <a-avatar :size="28" style="background: #6366f1; margin-right: 6px;">
                {{ loginUser?.userName?.charAt(0) || 'A' }}
              </a-avatar>
              <span>{{ loginUser?.userName || '管理员' }}</span>
            </a-button>
            <template #overlay>
              <a-menu>
                <a-menu-item @click="goToHome">
                  <HomeOutlined /> 前台首页
                </a-menu-item>
                <a-menu-item @click="goToSettings">
                  <SettingOutlined /> 系统设置
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item @click="handleLogout" style="color: #ef4444;">
                  <LogoutOutlined /> 退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </header>

      <!-- 页面内容 -->
      <div class="admin-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser'
import { message } from 'ant-design-vue'
import {
  BellOutlined,
  HomeOutlined,
  SettingOutlined,
  LogoutOutlined,
} from '@ant-design/icons-vue'
import { userLogout } from '@/api/userController'
import { adminMenuGroups, findAdminMenuItem } from '@/config/adminMenu'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const searchText = ref('')

const loginUser = computed(() => loginUserStore.loginUser)

// 菜单统一从 config/adminMenu.ts 读取，新增管理页只需改那一处
const menuGroups = adminMenuGroups

const breadcrumbs = computed(() => {
  const hit = findAdminMenuItem(route.path)
  if (hit) {
    return ['后台管理', hit.group.title, hit.item.label]
  }
  return ['后台管理']
})

function isActive(path: string) {
  return route.path.startsWith(path)
}

function navigateTo(path: string) {
  if (route.path !== path) {
    router.push(path)
  }
}

function handleSearch() {
  message.info('搜索功能开发中')
}

function goToHome() {
  router.push('/')
}

function goToSettings() {
  router.push('/admin/settings')
}

async function handleLogout() {
  try {
    await userLogout()
    loginUserStore.loginUser = null as any
    message.success('已退出登录')
    router.push('/user/login')
  } catch (e) {
    message.error('退出失败')
  }
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: #f1f5f9;
}

.admin-sidebar {
  width: 240px;
  background: linear-gradient(180deg, #1e1b4b 0%, #312e81 100%);
  color: #e0e7ff;
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 100;
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 24px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo-icon {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.5px;
}

.sidebar-menu {
  flex: 1;
  padding: 16px 12px;
  overflow-y: auto;
}

.menu-group-title {
  font-size: 12px;
  color: #818cf8;
  text-transform: uppercase;
  letter-spacing: 1px;
  padding: 12px 12px 8px;
  font-weight: 600;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
  color: #c7d2fe;
  margin-bottom: 2px;
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.menu-item.active {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
}

.menu-icon {
  font-size: 18px;
  width: 24px;
  text-align: center;
}

.menu-label {
  flex: 1;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.admin-user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-user-detail {
  flex: 1;
  min-width: 0;
}

.admin-user-name {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.admin-user-role {
  font-size: 12px;
  color: #a5b4fc;
  margin-top: 2px;
}

.admin-main {
  flex: 1;
  margin-left: 240px;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.admin-header {
  height: 60px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 50;
}

.header-left {
  flex: 1;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-btn {
  height: 40px;
  display: flex;
  align-items: center;
  color: #475569;
}

.admin-content {
  flex: 1;
  padding: 16px 24px 20px;
  overflow-y: auto;
}
</style>

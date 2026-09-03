<template>
  <div class="dashboard-page">
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <div>
        <h2>欢迎回来，{{ loginUser?.userName || '管理员' }} 👋</h2>
        <p>这是您的后台管理仪表盘，快速查看平台运营数据。</p>
      </div>
      <div class="banner-date">
        {{ currentDate }}
      </div>
    </div>

    <!-- 统计卡片 -->
    <a-row :gutter="20" class="stats-row">
      <a-col :span="6">
        <div class="stat-card stat-primary">
          <div class="stat-icon">👥</div>
          <div class="stat-info">
            <div class="stat-value">{{ formatNumber(stats.totalUsers) }}</div>
            <div class="stat-label">总用户数</div>
          </div>
          <div class="stat-trend up">
            <span>↑</span> 本周 {{ formatNumber(stats.weekNewUsers) }}
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="stat-card stat-success">
          <div class="stat-icon">📱</div>
          <div class="stat-info">
            <div class="stat-value">{{ formatNumber(stats.totalApps) }}</div>
            <div class="stat-label">应用总数</div>
          </div>
          <div class="stat-trend up">
            <span>↑</span> 持续增长
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="stat-card stat-warning">
          <div class="stat-icon">📝</div>
          <div class="stat-info">
            <div class="stat-value">{{ formatNumber(stats.totalPosts) }}</div>
            <div class="stat-label">帖子总数</div>
          </div>
          <div class="stat-trend up">
            <span>↑</span> 本周 {{ formatNumber(stats.weekNewPosts) }}
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="stat-card stat-danger">
          <div class="stat-icon">💬</div>
          <div class="stat-info">
            <div class="stat-value">{{ formatNumber(stats.totalChats) }}</div>
            <div class="stat-label">对话总数</div>
          </div>
          <div class="stat-trend up">
            <span>↑</span> 活跃
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 图表区域 -->
    <a-row :gutter="20" class="charts-row">
      <a-col :span="16">
        <a-card title="数据趋势" class="chart-card">
          <template #extra>
            <a-radio-group v-model:value="chartType" size="small">
              <a-radio-button value="users">用户增长</a-radio-button>
              <a-radio-button value="posts">帖子增长</a-radio-button>
            </a-radio-group>
          </template>
          <div class="chart-container">
            <div class="chart-bars">
              <div
                v-for="(value, index) in currentChartData"
                :key="index"
                class="chart-bar"
                :style="{ height: barHeight(value) + '%' }"
              >
                <span class="bar-value">{{ value }}</span>
                <span class="bar-label">{{ stats.dateLabels?.[index] || '' }}</span>
              </div>
            </div>
          </div>
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card title="快捷操作" class="chart-card">
          <div class="quick-actions">
            <div class="quick-action-item" @click="goToPage('/admin/userManage')">
              <div class="qa-icon" style="background: #eef2ff; color: #6366f1;">👥</div>
              <div class="qa-info">
                <div class="qa-title">用户管理</div>
                <div class="qa-desc">管理平台用户</div>
              </div>
            </div>
            <div class="quick-action-item" @click="goToPage('/admin/postManage')">
              <div class="qa-icon" style="background: #ecfdf5; color: #10b981;">📝</div>
              <div class="qa-info">
                <div class="qa-title">内容审核</div>
                <div class="qa-desc">审核社区帖子</div>
              </div>
            </div>
            <div class="quick-action-item" @click="goToPage('/admin/appManage')">
              <div class="qa-icon" style="background: #fffbeb; color: #f59e0b;">📱</div>
              <div class="qa-info">
                <div class="qa-title">上传技能</div>
                <div class="qa-desc">审核用户上传的技能</div>
              </div>
            </div>
            <div class="quick-action-item" @click="goToPage('/admin/settings')">
              <div class="qa-icon" style="background: #fdf2f8; color: #ec4899;">⚙️</div>
              <div class="qa-info">
                <div class="qa-title">系统设置</div>
                <div class="qa-desc">配置系统参数</div>
              </div>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 最近动态 -->
    <a-row :gutter="20">
      <a-col :span="24">
        <a-card title="今日概览" class="chart-card">
          <a-row :gutter="40">
            <a-col :span="6">
              <div class="overview-item">
                <div class="overview-value" style="color: #6366f1;">{{ formatNumber(stats.todayNewUsers) }}</div>
                <div class="overview-label">今日新增用户</div>
              </div>
            </a-col>
            <a-col :span="6">
              <div class="overview-item">
                <div class="overview-value" style="color: #10b981;">{{ formatNumber(stats.todayNewPosts) }}</div>
                <div class="overview-label">今日新增帖子</div>
              </div>
            </a-col>
            <a-col :span="6">
              <div class="overview-item">
                <div class="overview-value" style="color: #f59e0b;">{{ formatNumber(stats.weekNewUsers) }}</div>
                <div class="overview-label">本周新增用户</div>
              </div>
            </a-col>
            <a-col :span="6">
              <div class="overview-item">
                <div class="overview-value" style="color: #ec4899;">{{ formatNumber(stats.weekNewPosts) }}</div>
                <div class="overview-label">本周新增帖子</div>
              </div>
            </a-col>
          </a-row>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser'
import { getDashboardStats } from '@/api/adminController'
import dayjs from 'dayjs'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const loginUser = computed(() => loginUserStore.loginUser)
const chartType = ref<'users' | 'posts'>('users')

const stats = ref<any>({
  totalUsers: 0,
  totalApps: 0,
  totalPosts: 0,
  totalChats: 0,
  todayNewUsers: 0,
  todayNewPosts: 0,
  weekNewUsers: 0,
  weekNewPosts: 0,
  userGrowthTrend: [],
  postGrowthTrend: [],
  dateLabels: [],
})

const currentDate = computed(() => {
  return dayjs().format('YYYY年MM月DD日 dddd')
})

const currentChartData = computed(() => {
  return chartType.value === 'users'
    ? stats.value.userGrowthTrend || []
    : stats.value.postGrowthTrend || []
})

function barHeight(value: number) {
  const max = Math.max(...currentChartData.value, 1)
  return Math.max((value / max) * 100, 5)
}

function formatNumber(num: number | undefined) {
  if (num === undefined || num === null) return '0'
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  return num.toLocaleString()
}

function goToPage(path: string) {
  router.push(path)
}

async function fetchStats() {
  try {
    const res = await getDashboardStats()
    if (res.data.code === 0 && res.data.data) {
      stats.value = res.data.data
    }
  } catch (e) {
    console.error('获取仪表盘数据失败', e)
  }
}

onMounted(() => {
  fetchStats()
})
</script>

<style scoped>
.dashboard-page {
  width: 100%;
}

.welcome-banner {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 50%, #a78bfa 100%);
  border-radius: 16px;
  padding: 32px;
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  position: relative;
  overflow: hidden;
}

.welcome-banner::before {
  content: '';
  position: absolute;
  right: -50px;
  top: -50px;
  width: 200px;
  height: 200px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
}

.welcome-banner::after {
  content: '';
  position: absolute;
  right: 50px;
  bottom: -80px;
  width: 150px;
  height: 150px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 50%;
}

.welcome-banner h2 {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 700;
}

.welcome-banner p {
  margin: 0;
  font-size: 14px;
  opacity: 0.9;
}

.banner-date {
  font-size: 14px;
  background: rgba(255, 255, 255, 0.2);
  padding: 8px 16px;
  border-radius: 20px;
  backdrop-filter: blur(10px);
}

.stats-row {
  margin-bottom: 24px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  border-left: 4px solid;
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.stat-primary {
  border-left-color: #6366f1;
}

.stat-primary .stat-icon {
  background: #eef2ff;
  color: #6366f1;
}

.stat-success {
  border-left-color: #10b981;
}

.stat-success .stat-icon {
  background: #ecfdf5;
  color: #10b981;
}

.stat-warning {
  border-left-color: #f59e0b;
}

.stat-warning .stat-icon {
  background: #fffbeb;
  color: #f59e0b;
}

.stat-danger {
  border-left-color: #ef4444;
}

.stat-danger .stat-icon {
  background: #fef2f2;
  color: #ef4444;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.stat-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
}

.stat-label {
  font-size: 13px;
  color: #64748b;
}

.stat-trend {
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat-trend.up {
  color: #10b981;
}

.stat-trend.down {
  color: #ef4444;
}

.charts-row {
  margin-bottom: 24px;
}

.chart-card {
  border-radius: 12px;
}

.chart-container {
  height: 280px;
  display: flex;
  align-items: flex-end;
}

.chart-bars {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  width: 100%;
  height: 100%;
  padding: 20px 0;
}

.chart-bar {
  flex: 1;
  max-width: 50px;
  background: linear-gradient(180deg, #818cf8, #6366f1);
  border-radius: 8px 8px 0 0;
  position: relative;
  margin: 0 4px;
  transition: all 0.3s ease;
  min-height: 20px;
}

.chart-bar:hover {
  background: linear-gradient(180deg, #a5b4fc, #818cf8);
}

.bar-value {
  position: absolute;
  top: -20px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 12px;
  font-weight: 600;
  color: #475569;
  white-space: nowrap;
}

.bar-label {
  position: absolute;
  bottom: -24px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 12px;
  color: #94a3b8;
  white-space: nowrap;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quick-action-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s;
}

.quick-action-item:hover {
  background: #f8fafc;
}

.qa-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.qa-info {
  flex: 1;
}

.qa-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.qa-desc {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}

.overview-item {
  text-align: center;
  padding: 20px 0;
}

.overview-value {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 8px;
}

.overview-label {
  font-size: 14px;
  color: #64748b;
}
</style>

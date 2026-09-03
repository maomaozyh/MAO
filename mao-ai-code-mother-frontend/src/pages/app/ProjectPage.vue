<template>
  <div class="project-page">
    <div class="page-container">
      <!-- 顶部区域 -->
      <div class="header">
        <h1 class="title">项目</h1>
        <div class="toolbar">
          <!-- 左侧 Tab -->
          <div class="tabs">
            <div
              class="tab"
              :class="{ active: activeTab === 'mine' }"
              @click="switchTab('mine')"
            >我创建的</div>
            <div
              class="tab"
              :class="{ active: activeTab === 'shared' }"
              @click="switchTab('shared')"
            >共享后端</div>
          </div>

          <!-- 右侧操作区 -->
          <div class="actions">
            <!-- 类型筛选：自定义「全部 + ▼」下拉（单选） -->
            <div class="filter-select">
              <button class="filter-trigger" @click="toggleFilterDropdown('type')">
                <span>{{ typeLabel }}</span>
                <span class="tab-arrow">▼</span>
              </button>
              <div v-if="openFilter === 'type'" class="filter-dropdown" @click.stop>
                <div
                  v-for="opt in typeOptions"
                  :key="opt.value"
                  class="filter-item"
                  :class="{ active: typeFilter === opt.value }"
                  @click="pickTypeFilter(opt.value)"
                >
                  {{ opt.label }}
                </div>
              </div>
            </div>
            <div class="search-box">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#8f959e" stroke-width="2">
                <circle cx="11" cy="11" r="8"></circle>
                <path d="m21 21-4.35-4.35"></path>
              </svg>
              <input v-model="searchKey" type="text" placeholder="搜索" />
            </div>
            <div class="icon-btn" :class="{ active: bulkMode }" :title="bulkMode ? '确认删除所选' : '批量删除'" @click="toggleBulk">
              🗑
            </div>
            <button class="btn-primary" @click="goCreate"><span>+</span> 创建项目</button>
          </div>
        </div>
      </div>

      <!-- 卡片网格 -->
      <div v-if="filteredApps.length > 0" class="grid">
        <div
          v-for="(app, i) in filteredApps"
          :key="app.id"
          class="card"
          :class="{ 'in-bulk': bulkMode }"
          @click="onCardClick(app)"
        >
          <!-- 上半部分：有封面显示图片，否则彩色背景 + 内容预览 -->
          <div v-if="app.cover" class="card-image">
            <img :src="app.cover" :alt="app.appName" />
          </div>
          <div v-else class="card-preview" :class="bgPool[i % bgPool.length]">
            <span v-if="bulkMode" class="bulk-check" :class="{ checked: isSelected(app) }">
              {{ isSelected(app) ? '✓' : '' }}
            </span>
            <div class="quote">“</div>
            <div class="preview-text">{{ app.initPrompt || app.appName }}</div>
          </div>

          <!-- 下半部分：标题 + 信息 -->
          <div class="card-body">
            <div class="card-title">{{ app.appName || '未命名应用' }}</div>
            <div class="card-meta">对话于 {{ convTime(app) }}</div>
            <div class="card-footer">
              <span class="tag">任务</span>
              <div class="more" :class="{ open: menuAppId === String(app.id) }" @click.stop="toggleMenu(app)">
                ⋯
              </div>
            </div>
          </div>

          <!-- 更多菜单 -->
          <div v-if="menuAppId === String(app.id)" class="menu-pop" @click.stop>
            <div class="menu-item" @click="viewChat(app)">进入对话</div>
            <div class="menu-item danger" @click="removeApp(app)">删除</div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <div class="empty-icon">📦</div>
        <p class="empty-text">暂无项目，去首页创建一个吧</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onActivated, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { listMyAppVoByPage, listGoodAppVoByPage, deleteApp } from '@/api/appController'
import { useLoginUserStore } from '@/stores/loginUser'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const activeTab = ref<'mine' | 'shared'>('mine')
const apps = ref<API.AppVO[]>([])
const searchKey = ref('')
const statusFilter = ref('all')
const typeFilter = ref('all')
const loading = ref(false)

// 自定义筛选下拉（取代 native <select>，与素材库风格一致）
const statusOptions = [
  { value: 'all', label: '全部' },
  { value: 'paid', label: '消耗积分' },
  { value: 'free', label: '免费使用' },
]
const typeOptions = [
  { value: 'all', label: '全部' },
  { value: 'HTML', label: '网页' },
  { value: 'VUE_PROJECT', label: '项目' },
  { value: 'MULTI_FILE', label: '多文件' },
]
const openFilter = ref<'' | 'status' | 'type'>('')
// 当前选中值的展示文本（去掉「全部」前缀冗余：选了具体类型就直接显示）
const statusLabel = computed(() => {
  const o = statusOptions.find((x) => x.value === statusFilter.value)
  return o ? o.label : '全部'
})
const typeLabel = computed(() => {
  const o = typeOptions.find((x) => x.value === typeFilter.value)
  return o ? o.label : '全部'
})
const toggleFilterDropdown = (which: 'status' | 'type') => {
  openFilter.value = openFilter.value === which ? '' : which
}
const pickStatusFilter = (val: string) => {
  statusFilter.value = val
  openFilter.value = ''
}
const pickTypeFilter = (val: string) => {
  typeFilter.value = val
  openFilter.value = ''
}
const closeFilterOnOutside = (e: MouseEvent) => {
  const target = e.target as HTMLElement | null
  if (target && target.closest('.filter-select')) return
  openFilter.value = ''
}
onMounted(() => {
  loadApps()
  document.addEventListener('click', closeFilterOnOutside)
})
onUnmounted(() => {
  document.removeEventListener('click', closeFilterOnOutside)
})

// 批量删除
const bulkMode = ref(false)
const selectedIds = ref<Set<string>>(new Set())
const menuAppId = ref('')

const bgPool = ['bg-yellow', 'bg-green', 'bg-pink', 'bg-purple', 'bg-cyan', 'bg-blue', 'bg-mint']

// 「对话于 昨天 19:37 / 08月30日」相对时间
const convTime = (app: API.AppVO) => {
  const ts = app.lastOpenTime || app.createTime
  if (!ts) return '今天'
  const d = new Date(ts)
  if (Number.isNaN(d.getTime())) return '今天'
  const now = new Date()
  const startOfDay = (x: Date) => new Date(x.getFullYear(), x.getMonth(), x.getDate()).getTime()
  const dayDiff = Math.round((startOfDay(now) - startOfDay(d)) / 86400000)
  const pad = (n: number) => String(n).padStart(2, '0')
  const hm = `${pad(d.getHours())}:${pad(d.getMinutes())}`
  if (dayDiff <= 0) return `今天 ${hm}`
  if (dayDiff === 1) return `昨天 ${hm}`
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

const filteredApps = computed(() => {
  let result = apps.value
  const kw = searchKey.value.trim().toLowerCase()
  if (kw) {
    result = result.filter(
      (app) =>
        app.appName?.toLowerCase().includes(kw) ||
        app.initPrompt?.toLowerCase().includes(kw),
    )
  }
  if (typeFilter.value !== 'all') {
    result = result.filter((app) => app.codeGenType === typeFilter.value)
  }
  if (statusFilter.value === 'paid') {
    result = result.filter((app) => !!app.price && app.price !== '免费')
  } else if (statusFilter.value === 'free') {
    result = result.filter((app) => !app.price || app.price === '免费')
  }
  return result
})

const loadApps = async () => {
  loading.value = true
  try {
    const pageReq = { pageNum: 1, pageSize: 100 }
    let res
    if (activeTab.value === 'shared') {
      res = await listGoodAppVoByPage(pageReq)
    } else {
      res = await listMyAppVoByPage({
        ...pageReq,
        sortField: 'createTime',
        sortOrder: 'desc',
      })
    }
    if (res.data.code === 0 && res.data.data) {
      apps.value = res.data.data.records || []
    } else {
      apps.value = []
    }
  } catch (e) {
    console.error('加载项目失败', e)
    apps.value = []
  } finally {
    loading.value = false
  }
}

const switchTab = (tab: 'mine' | 'shared') => {
  if (activeTab.value === tab) return
  activeTab.value = tab
  menuAppId.value = ''
  bulkMode.value = false
  selectedIds.value = new Set()
  loadApps()
}

const goCreate = () => {
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录后创建项目')
    router.push('/user/login')
    return
  }
  router.push('/')
}

const viewChat = (app: API.AppVO) => {
  if (app.id) router.push(`/app/chat/${app.id}?view=1`)
}

const onCardClick = (app: API.AppVO) => {
  if (bulkMode.value) {
    toggleSelect(app)
    return
  }
  viewChat(app)
}

// 更多菜单
const toggleMenu = (app: API.AppVO) => {
  menuAppId.value = menuAppId.value === String(app.id) ? '' : String(app.id)
}

// 单删
const removeApp = async (app: API.AppVO) => {
  if (!app.id) return
  try {
    const res = await deleteApp({ id: app.id })
    if (res.data.code === 0) {
      message.success('删除成功')
      menuAppId.value = ''
      loadApps()
    } else {
      message.error(res.data.message || '删除失败')
    }
  } catch (e) {
    console.error('删除失败', e)
    message.error('删除失败，请重试')
  }
}

// 批量删除
const isSelected = (app: API.AppVO) => !!app.id && selectedIds.value.has(String(app.id))
const toggleSelect = (app: API.AppVO) => {
  if (!app.id) return
  const id = String(app.id)
  const next = new Set(selectedIds.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  selectedIds.value = next
}

const toggleBulk = async () => {
  if (!bulkMode.value) {
    // 进入批量模式
    bulkMode.value = true
    menuAppId.value = ''
    message.info('勾选要删除的项目，再次点击垃圾桶确认删除')
    return
  }
  if (selectedIds.value.size === 0) {
    bulkMode.value = false
    return
  }
  const ids = [...selectedIds.value]
  try {
    for (const id of ids) {
      await deleteApp({ id: Number(id) })
    }
    message.success(`已删除 ${ids.length} 个项目`)
    bulkMode.value = false
    selectedIds.value = new Set()
    loadApps()
  } catch (e) {
    console.error('批量删除失败', e)
    message.error('部分删除失败，请重试')
  }
}

onMounted(loadApps)

// keep-alive 恢复（Tab 切回）时刷新列表；初次激活 onMounted 已加载，跳过
let keepAliveActivatedOnce = false
onActivated(() => {
  if (keepAliveActivatedOnce) {
    loadApps()
  }
  keepAliveActivatedOnce = true
})
</script>

<style scoped>
.project-page {
  min-height: 100vh;
  background: #f7f8fa;
}

.page-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 32px 40px;
}

/* 顶部区域 */
.header {
  margin-bottom: 20px;
}

.title {
  font-size: 22px;
  font-weight: 600;
  color: #1f2329;
  margin-bottom: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

/* 左侧 Tab */
.tabs {
  display: flex;
  background: #fff;
  border-radius: 8px;
  padding: 4px;
  border: 1px solid #e4e5e7;
}

.tab {
  padding: 7px 16px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  color: #646a73;
  transition: 0.2s;
  user-select: none;
}

.tab:hover {
  color: #1f2329;
}

.tab.active {
  background: #1f2329;
  color: #fff;
}

/* 右侧操作区 */
.actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

/* 自定义筛选下拉（取代 native <select>） */
.filter-select {
  position: relative;
}
.filter-trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #fff;
  border: 1px solid #e4e5e7;
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 14px;
  color: #1f2329;
  cursor: pointer;
  outline: none;
  min-width: 100px;
}
.filter-trigger:hover {
  border-color: #c8cad0;
}
.tab-arrow {
  font-size: 10px;
  opacity: 0.65;
}
.filter-dropdown {
  position: absolute;
  top: 42px;
  right: 0;
  min-width: 140px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  padding: 6px 0;
  z-index: 100;
}
.filter-item {
  padding: 8px 14px;
  font-size: 14px;
  color: #1f2329;
  cursor: pointer;
}
.filter-item:hover {
  background: #f7f8fa;
}
.filter-item.active {
  color: #1f2329;
  font-weight: 500;
  background: #f0f4ff;
}

.filter,
.search-box {
  background: #fff;
  border: 1px solid #e4e5e7;
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 14px;
  color: #646a73;
  cursor: pointer;
  outline: none;
  height: 36px;
  box-sizing: border-box;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 160px;
}

.search-box input {
  border: none;
  outline: none;
  font-size: 14px;
  background: transparent;
  width: 100%;
}

.search-box input::placeholder {
  color: #b0b6bf;
}

.icon-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #e4e5e7;
  cursor: pointer;
  color: #646a73;
  font-size: 15px;
  transition: 0.2s;
}

.icon-btn:hover {
  border-color: #d0d3d8;
  color: #1f2329;
}

.icon-btn.active {
  background: #fff0f0;
  border-color: #f5a8a8;
  color: #d4380d;
}

.btn-primary {
  background: #1f2329;
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 9px 16px;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  height: 36px;
  box-sizing: border-box;
  transition: background 0.2s;
  white-space: nowrap;
}

.btn-primary:hover {
  background: #333a45;
}

/* 卡片网格 */
.grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.card {
  background: #fff;
  border-radius: 12px;
  overflow: visible;
  border: 1px solid #f0f0f0;
  transition: box-shadow 0.2s, transform 0.2s;
  cursor: pointer;
  position: relative;
}

.card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.card.in-bulk {
  border-color: #d9d9d9;
}

/* 卡片上半部分：彩色背景 + 内容预览 */
.card-preview {
  min-height: 140px;
  padding: 16px;
  position: relative;
  border-radius: 12px 12px 0 0;
}

/* 带图片预览的卡片 */
.card-image {
  min-height: 140px;
  background: #f7f8fa;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  border-radius: 12px 12px 0 0;
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.placeholder-illustration {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #e8f4ff 0%, #f0f8ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
}

.quote {
  font-size: 28px;
  line-height: 1;
  color: rgba(0, 0, 0, 0.15);
  margin-bottom: 8px;
}

.preview-text {
  font-size: 14px;
  line-height: 1.6;
  color: #1f2329;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.bulk-check {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 20px;
  height: 20px;
  border-radius: 6px;
  background: #fff;
  border: 1px solid #c9cdd4;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #fff;
}

.bulk-check.checked {
  background: #1f2329;
  border-color: #1f2329;
}

/* 下半部分：标题 + 信息 */
.card-body {
  padding: 14px 16px;
  background: #fff;
  border-radius: 0 0 12px 12px;
}

.card-title {
  font-size: 15px;
  font-weight: 500;
  color: #1f2329;
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-meta {
  font-size: 12px;
  color: #8f959e;
  margin-bottom: 12px;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.tag {
  font-size: 12px;
  color: #8f959e;
}

.more {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  cursor: pointer;
  color: #8f959e;
  font-size: 16px;
  line-height: 1;
}

.more:hover,
.more.open {
  background: #f2f3f5;
  color: #1f2329;
}

/* 更多菜单 */
.menu-pop {
  position: absolute;
  right: 14px;
  top: calc(100% - 10px);
  background: #fff;
  border: 1px solid #e4e5e7;
  border-radius: 8px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
  z-index: 20;
  min-width: 96px;
  overflow: hidden;
}

.menu-item {
  padding: 9px 14px;
  font-size: 13px;
  color: #333;
  cursor: pointer;
  transition: background 0.15s;
}

.menu-item:hover {
  background: #f5f6f8;
}

.menu-item.danger {
  color: #d4380d;
}

/* 卡片背景色 */
.bg-yellow { background: #fffbe6; }
.bg-green  { background: #f0fff0; }
.bg-pink   { background: #fff0f5; }
.bg-purple { background: #f5f0ff; }
.bg-cyan   { background: #f0ffff; }
.bg-blue   { background: #f0f5ff; }
.bg-mint   { background: #f0fff5; }

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 0;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 15px;
  color: #888;
  margin: 0;
}

/* 响应式 */
@media (max-width: 1200px) {
  .grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 900px) {
  .grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .page-container {
    padding: 16px;
  }

  .grid {
    grid-template-columns: 1fr;
  }
}
</style>

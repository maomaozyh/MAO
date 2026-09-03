<template>
  <div class="logs-page">
    <a-card class="filter-card">
      <a-form layout="inline" :model="searchParams" @finish="doSearch">
        <a-form-item label="关键词">
          <a-input v-model:value="searchParams.keyword" placeholder="操作人 / 操作内容" style="width: 180px" allow-clear />
        </a-form-item>
        <a-form-item label="模块">
          <a-select v-model:value="searchParams.module" placeholder="全部模块" style="width: 140px" allow-clear>
            <a-select-option value="user">用户管理</a-select-option>
            <a-select-option value="post">内容管理</a-select-option>
            <a-select-option value="app">应用管理</a-select-option>
            <a-select-option value="system">系统设置</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="操作类型">
          <a-select v-model:value="searchParams.operation" placeholder="全部类型" style="width: 120px" allow-clear>
            <a-select-option value="新增">新增</a-select-option>
            <a-select-option value="编辑">编辑</a-select-option>
            <a-select-option value="删除">删除</a-select-option>
            <a-select-option value="登录">登录</a-select-option>
            <a-select-option value="审核">审核</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="时间范围">
          <a-range-picker v-model:value="searchParams.dateRange" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">搜索</a-button>
        </a-form-item>
        <a-form-item>
          <a-button @click="resetSearch">重置</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card class="table-card">
      <template #title>
        <span>操作日志</span>
      </template>
      <template #extra>
        <a-space>
          <a-button size="small" @click="exportLogs">📥 导出日志</a-button>
          <a-button size="small" danger @click="clearLogs">🗑️ 清空日志</a-button>
        </a-space>
      </template>

      <a-table
        :columns="columns"
        :data-source="logList"
        :pagination="pagination"
        :loading="loading"
        @change="handleTableChange"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'operator'">
            <div class="operator-cell">
              <a-avatar :size="28" :style="{ background: getAvatarColor(record.operator) }">
                {{ record.operator?.charAt(0) }}
              </a-avatar>
              <span class="operator-name">{{ record.userName || '系统' }}</span>
            </div>
          </template>
          <template v-else-if="column.dataIndex === 'module'">
            <a-tag :color="moduleColors[record.module] || 'default'">{{ moduleLabels[record.module] || record.module }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'operation'">
            <a-tag :color="getOperationColor(record.operation)">{{ record.operation }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag v-if="record.status === 1" color="success">成功</a-tag>
            <a-tag v-else color="error">失败</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            {{ formatTime(record.createTime) }}
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { listOperationLogByPage } from '@/api/adminController'

const loading = ref(false)
const logList = ref<API.OperationLog[]>([])
const total = ref(0)

const searchParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  module: undefined as string | undefined,
  operation: undefined as string | undefined,
  dateRange: [] as any[],
})

const columns = [
  { title: '日志ID', dataIndex: 'id', width: 170 },
  { title: '操作人', dataIndex: 'userName', width: 130 },
  { title: '模块', dataIndex: 'module', width: 100 },
  { title: '操作类型', dataIndex: 'operation', width: 100 },
  { title: '操作内容', dataIndex: 'detail' },
  { title: 'IP地址', dataIndex: 'ip', width: 140 },
  { title: '状态', dataIndex: 'status', width: 80 },
  { title: '操作时间', dataIndex: 'createTime', width: 170 },
]

const moduleLabels: Record<string, string> = {
  user: '用户管理',
  post: '内容管理',
  app: '应用管理',
  system: '系统设置',
}

const moduleColors: Record<string, string> = {
  user: 'blue',
  post: 'green',
  app: 'orange',
  system: 'purple',
}

// 后端 operation 存的是中文（新增/编辑/删除/登录/审核/管理员删除），按关键字推断颜色
function getOperationColor(operation?: string) {
  if (!operation) return 'default'
  if (operation.includes('删除')) return 'red'
  if (operation.includes('登录')) return 'blue'
  if (operation.includes('审核')) return 'purple'
  if (operation.includes('新增')) return 'green'
  if (operation.includes('编辑') || operation.includes('更新')) return 'orange'
  return 'default'
}

const pagination = computed(() => ({
  current: searchParams.pageNum || 1,
  pageSize: searchParams.pageSize || 10,
  total: total.value,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`,
}))

function formatTime(time: string) {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

function getAvatarColor(name: string) {
  const colors = ['#6366f1', '#10b981', '#f59e0b', '#ec4899', '#8b5cf6', '#06b6d4']
  let hash = 0
  for (let i = 0; i < name?.length || 0; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash)
  }
  return colors[Math.abs(hash) % colors.length]
}

async function fetchData() {
  loading.value = true
  try {
    const [startTime, endTime] = searchParams.dateRange?.length === 2
      ? [
          dayjs(searchParams.dateRange[0]).format('YYYY-MM-DD 00:00:00'),
          dayjs(searchParams.dateRange[1]).format('YYYY-MM-DD 23:59:59'),
        ]
      : [undefined, undefined]
    const res = await listOperationLogByPage({
      pageNum: searchParams.pageNum,
      pageSize: searchParams.pageSize,
      keyword: searchParams.keyword || undefined,
      module: searchParams.module,
      operation: searchParams.operation,
      startTime,
      endTime,
    })
    if (res.data.code === 0 && res.data.data) {
      logList.value = res.data.data.records || []
      total.value = Number(res.data.data.totalRow || 0)
    } else {
      message.error('加载日志失败：' + (res.data.message || '未知错误'))
    }
  } catch (error: any) {
    console.error('加载操作日志失败', error)
    message.error('加载日志失败，请确认已登录管理员账号')
  } finally {
    loading.value = false
  }
}

function doSearch() {
  searchParams.pageNum = 1
  fetchData()
}

function resetSearch() {
  searchParams.keyword = ''
  searchParams.module = undefined
  searchParams.operation = undefined
  searchParams.dateRange = []
  searchParams.pageNum = 1
  fetchData()
}

function handleTableChange(page: { current: number; pageSize: number }) {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

// 导出当前查询结果为 CSV（纯前端，不依赖后端接口）
function exportLogs() {
  if (logList.value.length === 0) {
    message.warning('暂无日志可导出')
    return
  }
  const headers = ['日志ID', '操作人', '模块', '操作类型', '操作内容', 'IP地址', '状态', '操作时间']
  const rows = logList.value.map((item) => [
    item.id,
    item.userName || '系统',
    moduleLabels[item.module || ''] || item.module || '',
    item.operation || '',
    item.detail || '',
    item.ip || '',
    item.status === 1 ? '成功' : '失败',
    formatTime(item.createTime || ''),
  ])
  // 转义引号和逗号，避免破坏 CSV 结构
  const csv = [headers, ...rows]
    .map((row) => row.map((cell) => `"${String(cell ?? '').replace(/"/g, '""')}"`).join(','))
    .join('\n')
  // 加 BOM，避免 Excel 打开中文乱码
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `操作日志_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
  link.click()
  URL.revokeObjectURL(url)
  message.success(`已导出 ${logList.value.length} 条日志`)
}

function clearLogs() {
  message.warning('清空日志为高危操作，暂未开放')
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.logs-page {
  width: 100%;
}

.filter-card {
  border-radius: 10px;
  margin-bottom: 16px;
}

.table-card {
  border-radius: 10px;
}

.operator-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.operator-name {
  font-size: 14px;
  color: #334155;
}
</style>

<template>
  <div class="post-manage-page">
    <!-- 统计卡片 -->
    <a-row :gutter="20" class="stats-row">
      <a-col :span="6">
        <div class="mini-stat">
          <div class="ms-value" style="color: #6366f1;">{{ totalCount }}</div>
          <div class="ms-label">全部帖子</div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="mini-stat">
          <div class="ms-value" style="color: #10b981;">{{ publishedCount }}</div>
          <div class="ms-label">已发布</div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="mini-stat">
          <div class="ms-value" style="color: #f59e0b;">{{ pendingCount }}</div>
          <div class="ms-label">待审核</div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="mini-stat">
          <div class="ms-value" style="color: #ef4444;">{{ offlineCount }}</div>
          <div class="ms-label">已下架</div>
        </div>
      </a-col>
    </a-row>

    <!-- 搜索和筛选 -->
    <a-card class="filter-card">
      <a-form layout="inline" :model="searchParams" @finish="doSearch">
        <a-form-item label="关键词">
          <a-input v-model:value="searchParams.title" placeholder="帖子标题" style="width: 200px" />
        </a-form-item>
        <a-form-item label="分类">
          <a-select v-model:value="searchParams.category" placeholder="全部分类" style="width: 150px" allow-clear>
            <a-select-option value="技术">技术</a-select-option>
            <a-select-option value="分享">分享</a-select-option>
            <a-select-option value="问答">问答</a-select-option>
            <a-select-option value="吐槽">吐槽</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="currentStatus" placeholder="全部状态" style="width: 150px" allow-clear>
            <a-select-option :value="null">全部</a-select-option>
            <a-select-option :value="1">已发布</a-select-option>
            <a-select-option :value="0">待审核</a-select-option>
            <a-select-option :value="2">已下架</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">搜索</a-button>
        </a-form-item>
        <a-form-item>
          <a-button @click="resetSearch">重置</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 列表 -->
    <a-card class="table-card">
      <template #title>
        <span>帖子列表</span>
      </template>
      <template #extra>
        <a-space>
          <a-button v-if="selectedRowKeys.length > 0" type="primary" danger size="small" @click="batchDelete">
            批量删除 ({{ selectedRowKeys.length }})
          </a-button>
          <a-button size="small" @click="fetchData">
            <ReloadOutlined /> 刷新
          </a-button>
        </a-space>
      </template>

      <a-table
        :columns="columns"
        :data-source="dataList"
        :pagination="pagination"
        :loading="loading"
        :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange }"
        @change="handleTableChange"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'coverImage'">
            <div v-if="record.coverImage" class="cover-img">
              <img :src="record.coverImage" alt="" />
            </div>
            <div v-else class="cover-placeholder">
              <FileImageOutlined />
            </div>
          </template>
          <template v-else-if="column.dataIndex === 'title'">
            <div class="post-title">{{ record.title }}</div>
            <div class="post-meta">
              <span v-if="record.category" class="post-category">{{ record.category }}</span>
              <a-tag v-for="tag in record.tags?.slice(0, 3)" :key="tag" color="blue" style="font-size: 11px;">
                {{ tag }}
              </a-tag>
            </div>
          </template>
          <template v-else-if="column.dataIndex === 'user'">
            <div class="author-info">
              <a-avatar :size="28" :src="record.user?.userAvatar">
                {{ record.user?.userName?.charAt(0) }}
              </a-avatar>
              <span class="author-name">{{ record.user?.userName || '未知' }}</span>
            </div>
          </template>
          <template v-else-if="column.dataIndex === 'viewCount'">
            <span class="count-num">
              <EyeOutlined /> {{ record.viewCount || 0 }}
            </span>
          </template>
          <template v-else-if="column.dataIndex === 'likeCount'">
            <span class="count-num">
              <LikeOutlined /> {{ record.likeCount || 0 }}
            </span>
          </template>
          <template v-else-if="column.dataIndex === 'commentCount'">
            <span class="count-num">
              <MessageOutlined /> {{ record.commentCount || 0 }}
            </span>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag v-if="record.status === 1" color="green">已发布</a-tag>
            <a-tag v-else-if="record.status === 0" color="orange">待审核</a-tag>
            <a-tag v-else-if="record.status === 2" color="red">已下架</a-tag>
            <a-tag v-else color="default">未知</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            {{ formatTime(record.createTime) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a-button type="link" size="small" @click="viewPost(record)">查看</a-button>
              <a-button
                v-if="record.status === 0"
                type="link"
                size="small"
                @click="updateStatus(record, 1)"
              >
                通过
              </a-button>
              <a-button
                v-if="record.status === 1"
                type="link"
                size="small"
                danger
                @click="updateStatus(record, 2)"
              >
                下架
              </a-button>
              <a-button
                v-if="record.status === 2"
                type="link"
                size="small"
                @click="updateStatus(record, 1)"
              >
                恢复
              </a-button>
              <a-popconfirm title="确定删除这个帖子吗？" @confirm="deletePost(record.id)">
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  EyeOutlined,
  LikeOutlined,
  MessageOutlined,
  FileImageOutlined,
  ReloadOutlined,
} from '@ant-design/icons-vue'
import { adminListPostVoByPage, adminDeletePost, adminUpdatePost } from '@/api/adminController'
import dayjs from 'dayjs'

const loading = ref(false)
const dataList = ref<any[]>([])
const total = ref(0)
const selectedRowKeys = ref<any[]>([])

const currentStatus = ref<number | null>(null)

const searchParams = reactive<API.CommunityPostQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  title: '',
  category: '',
  allStatus: true,
})

const columns = [
  { title: '封面', dataIndex: 'coverImage', width: 100 },
  { title: '标题', dataIndex: 'title', width: 280 },
  { title: '作者', dataIndex: 'user', width: 140 },
  { title: '浏览', dataIndex: 'viewCount', width: 90 },
  { title: '点赞', dataIndex: 'likeCount', width: 90 },
  { title: '评论', dataIndex: 'commentCount', width: 90 },
  { title: '状态', dataIndex: 'status', width: 100 },
  { title: '发布时间', dataIndex: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' },
]

const pagination = computed(() => ({
  current: searchParams.pageNum || 1,
  pageSize: searchParams.pageSize || 10,
  total: total.value,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`,
}))

const totalCount = computed(() => total.value)
const publishedCount = computed(
  () => dataList.value.filter((p) => p.status === 1).length
)
const pendingCount = computed(
  () => dataList.value.filter((p) => p.status === 0).length
)
const offlineCount = computed(
  () => dataList.value.filter((p) => p.status === 2).length
)

function formatTime(time: string) {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}

async function fetchData() {
  loading.value = true
  try {
    const params: any = { ...searchParams }
    if (currentStatus.value !== null) {
      params.status = currentStatus.value
    }
    const res = await adminListPostVoByPage(params)
    if (res.data.code === 0 && res.data.data) {
      dataList.value = res.data.data.records || []
      total.value = res.data.data.totalRow || 0
    } else {
      message.error('获取数据失败')
    }
  } catch (e) {
    console.error(e)
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

function doSearch() {
  searchParams.pageNum = 1
  fetchData()
}

function resetSearch() {
  searchParams.title = ''
  searchParams.category = '' as any
  currentStatus.value = null
  searchParams.pageNum = 1
  fetchData()
}

function handleTableChange(page: { current: number; pageSize: number }) {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

function onSelectChange(keys: any[]) {
  selectedRowKeys.value = keys
}

async function updateStatus(record: any, status: number) {
  try {
    const res = await adminUpdatePost({ id: record.id, status })
    if (res.data.code === 0) {
      message.success('操作成功')
      fetchData()
    } else {
      message.error('操作失败：' + res.data.message)
    }
  } catch (e) {
    message.error('操作失败')
  }
}

async function deletePost(id: number) {
  try {
    const res = await adminDeletePost({ id })
    if (res.data.code === 0) {
      message.success('删除成功')
      fetchData()
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (e) {
    message.error('删除失败')
  }
}

async function batchDelete() {
  // 简单实现：逐个删除
  for (const id of selectedRowKeys.value) {
    await adminDeletePost({ id })
  }
  message.success(`已删除 ${selectedRowKeys.value.length} 条`)
  selectedRowKeys.value = []
  fetchData()
}

function viewPost(record: any) {
  window.open(`/community/post/${record.id}`, '_blank')
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.post-manage-page {
  width: 100%;
}

.stats-row {
  margin-bottom: 16px;
}

.mini-stat {
  background: #fff;
  border-radius: 10px;
  padding: 16px 20px;
  text-align: center;
}

.ms-value {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 4px;
}

.ms-label {
  font-size: 13px;
  color: #64748b;
}

.filter-card {
  border-radius: 10px;
  margin-bottom: 16px;
}

.table-card {
  border-radius: 10px;
}

.cover-img {
  width: 60px;
  height: 40px;
  border-radius: 6px;
  overflow: hidden;
  background: #f1f5f9;
}

.cover-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  width: 60px;
  height: 40px;
  border-radius: 6px;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 20px;
}

.post-title {
  font-weight: 500;
  color: #1e293b;
  margin-bottom: 4px;
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.post-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.post-category {
  font-size: 11px;
  padding: 1px 6px;
  background: #eef2ff;
  color: #6366f1;
  border-radius: 4px;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.author-name {
  font-size: 13px;
  color: #475569;
}

.count-num {
  font-size: 13px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>

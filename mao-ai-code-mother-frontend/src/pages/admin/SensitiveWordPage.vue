<template>
  <div id="sensitiveWordPage">
    <!-- 顶部操作栏 -->
    <div class="top-bar">
      <div class="search-section">
        <a-form layout="inline" :model="searchParams" @finish="doSearch">
          <a-form-item label="关键词">
            <a-input v-model:value="searchParams.keyword" placeholder="敏感词" allow-clear />
          </a-form-item>
          <a-form-item label="分类">
            <a-select v-model:value="searchParams.category" placeholder="全部" allow-clear style="width: 120px">
              <a-select-option value="POLITICS">政治</a-select-option>
              <a-select-option value="PORN">色情</a-select-option>
              <a-select-option value="VIOLENCE">暴力</a-select-option>
              <a-select-option value="AD">广告</a-select-option>
              <a-select-option value="INSULT">辱骂</a-select-option>
              <a-select-option value="OTHER">其他</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="状态">
            <a-select v-model:value="searchParams.enabled" placeholder="全部" allow-clear style="width: 100px">
              <a-select-option :value="1">启用</a-select-option>
              <a-select-option :value="0">禁用</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-button type="primary" html-type="submit">搜索</a-button>
            <a-button style="margin-left: 8px" @click="doReset">重置</a-button>
          </a-form-item>
        </a-form>
      </div>
      <div class="action-section">
        <a-button type="primary" @click="openAddModal">
          <template #icon><PlusOutlined /></template>
          新增
        </a-button>
        <a-button @click="openBatchModal" style="margin-left: 8px">
          <template #icon><ImportOutlined /></template>
          批量导入
        </a-button>
        <a-button @click="handleRefresh" style="margin-left: 8px">
          <template #icon><ReloadOutlined /></template>
          刷新缓存
        </a-button>
      </div>
    </div>

    <a-divider style="margin: 12px 0" />

    <!-- 统计卡片 -->
    <a-row :gutter="16" style="margin-bottom: 16px">
      <a-col :span="6">
        <a-card size="small">
          <a-statistic title="敏感词总数" :value="total" :value-style="{ color: '#1890ff' }" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card size="small">
          <a-statistic title="已启用" :value="enabledCount" :value-style="{ color: '#52c41a' }" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card size="small">
          <a-statistic title="已禁用" :value="total - enabledCount" :value-style="{ color: '#ff4d4f' }" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card size="small">
          <a-statistic title="分类数" :value="categoryCount" :value-style="{ color: '#722ed1' }" />
        </a-card>
      </a-col>
    </a-row>

    <!-- 表格 -->
    <a-table
      :columns="columns"
      :data-source="data"
      :pagination="pagination"
      :row-selection="{ selectedRowKeys, onChange: (keys) => selectedRowKeys = keys }"
      @change="doTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'category'">
          <a-tag :color="getCategoryColor(record.category)">{{ getCategoryLabel(record.category) }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'enabled'">
          <a-switch
            :checked="record.enabled === 1"
            size="small"
            @change="(checked) => toggleEnabled(record, checked)"
          />
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm') }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" size="small" @click="openEditModal(record)">编辑</a-button>
          <a-button type="link" size="small" danger @click="doDelete(record.id)">删除</a-button>
        </template>
      </template>
    </a-table>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="wordModalVisible"
      :title="isEdit ? '编辑敏感词' : '新增敏感词'"
      @ok="handleSave"
      @cancel="wordModalVisible = false"
      width="450px"
    >
      <a-form :model="wordForm" layout="vertical">
        <a-form-item label="敏感词" required>
          <a-input v-model:value="wordForm.word" placeholder="请输入敏感词" :disabled="isEdit" />
        </a-form-item>
        <a-form-item label="分类">
          <a-select v-model:value="wordForm.category">
            <a-select-option value="POLITICS">政治</a-select-option>
            <a-select-option value="PORN">色情</a-select-option>
            <a-select-option value="VIOLENCE">暴力</a-select-option>
            <a-select-option value="AD">广告</a-select-option>
            <a-select-option value="INSULT">辱骂</a-select-option>
            <a-select-option value="OTHER">其他</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态">
          <a-switch v-model:checked="wordForm.enabled" checked-children="启用" un-checked-children="禁用" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="wordForm.remark" placeholder="备注说明" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 批量导入弹窗 -->
    <a-modal
      v-model:open="batchModalVisible"
      title="批量导入敏感词"
      @ok="handleBatchAdd"
      @cancel="batchModalVisible = false"
      width="500px"
    >
      <a-form layout="vertical">
        <a-form-item label="选择分类">
          <a-select v-model:value="batchForm.category" style="width: 100%">
            <a-select-option value="POLITICS">政治</a-select-option>
            <a-select-option value="PORN">色情</a-select-option>
            <a-select-option value="VIOLENCE">暴力</a-select-option>
            <a-select-option value="AD">广告</a-select-option>
            <a-select-option value="INSULT">辱骂</a-select-option>
            <a-select-option value="OTHER">其他</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="敏感词列表" required>
          <a-textarea
            v-model:value="batchForm.wordsText"
            placeholder="每行一个敏感词，例如：&#10;敏感词1&#10;敏感词2&#10;敏感词3"
            :rows="8"
          />
          <div style="color: #999; font-size: 12px; margin-top: 4px;">
            已输入 {{ wordLines.length }} 个敏感词（空行会自动忽略）
          </div>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  pageSensitiveWords,
  addSensitiveWord,
  updateSensitiveWord,
  deleteSensitiveWord,
  batchAddSensitiveWords,
  refreshSensitiveWords,
} from '@/api/sensitiveWordController.ts'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import { PlusOutlined, ImportOutlined, ReloadOutlined } from '@ant-design/icons-vue'

const categoryMap: Record<string, { label: string; color: string }> = {
  POLITICS: { label: '政治', color: 'red' },
  PORN: { label: '色情', color: 'orange' },
  VIOLENCE: { label: '暴力', color: 'volcano' },
  AD: { label: '广告', color: 'gold' },
  INSULT: { label: '辱骂', color: 'magenta' },
  OTHER: { label: '其他', color: 'default' },
}

const getCategoryLabel = (cat: string) => categoryMap[cat]?.label || cat
const getCategoryColor = (cat: string) => categoryMap[cat]?.color || 'default'

const columns = [
  { title: 'ID', dataIndex: 'id', width: '80px' },
  { title: '敏感词', dataIndex: 'word', width: '200px' },
  { title: '分类', dataIndex: 'category', width: '100px' },
  { title: '状态', dataIndex: 'enabled', width: '100px' },
  { title: '备注', dataIndex: 'remark' },
  { title: '创建时间', dataIndex: 'createTime', width: '160px' },
  { title: '操作', key: 'action', width: '150px', fixed: 'right' as const },
]

const data = ref<any[]>([])
const total = ref(0)
const enabledCount = ref(0)
const selectedRowKeys = ref<number[]>([])

const searchParams = reactive<any>({
  pageNum: 1,
  pageSize: 10,
})

const categoryCount = computed(() => {
  const cats = new Set(data.value.map(item => item.category))
  return cats.size
})

const fetchData = async () => {
  const res = await pageSensitiveWords({ ...searchParams })
  if (res.data.code === 0 && res.data.data) {
    data.value = res.data.data.records ?? []
    total.value = res.data.data.totalRow ?? 0
    enabledCount.value = data.value.filter(item => item.enabled === 1).length
  }
}

const pagination = computed(() => ({
  current: searchParams.pageNum ?? 1,
  pageSize: searchParams.pageSize ?? 10,
  total: total.value,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
}))

const doTableChange = (page: { current: number; pageSize: number }) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

const doSearch = () => {
  searchParams.pageNum = 1
  fetchData()
}

const doReset = () => {
  searchParams.keyword = undefined
  searchParams.category = undefined
  searchParams.enabled = undefined
  searchParams.pageNum = 1
  fetchData()
}

// 新增/编辑
const wordModalVisible = ref(false)
const isEdit = ref(false)
const wordForm = reactive<any>({
  word: '',
  category: 'OTHER',
  enabled: 1,
  remark: '',
})

const openAddModal = () => {
  isEdit.value = false
  Object.assign(wordForm, {
    word: '',
    category: 'OTHER',
    enabled: 1,
    remark: '',
    id: undefined,
  })
  wordModalVisible.value = true
}

const openEditModal = (record: any) => {
  isEdit.value = true
  Object.assign(wordForm, {
    id: record.id,
    word: record.word,
    category: record.category,
    enabled: record.enabled,
    remark: record.remark,
  })
  wordModalVisible.value = true
}

const handleSave = async () => {
  if (!wordForm.word?.trim()) {
    message.error('请输入敏感词')
    return
  }
  const res = isEdit.value
    ? await updateSensitiveWord(wordForm)
    : await addSensitiveWord(wordForm)
  if (res.data.code === 0) {
    message.success(isEdit.value ? '更新成功' : '添加成功')
    wordModalVisible.value = false
    fetchData()
  } else {
    message.error(res.data.message || '操作失败')
  }
}

const toggleEnabled = async (record: any, checked: boolean) => {
  const res = await updateSensitiveWord({
    id: record.id,
    enabled: checked ? 1 : 0,
  })
  if (res.data.code === 0) {
    message.success(checked ? '已启用' : '已禁用')
    fetchData()
  }
}

const doDelete = (id: number) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除该敏感词吗？',
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      const res = await deleteSensitiveWord({ id })
      if (res.data.code === 0) {
        message.success('删除成功')
        fetchData()
      } else {
        message.error('删除失败')
      }
    },
  })
}

// 批量导入
const batchModalVisible = ref(false)
const batchForm = reactive<any>({
  category: 'OTHER',
  wordsText: '',
})

const wordLines = computed(() => {
  if (!batchForm.wordsText) return []
  return batchForm.wordsText.split('\n').filter((line: string) => line.trim())
})

const openBatchModal = () => {
  batchForm.category = 'OTHER'
  batchForm.wordsText = ''
  batchModalVisible.value = true
}

const handleBatchAdd = async () => {
  const words = wordLines.value
  if (words.length === 0) {
    message.error('请输入敏感词')
    return
  }
  const res = await batchAddSensitiveWords({
    words,
    category: batchForm.category,
  })
  if (res.data.code === 0) {
    message.success(`成功导入 ${res.data.data} 个敏感词`)
    batchModalVisible.value = false
    fetchData()
  } else {
    message.error(res.data.message || '导入失败')
  }
}

// 刷新缓存
const handleRefresh = async () => {
  const res = await refreshSensitiveWords()
  if (res.data.code === 0) {
    message.success('缓存刷新成功')
    fetchData()
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
#sensitiveWordPage {
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  margin: 16px;
}
.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 12px;
}
.search-section {
  flex: 1;
}
.action-section {
  display: flex;
  align-items: center;
}
</style>

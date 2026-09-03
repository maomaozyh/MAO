<template>
  <div class="upload-skill-manage-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">上传技能管理</h2>
        <p class="page-sub">管理用户通过「上传技能」提交的技能（默认显示待审核，上架后进入发现页）</p>
      </div>
    </div>

    <!-- 待审核 / 已上架 / 全部 切换 -->
    <a-segmented
      v-model:value="viewStatus"
      :options="viewStatusOptions"
      class="status-segmented"
      @change="onViewStatusChange"
    />

    <AdminTable
      ref="tableRef"
      :columns="columns"
      :filters="filters"
      :list-api="listUploadedSkills"
      :delete-api="deleteSkill"
      :extra-params="extraParams"
    >
      <!-- 操作列：上架 / 下架 -->
      <template #actionExtra="{ record }">
        <a-button
          v-if="record.status !== 1"
          size="small"
          type="link"
          @click="toggleStatus(record, 1)"
        >上架</a-button>
        <a-button
          v-else
          size="small"
          type="link"
          @click="toggleStatus(record, 0)"
        >下架</a-button>
      </template>
    </AdminTable>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { message } from 'ant-design-vue'
import AdminTable from '@/components/admin/AdminTable.vue'
import type { AdminColumn, AdminFilter } from '@/components/admin/types'
import { listSkillVOByPage, updateSkill, deleteSkill } from '@/api/skillController'
import type { API } from '@/api/typings'

/** 与技能中心前端分类保持一致（后端 skill.category 存的就是这些 key） */
const CATEGORY_LABELS: Record<string, string> = {
  create: '内容创作与生成',
  understand: '内容理解与处理',
  voice: '语音交互',
  search: '搜索查询',
  office: '办公提效',
  design: '设计美化',
  pay: '支付交易',
  auth: '登录验证',
  map: '地图出行',
}

const categoryOptions = Object.entries(CATEGORY_LABELS).map(([value, label]) => ({
  value,
  label,
}))

const columns: AdminColumn[] = [
  { title: 'ID', dataIndex: 'id', width: 180, ellipsis: true },
  { title: '图标', dataIndex: 'icon', width: 70 },
  { title: '技能名称', dataIndex: 'skillName', width: 180 },
  {
    title: '分类',
    dataIndex: 'category',
    width: 130,
    type: 'tag',
    tags: categoryOptions.map((o) => ({ value: o.value, label: o.label, color: 'blue' })),
  },
  { title: '上传者', dataIndex: 'uploaderName', width: 140 },
  { title: '简介', dataIndex: 'skillDesc', ellipsis: true },
  {
    title: '状态',
    dataIndex: 'status',
    width: 100,
    type: 'tag',
    tags: [
      { value: 1, label: '已上架', color: 'green' },
      { value: 0, label: '待审核/下架', color: 'default' },
    ],
  },
  { title: '创建时间', dataIndex: 'createTime', width: 170, type: 'time' },
  { title: '操作', key: 'action', width: 140 },
]

const filters: AdminFilter[] = [
  { label: '技能名称', field: 'skillName', placeholder: '按名称模糊搜索' },
  { label: '分类', field: 'category', type: 'select', options: categoryOptions },
]

/** 待审核 / 已上架 / 全部：上传的技能默认待审核(status=0) */
const viewStatus = ref<'pending' | 'published' | 'all'>('pending')
const viewStatusOptions = [
  { label: '待审核', value: 'pending' },
  { label: '已上架', value: 'published' },
  { label: '全部', value: 'all' },
]
const extraParams = computed(() => {
  if (viewStatus.value === 'pending') return { status: 0 }
  if (viewStatus.value === 'published') return { status: 1 }
  return {}
})
const onViewStatusChange = () => tableRef.value?.refresh()

const tableRef = ref<InstanceType<typeof AdminTable> | null>(null)

/**
 * 包装 listSkillVOByPage：把 user.userName 拍平为 uploaderName 列，
 * 便于 AdminTable 直接展示「上传者」。
 */
const listUploadedSkills = async (params: any) => {
  const res = await listSkillVOByPage(params)
  const data = res?.data?.data
  if (data?.records) {
    data.records = data.records.map((r: API.SkillVO) => ({
      ...r,
      uploaderName:
        r.user?.userName || (r.userId ? `用户${r.userId}` : '官方'),
    }))
  }
  return res
}

/** 上架 / 下架 */
const toggleStatus = async (record: API.SkillVO, target: number) => {
  if (!record.id) return
  try {
    const res = await updateSkill({ id: record.id, status: target })
    if (res?.data?.code === 0) {
      message.success(target === 1 ? '已上架' : '已下架')
      tableRef.value?.refresh()
    } else {
      message.error(res?.data?.message || '操作失败')
    }
  } catch (e) {
    console.error(e)
    message.error('操作失败')
  }
}
</script>

<style scoped>
.upload-skill-manage-page {
  padding: 8px 0;
}

.page-head {
  margin-bottom: 8px;
}

.page-title {
  font-size: 20px;
  font-weight: 500;
  color: #1f2329;
  margin: 0;
}

.page-sub {
  font-size: 12px;
  color: #8f959e;
  margin: 4px 0 0;
}

.status-segmented {
  margin-bottom: 12px;
}
</style>

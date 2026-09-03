<template>
  <div class="points-manage-page">
    <div class="page-head">
      <h2 class="page-title">积分流水</h2>
      <span class="page-hint">扣费 / 入账 / 退回全量明细</span>
    </div>
    <AdminTable :columns="columns" :filters="filters" :list-api="listAllPointsByPage" />
  </div>
</template>

<script setup lang="ts">
import AdminTable from '@/components/admin/AdminTable.vue'
import type { AdminColumn, AdminFilter } from '@/components/admin/types'
import { listAllPointsByPage } from '@/api/pointsController'

const BIZ_TYPES = [
  { value: 'PURCHASE', label: '购买积分' },
  { value: 'GIFT', label: '赠送积分' },
  { value: 'REFUND', label: '失败退回' },
  { value: 'GEN_CODE', label: 'AI 生成应用' },
  { value: 'GEN_IMAGE', label: 'AI 生成图片' },
  { value: 'GEN_VIDEO', label: 'AI 生成视频' },
  { value: 'GEN_3D', label: 'AI 生成 3D' },
  { value: 'GEN_PPT', label: 'AI 生成 PPT' },
  { value: 'EXPAND', label: '描述扩写' },
  { value: 'SEMANTIC_SEARCH', label: '语义搜索' },
  { value: 'SELF_CHECK', label: '代码自查' },
]

const columns: AdminColumn[] = [
  { title: '流水ID', dataIndex: 'id', width: 180, ellipsis: true },
  { title: '用户ID', dataIndex: 'userId', width: 180, ellipsis: true },
  { title: '积分变动', dataIndex: 'amount', width: 100 },
  { title: '购买余额(变后)', dataIndex: 'balanceAfter', width: 130 },
  { title: '赠送额度(变后)', dataIndex: 'giftAfter', width: 130 },
  {
    title: '业务类型',
    dataIndex: 'bizTypeText',
    width: 130,
    type: 'tag',
    tags: [
      { value: '购买积分', label: '购买积分', color: 'green' },
      { value: '赠送积分', label: '赠送积分', color: 'cyan' },
      { value: '失败退回', label: '失败退回', color: 'orange' },
      { value: 'AI 生成应用', label: 'AI 生成应用', color: 'blue' },
      { value: 'AI 生成图片', label: 'AI 生成图片', color: 'blue' },
      { value: 'AI 生成视频', label: 'AI 生成视频', color: 'blue' },
      { value: 'AI 生成 3D', label: 'AI 生成 3D', color: 'blue' },
      { value: 'AI 生成 PPT', label: 'AI 生成 PPT', color: 'blue' },
    ],
  },
  { title: '说明', dataIndex: 'bizDesc', ellipsis: true },
  {
    title: '状态',
    dataIndex: 'status',
    width: 100,
    type: 'tag',
    tags: [
      { value: 0, label: '有效', color: 'green' },
      { value: 1, label: '已退回', color: 'orange' },
    ],
  },
  { title: '时间', dataIndex: 'createTime', width: 170, type: 'time' },
]

const filters: AdminFilter[] = [
  { label: '用户ID', field: 'userId', placeholder: '按用户查' },
  { label: '业务类型', field: 'bizType', type: 'select', options: BIZ_TYPES },
]
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 4px;
}

.page-title {
  font-size: 20px;
  font-weight: 500;
  color: #1f2329;
  margin: 0;
}

.page-hint {
  font-size: 12px;
  color: #868c96;
}
</style>

<template>
  <div class="material-manage-page">
    <div class="page-head">
      <h2 class="page-title">素材管理</h2>
      <span class="page-hint">不填用户ID = 查看全部用户的素材</span>
    </div>
    <AdminTable :columns="columns" :filters="filters" :list-api="listMyMaterialVoByPage" :delete-api="deleteMaterial" />
  </div>
</template>

<script setup lang="ts">
import AdminTable from '@/components/admin/AdminTable.vue'
import type { AdminColumn, AdminFilter } from '@/components/admin/types'
import { listMyMaterialVoByPage, deleteMaterial } from '@/api/materialController'

const TYPE_OPTIONS = [
  { value: 'image', label: '图片' },
  { value: 'video', label: '视频' },
  { value: 'audio', label: '音频' },
  { value: '3d', label: '3D 模型' },
  { value: 'other', label: '其他' },
]

const columns: AdminColumn[] = [
  { title: 'ID', dataIndex: 'id', width: 180, ellipsis: true },
  { title: '名称', dataIndex: 'name', width: 200, ellipsis: true },
  {
    title: '类型',
    dataIndex: 'type',
    width: 100,
    type: 'tag',
    tags: TYPE_OPTIONS.map((o) => ({ ...o, color: 'blue' })),
  },
  { title: '预览', dataIndex: 'url', width: 110, type: 'image' },
  { title: '大小(字节)', dataIndex: 'size', width: 120 },
  { title: '所属用户', dataIndex: 'userId', width: 180, ellipsis: true },
  { title: '上传时间', dataIndex: 'createTime', width: 170, type: 'time' },
  { title: '操作', key: 'action', width: 90 },
]

const filters: AdminFilter[] = [
  { label: '名称', field: 'name', placeholder: '按名称模糊搜索' },
  { label: '类型', field: 'type', type: 'select', options: TYPE_OPTIONS },
  { label: '用户ID', field: 'userId', placeholder: '留空查全部' },
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

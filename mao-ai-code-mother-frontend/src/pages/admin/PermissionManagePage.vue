<template>
  <div id="permissionManagePage">
    <div class="header">
      <a-button type="primary" @click="openAddModal(null)">新增权限</a-button>
    </div>
    <a-divider />
    <a-table
      :columns="columns"
      :data-source="flatPermissions"
      :pagination="false"
      :default-expand-all="true"
      :indent-size="20"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'permissionName'">
          <span>{{ record.permissionName }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'type'">
          <a-tag :color="record.type === 'menu' ? 'blue' : 'orange'">
            {{ record.type === 'menu' ? '菜单' : '按钮' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button size="small" type="link" @click="openAddModal(record)">新增子权限</a-button>
          <a-button size="small" @click="openEditModal(record)">编辑</a-button>
          <a-button size="small" danger @click="doDelete(record.id)">删除</a-button>
        </template>
      </template>
    </a-table>

    <!-- 新增/编辑权限弹窗 -->
    <a-modal
      v-model:open="permissionModalVisible"
      :title="editingPermission ? '编辑权限' : (parentPermission ? '新增子权限' : '新增权限')"
      @ok="handlePermissionSubmit"
      @cancel="permissionModalVisible = false"
    >
      <a-form :model="permissionForm" layout="vertical">
        <a-form-item label="权限编码">
          <a-input v-model:value="permissionForm.permissionCode" placeholder="请输入权限编码，如 user:add" />
        </a-form-item>
        <a-form-item label="权限名称">
          <a-input v-model:value="permissionForm.permissionName" placeholder="请输入权限名称" />
        </a-form-item>
        <a-form-item label="类型">
          <a-radio-group v-model:value="permissionForm.type">
            <a-radio value="menu">菜单</a-radio>
            <a-radio value="button">按钮</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="permissionForm.sortOrder" :min="0" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  getPermissionTree,
  addPermission,
  updatePermission,
  deletePermission,
} from '@/api/sysPermissionController.ts'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'

const columns = [
  {
    title: '权限名称',
    dataIndex: 'permissionName',
    key: 'permissionName',
  },
  {
    title: '权限编码',
    dataIndex: 'permissionCode',
  },
  {
    title: '类型',
    dataIndex: 'type',
  },
  {
    title: '排序',
    dataIndex: 'sortOrder',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '操作',
    key: 'action',
    width: '240px',
  },
]

// 权限树数据
const permissionTree = ref<any[]>([])

// 扁平化的权限列表（用于表格展示树形结构）
const flatPermissions = computed(() => {
  const result: any[] = []
  const flatten = (items: any[], level: number) => {
    items.forEach((item) => {
      result.push({ ...item, _level: level })
      if (item.children && item.children.length > 0) {
        flatten(item.children, level + 1)
      }
    })
  }
  flatten(permissionTree.value, 0)
  return result
})

// 获取数据
const fetchData = async () => {
  const res = await getPermissionTree()
  if (res.data.code === 0 && res.data.data) {
    permissionTree.value = res.data.data
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
}

// 删除数据
const doDelete = async (id: number) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除该权限吗？删除后子权限也会受到影响。',
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      const res = await deletePermission({ id })
      if (res.data.code === 0) {
        message.success('删除成功')
        fetchData()
      } else {
        message.error('删除失败，' + res.data.message)
      }
    },
  })
}

// 新增/编辑权限弹窗
const permissionModalVisible = ref(false)
const editingPermission = ref<any>(null)
const parentPermission = ref<any>(null)
const permissionForm = reactive<any>({
  permissionCode: '',
  permissionName: '',
  type: 'button',
  parentId: null,
  sortOrder: 0,
})

const openAddModal = (parent: any) => {
  editingPermission.value = null
  parentPermission.value = parent
  permissionForm.permissionCode = ''
  permissionForm.permissionName = ''
  permissionForm.type = 'button'
  permissionForm.parentId = parent ? parent.id : null
  permissionForm.sortOrder = 0
  permissionModalVisible.value = true
}

const openEditModal = (record: any) => {
  editingPermission.value = record
  parentPermission.value = null
  permissionForm.permissionCode = record.permissionCode
  permissionForm.permissionName = record.permissionName
  permissionForm.type = record.type
  permissionForm.parentId = record.parentId
  permissionForm.sortOrder = record.sortOrder
  permissionModalVisible.value = true
}

const handlePermissionSubmit = async () => {
  if (!permissionForm.permissionCode || !permissionForm.permissionName) {
    message.error('权限编码和名称不能为空')
    return
  }
  let res
  if (editingPermission.value) {
    res = await updatePermission({
      id: editingPermission.value.id,
      ...permissionForm,
    })
  } else {
    res = await addPermission(permissionForm)
  }
  if (res.data.code === 0) {
    message.success(editingPermission.value ? '更新成功' : '新增成功')
    permissionModalVisible.value = false
    fetchData()
  } else {
    message.error('操作失败，' + res.data.message)
  }
}

// 页面加载时请求一次
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
#permissionManagePage {
  padding: 24px;
  background: white;
  margin-top: 16px;
}
.header {
  display: flex;
  justify-content: flex-end;
}
</style>

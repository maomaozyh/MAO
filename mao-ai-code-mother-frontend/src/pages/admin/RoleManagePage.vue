<template>
  <div id="roleManagePage">
    <!-- 搜索表单 -->
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="角色编码">
        <a-input v-model:value="searchParams.roleCode" placeholder="输入角色编码" />
      </a-form-item>
      <a-form-item label="角色名称">
        <a-input v-model:value="searchParams.roleName" placeholder="输入角色名称" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">搜索</a-button>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="openAddModal">新增角色</a-button>
      </a-form-item>
    </a-form>
    <a-divider />
    <!-- 表格 -->
    <a-table
      :columns="columns"
      :data-source="data"
      :pagination="pagination"
      @change="doTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="record.status === 1 ? 'green' : 'red'">
            {{ record.status === 1 ? '启用' : '禁用' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button size="small" @click="openEditModal(record)">编辑</a-button>
          <a-button size="small" type="link" @click="openPermissionModal(record)">分配权限</a-button>
          <a-button size="small" type="link" @click="openMenuModal(record)">分配菜单</a-button>
          <a-button size="small" danger @click="doDelete(record.id)">删除</a-button>
        </template>
      </template>
    </a-table>

    <!-- 新增/编辑角色弹窗 -->
    <a-modal
      v-model:open="roleModalVisible"
      :title="editingRole ? '编辑角色' : '新增角色'"
      @ok="handleRoleSubmit"
      @cancel="roleModalVisible = false"
    >
      <a-form :model="roleForm" layout="vertical">
        <a-form-item label="角色编码">
          <a-input v-model:value="roleForm.roleCode" placeholder="请输入角色编码" />
        </a-form-item>
        <a-form-item label="角色名称">
          <a-input v-model:value="roleForm.roleName" placeholder="请输入角色名称" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="roleForm.description" placeholder="请输入描述" :rows="3" />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="roleForm.sortOrder" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="状态">
          <a-radio-group v-model:value="roleForm.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 分配权限弹窗 -->
    <a-modal
      v-model:open="permissionModalVisible"
      title="分配权限"
      width="600px"
      @ok="handlePermissionAssign"
      @cancel="permissionModalVisible = false"
    >
      <a-tree
        checkable
        :tree-data="permissionTreeData"
        v-model:checkedKeys="checkedPermissionKeys"
        :default-expand-all="true"
        field-names="{ title: 'permissionName', key: 'id', children: 'children' }"
      />
    </a-modal>

    <!-- 分配菜单弹窗 -->
    <a-modal
      v-model:open="menuModalVisible"
      title="分配菜单"
      width="600px"
      @ok="handleMenuAssign"
      @cancel="menuModalVisible = false"
    >
      <a-tree
        checkable
        :tree-data="menuTreeData"
        v-model:checkedKeys="checkedMenuKeys"
        :default-expand-all="true"
        field-names="{ title: 'menuName', key: 'id', children: 'children' }"
      />
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  listRoleByPage,
  addRole,
  updateRole,
  deleteRole,
  getRolePermissionIds,
  assignPermissionsToRole,
  getRoleMenuIds,
  assignMenusToRole,
} from '@/api/sysRoleController.ts'
import { getPermissionTree } from '@/api/sysPermissionController.ts'
import { getMenuTree } from '@/api/sysMenuController.ts'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'

const columns = [
  {
    title: 'id',
    dataIndex: 'id',
  },
  {
    title: '角色编码',
    dataIndex: 'roleCode',
  },
  {
    title: '角色名称',
    dataIndex: 'roleName',
  },
  {
    title: '描述',
    dataIndex: 'description',
  },
  {
    title: '排序',
    dataIndex: 'sortOrder',
  },
  {
    title: '状态',
    dataIndex: 'status',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '操作',
    key: 'action',
    width: '280px',
  },
]

// 展示的数据
const data = ref<any[]>([])
const total = ref(0)

// 搜索条件
const searchParams = reactive<any>({
  pageNum: 1,
  pageSize: 10,
  roleCode: '',
  roleName: '',
})

// 获取数据
const fetchData = async () => {
  const res = await listRoleByPage({
    ...searchParams,
  })
  if (res.data.code === 0 && res.data.data) {
    data.value = res.data.data.records ?? []
    total.value = res.data.data.totalRow ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
}

// 分页参数
const pagination = computed(() => {
  return {
    current: searchParams.pageNum ?? 1,
    pageSize: searchParams.pageSize ?? 10,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total: number) => `共 ${total} 条`,
  }
})

// 表格分页变化时的操作
const doTableChange = (page: { current: number; pageSize: number }) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

// 搜索数据
const doSearch = () => {
  searchParams.pageNum = 1
  fetchData()
}

// 删除数据
const doDelete = async (id: number) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除该角色吗？',
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      const res = await deleteRole({ id })
      if (res.data.code === 0) {
        message.success('删除成功')
        fetchData()
      } else {
        message.error('删除失败，' + res.data.message)
      }
    },
  })
}

// 新增/编辑角色弹窗
const roleModalVisible = ref(false)
const editingRole = ref<any>(null)
const roleForm = reactive<any>({
  roleCode: '',
  roleName: '',
  description: '',
  sortOrder: 0,
  status: 1,
})

const openAddModal = () => {
  editingRole.value = null
  roleForm.roleCode = ''
  roleForm.roleName = ''
  roleForm.description = ''
  roleForm.sortOrder = 0
  roleForm.status = 1
  roleModalVisible.value = true
}

const openEditModal = (record: any) => {
  editingRole.value = record
  roleForm.roleCode = record.roleCode
  roleForm.roleName = record.roleName
  roleForm.description = record.description
  roleForm.sortOrder = record.sortOrder
  roleForm.status = record.status
  roleModalVisible.value = true
}

const handleRoleSubmit = async () => {
  if (!roleForm.roleCode || !roleForm.roleName) {
    message.error('角色编码和名称不能为空')
    return
  }
  let res
  if (editingRole.value) {
    res = await updateRole({
      id: editingRole.value.id,
      ...roleForm,
    })
  } else {
    res = await addRole(roleForm)
  }
  if (res.data.code === 0) {
    message.success(editingRole.value ? '更新成功' : '新增成功')
    roleModalVisible.value = false
    fetchData()
  } else {
    message.error('操作失败，' + res.data.message)
  }
}

// 权限树
const permissionModalVisible = ref(false)
const permissionTreeData = ref<any[]>([])
const checkedPermissionKeys = ref<number[]>([])
const currentRoleId = ref<number | null>(null)

const fetchPermissionTree = async () => {
  const res = await getPermissionTree()
  if (res.data.code === 0 && res.data.data) {
    permissionTreeData.value = res.data.data
  }
}

const openPermissionModal = async (record: any) => {
  currentRoleId.value = record.id
  permissionModalVisible.value = true
  await fetchPermissionTree()
  // 获取角色已有的权限
  const res = await getRolePermissionIds(record.id)
  if (res.data.code === 0 && res.data.data) {
    checkedPermissionKeys.value = res.data.data
  }
}

const handlePermissionAssign = async () => {
  if (!currentRoleId.value) return
  const res = await assignPermissionsToRole({
    roleId: currentRoleId.value,
    permissionIds: checkedPermissionKeys.value,
  })
  if (res.data.code === 0) {
    message.success('权限分配成功')
    permissionModalVisible.value = false
  } else {
    message.error('分配失败，' + res.data.message)
  }
}

// 菜单树
const menuModalVisible = ref(false)
const menuTreeData = ref<any[]>([])
const checkedMenuKeys = ref<number[]>([])

const fetchMenuTree = async () => {
  const res = await getMenuTree()
  if (res.data.code === 0 && res.data.data) {
    menuTreeData.value = res.data.data
  }
}

const openMenuModal = async (record: any) => {
  currentRoleId.value = record.id
  menuModalVisible.value = true
  await fetchMenuTree()
  // 获取角色已有的菜单
  const res = await getRoleMenuIds(record.id)
  if (res.data.code === 0 && res.data.data) {
    checkedMenuKeys.value = res.data.data
  }
}

const handleMenuAssign = async () => {
  if (!currentRoleId.value) return
  const res = await assignMenusToRole({
    roleId: currentRoleId.value,
    menuIds: checkedMenuKeys.value,
  })
  if (res.data.code === 0) {
    message.success('菜单分配成功')
    menuModalVisible.value = false
  } else {
    message.error('分配失败，' + res.data.message)
  }
}

// 页面加载时请求一次
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
#roleManagePage {
  padding: 24px;
  background: white;
  margin-top: 16px;
}
</style>

<template>
  <div id="menuManagePage">
    <div class="header">
      <a-button type="primary" @click="openAddModal(null)">新增菜单</a-button>
    </div>
    <a-divider />
    <a-table
      :columns="columns"
      :data-source="flatMenus"
      :pagination="false"
      :default-expand-all="true"
      :indent-size="20"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'menuName'">
          <span style="margin-right: 8px">{{ record.icon }}</span>
          <span>{{ record.menuName }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'visible'">
          <a-tag :color="record.visible === 1 ? 'green' : 'default'">
            {{ record.visible === 1 ? '显示' : '隐藏' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button size="small" type="link" @click="openAddModal(record)">新增子菜单</a-button>
          <a-button size="small" @click="openEditModal(record)">编辑</a-button>
          <a-button size="small" danger @click="doDelete(record.id)">删除</a-button>
        </template>
      </template>
    </a-table>

    <!-- 新增/编辑菜单弹窗 -->
    <a-modal
      v-model:open="menuModalVisible"
      :title="editingMenu ? '编辑菜单' : (parentMenu ? '新增子菜单' : '新增菜单')"
      width="560px"
      @ok="handleMenuSubmit"
      @cancel="menuModalVisible = false"
    >
      <a-form :model="menuForm" layout="vertical">
        <a-form-item label="菜单名称">
          <a-input v-model:value="menuForm.menuName" placeholder="请输入菜单名称" />
        </a-form-item>
        <a-form-item label="菜单路径">
          <a-input v-model:value="menuForm.menuPath" placeholder="请输入菜单路径，如 /admin/userManage" />
        </a-form-item>
        <a-form-item label="组件路径">
          <a-input v-model:value="menuForm.menuComponent" placeholder="请输入组件路径，如 pages/admin/UserManagePage.vue" />
        </a-form-item>
        <a-form-item label="图标（emoji）">
          <a-input v-model:value="menuForm.icon" placeholder="请输入图标 emoji，如 👥" />
        </a-form-item>
        <a-form-item label="权限编码">
          <a-input v-model:value="menuForm.permissionCode" placeholder="绑定的权限编码，可选" />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="menuForm.sortOrder" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="是否显示">
          <a-radio-group v-model:value="menuForm.visible">
            <a-radio :value="1">显示</a-radio>
            <a-radio :value="0">隐藏</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  getMenuTree,
  addMenu,
  updateMenu,
  deleteMenu,
} from '@/api/sysMenuController.ts'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'

const columns = [
  {
    title: '菜单名称',
    dataIndex: 'menuName',
    key: 'menuName',
  },
  {
    title: '菜单路径',
    dataIndex: 'menuPath',
  },
  {
    title: '组件路径',
    dataIndex: 'menuComponent',
  },
  {
    title: '权限编码',
    dataIndex: 'permissionCode',
  },
  {
    title: '排序',
    dataIndex: 'sortOrder',
  },
  {
    title: '显示状态',
    dataIndex: 'visible',
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

// 菜单树数据
const menuTree = ref<any[]>([])

// 扁平化的菜单列表
const flatMenus = computed(() => {
  const result: any[] = []
  const flatten = (items: any[], level: number) => {
    items.forEach((item) => {
      result.push({ ...item, _level: level })
      if (item.children && item.children.length > 0) {
        flatten(item.children, level + 1)
      }
    })
  }
  flatten(menuTree.value, 0)
  return result
})

// 获取数据
const fetchData = async () => {
  const res = await getMenuTree()
  if (res.data.code === 0 && res.data.data) {
    menuTree.value = res.data.data
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
}

// 删除数据
const doDelete = async (id: number) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除该菜单吗？删除后子菜单也会被删除。',
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      const res = await deleteMenu({ id })
      if (res.data.code === 0) {
        message.success('删除成功')
        fetchData()
      } else {
        message.error('删除失败，' + res.data.message)
      }
    },
  })
}

// 新增/编辑菜单弹窗
const menuModalVisible = ref(false)
const editingMenu = ref<any>(null)
const parentMenu = ref<any>(null)
const menuForm = reactive<any>({
  menuName: '',
  menuPath: '',
  menuComponent: '',
  icon: '',
  permissionCode: '',
  parentId: null,
  sortOrder: 0,
  visible: 1,
})

const openAddModal = (parent: any) => {
  editingMenu.value = null
  parentMenu.value = parent
  menuForm.menuName = ''
  menuForm.menuPath = ''
  menuForm.menuComponent = ''
  menuForm.icon = ''
  menuForm.permissionCode = ''
  menuForm.parentId = parent ? parent.id : null
  menuForm.sortOrder = 0
  menuForm.visible = 1
  menuModalVisible.value = true
}

const openEditModal = (record: any) => {
  editingMenu.value = record
  parentMenu.value = null
  menuForm.menuName = record.menuName
  menuForm.menuPath = record.menuPath
  menuForm.menuComponent = record.menuComponent
  menuForm.icon = record.icon
  menuForm.permissionCode = record.permissionCode || ''
  menuForm.parentId = record.parentId
  menuForm.sortOrder = record.sortOrder
  menuForm.visible = record.visible
  menuModalVisible.value = true
}

const handleMenuSubmit = async () => {
  if (!menuForm.menuName || !menuForm.menuPath) {
    message.error('菜单名称和路径不能为空')
    return
  }
  let res
  if (editingMenu.value) {
    res = await updateMenu({
      id: editingMenu.value.id,
      ...menuForm,
    })
  } else {
    res = await addMenu(menuForm)
  }
  if (res.data.code === 0) {
    message.success(editingMenu.value ? '更新成功' : '新增成功')
    menuModalVisible.value = false
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
#menuManagePage {
  padding: 24px;
  background: white;
  margin-top: 16px;
}
.header {
  display: flex;
  justify-content: flex-end;
}
</style>

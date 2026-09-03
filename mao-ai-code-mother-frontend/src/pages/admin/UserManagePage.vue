<template>
  <div id="userManagePage">
    <!-- 顶部操作栏 -->
    <div class="top-bar">
      <div class="search-section">
        <a-form layout="inline" :model="searchParams" @finish="doSearch">
          <a-form-item label="账号">
            <a-input v-model:value="searchParams.userAccount" placeholder="输入账号" allow-clear />
          </a-form-item>
          <a-form-item label="用户名">
            <a-input v-model:value="searchParams.userName" placeholder="输入用户名" allow-clear />
          </a-form-item>
          <a-form-item label="角色">
            <a-select v-model:value="searchParams.userRole" placeholder="全部" allow-clear style="width: 120px">
              <a-select-option value="user">普通用户</a-select-option>
              <a-select-option value="admin">管理员</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-button type="primary" html-type="submit">
              <template #icon><SearchOutlined /></template>
              搜索
            </a-button>
            <a-button style="margin-left: 8px" @click="doReset">重置</a-button>
          </a-form-item>
        </a-form>
      </div>
      <div class="action-section">
        <a-button type="primary" @click="openAddModal">
          <template #icon><PlusOutlined /></template>
          新增用户
        </a-button>
        <a-button danger :disabled="selectedRowKeys.length === 0" @click="doBatchDelete" style="margin-left: 8px">
          <template #icon><DeleteOutlined /></template>
          批量删除
        </a-button>
      </div>
    </div>

    <a-divider style="margin: 12px 0" />

    <!-- 表格 -->
    <a-table
      :columns="columns"
      :data-source="data"
      :pagination="pagination"
      :row-selection="{ selectedRowKeys, onChange: (keys) => selectedRowKeys = keys }"
      @change="doTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'userAvatar'">
          <a-avatar :src="record.userAvatar" size="small" />
        </template>
        <template v-else-if="column.dataIndex === 'userRole'">
          <a-tag v-if="record.userRole === 'admin'" color="red">管理员</a-tag>
          <a-tag v-else color="blue">普通用户</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'membershipTier'">
          <a-tag v-if="record.membershipTier === 'FREE'" color="default">免费版</a-tag>
          <a-tag v-else-if="record.membershipTier === 'PROFESSIONAL'" color="gold">专业版</a-tag>
          <a-tag v-else-if="record.membershipTier === 'FLAGSHIP'" color="purple">旗舰版</a-tag>
          <a-tag v-else>{{ record.membershipTier }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'secondsBalance'">
          <span style="color: #faad14; font-weight: 500">{{ record.secondsBalance || 0 }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm') }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-dropdown>
            <a-button type="link" size="small">
              更多操作 <DownOutlined />
            </a-button>
            <template #overlay>
              <a-menu>
                <a-menu-item @click="openRoleModal(record)">
                  <UserOutlined /> 分配角色
                </a-menu-item>
                <a-menu-item @click="openPasswordModal(record)">
                  <KeyOutlined /> 重置密码
                </a-menu-item>
                <a-menu-item @click="openBalanceModal(record)">
                  <WalletOutlined /> 调整积分
                </a-menu-item>
                <a-menu-item @click="openMembershipModal(record)">
                  <CrownOutlined /> 会员管理
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item danger @click="doDelete(record.id)">
                  <DeleteOutlined /> 删除用户
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </template>
      </template>
    </a-table>

    <!-- 新增/编辑用户弹窗 -->
    <a-modal
      v-model:open="userModalVisible"
      :title="isEdit ? '编辑用户' : '新增用户'"
      @ok="handleSaveUser"
      @cancel="userModalVisible = false"
      width="500px"
    >
      <a-form :model="userForm" layout="vertical">
        <a-form-item label="账号" required>
          <a-input v-model:value="userForm.userAccount" placeholder="请输入账号" :disabled="isEdit" />
        </a-form-item>
        <a-form-item label="密码" v-if="!isEdit" required>
          <a-input-password v-model:value="userForm.userPassword" placeholder="请输入初始密码" />
        </a-form-item>
        <a-form-item label="用户名">
          <a-input v-model:value="userForm.userName" placeholder="请输入用户名" />
        </a-form-item>
        <a-form-item label="头像">
          <a-input v-model:value="userForm.userAvatar" placeholder="头像URL" />
        </a-form-item>
        <a-form-item label="简介">
          <a-textarea v-model:value="userForm.userProfile" placeholder="用户简介" :rows="3" />
        </a-form-item>
        <a-form-item label="用户角色">
          <a-select v-model:value="userForm.userRole">
            <a-select-option value="user">普通用户</a-select-option>
            <a-select-option value="admin">管理员</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 分配角色弹窗 -->
    <a-modal
      v-model:open="roleModalVisible"
      title="分配角色"
      @ok="handleAssignRole"
      @cancel="roleModalVisible = false"
    >
      <div v-if="currentUser">
        <p style="margin-bottom: 16px;">
          用户：<strong>{{ currentUser.userName }}</strong>（{{ currentUser.userAccount }}）
        </p>
        <a-checkbox-group v-model:value="checkedRoleIds">
          <a-space direction="vertical">
            <a-checkbox v-for="role in allRoles" :key="role.id" :value="role.id">
              {{ role.roleName }}（{{ role.roleCode }}）
            </a-checkbox>
          </a-space>
        </a-checkbox-group>
      </div>
    </a-modal>

    <!-- 重置密码弹窗 -->
    <a-modal
      v-model:open="passwordModalVisible"
      title="重置密码"
      @ok="handleResetPassword"
      @cancel="passwordModalVisible = false"
    >
      <div v-if="currentUser">
        <p style="margin-bottom: 16px;">
          为用户 <strong>{{ currentUser.userName }}</strong> 重置密码
        </p>
        <a-form layout="vertical">
          <a-form-item label="新密码" required>
            <a-input-password v-model:value="newPassword" placeholder="请输入新密码" />
          </a-form-item>
          <a-form-item label="确认密码" required>
            <a-input-password v-model:value="confirmPassword" placeholder="请再次输入新密码" />
          </a-form-item>
        </a-form>
      </div>
    </a-modal>

    <!-- 调整积分弹窗 -->
    <a-modal
      v-model:open="balanceModalVisible"
      title="调整积分"
      @ok="handleAdjustBalance"
      @cancel="balanceModalVisible = false"
    >
      <div v-if="currentUser">
        <p style="margin-bottom: 16px;">
          用户：<strong>{{ currentUser.userName }}</strong>
          当前积分：<span style="color: #faad14; font-weight: 500">{{ currentUser.secondsBalance || 0 }}</span>
        </p>
        <a-form layout="vertical">
          <a-form-item label="调整数额" required>
            <a-input-number v-model:value="adjustAmount" placeholder="正数增加，负数扣除" style="width: 100%" />
            <div style="color: #999; font-size: 12px; margin-top: 4px;">
              调整后余额：{{ (currentUser.secondsBalance || 0) + (adjustAmount || 0) }}
            </div>
          </a-form-item>
          <a-form-item label="调整原因">
            <a-textarea v-model:value="adjustReason" placeholder="请输入调整原因" :rows="2" />
          </a-form-item>
        </a-form>
      </div>
    </a-modal>

    <!-- 会员管理弹窗 -->
    <a-modal
      v-model:open="membershipModalVisible"
      title="会员管理"
      @ok="handleUpdateMembership"
      @cancel="membershipModalVisible = false"
    >
      <div v-if="currentUser">
        <p style="margin-bottom: 16px;">
          用户：<strong>{{ currentUser.userName }}</strong>
          当前会员：<a-tag>{{ currentUser.membershipTier || '免费版' }}</a-tag>
        </p>
        <a-form layout="vertical">
          <a-form-item label="会员等级" required>
            <a-select v-model:value="membershipForm.membershipTier" style="width: 100%">
              <a-select-option value="FREE">免费版</a-select-option>
              <a-select-option value="PROFESSIONAL">专业版</a-select-option>
              <a-select-option value="FLAGSHIP">旗舰版</a-select-option>
              <a-select-option value="ENTERPRISE_STANDARD">企业标准版</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="到期时间">
            <a-date-picker
              v-model:value="membershipForm.membershipExpireTime"
              show-time
              style="width: 100%"
              valueFormat="YYYY-MM-DD HH:mm:ss"
            />
          </a-form-item>
        </a-form>
      </div>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  deleteUser,
  listUserVoByPage,
  addUser,
  updateUser,
  resetUserPassword,
  adjustUserBalance,
  updateUserMembership,
  batchDeleteUsers,
} from '@/api/userController.ts'
import { listAllRoles, getUserRoleIds, assignUserRoles } from '@/api/sysRoleController.ts'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  SearchOutlined,
  PlusOutlined,
  DeleteOutlined,
  DownOutlined,
  UserOutlined,
  KeyOutlined,
  WalletOutlined,
  CrownOutlined,
} from '@ant-design/icons-vue'

const columns = [
  { title: 'ID', dataIndex: 'id', width: '80px' },
  { title: '头像', dataIndex: 'userAvatar', width: '70px' },
  { title: '账号', dataIndex: 'userAccount' },
  { title: '用户名', dataIndex: 'userName' },
  { title: '角色', dataIndex: 'userRole', width: '100px' },
  { title: '会员', dataIndex: 'membershipTier', width: '100px' },
  { title: '积分', dataIndex: 'secondsBalance', width: '90px' },
  { title: '创建时间', dataIndex: 'createTime', width: '160px' },
  { title: '操作', key: 'action', width: '120px', fixed: 'right' as const },
]

const data = ref<API.UserVO[]>([])
const total = ref(0)
const selectedRowKeys = ref<number[]>([])

const searchParams = reactive<API.UserQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})

const fetchData = async () => {
  const res = await listUserVoByPage({ ...searchParams })
  if (res.data.data) {
    data.value = res.data.data.records ?? []
    total.value = res.data.data.totalRow ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
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
  searchParams.userAccount = undefined
  searchParams.userName = undefined
  searchParams.userRole = undefined
  searchParams.pageNum = 1
  fetchData()
}

const doDelete = async (id: string) => {
  if (!id) return
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除该用户吗？',
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      const res = await deleteUser({ id })
      if (res.data.code === 0) {
        message.success('删除成功')
        fetchData()
      } else {
        message.error('删除失败')
      }
    },
  })
}

const doBatchDelete = () => {
  if (selectedRowKeys.value.length === 0) return
  Modal.confirm({
    title: '批量删除',
    content: `确定要删除选中的 ${selectedRowKeys.value.length} 个用户吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      const res = await batchDeleteUsers({ ids: selectedRowKeys.value })
      if (res.data.code === 0) {
        message.success(`成功删除 ${res.data.data} 个用户`)
        selectedRowKeys.value = []
        fetchData()
      } else {
        message.error('删除失败，' + res.data.message)
      }
    },
  })
}

// 新增/编辑用户
const userModalVisible = ref(false)
const isEdit = ref(false)
const userForm = reactive<any>({
  userAccount: '',
  userPassword: '',
  userName: '',
  userAvatar: '',
  userProfile: '',
  userRole: 'user',
})

const openAddModal = () => {
  isEdit.value = false
  Object.assign(userForm, {
    userAccount: '',
    userPassword: '',
    userName: '',
    userAvatar: '',
    userProfile: '',
    userRole: 'user',
    id: undefined,
  })
  userModalVisible.value = true
}

const handleSaveUser = async () => {
  if (!userForm.userAccount) {
    message.error('请输入账号')
    return
  }
  if (!isEdit.value && !userForm.userPassword) {
    message.error('请输入密码')
    return
  }
  const res = isEdit.value
    ? await updateUser({ id: userForm.id, userName: userForm.userName, userAvatar: userForm.userAvatar, userProfile: userForm.userProfile, userRole: userForm.userRole })
    : await addUser(userForm)
  if (res.data.code === 0) {
    message.success(isEdit.value ? '更新成功' : '创建成功')
    userModalVisible.value = false
    fetchData()
  } else {
    message.error(res.data.message || '操作失败')
  }
}

// 分配角色
const roleModalVisible = ref(false)
const currentUser = ref<any>(null)
const allRoles = ref<any[]>([])
const checkedRoleIds = ref<number[]>([])

const fetchAllRoles = async () => {
  const res = await listAllRoles()
  if (res.data.code === 0 && res.data.data) {
    allRoles.value = res.data.data
  }
}

const openRoleModal = async (record: any) => {
  currentUser.value = record
  roleModalVisible.value = true
  await fetchAllRoles()
  const res = await getUserRoleIds(record.id)
  if (res.data.code === 0 && res.data.data) {
    checkedRoleIds.value = res.data.data
  }
}

const handleAssignRole = async () => {
  if (!currentUser.value) return
  const res = await assignUserRoles({
    userId: currentUser.value.id,
    roleIds: checkedRoleIds.value,
  })
  if (res.data.code === 0) {
    message.success('角色分配成功')
    roleModalVisible.value = false
  } else {
    message.error('分配失败，' + res.data.message)
  }
}

// 重置密码
const passwordModalVisible = ref(false)
const newPassword = ref('')
const confirmPassword = ref('')

const openPasswordModal = (record: any) => {
  currentUser.value = record
  newPassword.value = ''
  confirmPassword.value = ''
  passwordModalVisible.value = true
}

const handleResetPassword = async () => {
  if (!newPassword.value) {
    message.error('请输入新密码')
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    message.error('两次密码输入不一致')
    return
  }
  const res = await resetUserPassword({
    userId: currentUser.value.id,
    newPassword: newPassword.value,
  })
  if (res.data.code === 0) {
    message.success('密码重置成功')
    passwordModalVisible.value = false
  } else {
    message.error('重置失败，' + res.data.message)
  }
}

// 调整积分
const balanceModalVisible = ref(false)
const adjustAmount = ref<number>(0)
const adjustReason = ref('')

const openBalanceModal = (record: any) => {
  currentUser.value = record
  adjustAmount.value = 0
  adjustReason.value = ''
  balanceModalVisible.value = true
}

const handleAdjustBalance = async () => {
  if (adjustAmount.value === 0) {
    message.error('请输入调整数额')
    return
  }
  const res = await adjustUserBalance({
    userId: currentUser.value.id,
    amount: adjustAmount.value,
    reason: adjustReason.value,
  })
  if (res.data.code === 0) {
    message.success('积分调整成功')
    balanceModalVisible.value = false
    fetchData()
  } else {
    message.error('调整失败，' + res.data.message)
  }
}

// 会员管理
const membershipModalVisible = ref(false)
const membershipForm = reactive<any>({
  membershipTier: 'FREE',
  membershipExpireTime: undefined,
})

const openMembershipModal = (record: any) => {
  currentUser.value = record
  membershipForm.membershipTier = record.membershipTier || 'FREE'
  membershipForm.membershipExpireTime = record.membershipExpireTime
  membershipModalVisible.value = true
}

const handleUpdateMembership = async () => {
  const res = await updateUserMembership({
    userId: currentUser.value.id,
    membershipTier: membershipForm.membershipTier,
    membershipExpireTime: membershipForm.membershipExpireTime,
  })
  if (res.data.code === 0) {
    message.success('会员等级更新成功')
    membershipModalVisible.value = false
    fetchData()
  } else {
    message.error('更新失败，' + res.data.message)
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
#userManagePage {
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

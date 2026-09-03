<template>
  <div class="order-manage-page">
    <div class="page-head">
      <h2 class="page-title">订单管理</h2>
      <span class="page-hint">只读 · 资金记录不提供删除</span>
    </div>
    <AdminTable :columns="columns" :filters="filters" :list-api="listOrdersAdminByPage" ref="tableRef">
      <template #actionExtra="{ record }">
        <a-button size="small" @click="openDetail(record)">详情</a-button>
        <a-popconfirm
          v-if="record.status === 'PENDING'"
          title="确定取消该订单？取消后不可恢复"
          ok-text="取消订单"
          cancel-text="返回"
          @confirm="handleCancel(record)"
        >
          <a-button size="small" danger>取消订单</a-button>
        </a-popconfirm>
        <a-popconfirm
          v-if="record.status !== 'PAID'"
          title="确定将该订单标记为已支付？此操作会发放对应权益"
          ok-text="标记已支付"
          cancel-text="返回"
          @confirm="handleMarkPaid(record)"
        >
          <a-button size="small" type="primary">标记已支付</a-button>
        </a-popconfirm>
      </template>
    </AdminTable>

    <!-- 订单详情弹窗 -->
    <a-modal
      v-model:open="detailOpen"
      title="订单详情"
      :footer="null"
      width="560px"
    >
      <div v-if="detail" class="order-detail">
        <div class="detail-section">
          <div class="detail-title">基本信息</div>
          <a-descriptions :column="1" size="small" bordered>
            <a-descriptions-item label="订单号">
              <span class="copyable" @click="copyText(detail.orderNo)" title="点击复制">
                {{ detail.orderNo }}
                <CopyOutlined />
              </span>
            </a-descriptions-item>
            <a-descriptions-item label="用户">
              {{ detail.userAccount || '-' }} (ID: {{ detail.userId }})
            </a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-tag :color="statusColor(detail.status)">
                {{ statusLabel(detail.status) }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="支付渠道">
              {{ detail.channel || '-' }}
            </a-descriptions-item>
          </a-descriptions>
        </div>

        <div class="detail-section">
          <div class="detail-title">商品信息</div>
          <a-descriptions :column="1" size="small" bordered>
            <a-descriptions-item label="商品名称">
              {{ detail.productName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="商品类型">
              {{ productTypeLabel(detail.productType) }}
            </a-descriptions-item>
            <a-descriptions-item label="数量">
              {{ detail.quantity || 1 }}
            </a-descriptions-item>
            <a-descriptions-item label="金额">
              <span class="amount">¥ {{ formatAmount(detail.amount) }}</span>
            </a-descriptions-item>
          </a-descriptions>
        </div>

        <div class="detail-section">
          <div class="detail-title">时间信息</div>
          <a-descriptions :column="1" size="small" bordered>
            <a-descriptions-item label="创建时间">
              {{ formatTime(detail.createTime) }}
            </a-descriptions-item>
            <a-descriptions-item label="过期时间">
              {{ formatTime(detail.expireTime) }}
            </a-descriptions-item>
            <a-descriptions-item label="支付时间">
              {{ formatTime(detail.payTime) }}
            </a-descriptions-item>
          </a-descriptions>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { CopyOutlined } from '@ant-design/icons-vue'
import AdminTable from '@/components/admin/AdminTable.vue'
import type { AdminColumn, AdminFilter } from '@/components/admin/types'
import {
  listOrdersAdminByPage,
  adminCancelOrder,
  adminMarkPaid,
  type OrderVO,
} from '@/api/payment'
import dayjs from 'dayjs'

const tableRef = ref()
const detailOpen = ref(false)
const detail = ref<OrderVO | null>(null)

const columns: AdminColumn[] = [
  { title: '订单号', dataIndex: 'orderNo', width: 200, ellipsis: true },
  {
    title: '用户',
    dataIndex: 'userAccount',
    width: 140,
    ellipsis: true,
  },
  { title: '商品', dataIndex: 'productName', width: 140, ellipsis: true },
  {
    title: '类型',
    dataIndex: 'productType',
    width: 90,
    type: 'tag',
    tags: [
      { value: 'MEMBERSHIP', label: '会员', color: 'purple' },
      { value: 'SECONDS', label: '积分', color: 'gold' },
    ],
  },
  { title: '数量', dataIndex: 'quantity', width: 60 },
  { title: '金额(¥)', dataIndex: 'amount', width: 100, type: 'money', divide: 1 },
  {
    title: '状态',
    dataIndex: 'status',
    width: 90,
    type: 'tag',
    tags: [
      { value: 'PENDING', label: '待支付', color: 'warning' },
      { value: 'PAID', label: '已支付', color: 'success' },
      { value: 'EXPIRED', label: '已过期', color: 'default' },
    ],
  },
  { title: '渠道', dataIndex: 'channel', width: 80 },
  { title: '创建时间', dataIndex: 'createTime', width: 160, type: 'time' },
  {
    title: '操作',
    key: 'action',
    width: 220,
    fixed: 'right',
  },
]

const filters: AdminFilter[] = [
  { label: '订单号', field: 'orderNo', placeholder: '精确匹配' },
  { label: '用户账号', field: 'userId', placeholder: '按用户ID查' },
  {
    label: '类型',
    field: 'productType',
    type: 'select',
    options: [
      { value: 'MEMBERSHIP', label: '会员' },
      { value: 'SECONDS', label: '积分' },
    ],
  },
  {
    label: '状态',
    field: 'status',
    type: 'select',
    options: [
      { value: 'PENDING', label: '待支付' },
      { value: 'PAID', label: '已支付' },
      { value: 'EXPIRED', label: '已过期' },
    ],
  },
]

function statusColor(status?: string) {
  const map: Record<string, string> = {
    PENDING: 'warning',
    PAID: 'success',
    EXPIRED: 'default',
  }
  return map[status || ''] || 'default'
}

function statusLabel(status?: string) {
  const map: Record<string, string> = {
    PENDING: '待支付',
    PAID: '已支付',
    EXPIRED: '已过期',
  }
  return map[status || ''] || status || '-'
}

function productTypeLabel(type?: string) {
  const map: Record<string, string> = {
    MEMBERSHIP: '会员',
    SECONDS: '积分',
  }
  return map[type || ''] || type || '-'
}

function formatAmount(val: any) {
  if (val === undefined || val === null) return '0.00'
  return Number(val).toFixed(2)
}

function formatTime(val?: string) {
  if (!val) return '-'
  return dayjs(val).format('YYYY-MM-DD HH:mm:ss')
}

function copyText(text?: string) {
  if (!text) return
  navigator.clipboard.writeText(text).then(() => {
    message.success('已复制到剪贴板')
  })
}

function openDetail(record: OrderVO) {
  detail.value = record
  detailOpen.value = true
}

async function handleCancel(record: OrderVO) {
  try {
    const res = await adminCancelOrder(record.id)
    if (res.data?.code === 0) {
      message.success('订单已取消')
      tableRef.value?.refresh()
    } else {
      message.error(res.data?.message || '取消失败')
    }
  } catch (e) {
    console.error(e)
    message.error('取消失败')
  }
}

async function handleMarkPaid(record: OrderVO) {
  try {
    const res = await adminMarkPaid(record.id)
    if (res.data?.code === 0) {
      message.success('已标记为已支付，权益已发放')
      tableRef.value?.refresh()
    } else {
      message.error(res.data?.message || '操作失败')
    }
  } catch (e) {
    console.error(e)
    message.error('操作失败')
  }
}
</script>

<style scoped>
.order-manage-page {
  padding: 8px 0;
}

.page-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 8px;
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

.order-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-section {
  background: #fafafa;
  border-radius: 6px;
  padding: 12px 16px;
}

.detail-title {
  font-size: 14px;
  font-weight: 500;
  color: #1f2329;
  margin-bottom: 8px;
}

.copyable {
  cursor: pointer;
  color: #1677ff;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.copyable:hover {
  opacity: 0.8;
}

.amount {
  color: #ff4d4f;
  font-weight: 500;
  font-size: 15px;
}
</style>

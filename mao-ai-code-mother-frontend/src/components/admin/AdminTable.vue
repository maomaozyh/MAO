<template>
  <div class="admin-table">
    <!-- 搜索区（传了 filters 才渲染） -->
    <a-form
      v-if="filters && filters.length"
      layout="inline"
      :model="searchParams"
      class="admin-table__search"
      @finish="doSearch"
    >
      <a-form-item v-for="f in filters" :key="f.field" :label="f.label">
        <a-select
          v-if="f.type === 'select'"
          v-model:value="searchParams[f.field]"
          :placeholder="f.placeholder || `请选择${f.label}`"
          :options="f.options"
          allow-clear
          style="min-width: 160px"
        />
        <a-input
          v-else
          v-model:value="searchParams[f.field]"
          :placeholder="f.placeholder || `输入${f.label}`"
          allow-clear
        />
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" html-type="submit">搜索</a-button>
          <a-button @click="doReset">重置</a-button>
        </a-space>
      </a-form-item>
    </a-form>
    <a-divider v-if="filters && filters.length" class="admin-table__divider" />

    <!-- 表格 -->
    <a-table
      :columns="columns"
      :data-source="data"
      :pagination="pagination"
      :loading="loading"
      :row-key="rowKey"
      @change="doTableChange"
    >
        <template #bodyCell="{ column, record }">
          <!-- 图片 -->
          <template v-if="column.type === 'image'">
            <a-image
              v-if="record[column.dataIndex]"
              :src="record[column.dataIndex]"
              :width="72"
            />
            <span v-else class="admin-table__empty">—</span>
          </template>

          <!-- 时间 -->
          <template v-else-if="column.type === 'time'">
            {{
              record[column.dataIndex]
                ? dayjs(record[column.dataIndex]).format(
                    column.format || 'YYYY-MM-DD HH:mm:ss'
                  )
                : '—'
            }}
          </template>

          <!-- 状态标签 -->
          <template v-else-if="column.type === 'tag'">
            <a-tag :color="tagColor(column, record[column.dataIndex])">
              {{ tagLabel(column, record[column.dataIndex]) }}
            </a-tag>
          </template>

          <!-- 金额（默认按分存储，divide 可改） -->
          <template v-else-if="column.type === 'money'">
            {{ formatMoney(record[column.dataIndex], column.divide ?? 100) }}
          </template>

          <!-- 操作列 -->
          <template v-else-if="column.key === 'action'">
            <a-space>
              <slot name="actionExtra" :record="record" />
              <a-popconfirm
                v-if="deleteApi"
                title="确定删除这条记录？删除后不可恢复"
                ok-text="删除"
                cancel-text="取消"
                @confirm="doDelete(record)"
              >
                <a-button danger size="small">删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>
  </template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import type { AdminColumn, AdminFilter } from './types'

const props = withDefaults(
  defineProps<{
    columns: AdminColumn[]
    filters?: AdminFilter[]
    /** 分页查询接口，形如 ({ pageNum, pageSize, ...filters }) => Promise */
    listApi: (params: any) => Promise<any>
    /** 删除接口，传了才显示删除按钮 */
    deleteApi?: (params: any) => Promise<any>
    rowKey?: string
    defaultPageSize?: number
    /** 每次查询都带上的固定参数 */
    extraParams?: Record<string, any>
  }>(),
  {
    filters: () => [],
    rowKey: 'id',
    defaultPageSize: 10,
    extraParams: () => ({}),
  }
)

const emit = defineEmits<{
  (e: 'loaded', records: any[], total: number): void
}>()

const data = ref<any[]>([])
const total = ref(0)
const loading = ref(false)

/** 搜索条件：分页参数 + filters 里声明的字段 */
const searchParams = reactive<any>({
  pageNum: 1,
  pageSize: props.defaultPageSize,
  ...props.filters.reduce((acc: any, f) => {
    acc[f.field] = undefined
    return acc
  }, {}),
})

const pagination = computed(() => ({
  current: searchParams.pageNum ?? 1,
  pageSize: searchParams.pageSize ?? 10,
  total: total.value,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`,
}))

const fetchData = async () => {
  loading.value = true
  try {
    // 清掉值为 undefined / 空串的筛选字段，避免后端收到无意义条件
    const payload: Record<string, any> = {
      ...searchParams,
      ...props.extraParams,
    }
    Object.keys(payload).forEach((k) => {
      if (payload[k] === undefined || payload[k] === '') delete payload[k]
    })

    const res = await props.listApi(payload)
    const payloadData = res?.data?.data
    if (payloadData) {
      // MyBatis-Flex Page: { records, totalRow }；也可能是纯数组
      data.value = payloadData.records ?? (Array.isArray(payloadData) ? payloadData : [])
      total.value = payloadData.totalRow ?? (Array.isArray(payloadData) ? payloadData.length : 0)
    } else {
      data.value = []
      total.value = 0
    }
    emit('loaded', data.value, total.value)
  } catch (e: any) {
    console.error('[AdminTable] 加载数据失败', e)
    message.error('获取数据失败')
    data.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

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
  props.filters.forEach((f) => {
    searchParams[f.field] = undefined
  })
  searchParams.pageNum = 1
  fetchData()
}

const doDelete = async (record: any) => {
  if (!props.deleteApi) return
  const id = record?.[props.rowKey]
  if (id === undefined || id === null) return
  try {
    const res = await props.deleteApi({ id })
    if (res?.data?.code === 0) {
      message.success('删除成功')
      fetchData()
    } else {
      message.error(res?.data?.message || '删除失败')
    }
  } catch (e) {
    console.error('[AdminTable] 删除失败', e)
    message.error('删除失败')
  }
}

const tagColor = (column: AdminColumn, value: any) => {
  const hit = column.tags?.find((t) => String(t.value) === String(value))
  return hit?.color || 'default'
}

const tagLabel = (column: AdminColumn, value: any) => {
  const hit = column.tags?.find((t) => String(t.value) === String(value))
  return hit?.label ?? (value ?? '—')
}

const formatMoney = (value: any, divide: number) => {
  if (value === undefined || value === null || value === '') return '—'
  const num = Number(value)
  if (Number.isNaN(num)) return String(value)
  return (num / divide).toFixed(2)
}

onMounted(() => {
  fetchData()
})

/** 供父组件在新增/编辑后主动刷新 */
defineExpose({ refresh: fetchData })
</script>

<style scoped>
.admin-table {
  padding: 24px;
  background: #fff;
  margin-top: 16px;
  border-radius: 8px;
}

.admin-table__divider {
  margin: 12px 0;
}

.admin-table__search {
  margin-bottom: 4px;
}

.admin-table__empty {
  color: #c0c4cc;
}
</style>

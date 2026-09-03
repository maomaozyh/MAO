<template>
  <div class="skill-manage-page">
    <div class="page-head">
      <h2 class="page-title">技能管理</h2>
      <a-button type="primary" @click="openAdd">新增技能</a-button>
    </div>

    <!-- 发现的技能 / 下架 / 全部 切换（发现的技能 = 已上架 status=1） -->
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
      :list-api="listSkillVOByPage"
      :delete-api="deleteSkill"
      :extra-params="extraParams"
    >
      <!-- 操作列里在「删除」前插一个编辑 -->
      <template #actionExtra="{ record }">
        <a-button size="small" @click="openEdit(record)">编辑</a-button>
      </template>
    </AdminTable>

    <!-- 新增 / 编辑弹窗 -->
    <a-modal
      v-model:open="editOpen"
      :title="editing ? '编辑技能' : '新增技能'"
      :confirm-loading="saving"
      ok-text="保存"
      cancel-text="取消"
      width="680px"
      @ok="handleSave"
    >
      <a-form :model="form" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <div class="form-section">基本信息</div>
        <a-form-item label="技能名称" required>
          <a-input v-model:value="form.skillName" placeholder="如：小红书文案生成" />
        </a-form-item>
        <a-form-item label="技能编码">
          <a-input v-model:value="form.skillCode" placeholder="英文标识，如 xiaohongshu_copy" />
        </a-form-item>
        <a-form-item label="分类">
          <a-select v-model:value="form.category" :options="categoryOptions" allow-clear placeholder="选择分类" />
        </a-form-item>
        <a-form-item label="图标">
          <a-input v-model:value="form.icon" placeholder="一个 emoji，如 🚀" />
        </a-form-item>
        <a-form-item label="简介">
          <a-textarea v-model:value="form.skillDesc" :rows="2" placeholder="一句话说明这个技能做什么" />
        </a-form-item>
        <a-form-item label="标签">
          <a-input v-model:value="form.tags" placeholder="多个标签用逗号分隔，如：写作,营销,爆款" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="form.status" :options="statusOptions" />
        </a-form-item>

        <div class="form-section">价格设置</div>
        <a-form-item label="价格">
          <a-input v-model:value="form.price" placeholder="如：免费 / 9.9" />
        </a-form-item>
        <a-form-item label="原价">
          <a-input v-model:value="form.originalPrice" placeholder="划线价，如：19.9" />
        </a-form-item>
        <a-form-item label="价格单位">
          <a-input v-model:value="form.priceUnit" placeholder="如：次 / 月 / 永久" />
        </a-form-item>

        <div class="form-section">AI 配置（核心）</div>
        <a-form-item label="系统提示词">
          <a-textarea
            v-model:value="form.systemPrompt"
            :rows="6"
            placeholder="定义 AI 的角色、风格、输出规范等，例如：&#10;你是一名资深小红书文案写手，擅长创作爆款笔记。&#10;要求：&#10;1. 标题要有吸引力，多用emoji&#10;2. 正文分点论述，口语化表达&#10;3. 结尾加相关话题标签"
          />
        </a-form-item>
        <a-form-item label="模型类型">
          <a-select v-model:value="form.modelType" :options="modelTypeOptions" />
        </a-form-item>
        <a-form-item label="采样温度">
          <a-slider
            v-model:value="form.temperature"
            :min="0"
            :max="1"
            :step="0.1"
            :marks="{ 0: '严谨', 0.5: '平衡', 1: '创意' }"
          />
        </a-form-item>

        <div class="form-section">详情介绍</div>
        <a-form-item label="功能介绍">
          <a-textarea v-model:value="form.featureDesc" :rows="3" placeholder="详细介绍这个技能的功能和亮点" />
        </a-form-item>
        <a-form-item label="使用说明">
          <a-textarea v-model:value="form.usageDesc" :rows="3" placeholder="告诉用户如何使用这个技能效果最好" />
        </a-form-item>

        <div class="form-section">MCP 工具配置</div>
        <a-form-item label="MCP 服务器">
          <a-textarea
            v-model:value="form.mcpServers"
            :rows="8"
            placeholder='JSON 数组格式，每个服务器含 name/type/url/headers。例如：&#10;[&#10;  {&#10;    "name": "魔塔搜索",&#10;    "type": "sse",&#10;    "url": "https://mcp.modelscope.cn/v1/xxx/sse",&#10;    "headers": { "Authorization": "Bearer xxx" }&#10;  }&#10;]'
          />
        </a-form-item>
        <div class="mcp-tip">
          <p>💡 支持的传输类型：<code>sse</code>（魔塔 MCP 推荐）、<code>streamable-http</code></p>
          <p>配置后，使用该技能的 AI 对话将自动加载对应 MCP 服务器提供的工具。</p>
        </div>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { message } from 'ant-design-vue'
import AdminTable from '@/components/admin/AdminTable.vue'
import type { AdminColumn, AdminFilter } from '@/components/admin/types'
import { listSkillVOByPage, addSkill, updateSkill, deleteSkill } from '@/api/skillController'
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

const statusOptions = [
  { value: 1, label: '已上架' },
  { value: 0, label: '已下架' },
]

const modelTypeOptions = [
  { value: 'DEFAULT', label: '默认模型（平衡速度与质量）' },
  { value: 'REASONING', label: '推理模型（复杂任务更精准）' },
]

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
  { title: '简介', dataIndex: 'skillDesc', ellipsis: true },
  { title: '使用次数', dataIndex: 'usageCount', width: 100 },
  {
    title: '状态',
    dataIndex: 'status',
    width: 100,
    type: 'tag',
    tags: [
      { value: 1, label: '已上架', color: 'green' },
      { value: 0, label: '已下架', color: 'default' },
    ],
  },
  { title: '创建时间', dataIndex: 'createTime', width: 170, type: 'time' },
  { title: '操作', key: 'action', width: 140 },
]

const filters: AdminFilter[] = [
  { label: '技能名称', field: 'skillName', placeholder: '按名称模糊搜索' },
  { label: '分类', field: 'category', type: 'select', options: categoryOptions },
]

/** 发现的技能 / 下架 / 全部：发现的技能 = 已上架(status=1) */
const viewStatus = ref<'published' | 'off' | 'all'>('published')
const viewStatusOptions = [
  { label: '发现的技能', value: 'published' },
  { label: '已下架', value: 'off' },
  { label: '全部', value: 'all' },
]
const extraParams = computed(() => {
  if (viewStatus.value === 'published') return { status: 1 }
  if (viewStatus.value === 'off') return { status: 0 }
  return {}
})
const onViewStatusChange = () => tableRef.value?.refresh()

const tableRef = ref<InstanceType<typeof AdminTable> | null>(null)
const editOpen = ref(false)
const saving = ref(false)
const editing = ref<API.SkillVO | null>(null)

const form = reactive<API.SkillAddRequest & { status?: number; temperature?: number; mcpServers?: string }>({
  skillName: '',
  skillCode: '',
  category: undefined,
  icon: '',
  skillDesc: '',
  tags: '',
  status: 1,
  price: '免费',
  originalPrice: '',
  priceUnit: '',
  systemPrompt: '',
  modelType: 'DEFAULT',
  temperature: 0.7,
  featureDesc: '',
  usageDesc: '',
  mcpServers: '',
})

const resetForm = () => {
  form.skillName = ''
  form.skillCode = ''
  form.category = undefined
  form.icon = ''
  form.skillDesc = ''
  form.tags = ''
  form.status = 1
  form.price = '免费'
  form.originalPrice = ''
  form.priceUnit = ''
  form.systemPrompt = ''
  form.modelType = 'DEFAULT'
  form.temperature = 0.7
  form.featureDesc = ''
  form.usageDesc = ''
  form.mcpServers = ''
}

const openAdd = () => {
  editing.value = null
  resetForm()
  editOpen.value = true
}

const openEdit = (record: API.SkillVO) => {
  editing.value = record
  form.skillName = record.skillName ?? ''
  form.skillCode = record.skillCode ?? ''
  form.category = record.category
  form.icon = record.icon ?? ''
  form.skillDesc = record.skillDesc ?? ''
  form.tags = record.tags ?? ''
  form.status = record.status ?? 1
  form.price = record.price ?? '免费'
  form.originalPrice = record.originalPrice ?? ''
  form.priceUnit = record.priceUnit ?? ''
  form.systemPrompt = record.systemPrompt ?? ''
  form.modelType = record.modelType ?? 'DEFAULT'
  form.temperature = record.temperature ?? 0.7
  form.featureDesc = record.featureDesc ?? ''
  form.usageDesc = record.usageDesc ?? ''
  form.mcpServers = record.mcpServers ?? ''
  editOpen.value = true
}

const handleSave = async () => {
  if (!form.skillName?.trim()) {
    message.warning('请填写技能名称')
    return
  }
  saving.value = true
  try {
    const payload: any = {
      skillName: form.skillName.trim(),
      skillCode: form.skillCode?.trim() || undefined,
      category: form.category,
      icon: form.icon,
      skillDesc: form.skillDesc,
      tags: form.tags,
      status: form.status,
      price: form.price,
      originalPrice: form.originalPrice,
      priceUnit: form.priceUnit,
      systemPrompt: form.systemPrompt,
      modelType: form.modelType,
      temperature: form.temperature,
      featureDesc: form.featureDesc,
      usageDesc: form.usageDesc,
      mcpServers: form.mcpServers || undefined,
    }
    let res
    if (editing.value?.id) {
      payload.id = editing.value.id
      res = await updateSkill(payload)
    } else {
      res = await addSkill(payload)
    }
    if (res?.data?.code === 0) {
      message.success(editing.value ? '修改成功' : '新增成功')
      editOpen.value = false
      tableRef.value?.refresh()
    } else {
      message.error(res?.data?.message || '保存失败')
    }
  } catch (e) {
    console.error(e)
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.skill-manage-page {
  padding: 8px 0;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.status-segmented {
  margin-bottom: 12px;
}

.form-section {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin: 8px 0 12px;
  padding-left: 8px;
  border-left: 3px solid #1677ff;
  line-height: 1;
}

.page-title {
  font-size: 20px;
  font-weight: 500;
  color: #1f2329;
  margin: 0;
}

.mcp-tip {
  margin-left: 84px;
  padding: 10px 12px;
  background: #f0f7ff;
  border-radius: 6px;
  font-size: 12px;
  color: #4b5563;
  line-height: 1.6;
}

.mcp-tip p {
  margin: 0;
}

.mcp-tip code {
  background: #e0eaff;
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 11px;
  color: #1677ff;
}
</style>

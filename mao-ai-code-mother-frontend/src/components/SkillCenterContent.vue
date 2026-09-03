<template>
  <!-- 左侧内容 -->
  <div class="content">
    <!-- 顶部 Tabs（发现/我的） -->
    <div v-if="showMyTab" class="tabs">
      <div
        class="tab"
        :class="{ active: activeNav === 'discover' }"
        @click="activeNav = 'discover'"
      >发现</div>
      <div
        class="tab"
        :class="{ active: activeNav === 'mine' }"
        @click="activeNav = 'mine'"
      >我的</div>
    </div>

    <!-- 轮播图（由父组件传入，位于 tabs 之后、分类筛选之前） -->
    <slot name="banner" />

    <!-- 分类筛选 + 我的 tab 下的新建按钮 -->
    <div class="category-bar-wrap">
      <div class="category-bar">
        <div
          v-for="cat in categories"
          :key="cat.key"
          class="category"
          :class="{ active: activeCategory === cat.key, more: cat.key === 'allMore' }"
          @click="activeCategory = cat.key"
        >
          {{ cat.label }}
        </div>
      </div>
      <!-- 我的技能：新建/上传技能按钮 -->
      <div
        v-if="showMyTab && activeNav === 'mine' && loginUser.id"
        class="skill-actions-group"
      >
        <button
          class="create-skill-btn-inline"
          type="button"
          @click="openCreate"
        >
          <span class="plus">+</span>新建技能
        </button>
        <button
          class="upload-skill-btn-inline"
          type="button"
          @click="triggerUpload"
        >
          <span class="upload-icon">↑</span>上传技能
        </button>
      </div>
    </div>

    <!-- 技能卡片网格 -->
    <div class="skill-grid">
      <div v-if="loading" class="empty-tip">加载中...</div>
      <div v-else-if="displayCards.length === 0" class="empty-tip">暂无技能</div>
      <div
        v-else
        v-for="(card, i) in displayCards"
        :key="card.id"
        class="skill-card"
        @click="openDetail(card)"
      >
        <div class="skill-header">
          <div class="skill-icon" :class="iconColors[i % iconColors.length]">{{ card.icon }}</div>
          <div class="skill-title-wrap">
            <div class="skill-title-row">
              <span class="skill-title">{{ card.title }}</span>
              <span v-if="card.hasDiscount" class="tag-discount">限时折扣</span>
            </div>
            <div class="skill-author">{{ card.official }}</div>
          </div>
        </div>
        <div class="skill-desc">{{ card.desc }}</div>
        <div class="skill-footer">
          <span v-if="card.price === '免费'" class="price free">免费</span>
          <span v-else class="price">
            {{ card.price }}<span class="unit">{{ card.originalPrice ? card.originalPrice + ' ' : '' }}{{ card.priceUnit || '积分/次' }}</span>
          </span>
          <span class="usage">👤 {{ card.usage }}</span>
        </div>
        <div class="card-actions" v-if="canManageSkill(card)">
          <span class="link-btn" @click.stop="openEdit(card)">编辑</span>
          <span class="link-btn danger" @click.stop="handleDelete(card)">删除</span>
        </div>
      </div>
    </div>

    <!-- 加载更多 -->
    <div
      v-if="!loading && !loadingMore && cardsData.length > 0 && pageNum * pageSize < total"
      class="load-more-wrap"
    >
      <button class="load-more-btn" @click="loadMore">加载更多</button>
    </div>
  </div>

  <!-- 上传技能弹窗 -->
  <a-modal
    :open="uploadModalVisible"
    title="上传技能"
    :footer="null"
    width="480"
    centered
    @update:open="(v: boolean) => (uploadModalVisible = v)"
  >
    <div
      class="upload-drop-area"
      :class="{ 'drag-over': isDragOver, 'has-file': !!uploadFile }"
      @dragover.prevent="isDragOver = true"
      @dragleave.prevent="isDragOver = false"
      @drop.prevent="handleDrop"
      @click="triggerUploadInput"
    >
      <template v-if="!uploadFile">
        <div class="upload-icon-big">📄</div>
        <div class="upload-tip-text">拖放或点击上传</div>
      </template>
      <template v-else>
        <div class="upload-icon-big">✅</div>
        <div class="upload-tip-text">已选择：{{ uploadFile.name }}</div>
        <div class="upload-rechoose" @click.stop="triggerUploadInput">重新选择</div>
      </template>
      <input
        ref="uploadFileInputRef"
        type="file"
        accept=".json,.zip,.skill"
        style="display: none"
        @change="handleFileSelect"
      />
    </div>
    <div class="upload-hints">
      <div class="hint-item">
        <span class="hint-dot"></span>
        包含根级 SKILL.md 文件的 zip 或 .skill 文件（后端自动解析创建）
      </div>
      <div class="hint-item">
        <span class="hint-dot"></span>
        也支持直接上传技能配置 .json 文件（导入后可在弹窗内复核）
      </div>
    </div>
    <div class="upload-footer">
      <a-button :disabled="uploading" @click="uploadModalVisible = false">取消</a-button>
      <a-button
        type="primary"
        :loading="uploading"
        :disabled="!uploadFile || uploading"
        @click="confirmUpload"
      >{{ uploading ? '上传中...' : '确认上传' }}</a-button>
    </div>
  </a-modal>

  <!-- 创建/编辑技能弹窗 -->
  <a-modal
    :open="modalVisible"
    :title="modalMode === 'create' ? '新建技能' : '编辑技能'"
    :footer="null"
    width="520"
    class="skill-modal"
    @update:open="(v: boolean) => (modalVisible = v)"
  >
    <a-form layout="vertical" :model="form">
      <div class="form-section">基本信息</div>
      <a-form-item label="技能名称" required>
        <a-input v-model:value="form.skillName" placeholder="如：小红书文案生成" maxlength="50" />
      </a-form-item>
      <a-row :gutter="12">
        <a-col :span="12">
          <a-form-item label="图标 (Emoji)">
            <a-input v-model:value="form.icon" placeholder="例如：🚀  留空则使用默认图标" maxlength="10" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="分类">
            <a-select v-model:value="form.category" placeholder="请选择分类" allow-clear>
              <a-select-option v-for="cat in categoryOptions" :key="cat.key" :value="cat.key">
                {{ cat.label }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="技能简介">
        <a-textarea v-model:value="form.skillDesc" placeholder="一句话说明这个技能做什么" :rows="2" maxlength="200" show-count />
      </a-form-item>
      <a-form-item label="标签">
        <a-input v-model:value="form.tags" placeholder="多个标签用英文逗号分隔，如：AI,创作,效率" />
      </a-form-item>

      <div class="form-section">AI 配置（核心）</div>
      <a-form-item label="系统提示词" required>
        <a-textarea
          v-model:value="form.systemPrompt"
          :rows="4"
          placeholder="定义 AI 的角色、风格、输出规范等，例如：&#10;你是一名资深小红书文案写手，擅长创作爆款笔记。&#10;要求：&#10;1. 标题要有吸引力，多用emoji&#10;2. 正文分点论述，口语化表达&#10;3. 结尾加相关话题标签"
        />
      </a-form-item>
      <a-row :gutter="12">
        <a-col :span="12">
          <a-form-item label="模型类型">
            <a-select v-model:value="form.modelType">
              <a-select-option value="DEFAULT">默认模型（平衡速度与质量）</a-select-option>
              <a-select-option value="REASONING">推理模型（复杂任务更精准）</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="采样温度">
            <a-slider
              v-model:value="form.temperature"
              :min="0"
              :max="1"
              :step="0.1"
              :marks="{ 0: '严谨', 0.5: '平衡', 1: '创意' }"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="form-section">详情介绍</div>
      <a-form-item label="功能介绍">
        <a-textarea v-model:value="form.featureDesc" placeholder="详细介绍这个技能的功能和亮点" :rows="2" maxlength="500" show-count />
      </a-form-item>
      <a-form-item label="使用说明">
        <a-textarea v-model:value="form.usageDesc" placeholder="告诉用户如何使用这个技能效果最好" :rows="2" maxlength="500" show-count />
      </a-form-item>

      <div class="form-section">价格设置</div>
      <a-row :gutter="12">
        <a-col :span="8">
          <a-form-item label="价格">
            <a-input v-model:value="form.price" placeholder="免费 / 10" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="原价">
            <a-input v-model:value="form.originalPrice" placeholder="可选，用于显示折扣" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="价格单位">
            <a-input v-model:value="form.priceUnit" placeholder="积分/次" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item v-if="isAdmin" label="状态">
        <a-radio-group v-model:value="form.status">
          <a-radio :value="1">上架</a-radio>
          <a-radio :value="0">下架</a-radio>
        </a-radio-group>
      </a-form-item>
    </a-form>
    <div class="modal-footer">
      <a-button @click="modalVisible = false">取消</a-button>
      <a-button type="primary" :loading="submitting" @click="handleSubmit">
        {{ modalMode === 'create' ? '创建' : '保存' }}
      </a-button>
    </div>
  </a-modal>

  <!-- 技能详情抽屉 -->
  <a-drawer
    :open="detailVisible"
    :title="detailSkill?.skillName || '技能详情'"
    width="480"
    :footer="null"
    @update:open="(v: boolean) => (detailVisible = v)"
  >
    <div v-if="detailSkill">
      <div class="detail-head">
        <span class="detail-icon">{{ detailSkill.icon || '🛠️' }}</span>
        <div class="detail-head-info">
          <div class="detail-name">{{ detailSkill.skillName }}</div>
          <div class="detail-meta">
            分类：{{ detailSkill.category || '-' }} ｜ 价格：{{ detailSkill.price || '免费' }} ｜ 使用：{{ formatUsage(detailSkill.usageCount) }}
          </div>
        </div>
      </div>
      <div class="detail-section">
        <div class="detail-label">介绍</div>
        <div class="detail-text">{{ detailSkill.skillDesc || '暂无' }}</div>
      </div>
      <div class="detail-section">
        <div class="detail-label">功能介绍</div>
        <div class="detail-text">{{ detailSkill.featureDesc || '暂无' }}</div>
      </div>
      <div class="detail-section">
        <div class="detail-label">使用流程说明</div>
        <div class="detail-text">{{ detailSkill.usageDesc || '暂无' }}</div>
      </div>
      <div class="detail-section" v-if="detailSkill.tags">
        <div class="detail-label">标签</div>
        <div class="detail-tags">
          <span class="detail-tag" v-for="t in (detailSkill.tags || '').split(',').filter(Boolean)" :key="t">{{ t }}</span>
        </div>
      </div>
      <div class="detail-actions">
        <button class="use-btn" @click="onUseSkill">使用该技能</button>
      </div>
    </div>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import {
  listSkillVOByPage,
  getSkillVOById,
  getSkillCenterConfig,
  addSkill,
  updateSkill,
  deleteSkill,
  uploadSkill,
} from '@/api/skillController'

const props = withDefaults(
  defineProps<{
    /** 是否显示「我的」tab 和管理员操作（创建/编辑/删除）；独立页面=true，弹窗内=false */
    showMyTab?: boolean
    /** 是否自动加载（弹窗打开时通常需要）；默认 true */
    autoLoad?: boolean
    /** 外部传入的搜索关键词（受控），来自页面右上角搜索框；空字符串 = 不过滤 */
    searchKey?: string
  }>(),
  { showMyTab: true, autoLoad: true, searchKey: '' },
)

const emit = defineEmits<{
  /** 用户点击「使用该技能」时触发；父组件可在弹窗里关闭并选用技能 */
  (e: 'use', skill: API.SkillVO): void
}>()

const router = useRouter()
const loginUserStore = useLoginUserStore()
const loginUser = loginUserStore.loginUser
const isAdmin = computed(() => loginUser.userRole === 'admin')
const goLogin = () => router.push('/user/login')

const PAGE_SIZE = 30

const activeNav = ref<'discover' | 'mine'>('discover')
const activeCategory = ref('all')

const categories = ref([
  { key: 'all', label: '全部' },
  { key: 'create', label: '内容创作与生成' },
  { key: 'understand', label: '内容理解与处理' },
  { key: 'voice', label: '语音交互' },
  { key: 'search', label: '搜索查询' },
  { key: 'office', label: '办公提效' },
  { key: 'design', label: '设计美化' },
  { key: 'pay', label: '支付交易' },
  { key: 'auth', label: '登录验证' },
  { key: 'map', label: '地图出行' },
  { key: 'allMore', label: '全部 ▾' },
])

/** 创建/编辑弹窗中的分类选项（排除"全部"和"更多"） */
const categoryOptions = computed(() =>
  categories.value.filter((c) => c.key !== 'all' && c.key !== 'allMore'),
)

const iconColors = ['icon-purple', 'icon-orange', 'icon-pink', 'icon-blue', 'icon-yellow', 'icon-green', 'icon-cyan', 'icon-red', 'icon-gray']

const cardsData = ref<API.SkillVO[]>([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = PAGE_SIZE
const total = ref(0)
const loadingMore = ref(false)

const formatUsage = (count?: number) => {
  if (!count) return '0'
  if (count >= 1000) return (count / 1000).toFixed(1) + 'k'
  return String(count)
}

const displayCards = computed(() =>
  cardsData.value.map((s) => ({
    id: s.id,
    icon: s.icon || '🛠️',
    title: s.skillName,
    official: s.user?.userName || (s.userId ? `用户${s.userId}` : '官方'),
    desc: s.skillDesc || '',
    price: s.price || '免费',
    originalPrice: s.originalPrice,
    priceUnit: s.priceUnit,
    hasDiscount: !!s.originalPrice && s.originalPrice !== s.price && s.price !== '免费',
    usage: formatUsage(s.usageCount),
    category: s.category,
    userId: s.userId,
  })),
)

const fetchSkills = async (isLoadMore = false) => {
  if (isLoadMore) {
    if (loadingMore.value || pageNum.value * pageSize >= total.value) return
    loadingMore.value = true
  } else {
    loading.value = true
  }
  try {
    const params: API.SkillQueryRequest = {
      pageNum: isLoadMore ? pageNum.value + 1 : 1,
      pageSize,
      sortField: 'createTime',
      sortOrder: 'descend',
    }
    if (activeNav.value === 'mine') {
      // 「我的」= 玩家上传的技能，严格区别于「发现」(status=1 的公开库)
      // 只查 status=0（刚上传、待审核、未进发现页的 UGC），与后台「上传技能」管理页同一桶。
      params.status = 0
    } else {
      params.status = 1
    }
    // 分类与搜索过滤在「发现 / 我的」两个 tab 通用（我的 = 在 status=0 待审核桶内再筛选）
    if (
      activeCategory.value &&
      activeCategory.value !== 'all' &&
      activeCategory.value !== 'allMore'
    ) {
      params.category = activeCategory.value
    }
    if (props.searchKey.trim()) {
      params.skillName = props.searchKey.trim()
    }
    let records: API.SkillVO[] = []
    let totalCount = 0
    try {
      const res = await listSkillVOByPage(params)
      if (res.data.code === 0 && res.data.data) {
        records = res.data.data.records || []
        totalCount = res.data.data.totalRow ?? records.length
      } else {
        throw new Error(res.data.message || '未知错误')
      }
    } catch (e) {
      // 后端不可用时回退到 mock 数据
      console.warn('[SkillCenter] 接口获取失败，回退 mock 数据：', e)
      let mockRecords = getMockSkills()
      if (
        activeNav.value !== 'mine' &&
        activeCategory.value &&
        activeCategory.value !== 'all' &&
        activeCategory.value !== 'allMore'
      ) {
        mockRecords = mockRecords.filter((s) => s.category === activeCategory.value)
      }
      const kw = props.searchKey.trim().toLowerCase()
      if (kw) {
        mockRecords = mockRecords.filter((s) => (s.skillName || '').toLowerCase().includes(kw))
      }
      records = mockRecords
      totalCount = records.length
    }
    if (isLoadMore) {
      pageNum.value += 1
      cardsData.value = [...cardsData.value, ...records]
    } else {
      pageNum.value = 1
      cardsData.value = records
    }
    total.value = totalCount
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const loadMore = () => fetchSkills(true)

const detailVisible = ref(false)
const detailSkill = ref<API.SkillVO | null>(null)
const openDetail = async (card: { id?: number }) => {
  const local = cardsData.value.find((s) => String(s.id) === String(card.id))
  if (local) {
    detailSkill.value = local
    detailVisible.value = true
  }
  if (!card.id) return
  try {
    const res = await getSkillVOById({ id: card.id })
    if (res.data.code === 0 && res.data.data) {
      detailSkill.value = res.data.data
      detailVisible.value = true
    }
  } catch (e) {
    console.warn('[SkillCenter] 详情获取失败，使用本地数据：', e)
  }
}

const onUseSkill = () => {
  if (detailSkill.value) emit('use', detailSkill.value)
}

// 创建/编辑技能
const modalVisible = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const submitting = ref(false)
const form = reactive<API.SkillAddRequest & { id?: number; status?: number }>({
  id: undefined,
  skillName: '',
  skillCode: '',
  skillDesc: '',
  featureDesc: '',
  usageDesc: '',
  icon: '',
  category: '',
  price: '',
  originalPrice: '',
  priceUnit: '',
  tags: '',
  systemPrompt: '',
  modelType: 'DEFAULT',
  temperature: 0.7,
  mcpServers: '',
  status: 1,
})

const resetForm = () => {
  form.id = undefined
  form.skillName = ''
  form.skillCode = ''
  form.skillDesc = ''
  form.featureDesc = ''
  form.usageDesc = ''
  form.icon = ''
  form.category = ''
  form.price = ''
  form.originalPrice = ''
  form.priceUnit = ''
  form.tags = ''
  form.systemPrompt = ''
  form.modelType = 'DEFAULT'
  form.temperature = 0.7
  form.mcpServers = ''
  form.status = 1
}

const openEdit = (card: { id?: number }) => {
  const skill = cardsData.value.find((s) => s.id === card.id)
  if (!skill) return
  modalMode.value = 'edit'
  form.id = skill.id
  form.skillName = skill.skillName
  form.skillCode = skill.skillCode || ''
  form.skillDesc = skill.skillDesc
  form.featureDesc = skill.featureDesc
  form.usageDesc = skill.usageDesc
  form.icon = skill.icon
  form.category = skill.category
  form.price = skill.price
  form.originalPrice = skill.originalPrice
  form.priceUnit = skill.priceUnit
  form.tags = skill.tags
  form.systemPrompt = skill.systemPrompt || ''
  form.modelType = skill.modelType || 'DEFAULT'
  form.temperature = skill.temperature ?? 0.7
  form.mcpServers = skill.mcpServers || ''
  form.status = skill.status ?? 1
  modalVisible.value = true
}

const openCreate = () => {
  resetForm()
  modalMode.value = 'create'
  modalVisible.value = true
}

/** 上传技能弹窗 */
const uploadModalVisible = ref(false)
const uploadFile = ref<File | null>(null)
const isDragOver = ref(false)
const uploading = ref(false)
const uploadFileInputRef = ref<HTMLInputElement | null>(null)

const triggerUpload = () => {
  uploadFile.value = null
  uploadModalVisible.value = true
}

const triggerUploadInput = () => {
  uploadFileInputRef.value?.click()
}

const handleDrop = (e: DragEvent) => {
  isDragOver.value = false
  const file = e.dataTransfer?.files?.[0]
  if (!file) return
  validateAndSetFile(file)
}

const handleFileSelect = (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  validateAndSetFile(file)
  input.value = ''
}

const validateAndSetFile = (file: File) => {
  const name = file.name.toLowerCase()
  if (!name.endsWith('.json') && !name.endsWith('.zip') && !name.endsWith('.skill')) {
    message.error('仅支持 .json、.zip 或 .skill 格式的文件')
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    message.error('文件大小不能超过 10MB')
    return
  }
  uploadFile.value = file
}

/** 解析 JSON 技能配置并填充表单 */
const parseSkillJson = (text: string, fileName: string) => {
  const data = JSON.parse(text)
  resetForm()
  form.skillName = data.skillName || data.name || fileName.replace(/\.(json|zip|skill)$/i, '')
  form.skillCode = data.skillCode || data.code || ''
  form.skillDesc = data.skillDesc || data.description || ''
  form.featureDesc = data.featureDesc || ''
  form.usageDesc = data.usageDesc || ''
  form.icon = data.icon || ''
  form.category = data.category || ''
  form.price = data.price || ''
  form.originalPrice = data.originalPrice || ''
  form.priceUnit = data.priceUnit || ''
  form.tags = data.tags || (Array.isArray(data.tagList) ? data.tagList.join(',') : '')
  form.systemPrompt = data.systemPrompt || data.prompt || ''
  form.modelType = data.modelType || 'DEFAULT'
  form.temperature = data.temperature ?? 0.7
  form.mcpServers = data.mcpServers || ''
  form.status = 1
}

const confirmUpload = async () => {
  if (!uploadFile.value || uploading.value) return
  const file = uploadFile.value
  const name = file.name.toLowerCase()

  try {
    if (name.endsWith('.json')) {
      // JSON：本地解析填表，用户在「新建技能」弹窗复核后再创建（可改）
      const text = await file.text()
      parseSkillJson(text, file.name)
      uploadModalVisible.value = false
      modalMode.value = 'create'
      modalVisible.value = true
      message.success('技能配置已导入，请确认后创建')
      return
    }

    // .zip / .skill：上传到后端自动解析 SKILL.md 并创建
    uploading.value = true
    const formData = new FormData()
    formData.append('file', file)
    const res = await uploadSkill(formData)
    if (res.data?.code === 0 && res.data.data) {
      message.success('技能上传成功，已自动创建')
      uploadModalVisible.value = false
      uploadFile.value = null
      // 切到「我的」并刷新列表
      activeNav.value = 'mine'
      pageNum.value = 1
      await fetchSkills()
    } else {
      message.error((res.data?.message as string) || '上传失败，请检查文件内容')
    }
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '上传失败，请检查文件内容'
    message.error(msg)
  } finally {
    uploading.value = false
  }
}

/** 判断当前用户是否可管理该技能（管理员 或 自己创建的技能） */
const canManageSkill = (card: { id?: number; userId?: number }) => {
  if (isAdmin.value) return true
  if (!loginUser.value?.id) return false
  const skill = cardsData.value.find((s) => s.id === card.id)
  return skill?.userId === loginUser.value.id
}

const handleSubmit = async () => {
  if (!form.skillName || !form.skillName.trim()) {
    message.error('技能名称不能为空')
    return
  }
  if (!form.systemPrompt || !form.systemPrompt.trim()) {
    message.error('系统提示词不能为空，这是技能的核心配置')
    return
  }
  submitting.value = true
  try {
    const res =
      modalMode.value === 'create'
        ? await addSkill({
            skillName: form.skillName,
            skillCode: form.skillCode,
            skillDesc: form.skillDesc,
            featureDesc: form.featureDesc,
            usageDesc: form.usageDesc,
            icon: form.icon,
            category: form.category,
            price: form.price,
            originalPrice: form.originalPrice,
            priceUnit: form.priceUnit,
            tags: form.tags,
            systemPrompt: form.systemPrompt,
            modelType: form.modelType,
            temperature: form.temperature,
            mcpServers: form.mcpServers,
          })
        : await updateSkill({
            id: form.id,
            skillName: form.skillName,
            skillCode: form.skillCode,
            skillDesc: form.skillDesc,
            featureDesc: form.featureDesc,
            usageDesc: form.usageDesc,
            icon: form.icon,
            category: form.category,
            price: form.price,
            originalPrice: form.originalPrice,
            priceUnit: form.priceUnit,
            tags: form.tags,
            systemPrompt: form.systemPrompt,
            modelType: form.modelType,
            temperature: form.temperature,
            mcpServers: form.mcpServers,
            status: form.status,
          })
    if (res.data.code === 0) {
      message.success(modalMode.value === 'create' ? '创建成功' : '修改成功')
      modalVisible.value = false
      fetchSkills()
    } else {
      message.error((modalMode.value === 'create' ? '创建失败：' : '修改失败：') + (res.data.message || '未知错误'))
    }
  } catch (e) {
    message.error(modalMode.value === 'create' ? '创建失败' : '修改失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = (card: { id?: number; title?: string }) => {
  Modal.confirm({
    title: '确认删除该技能？',
    content: card.title,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      const res = await deleteSkill({ id: card.id! })
      if (res.data.code === 0) {
        message.success('删除成功')
        fetchSkills()
      } else {
        message.error('删除失败：' + (res.data.message || '未知错误'))
      }
    },
  })
}

// 后端配置
const loadSkillCenterConfig = async () => {
  try {
    const res = await getSkillCenterConfig()
    if (res.data.code === 0 && res.data.data) {
      // noop for now (banner/quota handled at page level)
    }
  } catch (e) {
    /* 静默 */
  }
}

watch(activeNav, () => {
  activeCategory.value = 'all'
  fetchSkills()
})
watch(activeCategory, () => fetchSkills())
let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(() => props.searchKey, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(fetchSkills, 400)
})

if (props.autoLoad) {
  onMounted(() => {
    loadSkillCenterConfig()
    fetchSkills()
  })
}

// mock 兜底数据（与 SkillCenterPage 同步，后端不可用时显示）
const getMockSkills = (): API.SkillVO[] => [
  { id: 3, skillName: '应用宠物', skillDesc: '提供应用内陪伴式宠物设计与接入能力，支持Web和通用任务，可进行角色与性格定制、应用事件反馈和互动状态设置。', icon: '🐶', usageCount: 179, category: 'create', status: 1 },
  { id: 4, skillName: 'three.js 3D创作', skillDesc: '提供 three.js 3D 创作能力，支持Web和通用任务，可构建网页中的三维场景、动态视觉、沉浸式数字艺术与交互体验。', icon: '🧊', usageCount: 8500, userId: 999, status: 1 },
  { id: 5, skillName: 'GSAP网页动效设计', skillDesc: '提供 GSAP 动画引擎的调用与应用能力，支持Web和通用任务，可打造流畅、专业级的动态交互与过渡效果。', icon: '🎨', usageCount: 2900, userId: 999, status: 1 },
  { id: 6, skillName: 'p5.js 创意编程', skillDesc: '提供 p5.js 创意编程库的调用与应用能力，支持Web和通用任务，快速实现交互式图形、动画、生成艺术与视觉实验。', icon: '🌸', usageCount: 2000, userId: 999, status: 1 },
  { id: 7, skillName: '产品设计助手', skillDesc: '提供产品与用户体验设计能力，支持Web和通用任务，可完成产品设计、原型设计、交互设计、用户体验评审等。', icon: '✏️', usageCount: 577, status: 1 },
  { id: 8, skillName: '登录', skillDesc: '提供完整的用户登录与注册能力，支持用户名、邮箱、手机号、第三方账号及微信登录，覆盖主流接入方式。', icon: '🔐', usageCount: 376400, status: 1 },
  { id: 9, skillName: '文本生成大模型', skillDesc: '使用文本生成大模型为应用构建AI功能，能够对文本内容进行理解、生成、改写、润色等处理。', icon: '📝', usageCount: 0, status: 1 },
  { id: 10, skillName: '图片生成与编辑（超级版）', skillDesc: '提供旗舰级图片生成与编辑能力，具备行业领先的画面表现与精细编辑能力。', icon: '🖼️', usageCount: 0, status: 1 },
  { id: 11, skillName: '图片生成与编辑（高级版）', skillDesc: '图片生成与编辑（高级版）提供高质量的图片生成与精细编辑能力。', icon: '🎑', usageCount: 0, status: 1 },
  { id: 12, skillName: 'Word', skillDesc: '提供Word文档创建、编辑与解析能力，支持Web和通用任务。', icon: 'W', usageCount: 0, status: 1 },
  { id: 18, skillName: '小红书爆款文案', skillDesc: '生成小红书风格种草文案，含标题、正文、话题标签，适配流量逻辑。', icon: '📕', usageCount: 5600, category: 'create', status: 1 },
  { id: 19, skillName: '公众号文章', skillDesc: '根据主题生成结构完整、风格自然的公众号长文，支持多题材。', icon: '📰', usageCount: 4300, category: 'create', status: 1 },
  { id: 20, skillName: '短视频脚本', skillDesc: '按黄金三秒结构生成口播/剧情短视频脚本，含分镜与台词。', icon: '🎬', usageCount: 3800, category: 'create', status: 1 },
  { id: 21, skillName: '商业计划书', skillDesc: '生成投资人视角的商业计划书，含市场分析、商业模式、财务预测。', icon: '💼', usageCount: 1700, category: 'create', status: 1 },
  { id: 22, skillName: '周报生成', skillDesc: '根据工作内容要点自动生成结构化周报，突出成果与规划。', icon: '📊', usageCount: 6800, category: 'create', status: 1 },
  { id: 23, skillName: '朋友圈文案', skillDesc: '生成生活化、有网感的朋友圈文案，多种语气可选。', icon: '💬', usageCount: 3200, category: 'create', status: 1 },
  { id: 24, skillName: '商品详情页文案', skillDesc: '生成电商商品详情页文案，突出卖点、场景与转化引导。', icon: '🛒', usageCount: 2600, category: 'create', status: 1 },
  { id: 25, skillName: '广告Slogan', skillDesc: '为品牌或产品生成朗朗上口的广告语与传播口号。', icon: '📢', usageCount: 1900, category: 'create', status: 1 },
  { id: 26, skillName: '诗歌创作', skillDesc: '生成现代诗或古体诗词，支持指定意象、韵脚与风格。', icon: '🌙', usageCount: 1300, category: 'create', status: 1 },
  { id: 27, skillName: '小说续写', skillDesc: '根据已有情节续写小说，保持人物性格与文风一致。', icon: '📖', usageCount: 1100, category: 'create', status: 1 },
  { id: 28, skillName: '文章摘要', skillDesc: '对长文进行智能摘要，提取核心观点，支持指定长度。', icon: '📝', usageCount: 7400, category: 'understand', status: 1 },
  { id: 29, skillName: 'PDF解析', skillDesc: '解析PDF文档内容、表格与结构，提取可编辑文本。', icon: '📄', usageCount: 5900, category: 'understand', status: 1 },
  { id: 30, skillName: '合同审查', skillDesc: '审查合同条款，识别风险点并给出修改建议。', icon: '⚖️', usageCount: 2400, category: 'understand', status: 1 },
  { id: 31, skillName: '简历解析', skillDesc: '解析简历信息，提取技能、经历与匹配度分析。', icon: '👤', usageCount: 3100, category: 'understand', status: 1 },
  { id: 32, skillName: '舆情分析', skillDesc: '监测并分析全网舆情，输出情感倾向与传播趋势报告。', icon: '🌐', usageCount: 900, category: 'understand', status: 1 },
  { id: 33, skillName: '情感分析', skillDesc: '对文本进行情感倾向判断，支持中文多级情感粒度。', icon: '💗', usageCount: 4700, category: 'understand', status: 1 },
  { id: 34, skillName: 'OCR文字识别', skillDesc: '识别图片中的文字，支持手写、印刷体与表格还原。', icon: '🔍', usageCount: 5200, category: 'understand', status: 1 },
  { id: 35, skillName: 'TTS语音合成', skillDesc: '文字转自然语音，支持多音色、多语种与语速调节。', icon: '🔊', usageCount: 6100, category: 'voice', status: 1 },
  { id: 36, skillName: '语音克隆', skillDesc: '基于少量样本克隆声音，用指定音色朗读任意文本。', icon: '🎙️', usageCount: 1500, category: 'voice', status: 1 },
  { id: 37, skillName: '实时翻译', skillDesc: '多语种文本与语音实时互译，支持会议同传场景。', icon: '🌍', usageCount: 4200, category: 'voice', status: 1 },
  { id: 38, skillName: '会议纪要', skillDesc: '将会议录音转写并整理为结构化纪要，提取待办事项。', icon: '📋', usageCount: 3500, category: 'voice', status: 1 },
  { id: 39, skillName: '联网搜索', skillDesc: '实时联网检索信息，返回带来源的最新结果。', icon: '🔎', usageCount: 8900, category: 'search', status: 1 },
  { id: 40, skillName: '学术搜索', skillDesc: '检索学术论文与期刊，支持引用格式导出。', icon: '🎓', usageCount: 1800, category: 'search', status: 1 },
  { id: 41, skillName: '行业报告', skillDesc: '聚合行业数据生成分析报告，覆盖市场规模与趋势。', icon: '📈', usageCount: 1300, category: 'search', status: 1 },
  { id: 42, skillName: '竞品分析', skillDesc: '对比竞品功能、定价与口碑，输出差异化建议。', icon: '🏁', usageCount: 2100, category: 'search', status: 1 },
  { id: 43, skillName: 'Excel表格助手', skillDesc: '根据描述生成或修改Excel表格，支持公式与数据清洗。', icon: '📊', usageCount: 7800, category: 'office', status: 1 },
  { id: 44, skillName: 'PPT生成', skillDesc: '输入大纲一键生成排版精美的演示文稿，多风格可选。', icon: '📽️', usageCount: 6600, category: 'office', status: 1 },
  { id: 45, skillName: '思维导图', skillDesc: '将文本/想法整理为层级清晰的思维导图结构。', icon: '🧠', usageCount: 4500, category: 'office', status: 1 },
  { id: 46, skillName: '邮件撰写', skillDesc: '生成专业商务邮件，支持语气、对象与场景定制。', icon: '✉️', usageCount: 3900, category: 'office', status: 1 },
  { id: 47, skillName: '日程规划', skillDesc: '根据目标自动规划日程，拆解任务并排期。', icon: '🗓️', usageCount: 2700, category: 'office', status: 1 },
  { id: 48, skillName: 'Logo设计', skillDesc: '根据品牌名称与风格生成Logo概念图与配色方案。', icon: '🎨', usageCount: 3300, category: 'design', status: 1 },
  { id: 49, skillName: '海报生成', skillDesc: '生成活动/商品海报，支持版式、配色与文案排版。', icon: '🖼️', usageCount: 5100, category: 'design', status: 1 },
  { id: 50, skillName: 'UI界面生成', skillDesc: '根据需求生成网页/App界面设计稿与组件方案。', icon: '🖥️', usageCount: 2900, category: 'design', status: 1 },
  { id: 51, skillName: '图标生成', skillDesc: '生成统一风格的图标集，支持自定义主题色。', icon: '✨', usageCount: 3700, category: 'design', status: 1 },
  { id: 52, skillName: '字体设计', skillDesc: '生成创意字体与标题排版方案。', icon: '🔠', usageCount: 900, category: 'design', status: 1 },
  { id: 53, skillName: '色彩搭配', skillDesc: '为品牌或界面生成和谐配色方案与使用规范。', icon: '🎯', usageCount: 1600, category: 'design', status: 1 },
  { id: 54, skillName: '微信支付接入', skillDesc: '一站式接入微信支付，含下单、回调与退款处理。', icon: '💳', usageCount: 2900, category: 'pay', status: 1 },
  { id: 55, skillName: '支付宝接入', skillDesc: '接入支付宝当面付/App支付，含密钥与回调配置。', icon: '💴', usageCount: 1400, category: 'pay', status: 1 },
  { id: 56, skillName: '对账工具', skillDesc: '自动对账交易流水，输出差异报表。', icon: '🧾', usageCount: 700, category: 'pay', status: 1 },
  { id: 57, skillName: '短信验证码', skillDesc: '接入短信验证码发送与校验，支持频控与模板管理。', icon: '📱', usageCount: 4600, category: 'auth', status: 1 },
  { id: 58, skillName: '邮箱验证', skillDesc: '邮件验证码与链接校验，支持防重放。', icon: '📧', usageCount: 2300, category: 'auth', status: 1 },
  { id: 59, skillName: '路线规划', skillDesc: '多方式出行路线规划，支持实时路况与耗时估算。', icon: '🗺️', usageCount: 3400, category: 'map', status: 1 },
  { id: 60, skillName: '地图标注', skillDesc: '批量生成地图标记与点位信息展示。', icon: '📍', usageCount: 1100, category: 'map', status: 1 },
  { id: 61, skillName: '天气查询', skillDesc: '按城市/坐标查询实时天气与未来预报。', icon: '⛅', usageCount: 5800, category: 'map', status: 1 },
]
</script>

<style scoped>
.content {
  min-width: 0;
}

.tabs {
  display: flex;
  background: #fff;
  border-radius: 8px;
  padding: 4px;
  border: 1px solid #e4e5e7;
  width: fit-content;
  margin-bottom: 14px;
}
.tab {
  padding: 7px 18px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  color: #646a73;
  transition: 0.2s;
  user-select: none;
}
.tab:hover {
  color: #1f2329;
}
.tab.active {
  background: #1f2329;
  color: #fff;
}

.category-bar-wrap {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}

.create-skill-btn-inline {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  background: #8b5cf6;
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 7px 16px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
  white-space: nowrap;
  flex-shrink: 0;
}
.create-skill-btn-inline:hover {
  background: #7c3aed;
}
.create-skill-btn-inline .plus {
  font-size: 14px;
  line-height: 1;
  font-weight: 600;
  margin-right: 2px;
}

.skill-actions-group {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
.upload-skill-btn-inline {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  background: #fff;
  color: #1f2329;
  border: 1px solid #e4e5e7;
  border-radius: 8px;
  padding: 7px 14px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}
.upload-skill-btn-inline:hover {
  background: #f9fafb;
  border-color: #8b5cf6;
  color: #8b5cf6;
}
.upload-skill-btn-inline .upload-icon {
  font-size: 13px;
  line-height: 1;
  font-weight: 600;
  margin-right: 2px;
}

.category-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 0;
  overflow-x: auto;
  padding-bottom: 4px;
  flex: 1;
}
.category-bar::-webkit-scrollbar {
  height: 4px;
}
.category-bar::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 2px;
}
.category {
  padding: 7px 14px;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  color: #646a73;
  background: #fff;
  border: 1px solid #e4e5e7;
}
.category.active {
  background: #1f2329;
  color: #fff;
  border-color: #1f2329;
}
.category.more::after {
  content: ' ▼';
  font-size: 10px;
}
.category.more::after {
  content: ' ▼';
  font-size: 10px;
}

.skill-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.skill-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 16px;
  transition: box-shadow 0.2s, transform 0.2s;
  cursor: pointer;
}
.skill-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}
.skill-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 10px;
}
.skill-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}
.skill-title-wrap {
  flex: 1;
  min-width: 0;
}
.skill-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.skill-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.tag-discount {
  font-size: 11px;
  color: #ff4d4f;
  background: #fff1f0;
  padding: 2px 6px;
  border-radius: 4px;
  white-space: nowrap;
}
.skill-author {
  font-size: 12px;
  color: #8f959e;
  margin-top: 2px;
}
.skill-desc {
  font-size: 13px;
  color: #646a73;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 14px;
  min-height: 42px;
}
.skill-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.price {
  font-size: 18px;
  font-weight: 700;
  color: #8b5cf6;
}
.price .unit {
  font-size: 12px;
  font-weight: 400;
  color: #8f959e;
  margin-left: 2px;
}
.price.free {
  color: #1f2329;
  font-size: 15px;
}
.usage {
  font-size: 12px;
  color: #8f959e;
}
.card-actions {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed #eee;
  display: flex;
  gap: 16px;
}
.link-btn {
  font-size: 13px;
  color: #1e6df2;
  cursor: pointer;
}
.link-btn.danger {
  color: #e11d48;
}

.load-more-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
.load-more-btn {
  padding: 8px 32px;
  border: 1px solid #e5e7eb;
  background: #fff;
  border-radius: 999px;
  font-size: 13px;
  color: #4a4f63;
  cursor: pointer;
}
.load-more-btn:hover {
  background: #f4f6fb;
  border-color: #8b5cf6;
  color: #8b5cf6;
}

.empty-tip,
.empty-panel {
  padding: 48px 0;
  text-align: center;
  color: #999;
  font-size: 14px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
}
.login-link {
  color: #1e6df2;
  cursor: pointer;
}

.icon-purple { background: #f3e8ff; }
.icon-orange { background: #ffedd5; }
.icon-pink   { background: #ffe4e6; }
.icon-blue   { background: #dbeafe; }
.icon-yellow { background: #fef3c7; }
.icon-green  { background: #d1fae5; }
.icon-cyan   { background: #cffafe; }
.icon-red    { background: #fee2e2; }
.icon-gray   { background: #f3f4f6; }

.detail-head {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 20px;
}
.detail-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  background: #f3e8ff;
}
.detail-name {
  font-size: 18px;
  font-weight: 600;
  color: #1f2329;
}
.detail-meta {
  font-size: 13px;
  color: #888;
  margin-top: 4px;
}
.detail-section {
  margin-bottom: 18px;
}
.detail-label {
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
  margin-bottom: 8px;
  padding-left: 8px;
  border-left: 3px solid #1e6df2;
}
.detail-text {
  font-size: 13px;
  color: #444;
  line-height: 1.7;
  white-space: pre-wrap;
}
.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.detail-tag {
  background: #f1f5f9;
  border-radius: 6px;
  padding: 2px 10px;
  font-size: 12px;
  color: #475569;
}
.detail-actions {
  margin-top: 24px;
}
.use-btn {
  width: 100%;
  background: #1f2329;
  color: #fff;
  border: none;
  border-radius: 999px;
  padding: 10px 16px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}
.use-btn:hover {
  background: #333a45;
}

/* 上传技能弹窗 */
.upload-drop-area {
  border: 2px dashed #d9dce0;
  border-radius: 10px;
  background: #f5f5f5;
  padding: 16px 10px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 10px;
}
.upload-drop-area:hover,
.upload-drop-area.drag-over {
  border-color: #8b5cf6;
  background: #f5f3ff;
}
.upload-drop-area.has-file {
  border-style: solid;
  border-color: #8b5cf6;
  background: #f5f3ff;
}
.upload-rechoose {
  margin-top: 6px;
  font-size: 11px;
  color: #8b5cf6;
  text-decoration: underline;
}
.upload-rechoose:hover {
  opacity: 0.8;
}
.upload-icon-big {
  font-size: 20px;
  margin-bottom: 4px;
  opacity: 0.6;
}
.upload-tip-text {
  font-size: 12px;
  color: #1f2329;
  font-weight: 500;
}
.upload-hints {
  margin-bottom: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.hint-item {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 10px;
  color: #4a4f63;
  line-height: 1.4;
  width: fit-content;
}
.hint-dot {
  display: inline-block;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #8b5cf6;
  margin-top: 5px;
  flex-shrink: 0;
}
.upload-footer {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 8px;
}

.form-section {
  font-size: 12px;
  font-weight: 600;
  color: #1f2329;
  margin: 12px 0 8px;
  padding-left: 6px;
  border-left: 3px solid #8b5cf6;
}
.form-section:first-child {
  margin-top: 0;
}

/* 技能弹窗：紧凑布局 */
:global(.skill-modal .ant-modal-body) {
  padding: 16px 20px;
}
:global(.skill-modal .ant-form-item) {
  margin-bottom: 12px;
}
:global(.skill-modal .ant-form-item-label) {
  padding-bottom: 4px;
}
:global(.skill-modal .ant-form-item-label > label) {
  font-size: 12px;
  height: auto;
}
:global(.skill-modal .ant-input),
:global(.skill-modal .ant-select-selector) {
  font-size: 12px;
}
:global(.skill-modal .ant-input-textarea-show-count::after) {
  font-size: 11px;
}

@media (max-width: 900px) {
  .skill-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>

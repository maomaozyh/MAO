<template>
  <div class="page-wrap">
    <div class="main">
      <!-- 左侧内容 -->
      <div class="content">
        <SkillCenterContent :search-key="toolbarSearchKey" @use="onUseSkill">
          <!-- 轮播图：渲染在「发现/我的」tab 之后 -->
          <template #banner>
            <div class="banner-carousel" v-if="bannerSlides.length > 0">
              <div
                v-for="(slide, i) in bannerSlides"
                :key="i"
                class="banner-slide"
                :class="{ active: currentSlide === i }"
              >
                <div class="banner-text">
                  <div class="banner-title">{{ slide.title }}</div>
                  <div class="banner-desc">{{ slide.desc }}</div>
                </div>
                <div class="banner-emoji">{{ slide.emoji }}</div>
              </div>
              <div v-if="bannerSlides.length > 1" class="banner-dots">
                <span
                  v-for="(_, i) in bannerSlides"
                  :key="i"
                  class="banner-dot"
                  :class="{ active: currentSlide === i }"
                  @click="currentSlide = i"
                ></span>
              </div>
            </div>
          </template>
        </SkillCenterContent>
      </div>

      <!-- 右侧：搜索/创建工具栏 + 额度卡 -->
      <aside class="quota-side">
        <div class="quick-bar">
          <div class="quick-search">
            <span class="search-icon">🔍</span>
            <input
              v-model="toolbarSearchKey"
              class="quick-search-input"
              placeholder="搜索技能"
              maxlength="50"
            />
          </div>
          <button
            v-if="isAdmin"
            class="create-skill-btn"
            type="button"
            @click="goCreateSkill"
          >
            <span class="plus">+</span>创建技能
          </button>
        </div>
        <div class="quota-card">
          <div class="quota-title">
            今日技能剩余免费额度
            <span class="badge">免费版</span>
          </div>
          <div v-for="item in quotaList" :key="item.label" class="quota-item">
            <span class="quota-name">{{ item.label }}</span>
            <span class="quota-value">{{ item.used }}/{{ item.total }}</span>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import SkillCenterContent from '@/components/SkillCenterContent.vue'
import { getSkillCenterConfig } from '@/api/skillController'
import { useLoginUserStore } from '@/stores/loginUser'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const isAdmin = computed(() => loginUserStore.loginUser?.userRole === 'admin')

/** 右侧工具栏的搜索关键词（受控传给 SkillCenterContent） */
const toolbarSearchKey = ref('')

/** 创建技能：管理员点创建技能后跳到后台的技能管理页（有新增弹窗） */
const goCreateSkill = () => {
  router.push('/admin/skillManage')
}

/** Banner 轮播（默认配置，后端 sys_config 可覆盖） */
const bannerSlides = ref([
  { title: '优秀创作', desc: '在网页里构建三维场景、动态视觉与沉浸式交互体验。', emoji: '🧊' },
  { title: '登录能力免费接入', desc: '一键接入，完全免费。', emoji: '🔐' },
])

/** 分类筛选 chips（默认配置，后端可覆盖） */
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

/** 右侧免费额度卡（默认配置，后端可覆盖） */
const quotaList = ref([
  { label: '视频生成类', used: 1, total: 1 },
  { label: '图片生成类', used: 5, total: 5 },
  { label: '其他类', used: 100, total: 100 },
])

/** 从后端 sys_config 加载公开配置，成功且非空时覆盖默认值 */
const loadSkillCenterConfig = async () => {
  try {
    const res = await getSkillCenterConfig()
    if (res.data.code !== 0 || !res.data.data) return
    const cfg = res.data.data
    if (cfg.banners && cfg.banners.length > 0) bannerSlides.value = cfg.banners as any
    if (cfg.categories && cfg.categories.length > 0) categories.value = cfg.categories as any
    if (cfg.quota && cfg.quota.length > 0) quotaList.value = cfg.quota as any
  } catch (e) {
    console.warn('[SkillCenter] 读取公开配置失败，使用默认值：', e)
  }
}

/** 「使用该技能」按钮跳转首页并把 @技能名 带到输入框 */
const onUseSkill = (skill: API.SkillVO) => {
  const name = skill?.skillName
  if (!name) return
  message.success('已选择技能「' + name + '」，即将前往创建')
  router.push({ path: '/', query: { skill: name, skillId: String(skill.id) } })
}

let timer: ReturnType<typeof setInterval> | null = null
const goNext = () => {
  currentSlide.value = (currentSlide.value + 1) % bannerSlides.value.length
}
const startAuto = () => {
  stopAuto()
  timer = setInterval(goNext, 3000)
}
const stopAuto = () => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}
const currentSlide = ref(0)

onMounted(() => {
  loadSkillCenterConfig()
  startAuto()
})
onBeforeUnmount(stopAuto)
</script>

<style scoped>
.page-wrap {
  padding: 24px 32px;
  background-color: #f7f8fa;
  min-height: 100vh;
}

.main {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 20px;
  align-items: start;
}

.content {
  min-width: 0;
}

/* ===== 轮播图 ===== */
.banner-carousel {
  position: relative;
  height: 180px;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 16px;
  background: linear-gradient(120deg, #f0eaff 0%, #e3f2ff 100%);
}
.banner-slide {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  opacity: 0;
  transition: opacity 0.5s ease;
  pointer-events: none;
}
.banner-slide.active {
  opacity: 1;
  pointer-events: auto;
}
.banner-text {
  max-width: 75%;
}
.banner-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2329;
  margin-bottom: 6px;
}
.banner-desc {
  font-size: 13px;
  color: #646a73;
  line-height: 1.5;
}
.banner-emoji {
  font-size: 52px;
  line-height: 1;
}
.banner-dots {
  position: absolute;
  bottom: 10px;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  gap: 6px;
}
.banner-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.18);
  cursor: pointer;
  transition: background 0.2s, width 0.2s;
}
.banner-dot.active {
  background: #1f2329;
  width: 16px;
  border-radius: 3px;
}

/* 右侧额度卡 */
.quota-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 18px;
}
.quota-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}
.quota-title .badge {
  font-size: 11px;
  color: #8f959e;
  background: #f2f3f5;
  padding: 2px 6px;
  border-radius: 4px;
}
.quota-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #f2f3f5;
}
.quota-item:last-child {
  border-bottom: none;
}
.quota-name {
  font-size: 13px;
  color: #646a73;
}
.quota-value {
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
}

/* 右侧：搜索/创建工具栏（在额度卡上方） */
.quota-side {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quick-bar {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.quick-search {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 10px;
  border: 1px solid #e4e5e7;
  border-radius: 8px;
  transition: border-color 0.15s;
}
.quick-search:focus-within {
  border-color: #8b5cf6;
}

.quick-search .search-icon {
  font-size: 14px;
  line-height: 1;
  color: #8f959e;
  flex-shrink: 0;
}

.quick-search-input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  color: #1f2329;
  font-family: inherit;
}
.quick-search-input::placeholder {
  color: #a8adb8;
}

.create-skill-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  background: #1f2329;
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 9px 12px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
  width: 100%;
}
.create-skill-btn:hover {
  background: #333a45;
}
.create-skill-btn .plus {
  font-size: 15px;
  line-height: 1;
  font-weight: 600;
  margin-right: 2px;
}

@media (max-width: 1100px) {
  .main {
    grid-template-columns: 1fr;
  }
}
</style>
<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'

// 点击芯片只把提示词回传给父组件（HomePage），由父组件填入输入框，不直接创建应用
const emit = defineEmits<{ (e: 'pick', prompt: string, category?: string): void }>()
const chipsRef = ref<HTMLElement | null>(null)
const atStart = ref(true)
const atEnd = ref(false)
const SCROLL_DELTA = 320

interface Preset {
  label: string
  prompt: string
  category?: string
  icon:
    | 'image' | 'research' | 'ppt' | 'video' | 'web'
    | 'poster' | 'logo' | 'chart' | 'copy' | 'resume'
    | 'translate' | 'lesson' | 'model3d' | 'avatar' | 'infographic'
    | 'miniprogram'
}

const presets: Preset[] = [
  { label: '小程序',     icon: 'miniprogram', prompt: '帮我做一个「城市生活指南」小程序风格的单页 H5：移动端竖屏布局（max-width 420px 居中模拟手机），含底部 4 Tab（首页/发现/消息/我的）+ 顶部搜索栏 + 卡片瀑布流列表与详情弹窗，整体配色清新轻量，可直接在浏览器预览。直接生成完整项目，不要只写说明、复述需求或总结思路。' },
  { label: '图片生成',   icon: 'image',       prompt: '帮我做一个「AI 配图生成器」Web 应用：左侧输入提示词 + 风格/尺寸/比例/色调选项（赛博朋克/水墨/极简/扁平/写实 6 种预设），右侧 2×2 结果网格展示，支持一键下载与「重新生成」，整体配色明亮、强调视觉表现。直接生成完整项目，不要只写说明。' },
  { label: '深度研究',   icon: 'research',    prompt: '帮我做一个「主题深度研究」报告 Web 应用：顶部输入研究主题，输出结构化研究报告（左侧可折叠目录 + 右侧正文），正文含摘要/背景/核心论点/数据支撑/结论五段，关键事实带 [来源 n] 上标，底部参考文献列表，支持一键导出 Markdown 与打印 PDF 排版。直接生成完整项目，不要只解释或总结。' },
  { label: 'PPT生成',    icon: 'ppt',         prompt: '帮我做一个「演示文稿自动生成」Web 应用：输入主题与可选大纲（自动生成 8 页左右），输出可翻页演示文稿（一屏一页 + 左右切换 + 底部缩略图导航条），每页包含标题/要点/页码/配图占位，支持键盘 ←→ 切页与全屏演讲模式，可导出 PDF。直接生成完整项目，不要只写说明。' },
  { label: '视频生成',   icon: 'video',       prompt: '帮我做一个「短视频脚本工坊」Web 应用：输入主题/时长（15s/30s/60s）/风格，输出含分镜表的脚本（镜号/画面/台词/时长/背景音乐建议），可一键导出 txt；下方放一个 16:9 canvas 预览区，依次播放文字分镜模拟演示效果。直接生成完整项目，不要只解释或总结。' },
  { label: '网页抓取',   icon: 'web',         prompt: '帮我做一个「网页内容抓取工具」Web 应用：输入 URL + 选择抓取字段（标题/正文/作者/发布时间/正文图片），输出结构化结果卡片（字段 + 内容 + 复制按钮），支持抓取历史记录与导出 JSON；可接入 mock 接口演示真实抓取流程，UI 偏工具型、信息密度高。直接生成完整项目，不要只写说明。' },
  { label: '海报设计',   icon: 'poster',      prompt: '帮我做一个「海报设计工坊」Web 应用：输入主题/主标题/副标题/主色调，选择海报尺寸（A3/A4/朋友圈竖图），实时渲染 9:16 或 16:9 预览画布，支持 3 套模板切换（极简/复古/拼贴），可一键导出 PNG/SVG 矢量。直接生成完整项目，不要只解释或复述需求。' },
  { label: 'Logo 设计',  icon: 'logo',        prompt: '帮我做一个「Logo 概念生成器」Web 应用：输入品牌名 + 行业 + 风格关键词（极简/几何/手绘/科技），下方 3×2 网格展示 6 个 SVG Logo 方案（黑底 + 单色背景），每个方案下方显示配色色值与释义，点击查看大图与导出 SVG 文件。直接生成完整项目，不要只写说明。' },
  { label: '数据可视化', icon: 'chart',       prompt: '帮我做一个「数据可视化演示工作台」：左侧可粘贴销售/用户/财务等多类型示例数据（CSV 或 JSON），中间选图表类型（柱状/折线/饼图/雷达/漏斗 5 种），右侧实时渲染带 hover 交互的高质量图表；提供 8 页「图表故事」自动叙事模式（一键切换翻页），支持导出 PNG/SVG 与打印 PDF 排版。直接生成完整项目，不要只解释、复述或总结。' },
  { label: '营销文案',   icon: 'copy',        prompt: '帮我做一个「营销文案生成器」Web 应用：输入产品名 + 核心卖点 + 目标人群（3 个标签），输出 4 类文案——长文案/朋友圈短文/小红书种草/抖音脚本（每类 3 版共 12 条），每条带复制按钮与字符统计，支持收藏夹与历史记录。直接生成完整项目，不要只写说明或复述需求。' },
  { label: '简历优化',   icon: 'resume',      prompt: '帮我做一个「智能简历工坊」Web 应用：左侧表单（个人信息/教育/3 段经历/技能/项目），右侧 A4 简历实时预览（左右两栏：左 30% 个人信息 + 右 70% 经历时间线），提供 3 套模板切换（经典/现代/极简），一键导出可打印 PDF（@media print 优化）。直接生成完整项目，不要只解释或总结。' },
  { label: '智能翻译',   icon: 'translate',   prompt: '帮我做一个「多语对照翻译台」Web 应用：顶部源/目标语言选择（中/英/日/韩/法 5 种），左右两个输入框（左侧原文 + 右侧实时译文），中间支持「自动检测语种 / 复制译文 / 互换语言」快捷按钮，可保存最近 5 次翻译历史与导出双语对照。直接生成完整项目，不要只写说明。' },
  { label: '教学课件',   icon: 'lesson',      prompt: '帮我做一个「互动课件生成器」Web 应用：输入课题 + 学段（小学/初中/高中），自动生成 6–8 页课件（封面/学习目标/概念讲解 3 页/互动练习 2 页/小结），每页支持翻页导航，关键概念高亮，底部嵌入一个随堂小测验（5 题选择 + 实时判分与解析）。直接生成完整项目，不要只解释或复述。' },
  { label: '3D 模型',    icon: 'model3d',     prompt: '帮我做一个「3D 模型预览台」Web 应用：左侧输入模型描述（如「赛博朋克机械鱼」），右侧用 Three.js 渲染一个可旋转/缩放/平移的 3D 画布（程序化生成简单几何体组合演示），下方提供材质/光照/背景色 3 组实时控件，支持截图导出 PNG。直接生成完整项目，不要只写说明或复述需求。' },
  { label: '头像生成',   icon: 'avatar',      prompt: '帮我做一个「个性头像生成器」Web 应用：顶部选风格（卡通/像素/插画/极简 4 种）+ 自定义配色 + 4 个特征滑块（发型/表情/背景/装饰），下方 2×3 网格展示 6 张随机头像 SVG 预览，点击放大查看 + 一键导出 PNG。直接生成完整项目，不要只解释或总结。' },
  { label: '信息图表',   icon: 'infographic', prompt: '帮我做一个「信息图表设计器」Web 应用：左侧输入「标题 + 5 条要点 + 数据/百分比」，右侧实时渲染一张 9:16 竖版信息图（含图标/数据块/进度条/配色），支持 3 套主题切换（商务/活力/极简），一键导出 PNG/SVG。直接生成完整项目，不要只写说明、复述需求或总结思路。' },
]

const onPick = (p: Preset) => {
  // icon 字符串与 AppCategoryEnum.value 完全一致，可直接作为后端 category 透传。
  // 后端按 getExternalAssetType 把 image/video/model3d/ppt/avatar/logo/poster
  // 路由到对应的真实 AI 素材服务，其余（research/web/copy 等）走代码生成。
  emit('pick', p.prompt, p.category ?? p.icon)
}

const scrollBy = (dir: number) => {
  const el = chipsRef.value
  if (!el) return
  el.scrollBy({ left: dir * SCROLL_DELTA, behavior: 'smooth' })
}
const onChipsScroll = () => {
  const el = chipsRef.value
  if (!el) return
  atStart.value = el.scrollLeft <= 1
  atEnd.value = el.scrollLeft + el.clientWidth >= el.scrollWidth - 1
}
let ro: ResizeObserver | null = null
onMounted(() => {
  onChipsScroll()
  if (chipsRef.value && typeof ResizeObserver !== 'undefined') {
    ro = new ResizeObserver(onChipsScroll)
    ro.observe(chipsRef.value)
  }
  window.addEventListener('resize', onChipsScroll)
})
onBeforeUnmount(() => {
  ro?.disconnect()
  window.removeEventListener('resize', onChipsScroll)
})
</script>

<template>
  <div class="quick-entry" :class="{ 'has-left': !atStart, 'has-right': !atEnd }">
    <button
      class="qnav"
      :class="{ disabled: atStart }"
      :disabled="atStart"
      aria-label="向左滚动"
      @click="scrollBy(-1)"
    >
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="15 6 9 12 15 18"/>
      </svg>
    </button>
    <div ref="chipsRef" class="qchips" @scroll="onChipsScroll">
      <div
        v-for="p in presets"
        :key="p.label"
        class="qchip2"
        @click="onPick(p)"
      >
        <!-- 图片 -->
        <svg v-if="p.icon === 'image'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="4" width="18" height="16" rx="2"/>
          <circle cx="9" cy="10" r="1.6" fill="currentColor" stroke="none"/>
          <path d="M21 16l-5-5-9 8"/>
        </svg>
        <!-- 深度研究 -->
        <svg v-else-if="p.icon === 'research'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M8 4c-2 3 2 5 0 8s-2 5 0 8"/>
          <path d="M16 4c2 3-2 5 0 8s2 5 0 8"/>
        </svg>
        <!-- PPT -->
        <svg v-else-if="p.icon === 'ppt'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="4" width="18" height="12" rx="1.5"/>
          <path d="M12 16v4"/>
          <path d="M8 20h8"/>
        </svg>
        <!-- 视频 -->
        <svg v-else-if="p.icon === 'video'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="5" width="18" height="14" rx="2"/>
          <path d="M11 9l5 3-5 3z" fill="currentColor" stroke="none"/>
        </svg>
        <!-- 海报设计 -->
        <svg v-else-if="p.icon === 'poster'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="3" width="18" height="18" rx="2"/>
          <circle cx="9" cy="9" r="1.6" fill="currentColor" stroke="none"/>
          <path d="M21 15l-5-5-8 8"/>
          <path d="M14 3v4h4"/>
        </svg>
        <!-- Logo 设计 -->
        <svg v-else-if="p.icon === 'logo'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8L12 3z"/>
          <path d="M19 16l.7 2.1L22 19l-2.3.9L19 22l-.7-2.1L16 19l2.3-.9L19 16z"/>
        </svg>
        <!-- 数据可视化 -->
        <svg v-else-if="p.icon === 'chart'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M3 3v18h18"/>
          <rect x="7" y="13" width="3" height="6"/>
          <rect x="13" y="9" width="3" height="10"/>
          <rect x="10" y="17" width="3" height="2"/>
        </svg>
        <!-- 营销文案 -->
        <svg v-else-if="p.icon === 'copy'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
          <polyline points="14 2 14 8 20 8"/>
          <line x1="8" y1="13" x2="16" y2="13"/>
          <line x1="8" y1="17" x2="16" y2="17"/>
        </svg>
        <!-- 简历优化 -->
        <svg v-else-if="p.icon === 'resume'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <rect x="2" y="7" width="20" height="14" rx="2"/>
          <path d="M16 21V5a2 2 0 00-2-2h-4a2 2 0 00-2 2v16"/>
          <path d="M2 13h20"/>
        </svg>
        <!-- 智能翻译 -->
        <svg v-else-if="p.icon === 'translate'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M5 8h8"/>
          <path d="M9 4v4c0 3-3 6-6 6"/>
          <path d="M3 14c3 0 6-2 7-6"/>
          <path d="M14 19l4-9 4 9"/>
          <path d="M15 16h6"/>
        </svg>
        <!-- 教学课件 -->
        <svg v-else-if="p.icon === 'lesson'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M2 4h7a4 4 0 014 4v12a3 3 0 00-3-3H2z"/>
          <path d="M22 4h-7a4 4 0 00-4 4v12a3 3 0 013-3h8z"/>
        </svg>
        <!-- 3D 模型 -->
        <svg v-else-if="p.icon === 'model3d'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z"/>
          <polyline points="3.27 6.96 12 12.01 20.73 6.96"/>
          <line x1="12" y1="22.08" x2="12" y2="12"/>
        </svg>
        <!-- 头像生成 -->
        <svg v-else-if="p.icon === 'avatar'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"/>
          <circle cx="12" cy="10" r="3"/>
          <path d="M6.5 18a7 7 0 0111 0"/>
        </svg>
        <!-- 信息图表 -->
        <svg v-else-if="p.icon === 'infographic'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 12a9 9 0 11-9-9v9h9z"/>
          <path d="M22 12A10 10 0 0012 2v10h10z" fill="currentColor" stroke="none"/>
        </svg>
        <!-- 小程序 -->
        <svg v-else-if="p.icon === 'miniprogram'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <rect x="6.5" y="2" width="11" height="20" rx="3"/>
          <line x1="10" y1="18.5" x2="14" y2="18.5"/>
          <path d="M9.5 7h5M9.5 10h5M9.5 13h3.5"/>
        </svg>
        <!-- 网页抓取 -->
        <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="9"/>
          <path d="M3 12h18"/>
          <path d="M12 3a14 14 0 010 18"/>
          <path d="M12 3a14 14 0 000 18"/>
        </svg>
        <span class="qchip-label">{{ p.label }}</span>
      </div>
    </div>
    <button
      class="qnav"
      :class="{ disabled: atEnd }"
      :disabled="atEnd"
      aria-label="向右滚动"
      @click="scrollBy(1)"
    >
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="9 6 15 12 9 18"/>
      </svg>
    </button>
  </div>
</template>

<style scoped>
.quick-entry {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 20px;
  width: 100%;
  position: relative;
}
/* 左右边缘渐隐遮罩，提示可滚动 */
.quick-entry::before,
.quick-entry::after {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  width: 28px;
  pointer-events: none;
  z-index: 2;
  opacity: 0;
  transition: opacity 0.2s;
}
.quick-entry::before {
  left: 48px;
  background: linear-gradient(to right, var(--qe-mask, #fff), transparent);
}
.quick-entry::after {
  right: 48px;
  background: linear-gradient(to left, var(--qe-mask, #fff), transparent);
}
.quick-entry.has-left::before { opacity: 1; }
.quick-entry.has-right::after { opacity: 1; }
.qnav {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: none;
  background: #fff;
  box-shadow: 0 6px 18px rgba(108, 92, 255, 0.16), 0 1px 2px rgba(20, 24, 40, 0.05);
  color: #5b6170;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color 0.18s, box-shadow 0.18s, transform 0.18s;
  z-index: 3;
}
.qnav:hover:not(.disabled) {
  color: #6c5cff;
  box-shadow: 0 8px 22px rgba(108, 92, 255, 0.28), 0 1px 2px rgba(20, 24, 40, 0.05);
  transform: translateY(-1px) scale(1.04);
}
.qnav:active:not(.disabled) {
  transform: translateY(0) scale(0.97);
}
.qnav.disabled {
  opacity: 0.28;
  cursor: default;
  box-shadow: 0 1px 3px rgba(20, 24, 40, 0.04);
}
.qnav svg {
  width: 18px;
  height: 18px;
}
.qchips {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  scroll-behavior: smooth;
  flex: 1;
  min-width: 0;
  scrollbar-width: none;
  -ms-overflow-style: none;
  padding: 4px 2px; /* 给阴影留出空间 */
  scroll-padding-inline: 12px;
}
.qchips::-webkit-scrollbar {
  display: none;
}
.qchip2 {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 11px 18px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid #ececf2;
  box-shadow: 0 2px 8px rgba(20, 24, 40, 0.04), 0 1px 2px rgba(20, 24, 40, 0.03);
  font-size: 14px;
  color: #1d1f29;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
  transition: border-color 0.18s, color 0.18s, transform 0.18s, box-shadow 0.18s;
}
.qchip2:hover {
  border-color: #6c5cff;
  color: #6c5cff;
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(108, 92, 255, 0.14), 0 1px 2px rgba(20, 24, 40, 0.04);
}
.qchip2:hover svg { color: #6c5cff; }
.qchip2 svg {
  width: 18px;
  height: 18px;
  color: #6c5cff;
  flex-shrink: 0;
}
.qchip-label {
  color: inherit;
}
</style>
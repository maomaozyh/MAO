<script setup lang="ts">
import { ref, computed, nextTick, onMounted, onBeforeUnmount, onActivated, onDeactivated, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { refreshBalance } from '@/composables/useBalance'
import {
  addApp,
  listGoodAppVoByPage,
  listMyAppVoByPage,
  expandPrompt,
  semanticSearchApps,
} from '@/api/appController'
import { listSkillVOByPage } from '@/api/skillController'
import { listMyMaterialVoByPage, uploadMaterial } from '@/api/materialController'
import SkillCenterContent from '@/components/SkillCenterContent.vue'
import QuickEntryChips from '@/components/QuickEntryChips.vue'
import { CodeGenTypeEnum } from '@/utils/codeGenTypes'
import { detectLang } from '@/composables/useDetectLang'

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()

// 从 SkillCenter 跳转过来的 ?skill=技能名&skillId=ID（显示为可关闭的 chip，提交时拼到 initPrompt 并传递 skillId）
const selectedSkill = ref<{ id: number; name: string } | null>(null)
const applySkillFromQuery = () => {
  const s = route.query.skill
  const sid = route.query.skillId
  if (typeof s === 'string' && s.trim()) {
    selectedSkill.value = {
      id: sid ? Number(sid) : 0,
      name: s.trim(),
    }
  } else {
    selectedSkill.value = null
  }
}
applySkillFromQuery()
const clearSelectedSkill = () => {
  selectedSkill.value = null
  router.replace({ path: '/', query: {} })
}
watch(() => route.query.skill, applySkillFromQuery)

const userPrompt = ref('')
// 快捷入口芯片点击后把提示词填入输入框（不直接创建应用），方便用户查看/修改后再发送
const promptEl = ref<HTMLTextAreaElement | null>(null)
// 快捷入口芯片携带的分类（AppCategoryEnum.value），提交创建时写入 app.category，
// 后端据此决定走真实 AI 素材服务还是代码生成
const pendingCategory = ref('')
const onQuickPick = (prompt: string, category?: string) => {
  userPrompt.value = prompt
  pendingCategory.value = category || ''
  nextTick(() => promptEl.value?.focus())
}
// 「我的作品」空状态：聚焦输入框，引导用户直接描述并创作
const startCreate = () => {
  nextTick(() => promptEl.value?.focus())
}
// 实时语言识别：根据输入框内容识别语言，输入框下方显示徽标
const detectedLang = computed(() => (userPrompt.value.trim() ? detectLang(userPrompt.value) : ''))

// ============ 分类示例（每个 Tab 5 个示例，点击填入输入框） ============
interface TabExample { title: string; prompt: string }
const TAB_EXAMPLES: Record<string, TabExample[]> = {
  '全部': [
    { title: '智能客服机器人', prompt: '帮我做一个「智能客服机器人」Web 应用：左侧多轮对话区 + 右侧常见问题快捷回复 + 上下文记忆 + 对话记录一键导出' },
    { title: 'AI 图像生成器', prompt: '帮我做一个「AI 图像生成器」：提示词输入 + 6 种风格预设 + 2×2 结果网格 + 一键下载/重新生成' },
    { title: '数据可视化看板', prompt: '帮我做一个「数据可视化演示工作台」：左侧数据输入 + 5 种图表切换 + 右侧实时渲染 + 8 页图表故事叙事模式' },
    { title: '智能简历工坊', prompt: '帮我做一个「智能简历工坊」：左侧表单 + 右侧 A4 简历实时预览 + 3 套模板 + 可打印 PDF' },
    { title: '营销文案生成器', prompt: '帮我做一个「营销文案生成器」：输入卖点输出 4 类文案 × 3 版共 12 条 + 一键复制 + 收藏夹' },
  ],
  '我的作品': [],
  '模板': [
    { title: '极简博客', prompt: '帮我做一个「极简个人博客」模板：首页文章列表 + 文章详情 + 标签分类 + 暗色模式 + 阅读时长统计' },
    { title: '后台管理', prompt: '帮我做一个「通用后台管理」模板：左侧导航 + 面包屑 + 数据表格（搜索/分页/筛选）+ 统计卡片 + 暗色模式' },
    { title: 'SaaS 落地页', prompt: '帮我做一个「SaaS 产品落地页」模板：Hero + 特性展示 + 客户案例 + 价格表 + FAQ + 底部 CTA' },
    { title: '电商商城首页', prompt: '帮我做一个「电商商城首页」模板：顶部搜索栏 + 分类导航 + 轮播 Banner + 商品瀑布流 + 限时秒杀专区' },
    { title: '活动报名页', prompt: '帮我做一个「活动报名落地页」：主视觉 + 活动详情 + 报名表单 + 嘉宾介绍 + 倒计时 + 分享海报' },
  ],
  '推荐': [
    { title: 'AI 配图生成器', prompt: '帮我做一个「AI 配图生成器」：6 种风格预设 + 2×2 结果网格 + 一键下载/重新生成' },
    { title: '智能简历工坊', prompt: '帮我做一个「智能简历工坊」：左右两栏 A4 预览 + 3 套模板 + PDF 导出' },
    { title: '短视频脚本工坊', prompt: '帮我做一个「短视频脚本工坊」：分镜表（镜号/画面/台词/时长/BGM）+ 16:9 canvas 模拟播放' },
    { title: '多语翻译台', prompt: '帮我做一个「多语对照翻译台」：5 种语言双向翻译 + 自动检测 + 历史记录' },
    { title: '演示文稿生成', prompt: '帮我做一个「演示文稿自动生成」：自动 8 页 + 缩略图导航 + 键盘切页 + 演讲模式' },
  ],
  '应用类手': [
    { title: '城市生活指南', prompt: '帮我做一个「城市生活指南」小程序风格单页 H5：底部 4 Tab + 顶部搜索 + 卡片瀑布流 + 详情弹窗' },
    { title: '美食推荐', prompt: '帮我做一个「美食推荐」小程序：首页附近餐厅卡片 + 详情页含评分/菜单/位置 + 收藏夹' },
    { title: '健身打卡', prompt: '帮我做一个「健身打卡」小程序：今日训练卡片 + 打卡日历 + 连续天数统计 + 训练记录' },
    { title: '记账小程序', prompt: '帮我做一个「每日记账」小程序：快速录入 + 分类图标选择 + 月度统计图表 + 预算提醒' },
    { title: '单词速记', prompt: '帮我做一个「单词速记」小程序：每日 20 词 + 卡片翻转记忆 + 复习提醒 + 学习进度' },
  ],
  '游戏': [
    { title: '2048 数字合成', prompt: '帮我做一个「2048 数字合成」小游戏：4×4 网格 + 上下左右滑动 + 同数合成 + 分数与最高纪录' },
    { title: '记忆翻牌', prompt: '帮我做一个「记忆翻牌」小游戏：4×4 卡片网格 + 翻牌配对 + 步数与计时 + 难度切换' },
    { title: '扫雷', prompt: '帮我做一个经典「扫雷」小游戏：可调难度（9×9/16×16/16×30）+ 数字提示 + 雷数显示 + 胜利失败弹窗' },
    { title: '贪吃蛇', prompt: '帮我做一个「贪吃蛇」小游戏：方向键控制 + 吃食物成长 + 撞墙/自身失败 + 分数与最高分' },
    { title: '五子棋', prompt: '帮我做一个「五子棋」小游戏：15×15 棋盘 + 双人轮流 + 五连判定 + 悔棋 + 重开' },
  ],
  '工具': [
    { title: 'JSON 格式化', prompt: '帮我做一个「JSON 格式化工具」：粘贴 JSON 一键美化/压缩 + 语法校验 + 树形折叠 + 转 XML/CSV' },
    { title: 'Markdown 编辑器', prompt: '帮我做一个「Markdown 编辑器」：左右实时预览 + 工具栏 + 代码高亮 + 导出 HTML/PDF' },
    { title: '二维码生成器', prompt: '帮我做一个「二维码生成器」：输入文本/URL + 自定义颜色与 logo + 多种尺寸下载' },
    { title: '颜色选择器', prompt: '帮我做一个「颜色选择器工具」：色盘 + HEX/RGB/HSL 互转 + 配色方案生成 + 历史收藏' },
    { title: '时间戳转换', prompt: '帮我做一个「时间戳转换工具」：Unix 时间戳 ↔ 日期时间 + 时区切换 + 批量转换' },
  ],
  '教育': [
    { title: '单词背记', prompt: '帮我做一个「单词背记」学习应用：每日 20 词 + 艾宾浩斯复习 + 测试模式 + 进度追踪' },
    { title: '数学公式练习', prompt: '帮我做一个「数学公式练习」应用：按章节选题 + 步骤引导 + 即时判分 + 错题本' },
    { title: '历史时间线', prompt: '帮我做一个「历史时间线」交互应用：可滚动时间轴 + 事件详情卡片 + 搜索过滤 + 收藏' },
    { title: '编程练习', prompt: '帮我做一个「在线编程练习」平台：题目列表 + 代码编辑器 + 实时运行 + 测试用例' },
    { title: '物理实验演示', prompt: '帮我做一个「物理实验演示」应用：3-5 个经典实验 + Canvas 动画 + 参数可调 + 公式同步' },
  ],
  '网站': [
    { title: '个人作品集', prompt: '帮我做一个「个人作品集」网站：首页大图 Hero + 项目卡片网格 + 关于我 + 联系表单' },
    { title: '公司官网', prompt: '帮我做一个「公司官网」：顶部导航 + Hero 区 + 服务介绍 + 团队 + 客户案例 + 联系 + 页脚' },
    { title: '活动落地页', prompt: '帮我做一个「活动落地页」：倒计时 + 议程 + 嘉宾介绍 + 报名表单 + 常见问题 + 底部 CTA' },
    { title: '摄影作品展示', prompt: '帮我做一个「摄影作品集」网站：瀑布流相册 + 灯箱大图 + 分类筛选 + EXIF 信息展示' },
    { title: '美食博客', prompt: '帮我做一个「美食博客」网站：菜谱卡片列表 + 详情含食材步骤 + 分类标签 + 搜索' },
  ],
  '电商': [
    { title: '服装商城', prompt: '帮我做一个「服装电商」应用：商品瀑布流 + 筛选（尺码/颜色/价格）+ 详情页 + 购物车 + 结算' },
    { title: '数码商城', prompt: '帮我做一个「数码商城」应用：商品对比 + 规格参数 + 用户评价 + 配件推荐 + 购物车' },
    { title: '美妆购物', prompt: '帮我做一个「美妆购物」应用：色卡选择 + 肤质匹配 + 试用装申请 + 一键下单' },
    { title: '农产品电商', prompt: '帮我做一个「农产品电商」应用：产地溯源 + 当季推荐 + 拼团 + 冷链物流追踪' },
    { title: '二手交易', prompt: '帮我做一个「二手交易平台」应用：发布商品 + 议价聊天 + 信用评分 + 担保交易' },
  ],
  '办公': [
    { title: '任务待办', prompt: '帮我做一个「任务待办」应用：四象限 + 优先级 + 截止日期 + 子任务 + 完成统计' },
    { title: '日程日历', prompt: '帮我做一个「日程日历」应用：月/周/日视图 + 事件拖拽 + 提醒 + 重复事件' },
    { title: '会议纪要', prompt: '帮我做一个「会议纪要工具」：录音转写 + AI 提取待办 + 参与者标注 + 一键导出' },
    { title: '文件管理', prompt: '帮我做一个「文件管理」应用：文件夹树 + 拖拽上传 + 标签 + 全文搜索 + 预览' },
    { title: '笔记应用', prompt: '帮我做一个「笔记应用」：富文本编辑 + Markdown + 双链笔记 + 全局搜索 + 标签' },
  ],
  '营销': [
    { title: '营销落地页', prompt: '帮我做一个「高转化营销落地页」：痛点共鸣 + 解决方案 + 社会证明 + 限时优惠 + CTA 按钮' },
    { title: '抽奖活动', prompt: '帮我做一个「九宫格抽奖活动页」：奖品展示 + 抽奖动画 + 中奖记录 + 分享得额外机会' },
    { title: '裂变海报', prompt: '帮我做一个「邀请裂变海报生成器」：输入文案 + 选择模板 + 一键生成 + 二维码合成' },
    { title: '优惠券', prompt: '帮我做一个「优惠券营销」应用：批量生成 + 渠道分发 + 使用核销 + 效果统计' },
    { title: '分享卡片', prompt: '帮我做一个「社交分享卡片生成器」：3 种风格 + 自定义文案 + 一键导出 PNG' },
  ],
  '研究': [
    { title: '行业研究报告', prompt: '帮我做一个「行业研究报告」生成器：输入主题 + 自动生成大纲 + 结构化排版 + 图表引用' },
    { title: '竞品分析', prompt: '帮我做一个「竞品分析」工具：输入竞品列表 + 维度对比表格 + SWOT 分析 + 一键导出' },
    { title: '调研问卷', prompt: '帮我做一个「用户调研问卷」生成器：题型混排 + 逻辑跳转 + 结果统计图表 + 导出 Excel' },
    { title: '数据分析报告', prompt: '帮我做一个「数据分析报告」生成器：导入数据 + 自动分析 + 图表 + 结论 + 导出 PDF' },
    { title: '文献综述', prompt: '帮我做一个「文献综述助手」：导入文献列表 + 自动摘要 + 主题分类 + 引文格式导出' },
  ],
  '问答': [
    { title: '客服机器人', prompt: '帮我做一个「客服机器人」：知识库问答 + 多轮对话 + 人工转接 + 满意度评分' },
    { title: '知识库问答', prompt: '帮我做一个「知识库问答」应用：上传文档 + 智能检索 + 答案引用原文 + 置信度显示' },
    { title: 'FAQ 智能问答', prompt: '帮我做一个「FAQ 智能问答」：导入问答对 + 语义理解 + 相似问题推荐 + 数据统计' },
    { title: '法律咨询', prompt: '帮我做一个「法律咨询助手」：分类咨询（劳动/婚姻/合同）+ 法条引用 + 案例推荐' },
    { title: '健康问答', prompt: '帮我做一个「健康问答助手」：症状自查 + 科室推荐 + 紧急程度判断 + 免责声明' },
  ],
  '财务经营': [
    { title: '个人记账', prompt: '帮我做一个「个人记账」应用：快速录入 + 自动分类 + 月度统计 + 预算提醒' },
    { title: '预算管理', prompt: '帮我做一个「预算管理」应用：分类预算 + 实际支出对比 + 超支预警 + 月度复盘' },
    { title: '发票管理', prompt: '帮我做一个「发票管理」应用：OCR 识别 + 自动归档 + 报销流程 + 统计报表' },
    { title: '财务报表', prompt: '帮我做一个「财务报表」生成器：录入数据 + 利润表/资产负债表自动生成 + 趋势图' },
    { title: '投资追踪', prompt: '帮我做一个「投资追踪」应用：持仓管理 + 实时估值 + 收益分析 + 风险评估' },
  ],
  '办公协同': [
    { title: '团队任务看板', prompt: '帮我做一个「团队任务看板」：待办/进行中/已完成三栏 + 拖拽 + 负责人 + 截止日期' },
    { title: '项目管理', prompt: '帮我做一个「项目管理」工具：甘特图 + 里程碑 + 资源分配 + 进度报告' },
    { title: '团队日历', prompt: '帮我做一个「团队共享日历」：成员日程可见 + 会议预约 + 时区显示 + 提醒' },
    { title: '文档协作', prompt: '帮我做一个「文档协作平台」：实时多人编辑 + 评论 + 版本历史 + 权限管理' },
    { title: '周报生成器', prompt: '帮我做一个「周报生成器」：本周完成/下周计划/问题 + 模板选择 + 一键导出' },
  ],
  '营销增长': [
    { title: '增长漏斗看板', prompt: '帮我做一个「增长漏斗看板」：访问→注册→激活→付费 4 段漏斗 + 转化率 + 趋势' },
    { title: 'A/B 测试', prompt: '帮我做一个「A/B 测试分析」工具：实验配置 + 流量分配 + 显著性检验 + 结论' },
    { title: '用户画像', prompt: '帮我做一个「用户画像生成器」：导入用户数据 + 自动分群 + 标签可视化 + 行为分析' },
    { title: '活动效果追踪', prompt: '帮我做一个「活动效果追踪」：曝光/点击/转化全链路 + ROI 计算 + 多活动对比' },
    { title: '转化率优化', prompt: '帮我做一个「转化率优化」诊断工具：页面热力图模拟 + 流失分析 + 优化建议' },
  ],
  '其他': [
    { title: '创意名片', prompt: '帮我做一个「创意个人名片」：动画 Hero + 技能标签 + 作品链接 + 一键生成 PNG' },
    { title: '婚礼请柬', prompt: '帮我做一个「婚礼电子请柬」：封面动效 + 爱情故事 + 地点时间 + RSVP + 祝福墙' },
    { title: '生日贺卡', prompt: '帮我做一个「生日贺卡」：选择模板 + 自定义文案 + 配乐 + 一键分享' },
    { title: '时间胶囊', prompt: '帮我做一个「时间胶囊」：现在写信 + 设置开启日期 + 倒计时 + 到达时邮件提醒' },
    { title: '心情日记', prompt: '帮我做一个「心情日记」：每日记录 + 情绪图标 + 天气 + 月度心情曲线' },
  ],
}
const activeTabExamples = computed<TabExample[]>(() => TAB_EXAMPLES[activeFilter.value] || [])
const pickExample = (prompt: string, category?: string) => {
  userPrompt.value = prompt
  pendingCategory.value = category || ''
  nextTick(() => promptEl.value?.focus())
}
const creating = ref(false)
const depthMode = ref<'deep' | 'quick'>('deep')
const depthLabel = computed(() => (depthMode.value === 'deep' ? '深度开发' : '快速开发'))
const depthDropdownOpen = ref(false)
const toggleDepthDropdown = () => {
  depthDropdownOpen.value = !depthDropdownOpen.value
}
const selectDepth = (mode: 'deep' | 'quick') => {
  depthMode.value = mode
  depthDropdownOpen.value = false
}
const onDocClickForDepth = (e: MouseEvent) => {
  const t = e.target as HTMLElement
  if (!t.closest('.depth-chip')) depthDropdownOpen.value = false
  if (!t?.closest('.add-wrap')) addDropdownOpen.value = false
}

// 「+」添加菜单
const addDropdownOpen = ref(false)
const toggleAddDropdown = () => {
  addDropdownOpen.value = !addDropdownOpen.value
}

// ============ 语音输入（Web Speech API） ============
const voiceListening = ref(false)
let recognition: any = null
// 语音识别错误码 → 友好中文提示（audio-capture = 拿不到麦克风音频流，多为设备/系统拦截）
const VOICE_ERROR_TEXT: Record<string, string> = {
  'audio-capture': '无法访问麦克风：请确认麦克风已连接且未被其他程序占用，并在浏览器中允许本站使用麦克风',
  'not-allowed': '麦克风权限被拒绝：请点击地址栏左侧的「允许」以启用麦克风',
  'service-not-allowed': '麦克风权限被拒绝：请在浏览器站点设置中允许本站使用麦克风',
  'no-speech': '没有检测到语音，请靠近麦克风再说一次',
  'aborted': '语音识别已中断',
  'network': '语音识别网络异常，请检查网络后重试',
  'bad-grammar': '语音识别配置错误',
  'language-not-supported': '当前浏览器不支持中文语音识别，请使用 Chrome / Edge',
}
const isVoiceSoftError = (code?: string) => code === 'no-speech' || code === 'aborted'
const voiceErrorText = (code?: string) =>
  (code && VOICE_ERROR_TEXT[code]) || `语音识别失败：${code || '未知错误'}`

// 麦克风预检：用 getUserMedia 提前探测，给出比 SpeechRecognition 的 audio-capture 更精准的原因
const MIC_ERROR_TEXT: Record<string, string> = {
  NotAllowedError: '麦克风权限被拒绝：请在浏览器地址栏左侧「允许」本站使用麦克风后重试',
  SecurityError: '麦克风访问被安全策略阻止：请通过 https 或 localhost 打开本页（Web Speech API 需安全上下文）',
  NotFoundError: '未找到麦克风设备：请连接麦克风后重试',
  DevicesNotFoundError: '未找到麦克风设备：请连接麦克风后重试',
  NotReadableError: '麦克风被其他程序占用：请关闭占用麦克风的应用（会议/录屏等）后重试',
  TrackStartError: '麦克风被其他程序占用：请关闭占用麦克风的应用（会议/录屏等）后重试',
  OverconstrainedError: '麦克风设备不满足要求',
}
const ensureMicAccess = async (): Promise<boolean> => {
  if (!navigator.mediaDevices?.getUserMedia) {
    message.error('当前环境不支持麦克风访问（需 HTTPS 或 localhost 安全上下文，且浏览器为 Chrome / Edge）')
    return false
  }
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    // 立即释放轨道，交给 SpeechRecognition 自己取流，避免「设备被占用」冲突
    stream.getTracks().forEach((t) => t.stop())
    return true
  } catch (err: any) {
    const name = err?.name
    message.error(MIC_ERROR_TEXT[name] || `麦克风不可用：${name || '未知错误'}`)
    return false
  }
}

const toggleVoice = async () => {
  const SR = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition
  if (!SR) {
    message.info('当前浏览器不支持语音输入，请使用 Chrome / Edge')
    return
  }
  if (voiceListening.value) {
    recognition?.stop()
    return
  }
  // 先预检麦克风，拿到精准失败原因再决定是否启动识别
  const ok = await ensureMicAccess()
  if (!ok) return
  recognition = new SR()
  recognition.lang = 'zh-CN'
  recognition.interimResults = true
  recognition.continuous = false
  recognition.onresult = (e: any) => {
    let text = ''
    for (let i = 0; i < e.results.length; i++) text += e.results[i][0].transcript
    const t = text.trim()
    if (!t) return
    userPrompt.value = userPrompt.value.trim() ? userPrompt.value.trim() + ' ' + t : t
  }
  recognition.onend = () => { voiceListening.value = false }
  recognition.onerror = (e: any) => {
    voiceListening.value = false
    const code = e?.error
    if (isVoiceSoftError(code)) {
      message.info(voiceErrorText(code))
    } else {
      message.error(voiceErrorText(code))
    }
  }
  try {
    recognition.start()
    voiceListening.value = true
  } catch (err) {
    voiceListening.value = false
  }
}

// ============ 素材库弹窗 ============
const materialsVisible = ref(false)
const matList = ref<API.MaterialVO[]>([])
const matLoading = ref(false)
const matKeyword = ref('')
const matPage = ref(1)
const matTotal = ref(0)
const matIcon = (t?: string) => {
  if (t === 'image') return '🖼️'
  if (t === 'video') return '🎬'
  if (t === 'audio') return '🎵'
  return '📄'
}
const formatSize = (n?: number) => {
  if (!n) return '—'
  if (n < 1024) return n + ' B'
  if (n < 1024 * 1024) return (n / 1024).toFixed(1) + ' KB'
  return (n / 1024 / 1024).toFixed(1) + ' MB'
}
const loadMaterials = async () => {
  matLoading.value = true
  try {
    const res = await listMyMaterialVoByPage({
      pageNum: matPage.value,
      pageSize: 30,
      name: matKeyword.value.trim() || undefined,
    })
    if (res.data?.code === 0 && res.data.data) {
      matList.value = res.data.data.records ?? []
      matTotal.value = res.data.data.totalRow ?? matList.value.length
    } else {
      matList.value = []
    }
  } catch (e) {
    message.error('素材加载失败')
    matList.value = []
  } finally {
    matLoading.value = false
  }
}
const openMaterials = () => {
  materialsVisible.value = true
  addDropdownOpen.value = false
  loadMaterials()
}
const insertMaterial = (m: API.MaterialVO) => {
  const tag = `[素材:${m.name}]`
  userPrompt.value = userPrompt.value.trim() ? userPrompt.value.trim() + ' ' + tag : tag
  materialsVisible.value = false
  addDropdownOpen.value = false
  message.success('已插入素材：' + (m.name || ''))
}

// ============ 上传文件 / 图片 ============
const uploading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const triggerUpload = () => {
  addDropdownOpen.value = false
  fileInput.value?.click()
}
const onFilePicked = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = '' // 允许重复选择同一文件
  if (!file) return
  const fd = new FormData()
  fd.append('file', file)
  uploading.value = true
  try {
    const res = await uploadMaterial(fd)
    if (res.data?.code === 0) {
      const tag = `[素材:${file.name}]`
      userPrompt.value = userPrompt.value.trim() ? userPrompt.value.trim() + ' ' + tag : tag
      message.success('上传成功，已插入：' + file.name)
    } else {
      message.error('上传失败：' + (res.data?.message || '未知错误'))
    }
  } catch (err) {
    message.error('上传失败')
  } finally {
    uploading.value = false
  }
}

// 顶部公告
const banners = ref<{ id: string; text: string; pink?: boolean }[]>([])
const dismissBanner = (id: string) => {
  banners.value = banners.value.filter((b) => b.id !== id)
}

// ============ 创建应用 ============
const createApp = async (preset?: string) => {
  const raw = (preset ?? userPrompt.value).trim()
  if (!raw) {
    message.warning('请输入应用描述')
    return
  }
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    await router.push('/user/login')
    return
  }
  const appName = raw.length > 12 ? `${raw.slice(0, 12)}…` : raw
  const codeGenType =
    depthMode.value === 'deep' ? CodeGenTypeEnum.VUE_PROJECT : CodeGenTypeEnum.HTML
  // 携带已选技能时，把 @技能名 加到 prompt 前面（与 selectSkill 行为一致）
  const initPrompt = selectedSkill.value
    ? `@${selectedSkill.value.name} ${raw}`
    : raw
  await submitCreate(initPrompt, appName, codeGenType)
}

const submitCreate = async (
  initPrompt: string,
  appName?: string,
  codeGenType?: string,
) => {
  creating.value = true
  try {
    const body: Record<string, any> = { initPrompt }
    if (appName) body.appName = appName
    if (codeGenType) body.codeGenType = codeGenType
    // 传递技能ID（如果有选中的技能）
    if (selectedSkill.value?.id) {
      body.skillId = selectedSkill.value.id
    }
    // 快捷入口芯片 / 分类示例携带的分类：写入 app.category，
    // 后端按 getExternalAssetType 路由到真实 AI 服务（图片/视频/3D/PPT 等）
    if (pendingCategory.value) {
      body.category = pendingCategory.value
    }
    const res = await addApp(body as API.AppAddRequest)
    if (res.data.code === 0 && res.data.data) {
      message.success('应用创建成功')
      userPrompt.value = ''
      selectedSkill.value = null
      pendingCategory.value = ''
      const appId = String(res.data.data)
      await router.push(`/app/chat/${appId}`)
    } else {
      message.error('创建失败：' + res.data.message)
    }
  } catch (error) {
    console.error('创建应用失败：', error)
    message.error('创建失败，请重试')
  } finally {
    creating.value = false
  }
}

// 扩写
const expanding = ref(false)
const handleExpandPrompt = async () => {
  const raw = userPrompt.value.trim()
  if (!raw) {
    message.warning('请先输入要扩写的内容')
    return
  }
  expanding.value = true
  try {
    const res = await expandPrompt({ prompt: raw })
    if (res.data.code === 0 && res.data.data) {
      userPrompt.value = res.data.data
      message.success('已智能扩写，检查后可发送')
      // 智能扩写会扣积分，刷新余额
      refreshBalance()
    } else {
      message.error(res.data.message || '扩写失败')
    }
  } catch {
    message.error('扩写失败，请稍后再试')
  } finally {
    expanding.value = false
  }
}

// ============ 技能弹窗 ============
const skillsVisible = ref(false)
const openSkills = () => {
  skillsVisible.value = true
}
const onPickSkill = (s: API.SkillVO) => {
  const name = s?.skillName || ''
  if (!name) return
  selectedSkill.value = { id: Number(s.id), name }
  userPrompt.value = userPrompt.value.trim() ? `${userPrompt.value.trim()} @${name}` : `@${name}`
  skillsVisible.value = false
  message.success(`已添加技能：${name}`)
}

// ============ AI 搜灵感 ============
const inspireKeyword = ref('')
const inspireMode = ref(false)
const inspireList = ref<any[]>([])
const onInspireSearch = async () => {
  const kw = inspireKeyword.value.trim()
  if (!kw) return
  inspireMode.value = true
  pageNum.value = 1
  await loadApps()
  if (inspireList.value.length > 0) {
    message.success(`找到 ${inspireList.value.length} 个相关灵感`)
  } else {
    message.info('暂无相关灵感，换个词试试')
  }
}
const clearInspire = () => {
  inspireMode.value = false
  inspireList.value = []
  pageNum.value = 1
  loadApps()
}

// ============ 卡片网格 ============
const filterTabs = [
  '全部', '我的作品', '模板', '推荐', '应用类手', '游戏', '工具', '教育',
  '网站', '电商', '办公', '营销', '研究', '问答', '财务经营', '办公协同', '营销增长', '其他',
]
// 领域分类 tab（对应后端 category 字段）
const CATEGORY_TABS = ['应用类手', '游戏', '工具', '教育', '网站', '电商', '办公', '营销', '研究', '问答', '财务经营', '办公协同', '营销增长', '其他']
const isCategoryTab = (t: string) => CATEGORY_TABS.includes(t)

// 前端 tab 名 → 后端 category value 映射
const TAB_TO_CATEGORY: Record<string, string> = {
  '应用类手': 'app_mini',
  '游戏': 'game',
  '工具': 'tool',
  '教育': 'education',
  '网站': 'website',
  '电商': 'ecommerce',
  '办公': 'office_cat',
  '营销': 'marketing_cat',
  '研究': 'research_cat',
  '问答': 'qa',
  '财务经营': 'finance',
  '办公协同': 'office_collab',
  '营销增长': 'marketing_growth',
  '其他': 'other',
}
// 根据 tab 名获取后端 category value
const getCategoryValue = (tab: string): string => {
  return TAB_TO_CATEGORY[tab] || ''
}

// ============ 快捷分类入口（chip → 展开相关应用面板） ============
const entryChips = [
  { key: 'mini', label: '🤖 小程序', cat: 'app_mini' },
  { key: 'life', label: '🛠️ 生活工具', cat: 'tool' },
  { key: 'fun', label: '🎮 AI 趣味', cat: 'game' },
  { key: 'shop', label: '🛒 电商团购', cat: 'ecommerce' },
  { key: 'grow', label: '📈 营销增长', cat: 'marketing_growth' },
  { key: 'office', label: '💼 办公协同', cat: 'office_collab' },
]
const activeEntryChip = ref<string | null>(null)
const entryPanelApps = ref<any[]>([])
const entryLoading = ref(false)
const activeEntryLabel = computed(
  () => entryChips.find((c) => c.key === activeEntryChip.value)?.label || '',
)
const onEntryChipClick = async (chip: { key: string; label: string; cat: string }) => {
  if (activeEntryChip.value === chip.key) {
    activeEntryChip.value = null // 再次点同一 chip → 收起
    return
  }
  activeEntryChip.value = chip.key
  entryLoading.value = true
  try {
    const res = await listGoodAppVoByPage({ pageNum: 1, pageSize: 3, category: chip.cat })
    if (res.data?.code === 0 && res.data.data?.records?.length) {
      entryPanelApps.value = res.data.data.records.map(mapApp)
    } else {
      // 后端无数据 → 兜底 3 张示例卡
      entryPanelApps.value = fallbackApps.slice(0, 3).map((a, i) => ({
        id: '',
        title: a.title,
        emoji: a.emoji,
        author: a.author,
        stat: a.stat,
        grad: gradPool[i % 20],
        av: avPool[i % 20],
        real: false,
      }))
    }
  } catch {
    entryPanelApps.value = []
  } finally {
    entryLoading.value = false
  }
}
const closeEntryPanel = () => {
  activeEntryChip.value = null
}

const activeFilter = ref('全部')
const appList = ref<any[]>([])
const loadingApps = ref(false)
const pageNum = ref(1)
const pageSize = ref(20)
const totalRow = ref(0)
const totalPage = computed(() => Math.max(1, Math.ceil(totalRow.value / pageSize.value)))

const gradPool = Array.from({ length: 20 }, (_, i) => `g${i + 1}`)
const avPool = Array.from({ length: 20 }, (_, i) => `a${i + 1}`)

// 稿子示例卡（后端无数据时兜底，保证页面饱满）
const fallbackApps = [
  { title: '智能客服机器人 · 多轮对话版', emoji: '🤖', author: '管理员', stat: '❤ 4.2W · 👁 394' },
  { title: 'AI 图像生成器 · 二次元风格', emoji: '🎨', author: '林清欢', stat: '❤ 3.8W · 👁 612' },
  { title: '智能写作助手 · 长文一键成稿', emoji: '✍️', author: '苏沐', stat: '❤ 2.9W · 👁 521' },
  { title: '数据可视化看板 · 拖拽搭建', emoji: '📊', author: '陈思远', stat: '❤ 2.1W · 👁 287' },
  { title: '智能翻译官 · 100+ 语种互译', emoji: '🌐', author: 'Alex', stat: '❤ 5.6W · 👁 1.2K' },
  { title: '语音转文字 · 会议实时记录', emoji: '🎙️', author: '王浩然', stat: '❤ 1.8W · 👁 412' },
  { title: '代码审查助手 · 智能重构建议', emoji: '💻', author: 'Kyle', stat: '❤ 3.4W · 👁 538' },
  { title: 'PPT 一键生成 · 大纲即出成品', emoji: '📽️', author: '赵小萌', stat: '❤ 4.7W · 👁 826' },
  { title: '视频剪辑助手 · 一键成片', emoji: '🎬', author: 'Mia', stat: '❤ 6.2W · 👁 1.5K' },
  { title: '电商导购机器人 · 全网比价', emoji: '🛒', author: '钱多多', stat: '❤ 2.3W · 👁 367' },
  { title: '学习陪练导师 · 因材施教', emoji: '📚', author: '李知行', stat: '❤ 3.1W · 👁 489' },
  { title: '健康饮食规划 · 千人千方', emoji: '🥗', author: '周慕云', stat: '❤ 1.4W · 👁 256' },
  { title: '财务记账助手 · 自动归类账单', emoji: '💰', author: '吴婉清', stat: '❤ 1.9W · 👁 298' },
  { title: '旅行规划师 · 一句话出攻略', emoji: '✈️', author: '郑凯乐', stat: '❤ 2.7W · 👁 451' },
  { title: '简历优化专家 · 智能润色', emoji: '📄', author: 'Frank', stat: '❤ 4.5W · 👁 723' },
  { title: '营销文案生成 · 多平台适配', emoji: '📣', author: '何小鱼', stat: '❤ 2.2W · 👁 376' },
  { title: '法律咨询助手 · 智能问答', emoji: '⚖️', author: '徐明轩', stat: '❤ 1.6W · 👁 234' },
  { title: '心理咨询树洞 · 24h 在线陪伴', emoji: '💗', author: '高若彤', stat: '❤ 5.1W · 👁 982' },
  { title: '会议纪要助手 · 自动提取待办', emoji: '📝', author: '罗子衿', stat: '❤ 3.7W · 👁 619' },
  { title: '智能家居控制中枢 · 一句话调度', emoji: '🏠', author: '梁博远', stat: '❤ 2.8W · 👁 467' },
]

// 每个 Tab 的 3 个示例应用（无真实数据时作为网格兜底；点击填入创建提示词）
const TAB_DEMO_APPS: Record<string, Array<{ title: string; emoji: string; author: string; stat: string; prompt: string }>> = {
  '全部': [
    { title: '智能客服机器人·多轮对话版', emoji: '🤖', author: '管理员', stat: '❤ 4.2W · 👁 394', prompt: '帮我做一个「智能客服机器人」Web 应用：左侧多轮对话区 + 右侧常见问题快捷回复 + 上下文记忆 + 对话记录导出' },
    { title: 'AI 图像生成器·二次元风', emoji: '🎨', author: '林清欢', stat: '❤ 3.8W · 👁 612', prompt: '帮我做一个「AI 图像生成器」：提示词输入 + 6 种风格预设 + 2×2 结果网格 + 一键下载/重新生成' },
    { title: '数据可视化看板·拖拽搭建', emoji: '📊', author: '陈思远', stat: '❤ 2.1W · 👁 287', prompt: '帮我做一个「数据可视化演示工作台」：左侧数据输入 + 5 种图表切换 + 右侧实时渲染 + 8 页图表故事叙事模式' },
  ],
  '我的作品': [],
  '模板': [
    { title: '极简博客模板', emoji: '✍️', author: '苏沐', stat: '❤ 2.9W · 👁 521', prompt: '帮我做一个「极简个人博客」模板：首页文章列表 + 文章详情 + 标签分类 + 暗色模式 + 阅读时长统计' },
    { title: '通用后台管理模板', emoji: '🛠️', author: 'Kyle', stat: '❤ 3.4W · 👁 538', prompt: '帮我做一个「通用后台管理」模板：左侧导航 + 面包屑 + 数据表格（搜索/分页/筛选）+ 统计卡片 + 暗色模式' },
    { title: 'SaaS 落地页模板', emoji: '🚀', author: '赵小萌', stat: '❤ 4.7W · 👁 826', prompt: '帮我做一个「SaaS 产品落地页」模板：Hero + 特性展示 + 客户案例 + 价格表 + FAQ + 底部 CTA' },
  ],
  '推荐': [
    { title: '营销文案生成·多平台', emoji: '📣', author: '何小鱼', stat: '❤ 2.2W · 👁 376', prompt: '帮我做一个「营销文案生成器」：输入卖点输出 4 类文案 × 3 版共 12 条 + 一键复制 + 收藏夹' },
    { title: '简历优化专家·润色', emoji: '📄', author: 'Frank', stat: '❤ 4.5W · 👁 723', prompt: '帮我做一个「智能简历工坊」：左右两栏 A4 预览 + 3 套模板 + 可打印 PDF 导出' },
    { title: '会议纪要助手·待办', emoji: '📝', author: '罗子衿', stat: '❤ 3.7W · 👁 619', prompt: '帮我做一个「会议纪要工具」：录音转写 + AI 提取待办 + 参与者标注 + 一键导出' },
  ],
  '应用类手': [
    { title: '城市生活指南小程序', emoji: '🏙️', author: '管理员', stat: '❤ 1.2W · 👁 210', prompt: '帮我做一个「城市生活指南」小程序风格单页 H5：底部 4 Tab + 顶部搜索 + 卡片瀑布流 + 详情弹窗' },
    { title: '美食探店小程序', emoji: '🍜', author: '林清欢', stat: '❤ 1.5W · 👁 288', prompt: '帮我做一个「美食推荐」小程序：附近餐厅卡片 + 详情含评分/菜单/位置 + 收藏夹' },
    { title: '健身打卡小程序', emoji: '💪', author: '苏沐', stat: '❤ 0.9W · 👁 176', prompt: '帮我做一个「健身打卡」小程序：今日训练卡片 + 打卡日历 + 连续天数统计 + 训练记录' },
  ],
  '游戏': [
    { title: '2048 数字合成', emoji: '🎮', author: '陈思远', stat: '❤ 2.3W · 👁 412', prompt: '帮我做一个「2048 数字合成」小游戏：4×4 网格 + 上下左右滑动 + 同数合成 + 分数与最高纪录' },
    { title: '经典扫雷', emoji: '💣', author: '王浩然', stat: '❤ 1.1W · 👁 201', prompt: '帮我做一个经典「扫雷」小游戏：可调难度（9×9/16×16/16×30）+ 数字提示 + 雷数显示 + 胜负弹窗' },
    { title: '五子棋对战', emoji: '♟️', author: 'Alex', stat: '❤ 0.8W · 👁 156', prompt: '帮我做一个「五子棋」小游戏：15×15 棋盘 + 双人轮流 + 五连判定 + 悔棋 + 重开' },
  ],
  '工具': [
    { title: 'JSON 格式化工具', emoji: '🔧', author: 'Kyle', stat: '❤ 3.4W · 👁 538', prompt: '帮我做一个「JSON 格式化工具」：粘贴 JSON 一键美化/压缩 + 语法校验 + 树形折叠 + 转 XML/CSV' },
    { title: 'Markdown 编辑器', emoji: '📝', author: '赵小萌', stat: '❤ 2.8W · 👁 467', prompt: '帮我做一个「Markdown 编辑器」：左右实时预览 + 工具栏 + 代码高亮 + 导出 HTML/PDF' },
    { title: '二维码生成器', emoji: '🔳', author: 'Mia', stat: '❤ 1.6W · 👁 256', prompt: '帮我做一个「二维码生成器」：输入文本/URL + 自定义颜色与 logo + 多种尺寸下载' },
  ],
  '教育': [
    { title: '单词背记·艾宾浩斯', emoji: '📚', author: '李知行', stat: '❤ 3.1W · 👁 489', prompt: '帮我做一个「单词背记」应用：每日 20 词 + 艾宾浩斯复习 + 测试模式 + 进度追踪' },
    { title: '数学公式练习', emoji: '✏️', author: '周慕云', stat: '❤ 1.4W · 👁 256', prompt: '帮我做一个「数学公式练习」应用：按章节选题 + 步骤引导 + 即时判分 + 错题本' },
    { title: '编程练习平台', emoji: '💻', author: 'Frank', stat: '❤ 2.5W · 👁 398', prompt: '帮我做一个「在线编程练习」平台：题目列表 + 代码编辑器 + 实时运行 + 测试用例' },
  ],
  '网站': [
    { title: '个人作品集网站', emoji: '🎨', author: '苏沐', stat: '❤ 2.9W · 👁 521', prompt: '帮我做一个「个人作品集」网站：首页大图 Hero + 项目卡片网格 + 关于我 + 联系表单' },
    { title: '公司官网模板', emoji: '🏢', author: '陈思远', stat: '❤ 1.8W · 👁 312', prompt: '帮我做一个「公司官网」：顶部导航 + Hero 区 + 服务介绍 + 团队 + 客户案例 + 联系 + 页脚' },
    { title: '美食博客', emoji: '🍰', author: '何小鱼', stat: '❤ 1.3W · 👁 234', prompt: '帮我做一个「美食博客」网站：菜谱卡片列表 + 详情含食材步骤 + 分类标签 + 搜索' },
  ],
  '电商': [
    { title: '服装商城', emoji: '🛍️', author: '钱多多', stat: '❤ 2.3W · 👁 367', prompt: '帮我做一个「服装电商」应用：商品瀑布流 + 筛选（尺码/颜色/价格）+ 详情页 + 购物车 + 结算' },
    { title: '美妆购物', emoji: '💄', author: 'Mia', stat: '❤ 1.9W · 👁 329', prompt: '帮我做一个「美妆购物」应用：色卡选择 + 肤质匹配 + 试用装申请 + 一键下单' },
    { title: '二手交易平台', emoji: '♻️', author: '郑凯乐', stat: '❤ 1.1W · 👁 198', prompt: '帮我做一个「二手交易平台」应用：发布商品 + 议价聊天 + 信用评分 + 担保交易' },
  ],
  '办公': [
    { title: '任务待办清单', emoji: '✅', author: '罗子衿', stat: '❤ 3.7W · 👁 619', prompt: '帮我做一个「任务待办」应用：四象限 + 优先级 + 截止日期 + 子任务 + 完成统计' },
    { title: '日程日历', emoji: '📅', author: '高若彤', stat: '❤ 1.6W · 👁 284', prompt: '帮我做一个「日程日历」应用：月/周/日视图 + 事件拖拽 + 提醒 + 重复事件' },
    { title: '笔记应用', emoji: '📒', author: '吴婉清', stat: '❤ 1.9W · 👁 298', prompt: '帮我做一个「笔记应用」：富文本编辑 + Markdown + 双链笔记 + 全局搜索 + 标签' },
  ],
  '营销': [
    { title: '营销落地页', emoji: '🚀', author: '何小鱼', stat: '❤ 2.2W · 👁 376', prompt: '帮我做一个「高转化营销落地页」：痛点共鸣 + 解决方案 + 社会证明 + 限时优惠 + CTA 按钮' },
    { title: '九宫格抽奖', emoji: '🎁', author: '赵小萌', stat: '❤ 1.7W · 👁 298', prompt: '帮我做一个「九宫格抽奖活动页」：奖品展示 + 抽奖动画 + 中奖记录 + 分享得额外机会' },
    { title: '裂变海报生成器', emoji: '🖼️', author: 'Mia', stat: '❤ 1.3W · 👁 234', prompt: '帮我做一个「邀请裂变海报生成器」：输入文案 + 选择模板 + 一键生成 + 二维码合成' },
  ],
  '研究': [
    { title: '行业研究报告生成', emoji: '📊', author: '陈思远', stat: '❤ 2.1W · 👁 287', prompt: '帮我做一个「行业研究报告」生成器：输入主题 + 自动生成大纲 + 结构化排版 + 图表引用' },
    { title: '竞品分析工具', emoji: '🔍', author: 'Kyle', stat: '❤ 1.5W · 👁 268', prompt: '帮我做一个「竞品分析」工具：竞品列表 + 维度对比表格 + SWOT 分析 + 一键导出' },
    { title: '调研问卷生成', emoji: '📋', author: '李知行', stat: '❤ 1.2W · 👁 223', prompt: '帮我做一个「调研问卷」生成器：题型混排 + 逻辑跳转 + 结果统计图表 + 导出 Excel' },
  ],
  '问答': [
    { title: '智能客服机器人·问答', emoji: '🤖', author: '管理员', stat: '❤ 4.2W · 👁 394', prompt: '帮我做一个「客服机器人」：知识库问答 + 多轮对话 + 人工转接 + 满意度评分' },
    { title: '知识库问答', emoji: '📚', author: 'Frank', stat: '❤ 2.4W · 👁 398', prompt: '帮我做一个「知识库问答」应用：上传文档 + 智能检索 + 答案引用原文 + 置信度显示' },
    { title: '法律咨询助手', emoji: '⚖️', author: '徐明轩', stat: '❤ 1.6W · 👁 234', prompt: '帮我做一个「法律咨询助手」：分类咨询（劳动/婚姻/合同）+ 法条引用 + 案例推荐' },
  ],
  '财务经营': [
    { title: '个人记账本', emoji: '💰', author: '吴婉清', stat: '❤ 1.9W · 👁 298', prompt: '帮我做一个「个人记账」应用：快速录入 + 自动分类 + 月度统计 + 预算提醒' },
    { title: '预算管理工具', emoji: '📊', author: '周慕云', stat: '❤ 1.4W · 👁 256', prompt: '帮我做一个「预算管理」应用：分类预算 + 实际支出对比 + 超支预警 + 月度复盘' },
    { title: '财务报表生成', emoji: '📈', author: '陈思远', stat: '❤ 1.2W · 👁 223', prompt: '帮我做一个「财务报表」生成器：录入数据 + 利润表/资产负债表自动生成 + 趋势图' },
  ],
  '办公协同': [
    { title: '团队任务看板', emoji: '🗂️', author: '罗子衿', stat: '❤ 3.7W · 👁 619', prompt: '帮我做一个「团队任务看板」：待办/进行中/已完成三栏 + 拖拽 + 负责人 + 截止日期' },
    { title: '项目管理甘特图', emoji: '📋', author: 'Kyle', stat: '❤ 2.1W · 👁 387', prompt: '帮我做一个「项目管理」工具：甘特图 + 里程碑 + 资源分配 + 进度报告' },
    { title: '团队共享日历', emoji: '📅', author: '高若彤', stat: '❤ 1.6W · 👁 284', prompt: '帮我做一个「团队共享日历」：成员日程可见 + 会议预约 + 时区 + 提醒' },
  ],
  '营销增长': [
    { title: '增长漏斗看板', emoji: '📉', author: '何小鱼', stat: '❤ 1.8W · 👁 298', prompt: '帮我做一个「增长漏斗看板」：访问→注册→激活→付费 4 段漏斗 + 转化率 + 趋势' },
    { title: 'A/B 测试分析', emoji: '🧪', author: 'Mia', stat: '❤ 1.3W · 👁 234', prompt: '帮我做一个「A/B 测试分析」工具：实验配置 + 流量分配 + 显著性检验 + 结论' },
    { title: '用户画像生成', emoji: '👤', author: '赵小萌', stat: '❤ 1.1W · 👁 198', prompt: '帮我做一个「用户画像生成器」：导入用户数据 + 自动分群 + 标签可视化 + 行为分析' },
  ],
  '其他': [
    { title: '创意个人名片', emoji: '💼', author: '苏沐', stat: '❤ 1.5W · 👁 268', prompt: '帮我做一个「创意个人名片」：动画 Hero + 技能标签 + 作品链接 + 一键生成 PNG' },
    { title: '婚礼电子请柬', emoji: '💍', author: '高若彤', stat: '❤ 2.0W · 👁 344', prompt: '帮我做一个「婚礼电子请柬」：封面动效 + 爱情故事 + 地点时间 + RSVP + 祝福墙' },
    { title: '心情日记', emoji: '🌈', author: '吴婉清', stat: '❤ 1.1W · 👁 198', prompt: '帮我做一个「心情日记」：每日记录 + 情绪图标 + 天气 + 月度心情曲线' },
  ],
}

const truncate = (s: string, n: number) => (s.length > n ? s.slice(0, n) + '…' : s)

const mapApp = (app: any, i: number) => ({
  id: app.id != null ? String(app.id) : '',
  title: app.appName || '未命名应用',
  emoji: '📱',
  author: app.user?.userName || app.userName || '匿名',
  stat: app.initPrompt ? `“${truncate(app.initPrompt, 18)}”` : '优质应用',
  grad: gradPool[i % 20],
  av: avPool[i % 20],
  real: true,
})

const displayApps = computed(() => {
  if (inspireMode.value) return inspireList.value.map(mapApp)
  if (appList.value.length > 0) return appList.value.map(mapApp)
  // 无真实应用：优先展示该 Tab 的 3 个示例应用，再退 20 张通用稿
  const demos = TAB_DEMO_APPS[activeFilter.value]
  if (demos && demos.length) {
    return demos.map((d, i) => ({
      id: '',
      title: d.title,
      emoji: d.emoji,
      author: d.author,
      stat: d.stat,
      grad: gradPool[i % 20],
      av: avPool[i % 20],
      real: false,
      prompt: d.prompt,
    }))
  }
  return fallbackApps.map((a, i) => ({
    id: '',
    title: a.title,
    emoji: a.emoji,
    author: a.author,
    stat: a.stat,
    grad: gradPool[i % 20],
    av: avPool[i % 20],
    real: false,
  }))
})

// 该 Tab 是否有示例应用可兜底显示（避免空状态把示例应用挡掉）
const hasDemoApps = computed(() => {
  const d = TAB_DEMO_APPS[activeFilter.value]
  return !!(d && d.length > 0)
})
const gridShowEmpty = computed(() => {
  if (inspireMode.value || appList.value.length > 0 || hasDemoApps.value) return false
  return true
})
const pagerVisible = computed(() => inspireMode.value || appList.value.length > 0)
const visiblePages = computed(() => {
  const tp = totalPage.value
  const cur = pageNum.value
  const start = Math.max(1, cur - 2)
  const end = Math.min(tp, start + 4)
  const arr: number[] = []
  for (let p = start; p <= end; p++) arr.push(p)
  return arr
})
const pagerTotal = computed(() =>
  inspireMode.value
    ? totalRow.value || inspireList.value.length
    : appList.value.length > 0
      ? totalRow.value
      : displayApps.value.length,
)

const loadApps = async () => {
  loadingApps.value = true
  try {
    let res
    if (inspireMode.value) {
      res = await semanticSearchApps({
        keyword: inspireKeyword.value,
        pageNum: pageNum.value,
        pageSize: pageSize.value,
      })
      if (res.data?.code === 0 && res.data.data?.records) {
        inspireList.value = res.data.data.records
        totalRow.value = res.data.data.totalRow ?? inspireList.value.length
        // 语义搜索会扣积分，刷新余额
        refreshBalance()
      } else {
        inspireList.value = []
        totalRow.value = 0
      }
      return
    }
    if (activeFilter.value === '我的作品') {
      res = await listMyAppVoByPage({ pageNum: pageNum.value, pageSize: pageSize.value })
    } else {
      const body: any = { pageNum: pageNum.value, pageSize: pageSize.value }
      if (activeFilter.value === '推荐') {
        body.sortField = 'priority'
        body.sortOrder = 'desc'
      } else if (isCategoryTab(activeFilter.value)) {
        // 分类 tab：传 category 参数给后端筛选
        body.category = getCategoryValue(activeFilter.value)
      }
      res = await listGoodAppVoByPage(body)
    }
    if (res.data?.code === 0 && res.data.data) {
      appList.value = res.data.data.records ?? []
      totalRow.value = res.data.data.totalRow ?? appList.value.length
    } else {
      appList.value = []
      totalRow.value = 0
    }
  } catch {
    if (inspireMode.value) inspireList.value = []
    else appList.value = []
    totalRow.value = 0
  } finally {
    loadingApps.value = false
  }
}

const goPage = (n: number) => {
  if (n < 1 || n > totalPage.value || n === pageNum.value) return
  pageNum.value = n
  loadApps()
}

watch(activeFilter, (t) => {
  if (t === '我的作品' && !loginUserStore.loginUser.id) {
    message.warning('请先登录后查看「我的作品」')
    activeFilter.value = '全部'
    return
  }
  inspireMode.value = false
  pageNum.value = 1
  loadApps()
})

const openApp = (app: any) => {
  if (app.real && app.id) {
    router.push(`/app/chat/${app.id}`)
  } else if (app.prompt) {
    // 示例应用：点击后把创建提示词填入输入框，用户编辑后自行发送
    pickExample(app.prompt)
  } else {
    message.info('示例应用，去广场看看真实作品吧')
    router.push('/community')
  }
}

const goSquare = () => router.push('/community')

onMounted(() => {
  document.addEventListener('click', onDocClickForDepth)
  loadApps()
})

// ===== keep-alive 配合（本页被 BasicLayout 缓存） =====
// 初次激活时 onMounted 已加载，跳过；从缓存恢复（Tab 切回）时刷新列表防脏数据
let keepAliveActivatedOnce = false
onActivated(() => {
  if (keepAliveActivatedOnce) {
    loadApps()
  }
  keepAliveActivatedOnce = true
  document.addEventListener('click', onDocClickForDepth)
})

// 缓存失活时移除全局监听并停止语音识别（失活不触发 onBeforeUnmount）
onDeactivated(() => {
  document.removeEventListener('click', onDocClickForDepth)
  recognition?.stop?.()
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClickForDepth)
  recognition?.stop?.()
})
</script>

<template>
  <div class="home-console">
    <!-- 顶部 promo 栏 -->
    <div class="topbar">
      <span class="new-tag">NEW</span>
      <button class="fb-btn" @click="message.info('共享反馈功能开发中')">❤ 共享反馈</button>
      <button class="biz-btn" @click="message.info('企业版咨询功能开发中')">企业版咨询</button>
      <div
        v-for="b in banners"
        :key="b.id"
        class="top-banner"
        :class="{ pink: b.pink }"
      >
        <span class="ico">📢</span> {{ b.text }}
        <span class="x" @click="dismissBanner(b.id)">×</span>
      </div>
    </div>

    <!-- Hero 区 -->
    <div class="stage">
      <div class="stage-inner">
        <!-- 快捷入口 -->
        <QuickEntryChips @pick="onQuickPick" />

        <div class="search-card">
          <div v-if="selectedSkill" class="selected-skill-chip">
            <span class="ss-icon">🎀</span>
            <span class="ss-name">{{ selectedSkill.name }}</span>
            <button class="ss-close" title="移除技能" @click="clearSelectedSkill">×</button>
          </div>
          <textarea
            v-model="userPrompt"
            ref="promptEl"
            class="search-input"
            rows="2"
            placeholder="描述你想要的应用或网站， ⌘ 快捷键唤起技能"
            @keydown.meta.enter="createApp()"
            @keydown.ctrl.enter="createApp()"
          ></textarea>
          <div class="search-foot">
            <span v-if="detectedLang" class="lang-badge" title="已自动识别输入语言">
              <span class="lb-ico">🌐</span>{{ detectedLang }}
            </span>
            <span class="add-wrap" @click.stop="toggleAddDropdown">
              <button class="ib add-btn" title="添加">+</button>
              <div v-if="addDropdownOpen" class="add-menu" @click.stop>
                <div class="add-item" @click="openMaterials">
                  <span class="ai-icon">🖨️</span>
                  <span>从素材库中添加</span>
                </div>
                <div class="add-item" @click="triggerUpload">
                  <span class="ai-icon">⬆️</span>
                  <span>上传文件或图片</span>
                </div>
              </div>
            </span>
            <span class="chip pink" @click="handleExpandPrompt">✱ 扩写</span>
            <span class="chip mint" @click="openSkills">⚡ 技能</span>
            <span
              class="chip depth-chip"
              :class="{ 'chip-on': depthDropdownOpen || depthMode === 'deep' }"
              @click.stop="toggleDepthDropdown"
            >
              + {{ depthLabel }}
              <span class="caret">▾</span>
              <div v-if="depthDropdownOpen" class="depth-menu" @click.stop>
                <div
                  class="depth-item"
                  :class="{ active: depthMode === 'deep' }"
                  @click="selectDepth('deep')"
                >
                  <span class="di-icon">🗂️</span>
                  <span class="di-text">深度开发</span>
                </div>
                <div
                  class="depth-item"
                  :class="{ active: depthMode === 'quick' }"
                  @click="selectDepth('quick')"
                >
                  <span class="di-icon bolt">⚡</span>
                  <span class="di-text">快速开发</span>
                </div>
              </div>
            </span>
            <div class="right">
              <div class="ib" title="菜单" @click="message.info('更多功能开发中')">☰</div>
              <div class="ib" title="语音输入" :class="{ active: voiceListening }" @click="toggleVoice">🎙️</div>
              <div class="ib" title="发送" :class="{ active: userPrompt.trim() && !creating }" @click="createApp()">
                {{ creating ? '生成中…' : '➤' }}
              </div>
            </div>
            <input
              ref="fileInput"
              type="file"
              accept="image/*,video/*,audio/*,.pdf,.doc,.docx,.txt,.zip,.ppt,.pptx"
              style="display: none"
              @change="onFilePicked"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 分类详情面板 -->
    <div v-if="activeEntryChip" class="entry-panel">
      <div class="ep-head">
        <span class="ep-title">{{ activeEntryLabel }}</span>
        <button class="ep-close" title="收起" @click="closeEntryPanel">×</button>
      </div>
      <div v-if="entryLoading" class="ep-state">加载中…</div>
      <div v-else-if="entryPanelApps.length === 0" class="ep-state">该分类暂无应用</div>
      <div v-else class="ep-cards">
        <div
          v-for="a in entryPanelApps"
          :key="a.id || a.title"
          class="ep-card"
          @click="openApp(a)"
        >
          <div class="ep-thumb">{{ a.emoji || '📱' }}</div>
          <div class="ep-name">{{ a.title }}</div>
          <div class="ep-author">{{ a.author }}</div>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-tabs">
        <div
          v-for="t in filterTabs"
          :key="t"
          class="ftab"
          :class="{ on: activeFilter === t }"
          @click="activeFilter = t"
        >{{ t }}</div>
      </div>
      <div class="filter-right">
        <div class="mini-search">
          <span class="si">⌕</span>
          <input
            v-model="inspireKeyword"
            placeholder="AI 搜灵感"
            @keyup.enter="onInspireSearch"
          />
        </div>
      </div>
    </div>

    <!-- 分类示例（点击填入输入框） -->
    <div v-if="activeTabExamples.length > 0" class="examples-row">
      <div class="examples-head">
        <span class="examples-title">✨ 试试这些</span>
        <span class="examples-sub">点击下方任意示例可直接填入输入框，再按 ➤ 发送</span>
      </div>
      <div class="examples-grid">
        <div
          v-for="(ex, i) in activeTabExamples"
          :key="i"
          class="example-card"
          :title="ex.prompt"
          @click="pickExample(ex.prompt, isCategoryTab(activeFilter.value) ? getCategoryValue(activeFilter.value) : undefined)"
        >
          <div class="ex-title">{{ ex.title }}</div>
          <div class="ex-prompt">{{ truncate(ex.prompt, 56) }}</div>
        </div>
      </div>
    </div>

    <!-- AI 搜灵感结果条 -->
    <div v-if="inspireMode" class="inspire-bar">
      <span>为「<b>{{ inspireKeyword }}</b>」找到 <b>{{ pagerTotal }}</b> 条灵感</span>
      <span style="cursor:pointer;color:var(--primary);" @click="clearInspire">清除</span>
    </div>

    <!-- 卡片网格 -->
    <div v-if="!gridShowEmpty && displayApps.length > 0" class="grid">
      <div
        v-for="(a, i) in displayApps"
        :key="a.id || a.title"
        class="card"
        @click="openApp(a)"
      >
        <div class="thumb" :class="a.grad">
          <div class="emoji">{{ a.emoji }}</div>
        </div>
        <div class="body-2">
          <div class="ttl">{{ a.title }}</div>
          <div class="meta">
            <div class="av" :class="a.av">{{ (a.author || '?').charAt(0) }}</div>
            <span>{{ a.author }}</span>
            <span class="stat">{{ a.stat }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 网格空状态 -->
    <div v-else class="grid-empty">
      <div class="ge-emoji">🍃</div>
      <div>{{ activeFilter === '我的作品' ? '你还没有作品' : '暂无相关应用' }}</div>
      <div class="ge-sub">
        {{ activeFilter === '我的作品' ? '在上方描述你想做的应用，点 ➤ 即可生成' : '试试切换分类或搜索其他关键词' }}
      </div>
      <button v-if="activeFilter === '我的作品'" class="go-btn" @click="startCreate">开始创作</button>
      <button v-else class="go-btn" @click="goSquare">去广场看看</button>
    </div>

    <!-- 分页 -->
    <div v-if="pagerVisible" class="pager">
      <div class="pager-info">第 {{ pageNum }} / {{ totalPage }} 页 · 共 {{ pagerTotal }} 条</div>
      <div class="pager-actions">
        <button class="pg-half prev" :disabled="pageNum <= 1" @click="goPage(pageNum - 1)">上一页</button>
        <button class="pg-half next" :disabled="pageNum >= totalPage" @click="goPage(pageNum + 1)">下一页</button>
      </div>
    </div>

    <!-- 技能弹窗 -->
    <a-modal
      v-model:open="skillsVisible"
      title="技能中心"
      :footer="null"
      width="960px"
    >
      <SkillCenterContent :show-my-tab="false" @use="onPickSkill" />
    </a-modal>

    <!-- 素材库弹窗 -->
    <a-modal
      v-model:open="materialsVisible"
      title="素材库"
      :footer="null"
      width="640px"
    >
      <div class="mat-modal">
        <div class="mat-toolbar">
          <input
            v-model="matKeyword"
            class="mat-search"
            placeholder="搜索素材名称"
            @keyup.enter="loadMaterials"
          />
          <button class="mat-upload" :disabled="uploading" @click="triggerUpload">
            {{ uploading ? '上传中…' : '上传素材' }}
          </button>
        </div>
        <div v-if="matLoading" class="mat-state">加载中…</div>
        <div v-else-if="matList.length === 0" class="mat-state">
          暂无素材，点右上角「上传素材」添加
        </div>
        <div v-else class="mat-list">
          <div
            v-for="m in matList"
            :key="m.id"
            class="mat-item"
            @click="insertMaterial(m)"
          >
            <div class="mat-ico">{{ matIcon(m.type) }}</div>
            <div class="mat-info">
              <div class="mat-name">{{ m.name }}</div>
              <div class="mat-sub">{{ m.type || '文件' }} · {{ formatSize(m.size) }}</div>
            </div>
            <span class="mat-add">＋ 插入</span>
          </div>
        </div>
        <div v-if="matTotal > matList.length" class="mat-more">
          共 {{ matTotal }} 个素材，仅显示前 {{ matList.length }} 个
        </div>
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
.home-console {
  --bg: #f4f6fb;
  --surface: #ffffff;
  --border: #ececf2;
  --primary: #6c5cff;
  --primary-soft: #efedff;
  --blue: #5b8cff;
  --pink: #ff7eb6;
  --pink-soft: #ffe4f0;
  --mint: #14c4a7;
  --mint-soft: #d6f7ef;
  --amber: #ff9a3d;
  --amber-soft: #ffefd6;
  --text: #1d1f29;
  --text-soft: #5b6373;
  --text-mute: #9aa1b1;
}
.home-console { color: var(--text); }

/* 顶部 promo 栏 */
.topbar {
  display: flex; align-items: center; justify-content: flex-end;
  gap: 12px; flex-wrap: wrap; padding: 14px 4px 0;
}
.new-tag {
  display: inline-flex; align-items: center;
  padding: 3px 10px; border-radius: 20px;
  background: linear-gradient(90deg, #ffd6f0, #d6e0ff);
  color: var(--primary); font-size: 11px; font-weight: 700; letter-spacing: .05em;
}
.biz-btn {
  padding: 6px 14px; border-radius: 8px;
  background: #fff7e6; color: var(--amber);
  border: 1px solid #ffe1b3; font-size: 12px; font-weight: 600; cursor: pointer;
}
.fb-btn {
  padding: 6px 14px; border-radius: 8px;
  background: #ffeaf3; color: #d63384;
  border: 1px solid #ffc9de; font-size: 12px; font-weight: 600; cursor: pointer;
}
.fb-btn:hover { background: #ffd9ea; }
.top-banner {
  display: flex; align-items: center; gap: 8px;
  background: #fff7e6; color: #8a5500;
  padding: 6px 12px; border-radius: 8px; font-size: 12px;
}
.top-banner.pink { background: #ffeaf3; color: #b1326e; }
.top-banner .x { color: var(--text-mute); cursor: pointer; margin-left: 4px; }

/* Hero 舞台 */
.stage {
  position: relative; padding: 230px 36px 28px; margin: 8px 0 0;
  border-radius: 18px;
  background: #fff;
}
.stage-inner { position: relative; z-index: 1; max-width: 880px; margin: 0 auto; }

/* 搜索卡 */
.search-card { max-width: 760px; margin: 0 auto; background: #fff; border-radius: 18px; box-shadow: 0 1px 2px rgba(20,24,40,.04), 0 4px 14px rgba(20,24,40,.04); padding: 16px 18px; }

/* 已选技能 chip（从 SkillCenter 跳转过来时显示在 textarea 上方） */
.selected-skill-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 6px 6px 10px;
  margin-bottom: 10px;
  background: linear-gradient(90deg, #f0f5ff 0%, #f5f0ff 100%);
  border: 1px solid #e4e5e7;
  border-radius: 999px;
  font-size: 13px;
  color: #1f2329;
  user-select: none;
}
.ss-icon {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  flex-shrink: 0;
}
.ss-name {
  font-weight: 500;
}
.ss-close {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: none;
  background: #fff;
  color: #8f959e;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background 0.15s, color 0.15s;
}
.ss-close:hover {
  background: #1f2329;
  color: #fff;
}
.search-input { width: 100%; padding: 10px 4px; font-size: 14px; border: 0; outline: none; background: transparent; color: var(--text); resize: none; font-family: inherit; line-height: 1.5; }
.search-input::placeholder { color: var(--text-mute); }
.search-foot { display: flex; align-items: center; gap: 8px; margin-top: 8px; padding-top: 10px; border-top: 1px dashed #e3e3ee; flex-wrap: wrap; }
/* 实时语言识别徽标 */
.lang-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 999px;
  background: #eef0f6;
  border: 1px solid #e3e3ee;
  color: #5b6373;
  font-size: 12px;
  line-height: 1.4;
  white-space: nowrap;
  user-select: none;
  animation: langPop .16s ease-out;
}
@keyframes langPop {
  from { opacity: 0; transform: translateY(-3px); }
  to { opacity: 1; transform: none; }
}
.lang-badge .lb-ico { font-size: 12px; }
.chip { display: inline-flex; align-items: center; gap: 5px; padding: 5px 10px; border-radius: 18px; background: var(--primary-soft); color: var(--primary); font-size: 12px; cursor: pointer; font-weight: 500; transition: all .15s; }
.chip:hover { background: #e1deff; }
.chip.chip-on { background: var(--primary); color: #fff; }
.chip.depth-chip { position: relative; }
.chip .caret { font-size: 9px; margin-left: 2px; opacity: .85; }
.depth-menu {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  background: #fff;
  border: 1px solid #e4e5e7;
  border-radius: 10px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
  padding: 4px;
  min-width: 130px;
  z-index: 20;
}
.depth-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  color: #1f2329;
  transition: background 0.15s;
}
.depth-item:hover { background: #f5f6f8; }
.depth-item.active { background: #f0f5ff; color: #1e6df2; font-weight: 500; }
.di-icon { width: 18px; text-align: center; }
.di-icon.bolt { color: #f59e0b; }

/* 「+」添加菜单 */
.add-wrap { position: relative; display: inline-flex; }
.add-menu {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  background: #fff;
  border: 1px solid #e4e5e7;
  border-radius: 10px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
  padding: 4px;
  min-width: 170px;
  z-index: 20;
}
.add-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  color: #1f2329;
  transition: background 0.15s;
}
.add-item:hover { background: #f5f6f8; }
.ai-icon { width: 18px; text-align: center; }

/* 素材库弹窗 */
.mat-modal { display: flex; flex-direction: column; gap: 12px; }
.mat-toolbar { display: flex; gap: 10px; }
.mat-search { flex: 1; padding: 8px 12px; border: 1px solid var(--border); border-radius: 8px; font-size: 13px; }
.mat-search:focus { outline: none; border-color: var(--primary); }
.mat-upload { padding: 8px 16px; border: none; border-radius: 8px; background: var(--primary); color: #fff; font-size: 13px; cursor: pointer; }
.mat-upload:disabled { opacity: .6; cursor: default; }
.mat-state { padding: 32px 0; text-align: center; color: var(--text-mute); font-size: 13px; }
.mat-list { display: flex; flex-direction: column; gap: 6px; max-height: 420px; overflow-y: auto; }
.mat-item { display: flex; align-items: center; gap: 12px; padding: 10px 12px; border: 1px solid var(--border); border-radius: 10px; cursor: pointer; transition: all .15s; }
.mat-item:hover { border-color: var(--primary); background: var(--primary-soft); }
.mat-ico { width: 38px; height: 38px; border-radius: 8px; background: #f3f4f8; display: grid; place-items: center; font-size: 18px; flex-shrink: 0; }
.mat-info { flex: 1; min-width: 0; }
.mat-name { font-size: 14px; color: var(--text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.mat-sub { font-size: 12px; color: var(--text-mute); margin-top: 2px; }
.mat-add { font-size: 12px; color: var(--primary); font-weight: 600; flex-shrink: 0; }
.mat-more { font-size: 12px; color: var(--text-mute); text-align: center; padding-top: 4px; }
.chip.pink { background: var(--pink-soft); color: var(--pink); }
.chip.mint { background: var(--mint-soft); color: var(--mint); }
.chip.amber { background: var(--amber-soft); color: var(--amber); }
.search-foot .right { margin-left: auto; display: flex; gap: 6px; }
.search-foot .right .ib { width: 30px; height: 30px; border-radius: 8px; display: grid; place-items: center; cursor: pointer; color: var(--text-mute); }
.search-foot .ib.add-btn { background: #fff; border: 1px solid var(--border); color: #1f2329; font-size: 18px; font-weight: 500; line-height: 1; transition: all .15s; }
.search-foot .ib.add-btn:hover { border-color: var(--primary); color: var(--primary); background: var(--primary-soft); }
.search-foot .right .ib:hover { background: var(--bg); color: var(--text); }
.search-foot .right .ib.active { background: var(--primary); color: #fff; }

/* 快捷入口 */
.qrow { display: none; }


/* 快捷分类入口 */
.entry-row { display: flex; gap: 10px; flex-wrap: wrap; max-width: 880px; margin: 14px auto 0; }
.entry-chip {
  padding: 8px 16px;
  border-radius: 999px;
  background: var(--surface);
  border: 1px solid var(--border);
  font-size: 13px;
  color: var(--text-soft);
  cursor: pointer;
  transition: all .15s;
  user-select: none;
}
.entry-chip:hover { border-color: var(--primary); color: var(--primary); }
.entry-chip.on { background: var(--primary); color: #fff; border-color: var(--primary); }

/* 分类详情面板 */
.entry-panel {
  max-width: 880px;
  margin: 12px auto 0;
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 14px 16px;
  box-shadow: 0 4px 16px rgba(20, 24, 40, .06);
  animation: slideDown .18s ease-out;
}
@keyframes slideDown {
  from { opacity: 0; transform: translateY(-6px); }
  to { opacity: 1; transform: none; }
}
.ep-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.ep-title { font-size: 14px; font-weight: 600; color: var(--text); }
.ep-close {
  background: transparent;
  border: 0;
  font-size: 18px;
  color: var(--text-mute);
  cursor: pointer;
  line-height: 1;
  padding: 0 4px;
}
.ep-close:hover { color: var(--text); }
.ep-state { padding: 24px 0; text-align: center; color: var(--text-mute); font-size: 13px; }
.ep-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.ep-card {
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: 10px;
  cursor: pointer;
  transition: transform .15s, border-color .15s;
}
.ep-card:hover { transform: translateY(-2px); border-color: var(--primary); }
.ep-thumb {
  height: 90px;
  border-radius: 8px;
  background: var(--primary-soft);
  display: grid;
  place-items: center;
  font-size: 36px;
}
.ep-name {
  font-size: 13px;
  font-weight: 600;
  margin-top: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.ep-author { font-size: 11px; color: var(--text-mute); margin-top: 2px; }

/* 筛选栏 */
.filter-bar { display: flex; align-items: center; gap: 10px; padding: 23px 4px 6px; flex-wrap: wrap; }
.filter-tabs { display: flex; gap: 4px; flex-wrap: wrap; }
.ftab { padding: 6px 12px; border-radius: 8px; font-size: 13px; color: var(--text-soft); cursor: pointer; }
.ftab.on { background: var(--text); color: #fff; font-weight: 600; }
.ftab:hover:not(.on) { background: var(--bg); }
.filter-right { margin-left: auto; display: flex; align-items: center; gap: 10px; }
.mini-search { position: relative; width: 200px; }
.mini-search input { width: 100%; padding: 7px 12px 7px 30px; border: 1px solid var(--border); border-radius: 9px; font-size: 12px; background: #fff; outline: none; }
.mini-search .si { position: absolute; left: 10px; top: 50%; transform: translateY(-50%); color: var(--text-mute); font-size: 12px; }
.go-btn { padding: 7px 14px; border-radius: 9px; background: var(--text); color: #fff; font-size: 12px; font-weight: 600; cursor: pointer; border: 0; }
.go-btn:hover { background: #000; }
.pager { padding: 10px 0 24px; }
.pager-info { text-align: center; font-size: 13px; color: var(--text-soft); margin-bottom: 10px; }
.pager-actions { display: flex; }
.pg-half { flex: 1; padding: 10px 16px; border-radius: 999px; border: none; font-size: 14px; font-weight: 500; cursor: pointer; transition: background .2s; white-space: nowrap; }
/* 上一页：黑色，1:1 各占一半 */
.pg-half.prev { background: #1f2329; color: #fff; }
.pg-half.prev:hover:not(:disabled) { background: #333a45; }
/* 下一页：红色 */
.pg-half.next { background: #e11d48; color: #fff; margin-left: 10px; }
.pg-half.next:hover:not(:disabled) { background: #f43f5e; }
.pg-half:disabled { background: #f2f3f5 !important; color: #b0b6bf; cursor: default; }

/* 卡片网格 */
.grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 18px; padding: 14px 4px 24px; }
.card { background: #fff; border: 1px solid var(--border); border-radius: 14px; overflow: hidden; box-shadow: 0 1px 2px rgba(20,24,40,.04); transition: transform .2s, box-shadow .2s; cursor: pointer; }
.card:hover { transform: translateY(-3px); box-shadow: 0 8px 24px rgba(20,24,40,.08); }
.card .thumb { height: 130px; display: grid; place-items: center; position: relative; }
.card .thumb .emoji { font-size: 52px; filter: drop-shadow(0 4px 8px rgba(0,0,0,.08)); }
.card .body-2 { padding: 10px 12px 12px; }
.card .ttl { font-size: 13px; font-weight: 600; display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical; overflow: hidden; }
.card .meta { display: flex; align-items: center; gap: 6px; margin-top: 8px; font-size: 11px; color: var(--text-mute); }
.card .av { width: 18px; height: 18px; border-radius: 50%; display: grid; place-items: center; color: #fff; font-size: 9px; font-weight: 700; }
.card .stat { margin-left: auto; display: flex; gap: 8px; }

/* 分页 */

/* AI 搜灵感结果条 */
.inspire-bar { display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 8px 14px; margin: 4px 4px 0; background: var(--primary-soft); border-radius: 10px; font-size: 13px; color: var(--text-soft); }

/* 分类示例（试试这些） */
.examples-row {
  max-width: 1240px;
  margin: 16px 4px 0;
  padding: 0 4px;
}
.examples-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 10px;
}
.examples-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}
.examples-sub {
  font-size: 12px;
  color: var(--text-mute);
}
.examples-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 10px;
}
.example-card {
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 12px 14px;
  cursor: pointer;
  transition: border-color .18s, transform .18s, box-shadow .18s;
}
.example-card:hover {
  border-color: var(--primary);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(108, 92, 255, 0.12);
}
.example-card:hover .ex-title { color: var(--primary); }
.ex-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 6px;
  transition: color .18s;
}
.ex-prompt {
  font-size: 12px;
  color: var(--text-mute);
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.inspire-bar b { color: var(--primary); }

/* 网格空状态 */
.grid-empty { grid-column: 1 / -1; display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 56px 0; color: var(--text-mute); text-align: center; }
.grid-empty .ge-emoji { font-size: 42px; }
.grid-empty .ge-sub { font-size: 12px; color: var(--text-mute); }
.grid-empty .go-btn { margin-top: 8px; }

/* 技能弹窗列表 */
.sk-tip { padding: 24px; text-align: center; color: var(--text-mute); }
.sk-list { max-height: 420px; overflow-y: auto; }
.sk-item { display: flex; align-items: center; gap: 12px; padding: 10px 8px; border-radius: 10px; cursor: pointer; }
.sk-item:hover { background: var(--bg); }
.sk-icon { width: 36px; height: 36px; border-radius: 8px; display: grid; place-items: center; font-size: 20px; background: var(--primary-soft); flex-shrink: 0; }
.sk-info { min-width: 0; }
.sk-name { font-size: 13px; font-weight: 600; }
.sk-desc { font-size: 12px; color: var(--text-mute); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* 缩略图渐变 g1-g20 */
.thumb.g1 { background: linear-gradient(135deg, #f3e6ff, #ffe2ee); }
.thumb.g2 { background: linear-gradient(135deg, #e6f0ff, #d6e9ff); }
.thumb.g3 { background: linear-gradient(135deg, #fff0d6, #ffe6c2); }
.thumb.g4 { background: linear-gradient(135deg, #d6f7ef, #c2eeff); }
.thumb.g5 { background: linear-gradient(135deg, #ece6ff, #d6f0ff); }
.thumb.g6 { background: linear-gradient(135deg, #ffd6e8, #c2e0ff); }
.thumb.g7 { background: linear-gradient(135deg, #2d2d3d, #4a4a60); }
.thumb.g8 { background: linear-gradient(135deg, #ffd6b3, #ffb3d9); }
.thumb.g9 { background: linear-gradient(135deg, #ffd0d0, #ffb8b8); }
.thumb.g10 { background: linear-gradient(135deg, #ffe8b3, #ffd699); }
.thumb.g11 { background: linear-gradient(135deg, #d6f0d6, #c2e6c2); }
.thumb.g12 { background: linear-gradient(135deg, #c8f0c8, #a8e0c8); }
.thumb.g13 { background: linear-gradient(135deg, #c2f0ff, #a8e0e8); }
.thumb.g14 { background: linear-gradient(135deg, #d6e6ff, #b8d4ff); }
.thumb.g15 { background: linear-gradient(135deg, #ffe2ee, #ffd0e0); }
.thumb.g16 { background: linear-gradient(135deg, #ffcce0, #ffb3d9); }
.thumb.g17 { background: linear-gradient(135deg, #2d3a5c, #4a5a7c); }
.thumb.g18 { background: linear-gradient(135deg, #e6d6ff, #d6c2ff); }
.thumb.g19 { background: linear-gradient(135deg, #c8e8d8, #a8d8c8); }
.thumb.g20 { background: linear-gradient(135deg, #c2d8ff, #a8c2e8); }

/* 头像色 a1-a20 */
.av.a1 { background: linear-gradient(135deg, #b8a3ff, #8b6cff); }
.av.a2 { background: linear-gradient(135deg, #ffd28a, #ff9a3d); }
.av.a3 { background: linear-gradient(135deg, #ff9eb3, #ff6b8a); }
.av.a4 { background: linear-gradient(135deg, #6bd9b8, #2db893); }
.av.a5 { background: linear-gradient(135deg, #6bb6ff, #2d7cd9); }
.av.a6 { background: linear-gradient(135deg, #c2b3ff, #8b6cff); }
.av.a7 { background: linear-gradient(135deg, #4a4a60, #2d2d3d); }
.av.a8 { background: linear-gradient(135deg, #ffb38a, #ff7a3d); }
.av.a9 { background: linear-gradient(135deg, #ff8a8a, #e94545); }
.av.a10 { background: linear-gradient(135deg, #ffd28a, #ffaa3d); }
.av.a11 { background: linear-gradient(135deg, #8adb8a, #4ab84a); }
.av.a12 { background: linear-gradient(135deg, #a8e0a8, #6bc26b); }
.av.a13 { background: linear-gradient(135deg, #6bb6d9, #2d7ca8); }
.av.a14 { background: linear-gradient(135deg, #8ab8ff, #4a7cd9); }
.av.a15 { background: linear-gradient(135deg, #ff9eb3, #e66b8a); }
.av.a16 { background: linear-gradient(135deg, #ff8ac2, #e64a8a); }
.av.a17 { background: linear-gradient(135deg, #6b7c9c, #3d4a6c); }
.av.a18 { background: linear-gradient(135deg, #b8a3ff, #8b5cff); }
.av.a19 { background: linear-gradient(135deg, #8ac8a8, #4aa87a); }
.av.a20 { background: linear-gradient(135deg, #8ab0d9, #4a7ca8); }

@media (max-width: 1180px) { .grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 820px) { .grid { grid-template-columns: repeat(2, 1fr); } }
</style>

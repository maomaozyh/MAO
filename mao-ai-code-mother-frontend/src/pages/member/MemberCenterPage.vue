<script setup lang="ts">
import { ref, computed, onUnmounted, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { createOrder, mockPay, getOrder } from '@/api/payment'
import { listMyPointsByPage } from '@/api/pointsController'
import { useLoginUserStore } from '@/stores/loginUser'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// ===== 登录态 / 权益（积分余额、会员等级、到期时间）=====
const TIER_LABELS: Record<string, string> = {
  FREE: '免费版',
  PROFESSIONAL: '专业版',
  FLAGSHIP: '旗舰版',
  ENTERPRISE_STANDARD: '企业标准版',
  ENTERPRISE_SEAT: '企业成员席位',
}

const membershipTierLabel = computed(() => {
  const t = loginUserStore.loginUser?.membershipTier
  return TIER_LABELS[t as string] || '免费版'
})

function formatExpire(dt?: string): string {
  if (!dt) return ''
  const d = new Date(dt)
  if (isNaN(d.getTime())) return ''
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}/${pad(d.getMonth() + 1)}/${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const secondsBalanceText = computed(() => {
  const purchased = Number(loginUserStore.loginUser?.secondsBalance ?? 0)
  const gift = Number(loginUserStore.loginUser?.giftSecondsBalance ?? 0)
  return purchased + gift
})
const membershipExpireText = computed(() => formatExpire(loginUserStore.loginUser?.membershipExpireTime))

// 进入会员中心时拉登录态：access.ts 已在首次导航全局拉过，
// 消费/支付后由 refreshBalance / 支付回调刷新，这里仅在缺失时兜底，避免重复请求
onMounted(() => {
  if (!loginUserStore.loginUser?.id) {
    loginUserStore.fetchLoginUser()
  }
})

// ===== 支付宝扫码支付弹窗 =====
const showQr = ref(false)
const qrCode = ref('')
const qrProductName = ref('')
let pollTimer: number | null = null

function startPolling(orderId: string | number) {
  stopPolling()
  pollTimer = window.setInterval(async () => {
    try {
      const res = await getOrder(orderId)
      if (res.code === 0 && res.data && res.data.status === 'PAID') {
        stopPolling()
        showQr.value = false
        message.success('支付成功：' + qrProductName.value)
        // 支付成功后同步最新积分余额 / 会员信息
        loginUserStore.fetchLoginUser()
      }
    } catch {
      // 轮询失败忽略，继续下一轮
    }
  }, 2500)
}

function stopPolling() {
  if (pollTimer !== null) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

function closeQr() {
  stopPolling()
  showQr.value = false
}

onUnmounted(stopPolling)

async function pay(productType: string, productCode: string, quantity = 1) {
  try {
    const res = await createOrder({ productType, productCode, quantity })
    if (res.code !== 0 || !res.data) {
      message.error(res.message || '下单失败')
      return
    }
    const order = res.data
    // 真实渠道（如支付宝扫码支付）：弹出二维码，轮询支付结果
    if (order.qrCode) {
      qrCode.value = order.qrCode
      qrProductName.value = order.productName || '秒哒订单'
      showQr.value = true
      startPolling(order.id)
      return
    }
    // 真实渠道（跳转式收银台）：直接跳转
    if (order.payUrl) {
      window.location.href = order.payUrl
      return
    }
    // 沙箱渠道：直接模拟支付
    const payRes = await mockPay({ id: order.id })
    if (payRes.code !== 0) {
      message.error(payRes.message || '支付失败')
      return
    }
    message.success('支付成功：' + order.productName)
    // 支付成功后同步最新积分余额 / 会员信息
    loginUserStore.fetchLoginUser()
  } catch (e: any) {
    message.error(e?.message || '支付失败')
  }
}

const subTab = ref('升级会员')
const subTabs = ['升级会员', '增购积分']

const showBuyPoints = computed(() => subTab.value === '增购积分')

const plans = [
  {
    name: '免费版',
    code: undefined as string | undefined,
    basePrice: 0,
    tag: '',
    price: '¥0',
    old: '',
    extra: '',
    note: '',
    highlight: false,
    progress: 12,
    badge: '',
  },
  {
    name: '专业版',
    code: 'PROFESSIONAL',
    basePrice: 14.9,
    tag: '首购立享8折',
    price: '¥11.92',
    old: '¥14.90',
    extra: '',
    note: '到期自动续费，可随时取消',
    highlight: false,
    progress: 28,
    badge: '',
  },
  {
    name: '旗舰版',
    code: 'FLAGSHIP',
    basePrice: 65,
    tag: '首购立享8折',
    price: '¥52.00',
    old: '¥65.00',
    extra: '',
    note: '到期自动续费，可随时取消',
    highlight: true,
    progress: 52,
    badge: '超高性价比',
  },
]

// 首购 8 折：未付费购买过会员的用户，会员价格打 8 折并展示原价划线；买过之后恢复原价
const FIRST_MEMBERSHIP_DISCOUNT = 0.8
const displayPlans = computed(() => {
  const firstDiscount = !loginUserStore.loginUser?.hasPaidMembership
  if (firstDiscount) {
    return plans.map((p) => {
      if (!p.code) return p
      const discounted = Number((p.basePrice * FIRST_MEMBERSHIP_DISCOUNT).toFixed(2))
      return { ...p, price: '¥' + discounted.toFixed(2), old: '¥' + p.basePrice.toFixed(2), extra: '' }
    })
  }
  return plans.map((p) => {
    if (!p.code) return p
    return { ...p, price: '¥' + p.basePrice.toFixed(2), old: '', extra: '', tag: '连续包月' }
  })
})

// ===== 增购积分 =====
const commonPlans = [
  { points: 1000, price: 22, origin: 40, code: 'SEC_1000' },
  { points: 2000, price: 44, origin: 80, code: 'SEC_2000' },
  { points: 4000, price: 88, origin: 160, code: 'SEC_4000' },
  { points: 8000, price: 176, origin: 320, code: 'SEC_8000' },
]
const valuePlans = [
  { points: 16000, price: 320, origin: 640, base: 14545, bonus: 1455, code: 'SEC_16000' },
  { points: 28000, price: 560, origin: 1120, base: 25455, bonus: 2545, code: 'SEC_28000' },
  { points: 48000, price: 960, origin: 1920, base: 43636, bonus: 4364, code: 'SEC_48000' },
  { points: 72000, price: 1440, origin: 2880, base: 65455, bonus: 6545, code: 'SEC_72000' },
  { points: 100000, price: 2000, origin: 4000, base: 90909, bonus: 9091, code: 'SEC_100000' },
  { points: 124500, price: 2490, origin: 4980, base: 113182, bonus: 11318, code: 'SEC_124500' },
  { points: 190000, price: 3800, origin: 7600, base: 172727, bonus: 17273, code: 'SEC_190000' },
  { points: 260000, price: 5200, origin: 10400, base: 236364, bonus: 23636, code: 'SEC_260000' },
]

// 增购积分首购 8 折：未付费购买过积分的用户，积分价格打 8 折并展示原价划线；买过之后恢复原价
const firstPointsDiscount = computed(() => !loginUserStore.loginUser?.hasPaidPoints)
const displayCommonPlans = computed(() => {
  if (!firstPointsDiscount.value) return commonPlans
  return commonPlans.map((p) => ({
    ...p,
    price: Number((p.price * FIRST_MEMBERSHIP_DISCOUNT).toFixed(2)),
    origin: p.price,
  }))
})
const displayValuePlans = computed(() => {
  if (!firstPointsDiscount.value) return valuePlans
  return valuePlans.map((p) => ({
    ...p,
    price: Number((p.price * FIRST_MEMBERSHIP_DISCOUNT).toFixed(2)),
    origin: p.price,
  }))
})

const selectedPoints = ref<number | null>(null)
function fmt(num: number) {
  return num.toLocaleString('en-US')
}
function onBuyPoints(plan: { points: number; price: number; code: string }) {
  pay('SECONDS', plan.code)
}

function onBuy(plan: { code?: string }) {
  if (!plan.code) {
    message.info('当前为免费版，无需购买')
    return
  }
  pay('MEMBERSHIP', plan.code)
}
function goBack() {
  router.push('/')
}

// ===== 积分详情 / 统计弹窗 =====
type PointsTab = 'all' | 'gain' | 'consume'
type PointsViewTab = 'detail' | 'statistics'
type PointsStatSubTab = 'app' | 'skill'

interface PointsRecord {
  id: string | number
  type: string
  time: string
  amount: number
  appName?: string
  skillName?: string
}

interface PointsStatSubItem {
  name: string
  count: number
  consumed: number
}

interface PointsStatGroup {
  type: string
  count: number // 涉及的应用 / 技能数量
  consumed: number // 累计消耗（绝对值）
  items: PointsStatSubItem[]
}

const pointsDetailVisible = ref(false)
const pointsDetailTab = ref<PointsTab>('all')
const pointsTabs = [
  { key: 'all' as const, label: '全部' },
  { key: 'gain' as const, label: '获取' },
  { key: 'consume' as const, label: '消耗' },
]
const pointsRecords = ref<PointsRecord[]>([])
const pointsLoaded = ref(false)
const pointsLoading = ref(false)
const pointsPageSize = ref(10)
const pointsPage = ref(1)

const filteredPoints = computed(() => {
  const list = pointsRecords.value
  if (pointsDetailTab.value === 'gain') return list.filter((r) => r.amount > 0)
  if (pointsDetailTab.value === 'consume') return list.filter((r) => r.amount < 0)
  return list
})

const pagedPoints = computed(() => {
  const start = (pointsPage.value - 1) * pointsPageSize.value
  return filteredPoints.value.slice(start, start + pointsPageSize.value)
})

const pointsTotalPages = computed(() =>
  Math.max(1, Math.ceil(filteredPoints.value.length / pointsPageSize.value)),
)

// ===== 积分统计（应用 / 技能维度） =====
const pointsViewTab = ref<PointsViewTab>('detail')
const pointsStatSubTab = ref<PointsStatSubTab>('app')
const pointsStatSubTabs = [
  { key: 'app' as const, label: '应用' },
  { key: 'skill' as const, label: '技能' },
]
// 记录被收起的分组（默认全部展开，便于直接查看数据）
const pointsStatCollapsed = ref<string[]>([])

const onViewTabChange = (t: PointsViewTab) => {
  pointsViewTab.value = t
  if (!pointsLoaded.value) {
    loadPoints()
  }
}

const onStatSubTabChange = (t: PointsStatSubTab) => {
  pointsStatSubTab.value = t
}

const statDimensionLabel = computed(() => (pointsStatSubTab.value === 'app' ? '应用' : '技能'))

const pointsStatGroups = computed<PointsStatGroup[]>(() => {
  const field: 'appName' | 'skillName' = pointsStatSubTab.value === 'app' ? 'appName' : 'skillName'
  // 仅统计带维度信息且为消耗的记录
  const list = pointsRecords.value.filter((r) => !!r[field] && r.amount < 0)
  const byType = new Map<string, PointsRecord[]>()
  for (const r of list) {
    if (!byType.has(r.type)) byType.set(r.type, [])
    byType.get(r.type)!.push(r)
  }
  const groups: PointsStatGroup[] = []
  for (const [type, recs] of byType.entries()) {
    const byName = new Map<string, PointsRecord[]>()
    for (const r of recs) {
      const n = r[field] as string
      if (!byName.has(n)) byName.set(n, [])
      byName.get(n)!.push(r)
    }
    const items: PointsStatSubItem[] = []
    let totalConsumed = 0
    for (const [name, nrecs] of byName.entries()) {
      const consumed = nrecs.reduce((s, x) => s + Math.abs(x.amount), 0)
      totalConsumed += consumed
      items.push({ name, count: nrecs.length, consumed })
    }
    items.sort((a, b) => b.consumed - a.consumed)
    groups.push({ type, count: byName.size, consumed: totalConsumed, items })
  }
  groups.sort((a, b) => b.consumed - a.consumed)
  return groups
})

const statSummary = computed(() => {
  const groups = pointsStatGroups.value
  const names = new Set<string>()
  let total = 0
  for (const g of groups) {
    for (const it of g.items) {
      names.add(it.name)
      total += it.consumed
    }
  }
  return { count: names.size, consumed: total }
})

const toggleStatGroup = (type: string) => {
  const idx = pointsStatCollapsed.value.indexOf(type)
  if (idx >= 0) pointsStatCollapsed.value.splice(idx, 1)
  else pointsStatCollapsed.value.push(type)
}
const isStatCollapsed = (type: string) => pointsStatCollapsed.value.includes(type)

const formatAmount = (n: number): string => (n > 0 ? `+${n}` : `${n}`)

const openPointsDetail = () => {
  pointsDetailVisible.value = true
  if (!pointsLoaded.value) {
    loadPoints()
  }
}

const loadPoints = async () => {
  pointsLoading.value = true
  try {
    const res = await listMyPointsByPage({ pageNum: 1, pageSize: 50 })
    if (res.data.code === 0 && Array.isArray(res.data.data?.records)) {
      pointsRecords.value = res.data.data.records.map((r) => ({
        id: r.id ?? '',
        type: r.bizTypeText || r.bizType || '积分变动',
        time: (r.createTime || '').replace('T', ' ').slice(0, 16),
        amount: Number(r.amount ?? 0),
        appName: r.appId ? `应用 ${r.appId}` : undefined,
      }))
      pointsLoaded.value = true
    } else {
      pointsRecords.value = getMockPoints()
    }
  } catch {
    // 接口不可用时回退到演示数据，保证弹窗不空白
    pointsRecords.value = getMockPoints()
  } finally {
    pointsLoading.value = false
  }
}

const onPointsTabChange = (t: PointsTab) => {
  pointsDetailTab.value = t
  pointsPage.value = 1
}

const exportPoints = () => {
  message.info('导出功能开发中')
}

const getMockPoints = (): PointsRecord[] => [
  { id: 1, type: '深度开发请求', time: '2026-08-30 19:45', amount: -40, appName: '智能客服助手' },
  { id: 2, type: '积分退回', time: '2026-08-30 19:41', amount: 40 },
  { id: 3, type: '深度开发请求', time: '2026-08-30 19:40', amount: -40, appName: '海报生成器' },
  { id: 4, type: '积分退回', time: '2026-08-30 19:40', amount: -40 },
  { id: 5, type: '深度开发请求', time: '2026-08-30 19:40', amount: -40, appName: '旅行行程规划师' },
  { id: 6, type: '登录赠送', time: '2026-08-30 19:40', amount: 100 },
  { id: 7, type: '积分退回', time: '2026-08-30 17:29', amount: 40 },
  { id: 8, type: '首页推荐词生成', time: '2026-08-29 07:29', amount: -30, appName: '海报生成器' },
  { id: 9, type: '指令优化', time: '2026-08-29 00:09', amount: -1, skillName: '提示词优化器' },
  { id: 10, type: '登录赠送', time: '2026-08-29 00:00', amount: 100 },
  { id: 11, type: '指令优化', time: '2026-08-28 22:13', amount: -1, skillName: '提示词优化器' },
  { id: 12, type: '指令优化', time: '2026-08-28 18:46', amount: -1, skillName: '提示词优化器' },
  { id: 13, type: '深度开发请求', time: '2026-08-28 15:02', amount: -40, appName: '智能客服助手' },
  { id: 14, type: '积分退回', time: '2026-08-28 14:58', amount: 40 },
  { id: 15, type: '登录赠送', time: '2026-08-28 00:00', amount: 100 },
]
</script>

<template>
  <div class="mc-page">
    <div class="mc-topbar">
      <span class="mc-title">会员中心</span>
      <button class="mc-back" @click="goBack">返回</button>
    </div>

    <div class="mc-body">
      <!-- 个人版 -->
      <div class="top-row-wrap">
          <div class="top-info-card">
            <div>{{ membershipTierLabel }}</div>
            <div class="muted">{{ membershipTierLabel === '免费版' ? '永久有效' : '有效期至 ' + membershipExpireText }}</div>
          </div>
          <div class="top-info-card" @click="openPointsDetail" style="cursor: pointer">
            <div class="row-between">
              <span>积分余额 <b class="purple">{{ secondsBalanceText }}✨</b></span>
              <span class="muted">详情 &gt;</span>
            </div>
            <div class="muted" style="margin-top: 6px" v-if="membershipTierLabel !== '免费版' && membershipExpireText">
              会员权益及积分有效期至 {{ membershipExpireText }}
            </div>
          </div>
        </div>

        <div class="sub-tab-wrap">
          <div
            v-for="t in subTabs"
            :key="t"
            class="sub-tab-item"
            :class="{ active: subTab === t }"
            @click="subTab = t"
          >
            {{ t }}
          </div>
        </div>

        <div class="plan-container" v-if="subTab === '升级会员'">
          <div
            v-for="plan in displayPlans"
            :key="plan.name"
            class="plan-card"
            :class="{ highlight: plan.highlight }"
          >
            <div v-if="plan.badge" class="badge-top">{{ plan.badge }}</div>
            <div class="plan-header">
              <div class="plan-name">{{ plan.name }}</div>
              <div v-if="plan.tag" class="tag-discount">{{ plan.tag }}</div>
            </div>
            <div class="price-row">
              <span class="price-main">{{ plan.price }}</span>
              <span v-if="plan.old" class="price-old">{{ plan.old }}</span>
              <span v-if="plan.extra">{{ plan.extra }}</span>
            </div>
            <div v-if="plan.note" class="muted" style="font-size: 11px">{{ plan.note }}</div>
            <button class="buy-btn" @click="onBuy(plan)">立即购买</button>
            <div class="progress-bar-wrap">
              <div class="progress-bg">
                <div class="progress-fill" :style="{ width: plan.progress + '%' }"></div>
              </div>
              <div class="progress-labels">
                <span>0</span><span>700</span><span>1700</span><span>6000</span>
              </div>
              <div class="muted" style="font-size: 12px; margin-top: 4px">每月最高获取积分</div>
            </div>
          </div>
        </div>

      <!-- 增购积分（个人版） -->
      <div v-if="showBuyPoints" class="buy-points">
        <div class="section-title">
          <span class="sparkle">✦</span>
          <span>常用推荐</span>
        </div>
        <div class="card-grid">
          <div
            v-for="plan in displayCommonPlans"
            :key="plan.points"
            class="recharge-card"
            :class="{ selected: selectedPoints === plan.points }"
            @click="selectedPoints = plan.points"
          >
            <div class="card-top">
              <div class="points-row">
                <span class="points-num">{{ fmt(plan.points) }}</span>
                <span class="points-unit">积分</span>
              </div>
            </div>
            <div class="card-bottom">
              <div class="price-row">
                <span v-if="firstPointsDiscount" class="discount-badge">首购8折</span>
                <span class="price-now">{{ plan.price }}</span>
                <span class="price-origin">{{ plan.origin }}</span>
              </div>
              <button class="buy-btn" @click.stop="onBuyPoints(plan)">立即购买</button>
            </div>
          </div>
        </div>

        <div class="section-title">
          <span class="sparkle">✦</span>
          <span>更具性价比</span>
        </div>
        <div class="card-grid">
          <div
            v-for="plan in displayValuePlans"
            :key="plan.points"
            class="recharge-card"
            :class="{ selected: selectedPoints === plan.points }"
            @click="selectedPoints = plan.points"
          >
            <div class="card-top">
              <div class="points-row">
                <span class="points-num">{{ fmt(plan.points) }}</span>
                <span class="points-unit">积分</span>
                <span class="bonus-tag">充值加赠</span>
              </div>
              <div class="bonus-detail">{{ fmt(plan.base) }} + <span class="gift-num">{{ fmt(plan.bonus) }}赠送</span></div>
            </div>
            <div class="card-bottom">
              <div class="price-row">
                <span v-if="firstPointsDiscount" class="discount-badge">首购8折</span>
                <span class="price-now">{{ plan.price }}</span>
                <span class="price-origin">{{ plan.origin }}</span>
              </div>
              <button class="buy-btn" @click.stop="onBuyPoints(plan)">立即购买</button>
            </div>
          </div>
        </div>

        <div class="page-bottom"></div>
      </div>
    </div>

    <!-- 积分详情弹窗 -->
    <a-modal
      v-model:open="pointsDetailVisible"
      :footer="null"
      :width="820"
      :title="null"
      :closable="false"
      class="points-modal"
      destroy-on-close
    >
      <div class="pm-header">
        <div class="pm-head-left">
          <div class="pm-head-tabs">
            <span
              class="pm-head-tab"
              :class="{ active: pointsViewTab === 'detail' }"
              @click="onViewTabChange('detail')"
            >积分详情</span>
            <span
              class="pm-head-tab"
              :class="{ active: pointsViewTab === 'statistics' }"
              @click="onViewTabChange('statistics')"
            >积分统计</span>
          </div>
          <div class="pm-head-desc">展示近一个月积分获取及消耗记录，技能调用消耗每小时更新一次</div>
        </div>
        <button class="pm-close" @click="pointsDetailVisible = false" aria-label="关闭">✕</button>
      </div>

      <div class="pm-toolbar" v-if="pointsViewTab === 'detail'">
        <div class="pm-tabs">
          <div
            v-for="t in pointsTabs"
            :key="t.key"
            class="pm-tab"
            :class="{ active: pointsDetailTab === t.key }"
            @click="onPointsTabChange(t.key)"
          >
            {{ t.label }}
          </div>
        </div>
        <button class="pm-export" @click="exportPoints" title="导出">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
            <polyline points="7 10 12 15 17 10"/>
            <line x1="12" y1="15" x2="12" y2="3"/>
          </svg>
          <span>下载量</span>
        </button>
      </div>

      <div class="pm-body" v-if="pointsViewTab === 'detail'">
        <table class="pm-table">
          <thead>
            <tr>
              <th class="col-type">操作类型</th>
              <th class="col-time">发生时间</th>
              <th class="col-amount">积分</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="pointsLoading">
              <td colspan="3" class="pm-empty">加载中…</td>
            </tr>
            <tr v-else-if="pagedPoints.length === 0">
              <td colspan="3" class="pm-empty">暂无记录</td>
            </tr>
            <tr v-for="r in pagedPoints" v-else :key="r.id">
              <td class="col-type">
                <span class="op-icon">✈</span>
                {{ r.type }}
              </td>
              <td class="col-time">{{ r.time }}</td>
              <td class="col-amount" :class="r.amount > 0 ? 'gain' : 'loss'">
                {{ formatAmount(r.amount) }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pm-pagination" v-if="pointsViewTab === 'detail'">
        <span class="pm-page-size">{{ pointsPageSize }}条/页</span>
        <button class="pm-page-btn" :disabled="pointsPage <= 1" @click="pointsPage--">‹</button>
        <span
          v-for="p in pointsTotalPages"
          :key="p"
          class="pm-page-num"
          :class="{ active: pointsPage === p }"
          @click="pointsPage = p"
        >
          {{ p }}
        </span>
        <button class="pm-page-btn" :disabled="pointsPage >= pointsTotalPages" @click="pointsPage++">›</button>
        <span class="pm-jump">
          <span>跳转至</span>
          <input
            type="number"
            min="1"
            :max="pointsTotalPages"
            class="pm-jump-input"
            :value="pointsPage"
            @change="(e) => {
              const v = parseInt((e.target as HTMLInputElement).value)
              if (!isNaN(v)) pointsPage = Math.min(Math.max(1, v), pointsTotalPages)
            }"
          />
          <button class="pm-go-btn" @click="() => {}">GO</button>
        </span>
      </div>

      <!-- 积分统计面板 -->
      <div class="pm-stat" v-if="pointsViewTab === 'statistics'">
        <div class="pm-stat-subtabs">
          <div
            v-for="t in pointsStatSubTabs"
            :key="t.key"
            class="pm-stat-subtab"
            :class="{ active: pointsStatSubTab === t.key }"
            @click="onStatSubTabChange(t.key)"
          >
            {{ t.label }}
          </div>
        </div>

        <div class="pm-stat-summary">
          共 {{ statSummary.count }} 个{{ statDimensionLabel }} · 累计消耗 {{ statSummary.consumed }} 积分
        </div>

        <div class="pm-stat-groups">
          <div v-if="pointsStatGroups.length === 0" class="pm-empty">暂无消耗记录</div>
          <div v-for="g in pointsStatGroups" :key="g.type" class="ps-group">
            <div class="ps-group-head" @click="toggleStatGroup(g.type)">
              <span class="ps-caret" :class="{ open: !isStatCollapsed(g.type) }">▸</span>
              <span class="ps-type">{{ g.type }}</span>
              <span class="ps-meta">{{ g.count }} 个{{ statDimensionLabel }} · 共消耗 {{ g.consumed }}</span>
            </div>
            <div class="ps-items" v-show="!isStatCollapsed(g.type)">
              <div v-for="it in g.items" :key="it.name" class="ps-item">
                <span class="ps-item-name">{{ it.name }}</span>
                <span class="ps-item-count">{{ it.count }} 次</span>
                <span class="ps-item-consumed">-{{ it.consumed }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </a-modal>

    <!-- 支付宝扫码支付弹窗 -->
    <a-modal
      v-model:open="showQr"
      title="支付宝扫码支付"
      :footer="null"
      width="360px"
      :mask-closable="false"
      @cancel="closeQr"
    >
      <div class="alipay-qr">
        <a-qrcode :value="qrCode" :size="220" />
        <p class="alipay-qr-tip">请使用支付宝「扫一扫」完成支付</p>
        <p class="alipay-qr-amount" v-if="qrProductName">商品：{{ qrProductName }}</p>
        <a-button block @click="closeQr">暂不支付</a-button>
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
.mc-page {
  background: #f7f8fc;
  border-radius: 12px;
  overflow: hidden;
  min-height: calc(100vh - 48px);
}
.mc-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: #fff;
  border-bottom: 1px solid #e9ebf1;
}
.mc-title {
  font-size: 18px;
  font-weight: 600;
}
.mc-back {
  padding: 6px 14px;
  border-radius: 6px;
  border: 1px solid #ddd;
  background: #fff;
  cursor: pointer;
}
.mc-back:hover {
  background: #f2f4f8;
}
.mc-body {
  padding: 24px 32px;
}
.tab-top-switch {
  display: flex;
  justify-content: center;
  gap: 4px;
  margin-bottom: 24px;
}
.tab-top-switch button {
  padding: 6px 16px;
  border-radius: 6px;
  border: 1px solid #ddd;
  background: #fff;
  cursor: pointer;
}
.tab-top-switch button.active {
  background: #eee;
}
.top-row-wrap {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 24px;
}
.top-info-card {
  background: #fff;
  padding: 16px;
  border-radius: 10px;
  border: 1px solid #ebecef;
  font-size: 14px;
}
.row-between {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.sub-tab-wrap {
  display: flex;
  justify-content: center;
  gap: 32px;
  margin-bottom: 32px;
}
.sub-tab-item {
  font-size: 16px;
  cursor: pointer;
  padding-bottom: 6px;
}
.sub-tab-item.active {
  border-bottom: 2px solid #222;
  font-weight: 600;
}
.plan-container {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}
.plan-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e8e9ef;
  padding: 24px;
  position: relative;
}
.plan-card.highlight {
  border: 1px solid #ffcc70;
  box-shadow: 0 4px 12px #ffe9bc55;
}
.badge-top {
  position: absolute;
  top: 12px;
  right: 12px;
  font-size: 12px;
  color: #ff9922;
}
.plan-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.plan-name {
  font-size: 17px;
  font-weight: 600;
}
.tag-discount {
  background: #e9d8ff;
  color: #8837d8;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 4px;
}
.price-row {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  margin: 14px 0;
}
.price-main {
  font-size: 32px;
  font-weight: bold;
}
.price-old {
  font-size: 14px;
  color: #999;
  text-decoration: line-through;
}
.buy-btn {
  width: 100%;
  background: linear-gradient(90deg, #7b42f5, #b148ff);
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 12px;
  font-size: 15px;
  cursor: pointer;
  margin: 12px 0 20px;
}
.progress-bar-wrap {
  margin: 10px 0 18px;
}
.progress-bg {
  height: 6px;
  background: #eef0f6;
  border-radius: 3px;
}
.progress-fill {
  height: 6px;
  background: #b88bff;
  border-radius: 3px;
}
.progress-labels {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}
.muted {
  color: #888;
}
.purple {
  color: #7b42f5;
}

/* 增购积分 */
.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 20px;
  font-weight: 600;
  color: #7b3ff2;
  margin: 28px 24px 18px;
}
.section-title .sparkle {
  font-size: 18px;
}
.card-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
  padding: 0 24px;
}
.recharge-card {
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}
.recharge-card:hover {
  border-color: #b37fff;
  box-shadow: 0 4px 16px rgba(123, 63, 242, 0.12);
  transform: translateY(-2px);
}
.recharge-card.selected {
  border-color: #7b3ff2;
  box-shadow: 0 0 0 2px rgba(123, 63, 242, 0.2);
}
.card-top {
  padding: 22px 20px 18px;
  position: relative;
  min-height: 120px;
}
.card-top::after {
  content: "";
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  width: 90px;
  height: 90px;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23e0d4f7' stroke-width='1.2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M12 2l2.4 5.2 5.6 0.8-4 4.1 0.9 5.8-4.9-2.6-4.9 2.6 0.9-5.8-4-4.1 5.6-0.8z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-size: contain;
  opacity: 0.6;
  pointer-events: none;
}
.card-top::before {
  content: "";
  position: absolute;
  right: 6px;
  top: 14px;
  width: 22px;
  height: 22px;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23e0d4f7' stroke-width='1.2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M12 3l1.5 3.5L17 8l-3.5 1.5L12 13l-1.5-3.5L7 8l3.5-1.5z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-size: contain;
  opacity: 0.7;
  pointer-events: none;
}
.points-row {
  display: flex;
  align-items: baseline;
  gap: 4px;
  position: relative;
  z-index: 1;
}
.points-num {
  font-size: 36px;
  font-weight: 700;
  color: #1f2329;
  line-height: 1;
}
.points-unit {
  font-size: 16px;
  color: #4e5969;
  font-weight: 500;
}
.bonus-tag {
  display: inline-block;
  background: linear-gradient(90deg, #7b3ff2, #a56bff);
  color: #fff;
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 5px;
  margin-left: 8px;
  font-weight: 500;
  vertical-align: middle;
}
.bonus-detail {
  font-size: 14px;
  color: #86909c;
  margin-top: 10px;
  position: relative;
  z-index: 1;
}
.bonus-detail .gift-num {
  color: #7b3ff2;
  font-weight: 600;
}
.card-bottom {
  background: #f7f8fc;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid #eef0f5;
}
.price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.price-now {
  font-size: 24px;
  font-weight: 700;
  color: #1f2329;
}
.price-now::before {
  content: "¥";
  font-size: 18px;
  margin-right: 1px;
}
.price-origin {
  font-size: 16px;
  color: #a9aeb8;
  text-decoration: line-through;
}
.price-origin::before {
  content: "¥";
  font-size: 13px;
}
.discount-badge {
  display: inline-block;
  background: linear-gradient(90deg, #ff7a45, #ff4d4f);
  color: #fff;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  margin-right: 6px;
  font-weight: 500;
  flex-shrink: 0;
}
.buy-btn {
  background: #f0e6ff;
  color: #7b3ff2;
  border: none;
  border-radius: 8px;
  padding: 8px 18px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}
.buy-btn:hover {
  background: #7b3ff2;
  color: #fff;
}
.page-bottom {
  height: 40px;
}
@media (max-width: 1200px) {
  .card-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
@media (max-width: 900px) {
  .card-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 560px) {
  .card-grid {
    grid-template-columns: 1fr;
  }
  .points-num {
    font-size: 28px;
  }
}

/* 选购卡券 */
.coupon-view {
  padding-bottom: 40px;
}
.step-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 32px;
}
.step-item {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f7f8fc;
  border-radius: 10px;
  padding: 12px 14px;
  font-size: 14px;
}
.step-sub {
  font-size: 12px;
  color: #777;
}
.step-divider {
  color: #c5c8d4;
}
.step-icon {
  width: 32px;
  height: 32px;
  background: #e8ebf8;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.close-x {
  font-size: 22px;
  cursor: pointer;
  padding: 4px 8px;
  user-select: none;
}
.grid-4 {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}
.vip-card {
  border: 1px solid #e6e8f1;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
}
.vip-card-header {
  height: 110px;
  background: linear-gradient(135deg, #b8c8ff, #d4b8ff);
  padding: 14px;
  position: relative;
}
.vip-card-header.blue {
  background: linear-gradient(135deg, #82b8ff, #4298ff);
}
.vip-name {
  font-size: 17px;
  font-weight: 600;
}
.vip-tag-row {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.vip-tag {
  background: rgba(255, 255, 255, 0.35);
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 4px;
}
.vip-card-body {
  padding: 16px 14px;
}
.price-line {
  margin-bottom: 12px;
}
.price-big {
  font-size: 24px;
  font-weight: bold;
}
.price-old {
  font-size: 14px;
  color: #aaa;
  text-decoration: line-through;
  margin-left: 6px;
}
.num-ctrl {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}
.num-ctrl button {
  width: 28px;
  height: 28px;
  border: 1px solid #dde0ec;
  background: #fff;
  cursor: pointer;
  font-size: 16px;
}
.num-ctrl input {
  width: 44px;
  height: 28px;
  border: 1px solid #dde0ec;
  border-left: none;
  border-right: none;
  text-align: center;
  outline: none;
}
.point-card {
  border: 1px solid #e6e8f1;
  border-radius: 12px;
  padding: 18px 16px;
  position: relative;
  overflow: hidden;
}
.star-mark {
  position: absolute;
  right: 10px;
  top: 12px;
  font-size: 70px;
  opacity: 0.07;
  color: #9c66ff;
  pointer-events: none;
}
.point-title {
  font-size: 28px;
  font-weight: bold;
}
.point-price-row {
  margin: 14px 0;
}
@media (max-width: 1300px) {
  .grid-4 {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 700px) {
  .grid-4 {
    grid-template-columns: 1fr;
  }
  .step-header {
    flex-wrap: wrap;
  }
}

/* 企业版 */
.ent-tab-header {
  display: flex;
  justify-content: center;
  gap: 32px;
  margin-bottom: 24px;
}
.ent-tab-item {
  font-size: 18px;
  cursor: pointer;
  padding: 6px 0;
  color: #666;
}
.ent-tab-item.active {
  color: #000;
  font-weight: 600;
  border-bottom: 2px solid #2159e8;
}
.page-title {
  text-align: center;
  font-size: 20px;
  margin-bottom: 36px;
}
.page-title strong {
  color: #2159e8;
}
.ent-upgrade {
  max-width: 1040px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}
.ent-card {
  border: 1px solid #e4e7f2;
  border-radius: 14px;
  padding: 28px;
}
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 8px;
}
.card-desc {
  font-size: 14px;
  color: #777;
  margin-bottom: 20px;
}
.price-tab-wrap {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}
.ent-price-tab {
  flex: 1;
  border: 1px solid #dde2f2;
  border-radius: 10px;
  padding: 16px;
  position: relative;
  cursor: pointer;
}
.ent-price-tab.active {
  border-color: #2159e8;
  box-shadow: 0 0 0 1px #2159e8;
}
.discount-tag {
  position: absolute;
  top: -10px;
  left: 12px;
  background: #2159e8;
  color: #fff;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
}
.price-label {
  font-size: 14px;
  color: #555;
  margin-bottom: 6px;
}
.price-num {
  font-size: 30px;
  font-weight: bold;
}
.price-tip {
  font-size: 13px;
  color: #777;
  margin-top: 4px;
}
.num-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}
.num-title {
  font-size: 15px;
}
.ent-buy {
  width: 100%;
  background: #2159e8;
  color: #fff;
  border: none;
  border-radius: 10px;
  height: 46px;
  font-size: 16px;
  cursor: pointer;
  margin-bottom: 20px;
}
.ent-buy:hover {
  background: #1a4fc9;
}
.point-badge {
  background: #f2f4fc;
  border-radius: 8px;
  padding: 12px 14px;
  font-size: 15px;
  margin-bottom: 20px;
}
.qr-box {
  text-align: center;
  margin: 24px 0;
}
.qr-img {
  width: 130px;
  height: 130px;
  background: #f7f8fc;
  margin: 0 auto 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
}
.qr-tip {
  font-size: 13px;
  color: #666;
}
.contact-btn {
  width: 100%;
  background: #f2f4fc;
  border: none;
  border-radius: 10px;
  height: 44px;
  color: #444;
  font-size: 15px;
  cursor: pointer;
  margin-bottom: 24px;
}
@media (max-width: 860px) {
  .ent-upgrade {
    grid-template-columns: 1fr;
  }
}

/* 积分详情弹窗 */
.points-modal :deep(.ant-modal-body) {
  padding: 0;
}

.pm-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 20px 24px 14px;
  border-bottom: 1px solid #f0f1f5;
  gap: 12px;
}

.pm-head-tabs {
  display: flex;
  gap: 20px;
  align-items: baseline;
  margin-bottom: 8px;
}

.pm-head-tab {
  font-size: 18px;
  color: #a0a4b0;
  cursor: pointer;
  padding-bottom: 4px;
  border-bottom: 2px solid transparent;
  transition: color 0.2s ease, border-color 0.2s ease;
  user-select: none;
}

.pm-head-tab:hover {
  color: #4a4f63;
}

.pm-head-tab.active {
  color: #1a1a1a;
  font-weight: 600;
  border-bottom-color: #6c3ce0;
}

.pm-head-desc {
  font-size: 12px;
  color: #8a8fa3;
  line-height: 1.6;
}

.pm-close {
  border: none;
  background: transparent;
  font-size: 22px;
  color: #888;
  cursor: pointer;
  line-height: 1;
  padding: 2px 6px;
  border-radius: 6px;
  flex-shrink: 0;
}

.pm-close:hover {
  background: #f2f4f8;
  color: #333;
}

.pm-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  border-bottom: 1px solid #f0f1f5;
}

.pm-tabs {
  display: flex;
  gap: 6px;
}

.pm-tab {
  padding: 5px 14px;
  border-radius: 8px;
  font-size: 13px;
  color: #4a4f63;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}

.pm-tab:hover {
  background: #f4f6fb;
}

.pm-tab.active {
  background: #1a1a1a;
  color: #fff;
  font-weight: 500;
}

.pm-export {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 12px;
  border: 1px solid #e8ebf5;
  background: #fff;
  border-radius: 8px;
  font-size: 12px;
  color: #4a4f63;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}

.pm-export:hover {
  background: #f4f6fb;
  color: #1a1a1a;
}

.pm-export svg {
  width: 13px;
  height: 13px;
}

.pm-body {
  padding: 4px 24px 8px;
  max-height: 50vh;
  overflow-y: auto;
}

.pm-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  color: #1a1a1a;
}

.pm-table thead th {
  text-align: left;
  padding: 12px 8px;
  background: #fafbfd;
  color: #6b7280;
  font-weight: 500;
  font-size: 12px;
  border-bottom: 1px solid #f0f1f5;
}

.pm-table tbody td {
  padding: 14px 8px;
  border-bottom: 1px solid #f5f6fa;
  vertical-align: middle;
}

.pm-table tbody tr:hover {
  background: #fafbfd;
}

.col-type {
  display: flex;
  align-items: center;
  gap: 8px;
}

.col-time {
  color: #4a4f63;
  white-space: nowrap;
}

.col-amount {
  text-align: right;
  font-weight: 600;
  white-space: nowrap;
}

.col-amount.gain {
  color: #16a34a;
}

.col-amount.loss {
  color: #dc2626;
}

.op-icon {
  display: inline-block;
  width: 18px;
  height: 18px;
  text-align: center;
  line-height: 18px;
  font-size: 11px;
  color: #6c3ce0;
  background: #f4f0ff;
  border-radius: 4px;
  flex-shrink: 0;
}

.pm-empty {
  text-align: center;
  padding: 40px 0;
  color: #888;
}

.pm-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px 24px 20px;
  border-top: 1px solid #f0f1f5;
  flex-wrap: wrap;
}

.pm-page-size {
  font-size: 12px;
  color: #8a8fa3;
  margin-right: 8px;
}

.pm-page-btn {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: 1px solid #e8ebf5;
  background: #fff;
  color: #4a4f63;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s ease;
}

.pm-page-btn:hover:not(:disabled) {
  background: #f4f6fb;
}

.pm-page-btn:disabled {
  color: #c9cdd4;
  cursor: not-allowed;
}

.pm-page-num {
  min-width: 28px;
  height: 28px;
  padding: 0 8px;
  border-radius: 6px;
  font-size: 13px;
  color: #4a4f63;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s ease, color 0.2s ease;
}

.pm-page-num:hover {
  background: #f4f6fb;
}

.pm-page-num.active {
  background: #1a1a1a;
  color: #fff;
  font-weight: 500;
}

.pm-jump {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #8a8fa3;
  margin-left: 8px;
}

.pm-jump-input {
  width: 44px;
  height: 28px;
  border: 1px solid #e8ebf5;
  border-radius: 6px;
  text-align: center;
  font-size: 13px;
  color: #1a1a1a;
  outline: none;
}

.pm-jump-input:focus {
  border-color: #6c3ce0;
}

.pm-go-btn {
  height: 28px;
  padding: 0 10px;
  border: 1px solid #e8ebf5;
  background: #fff;
  border-radius: 6px;
  font-size: 12px;
  color: #4a4f63;
  cursor: pointer;
}

.pm-go-btn:hover {
  background: #f4f6fb;
  color: #1a1a1a;
}

/* 积分统计面板 */
.pm-stat {
  padding: 8px 24px 16px;
  max-height: 56vh;
  overflow-y: auto;
}

.pm-stat-subtabs {
  display: flex;
  gap: 8px;
  padding: 6px 0 12px;
}

.pm-stat-subtab {
  padding: 6px 18px;
  border-radius: 999px;
  font-size: 13px;
  color: #4a4f63;
  background: #f4f6fb;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}

.pm-stat-subtab:hover {
  background: #ebeef6;
}

.pm-stat-subtab.active {
  background: #6c3ce0;
  color: #fff;
  font-weight: 500;
}

.pm-stat-summary {
  font-size: 13px;
  color: #6b7280;
  padding: 4px 0 14px;
  border-bottom: 1px dashed #eef0f5;
  margin-bottom: 8px;
}

.pm-stat-groups {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ps-group {
  border: 1px solid #f0f1f5;
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
}

.ps-group-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s ease;
}

.ps-group-head:hover {
  background: #fafbfd;
}

.ps-caret {
  display: inline-block;
  font-size: 12px;
  color: #9aa0b0;
  transition: transform 0.2s ease;
}

.ps-caret.open {
  transform: rotate(90deg);
}

.ps-type {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.ps-meta {
  margin-left: auto;
  font-size: 12px;
  color: #8a8fa3;
}

.ps-items {
  border-top: 1px solid #f5f6fa;
  background: #fcfcfe;
}

.ps-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px 10px 32px;
  font-size: 13px;
  border-bottom: 1px solid #f5f6fa;
}

.ps-item:last-child {
  border-bottom: none;
}

.ps-item-name {
  color: #1a1a1a;
}

.ps-item-count {
  color: #8a8fa3;
}

.ps-item-consumed {
  margin-left: auto;
  color: #dc2626;
  font-weight: 600;
}

@media (max-width: 700px) {
  .pm-stat {
    padding: 8px 18px 16px;
  }
  .pm-header {
    padding: 16px 18px 12px;
  }
  .pm-toolbar,
  .pm-body,
  .pm-pagination {
    padding-left: 18px;
    padding-right: 18px;
  }
  .pm-pagination {
    gap: 4px;
  }
}
.alipay-qr {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 8px 0 4px;
}
.alipay-qr-tip {
  margin: 0;
  color: #666;
  font-size: 13px;
}
.alipay-qr-amount {
  margin: 0;
  color: #1f2329;
  font-weight: 600;
}
</style>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser'
import { getUserVoById, sendBindEmailCode, bindEmail, updateMyPassword } from '@/api/userController'
import { listMyLikedCommunityPostByPage, listMyFootprintCommunityPostByPage } from '@/api/communityPostController'
import { message } from 'ant-design-vue'
import { IconShare3 } from '@tabler/icons-vue'
import { isValidEmail } from '@/utils/validators'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 当前正在查看的用户：有 :userId 参数时是他人的 profile，否则是自己的
const profileUser = ref<API.UserVO | null>(null)

// 关注 / 粉丝 / 获赞 —— 后端暂无接口，先占位 0
const stats = ref({ following: 0, followers: 0, likes: 0 })

const isSelf = computed(() => !route.params.userId)

const displayName = computed(() => {
  const u: any = profileUser.value || loginUserStore.loginUser
  if (u?.userName) return u.userName
  if (u?.userAccount) {
    const a: string = u.userAccount
    if (a.length <= 4) return a
    return a.slice(0, 3) + '****' + a.slice(-2)
  }
  return '未登录用户'
})

const avatarText = computed(() => {
  const u: any = profileUser.value || loginUserStore.loginUser
  return u?.userName?.[0] || u?.userAccount?.[0] || '1'
})

const bio = computed(() => {
  const u: any = profileUser.value || loginUserStore.loginUser
  return u?.userProfile?.trim() || '该用户还没有简介~'
})

const shareUrl = computed(() => {
  if (typeof window === 'undefined') return ''
  const u: any = profileUser.value || loginUserStore.loginUser
  const id = u?.id
  // 他人主页带 userId，自己主页不带
  return id ? `${window.location.origin}/user/profile/${id}` : `${window.location.origin}/user/profile`
})

const activeTab = ref<'works' | 'liked' | 'footprint'>('works')
const filterType = ref('全部')
const filterOpen = ref(false)
const filterOptions = ['全部', 'Web 应用', '小程序', '数据看板', '其他']

function shareHome() {
  const url = shareUrl.value
  if (!url) return
  try {
    if (navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(url).then(
        () => message.success('主页链接已复制到剪贴板'),
        () => message.warning('复制失败，请手动复制：' + url),
      )
    } else {
      message.warning('请手动复制：' + url)
    }
  } catch (e) {
    message.warning('请手动复制：' + url)
  }
}

function selectFilter(opt: string) {
  filterType.value = opt
  filterOpen.value = false
}

function switchTab(tab: 'works' | 'liked' | 'footprint') {
  activeTab.value = tab
  // 首次切到「赞过的」/「足迹」时加载数据
  if (tab === 'liked' && !likedLoaded.value) loadLikedPosts()
  if (tab === 'footprint' && !footprintLoaded.value) loadFootprintPosts()
}

// ===== 赞过的 / 足迹列表 =====
const likedList = ref<API.CommunityPostVO[]>([])
const likedLoading = ref(false)
const likedLoaded = ref(false)
const likedPage = ref(1)
const likedTotal = ref(0)
const likedLoadingMore = ref(false)

const footprintList = ref<API.CommunityPostVO[]>([])
const footprintLoading = ref(false)
const footprintLoaded = ref(false)
const footprintPage = ref(1)
const footprintTotal = ref(0)
const footprintLoadingMore = ref(false)

const PAGE_SIZE = 10

const loadLikedPosts = async (isLoadMore = false) => {
  if (likedLoading.value || likedLoadingMore.value) return
  if (isLoadMore) likedLoadingMore.value = true
  else likedLoading.value = true
  try {
    const nextPage = isLoadMore ? likedPage.value + 1 : 1
    const res = await listMyLikedCommunityPostByPage({ pageNum: nextPage, pageSize: PAGE_SIZE })
    const records = res.data.code === 0 && res.data.data ? res.data.data.records || [] : []
    likedList.value = isLoadMore ? [...likedList.value, ...records] : records
    likedTotal.value = res.data.data?.totalRow ?? records.length
    likedPage.value = nextPage
    likedLoaded.value = true
  } catch {
    /* 静默：接口异常时保持空态 */
  } finally {
    likedLoading.value = false
    likedLoadingMore.value = false
  }
}

const loadFootprintPosts = async (isLoadMore = false) => {
  if (footprintLoading.value || footprintLoadingMore.value) return
  if (isLoadMore) footprintLoadingMore.value = true
  else footprintLoading.value = true
  try {
    const nextPage = isLoadMore ? footprintPage.value + 1 : 1
    const res = await listMyFootprintCommunityPostByPage({ pageNum: nextPage, pageSize: PAGE_SIZE })
    const records = res.data.code === 0 && res.data.data ? res.data.data.records || [] : []
    footprintList.value = isLoadMore ? [...footprintList.value, ...records] : records
    footprintTotal.value = res.data.data?.totalRow ?? records.length
    footprintPage.value = nextPage
    footprintLoaded.value = true
  } catch {
    /* 静默 */
  } finally {
    footprintLoading.value = false
    footprintLoadingMore.value = false
  }
}

const goPostDetail = (id?: number) => {
  if (id == null) return
  router.push(`/community/post/${id}`)
}

// ===== 绑定邮箱（用于邮箱验证码找回密码）=====
const bindEmailVal = ref('')
const bindCode = ref('')
const bindSending = ref(false)
const bindSubmitting = ref(false)
const bindCountdown = ref(0)
let bindCountdownTimer: ReturnType<typeof setInterval> | null = null

const currentEmail = computed(() => loginUserStore.loginUser?.userEmail || '')
const maskedCurrentEmail = computed(() => {
  const e = currentEmail.value
  if (!e) return ''
  const at = e.indexOf('@')
  if (at <= 1) return e
  return e.charAt(0) + '***' + e.substring(at)
})

const startBindCountdown = (seconds: number) => {
  bindCountdown.value = seconds
  if (bindCountdownTimer) clearInterval(bindCountdownTimer)
  bindCountdownTimer = setInterval(() => {
    bindCountdown.value -= 1
    if (bindCountdown.value <= 0) {
      if (bindCountdownTimer) clearInterval(bindCountdownTimer)
      bindCountdownTimer = null
    }
  }, 1000)
}

const sendBindCode = async () => {
  const v = bindEmailVal.value.trim()
  if (!isValidEmail(v)) {
    message.warning('请输入正确的邮箱')
    return
  }
  bindSending.value = true
  try {
    const res = await sendBindEmailCode({ email: v })
    if (res.data.code === 0) {
      message.success('验证码已发送，请查收邮件')
      startBindCountdown(60)
    } else {
      message.error(res.data.message || '发送失败')
    }
  } catch {
    message.error('发送失败，请稍后再试')
  } finally {
    bindSending.value = false
  }
}

const submitBindEmail = async () => {
  const v = bindEmailVal.value.trim()
  if (!isValidEmail(v)) {
    message.warning('请输入正确的邮箱')
    return
  }
  if (!bindCode.value.trim()) {
    message.warning('请输入验证码')
    return
  }
  bindSubmitting.value = true
  try {
    const res = await bindEmail({ email: v, code: bindCode.value.trim() })
    if (res.data.code === 0) {
      message.success('邮箱绑定成功，已可用于找回密码')
      bindCode.value = ''
      // 刷新登录态，让页面显示出已绑定邮箱
      try {
        await loginUserStore.fetchLoginUser()
      } catch {
        /* 静默 */
      }
    } else {
      message.error(res.data.message || '绑定失败')
    }
  } catch {
    message.error('绑定失败，请稍后再试')
  } finally {
    bindSubmitting.value = false
  }
}

// ===== 修改密码（登录态，需校验原密码）=====
const oldPwd = ref('')
const newPwd = ref('')
const checkPwd = ref('')
const pwdSubmitting = ref(false)

const submitChangePwd = async () => {
  if (!oldPwd.value) {
    message.warning('请输入当前密码')
    return
  }
  if (newPwd.value.length < 8) {
    message.warning('新密码至少 8 位')
    return
  }
  if (newPwd.value !== checkPwd.value) {
    message.warning('两次输入的新密码不一致')
    return
  }
  if (newPwd.value === oldPwd.value) {
    message.warning('新密码不能与原密码相同')
    return
  }
  pwdSubmitting.value = true
  try {
    const res = await updateMyPassword({
      oldPassword: oldPwd.value,
      newPassword: newPwd.value,
      checkPassword: checkPwd.value,
    })
    if (res.data.code === 0) {
      message.success('密码修改成功，请使用新密码登录')
      oldPwd.value = ''
      newPwd.value = ''
      checkPwd.value = ''
    } else {
      message.error(res.data.message || '修改失败')
    }
  } catch {
    message.error('修改失败，请稍后再试')
  } finally {
    pwdSubmitting.value = false
  }
}

const formatTime = (t?: string) => {
  if (!t) return ''
  return t.replace('T', ' ').slice(0, 16)
}

function onDocClick(e: MouseEvent) {
  // 关闭 filter 下拉
  const t = e.target as HTMLElement
  if (!t.closest('.filter-wrap')) filterOpen.value = false
}

onMounted(async () => {
  document.addEventListener('click', onDocClick)
  const targetUserId = route.params.userId as string | undefined
  if (targetUserId) {
    // 他人主页：调 getUserVoById 拿该用户的 VO
    // 注意：雪花 ID 19 位，超 JS Number 安全整数，必须保持字符串，不能 Number() 转换
    try {
      const res = await getUserVoById({ id: targetUserId })
      if (res.data.code === 0 && res.data.data) {
        profileUser.value = res.data.data
      } else {
        message.error('用户不存在或已被删除')
        router.replace('/')
      }
    } catch {
      message.error('加载用户主页失败')
    }
  } else if (!loginUserStore.loginUser?.id) {
    // 自己的主页：未登录则尝试拉取一次
    try {
      await loginUserStore.fetchLoginUser()
    } catch {
      /* 静默，未登录时也允许浏览 */
    }
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClick)
  if (bindCountdownTimer) clearInterval(bindCountdownTimer)
})
</script>

<template>
  <div class="profile-page">
    <!-- 顶部用户信息 -->
    <div class="profile-header">
      <div class="profile-main">
        <div class="avatar-large">{{ avatarText }}</div>
        <div class="profile-info">
          <div class="profile-name">{{ displayName }}</div>
          <div class="profile-stats">
            <span><b>{{ stats.following }}</b>关注</span>
            <span class="sep">|</span>
            <span><b>{{ stats.followers }}</b>粉丝</span>
            <span class="sep">|</span>
            <span><b>{{ stats.likes }}</b>获赞</span>
          </div>
          <div class="profile-bio">{{ bio }}</div>
        </div>
        <button class="share-btn" @click="shareHome">
          <IconShare3 :size="16" />
          <span>分享主页</span>
        </button>
      </div>
    </div>

    <!-- 账号安全：仅本人可见（绑定邮箱用于找回密码） -->
    <div v-if="isSelf" class="security-card">
      <div class="security-title">账号安全</div>
      <div class="security-row">
        <div class="security-label">绑定邮箱</div>
        <div class="security-control">
          <template v-if="currentEmail">
            <span class="security-bound">已绑定：{{ maskedCurrentEmail }}</span>
          </template>
          <div class="security-inputs">
            <input v-model="bindEmailVal" class="security-input" placeholder="请输入邮箱（如 123@qq.com）" />
            <div class="security-code-row">
              <input v-model="bindCode" class="security-code-input" placeholder="请输入验证码" maxlength="6" />
              <button class="security-code-btn" :disabled="bindCountdown > 0 || bindSending" @click="sendBindCode">
                {{ bindCountdown > 0 ? bindCountdown + 's 后重发' : bindSending ? '发送中…' : '获取验证码' }}
              </button>
            </div>
            <button class="security-bind-btn" :disabled="bindSubmitting" @click="submitBindEmail">
              {{ bindSubmitting ? '绑定中…' : (currentEmail ? '更换绑定' : '绑定邮箱') }}
            </button>
          </div>
          <div class="security-tip">绑定后可通过「登录页 → 忘记密码 → 邮箱验证码」找回密码</div>
        </div>
      </div>

      <div class="security-row">
        <div class="security-label">修改密码</div>
        <div class="security-control">
          <div class="security-inputs">
            <input v-model="oldPwd" type="password" class="security-input" placeholder="请输入当前密码" />
            <input v-model="newPwd" type="password" class="security-input" placeholder="请输入新密码（至少 8 位）" />
            <input v-model="checkPwd" type="password" class="security-input" placeholder="请再次输入新密码" />
            <button class="security-bind-btn" :disabled="pwdSubmitting" @click="submitChangePwd">
              {{ pwdSubmitting ? '修改中…' : '修改密码' }}
            </button>
          </div>
          <div class="security-tip">修改密码后，其他设备的登录会话将失效</div>
        </div>
      </div>
    </div>

    <!-- 标签栏 -->
    <div class="tab-bar">
      <div class="tabs">
        <div
          class="tab"
          :class="{ active: activeTab === 'works' }"
          @click="switchTab('works')"
        >上架作品</div>
        <div
          class="tab"
          :class="{ active: activeTab === 'liked' }"
          @click="switchTab('liked')"
        >赞过的</div>
        <div
          class="tab"
          :class="{ active: activeTab === 'footprint' }"
          @click="switchTab('footprint')"
        >足迹</div>
      </div>
      <div class="filter-wrap">
        <button class="filter-btn" @click.stop="filterOpen = !filterOpen">
          <span>{{ filterType }}</span>
          <span class="caret">▾</span>
        </button>
        <div class="filter-menu" v-if="filterOpen">
          <div
            v-for="opt in filterOptions"
            :key="opt"
            class="filter-item"
            :class="{ active: filterType === opt }"
            @click="selectFilter(opt)"
          >{{ opt }}</div>
        </div>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="content-area">
      <!-- 上架作品：作品列表接口暂缺，先保持空态 -->
      <div v-if="activeTab === 'works'" class="empty-state">
        <svg class="empty-illustration" viewBox="0 0 200 180" width="200" height="180">
          <defs>
            <linearGradient id="fishBody" x1="0" x2="0" y1="0" y2="1">
              <stop offset="0" stop-color="#7ec0f5" />
              <stop offset="1" stop-color="#3d8ed5" />
            </linearGradient>
          </defs>
          <ellipse cx="100" cy="95" rx="55" ry="38" fill="url(#fishBody)" />
          <path d="M45 95 L18 70 L24 95 L18 120 Z" fill="#5aa8e3" />
          <path d="M95 60 L110 40 L116 62 Z" fill="#5aa8e3" />
          <ellipse cx="100" cy="115" rx="35" ry="14" fill="#e8f4fc" />
          <circle cx="125" cy="85" r="6" fill="#fff" />
          <circle cx="127" cy="85" r="3" fill="#1a1a1a" />
          <path d="M148 100 Q160 105 148 112" stroke="#1a1a1a" stroke-width="2" fill="none" stroke-linecap="round" />
          <rect x="60" y="105" width="60" height="40" rx="4" fill="#b8b8b8" />
          <rect x="60" y="105" width="60" height="10" fill="#9a9a9a" />
          <rect x="86" y="105" width="8" height="40" fill="#9a9a9a" />
          <circle cx="138" cy="50" r="16" fill="#ff5566" />
          <text x="138" y="58" text-anchor="middle" fill="#fff" font-size="22" font-weight="700" font-family="sans-serif">?</text>
        </svg>
        <div class="empty-text">暂无上架的作品~</div>
      </div>

      <!-- 赞过的 -->
      <div v-else-if="activeTab === 'liked'">
        <template v-if="isSelf">
          <div v-loading="likedLoading" class="post-list">
            <div v-if="!likedLoading && likedList.length === 0" class="empty-state">
              <div class="empty-text">还没有赞过任何作品~</div>
            </div>
            <div v-for="post in likedList" :key="post.id" class="post-card" @click="goPostDetail(post.id)">
              <div class="post-card-head">
                <span class="post-card-avatar">{{ post.user?.userName?.[0] || 'U' }}</span>
                <span class="post-card-name">{{ post.user?.userName || '匿名用户' }}</span>
              </div>
              <div class="post-card-title">{{ post.title }}</div>
              <div class="post-card-content">{{ post.content }}</div>
              <div class="post-card-foot">
                <span>{{ formatTime(post.createTime) }}</span>
                <span class="post-card-like">❤️ {{ post.likeCount || 0 }}</span>
              </div>
            </div>
          </div>
          <div
            v-if="likedList.length > 0 && likedPage * PAGE_SIZE < likedTotal"
            class="load-more-wrap"
          >
            <button class="load-more-btn" :disabled="likedLoadingMore" @click="loadLikedPosts(true)">
              {{ likedLoadingMore ? '加载中…' : '加载更多' }}
            </button>
          </div>
        </template>
        <div v-else class="empty-state">
          <div class="empty-text">对方的点赞记录不对外展示~</div>
        </div>
      </div>

      <!-- 足迹 -->
      <div v-else>
        <template v-if="isSelf">
          <div v-loading="footprintLoading" class="post-list">
            <div v-if="!footprintLoading && footprintList.length === 0" class="empty-state">
              <div class="empty-text">还没有浏览足迹~</div>
            </div>
            <div v-for="post in footprintList" :key="post.id" class="post-card" @click="goPostDetail(post.id)">
              <div class="post-card-head">
                <span class="post-card-avatar">{{ post.user?.userName?.[0] || 'U' }}</span>
                <span class="post-card-name">{{ post.user?.userName || '匿名用户' }}</span>
              </div>
              <div class="post-card-title">{{ post.title }}</div>
              <div class="post-card-content">{{ post.content }}</div>
              <div class="post-card-foot">
                <span>{{ formatTime(post.createTime) }}</span>
                <span class="post-card-like">👁 {{ post.viewCount || 0 }}</span>
              </div>
            </div>
          </div>
          <div
            v-if="footprintList.length > 0 && footprintPage * PAGE_SIZE < footprintTotal"
            class="load-more-wrap"
          >
            <button class="load-more-btn" :disabled="footprintLoadingMore" @click="loadFootprintPosts(true)">
              {{ footprintLoadingMore ? '加载中…' : '加载更多' }}
            </button>
          </div>
        </template>
        <div v-else class="empty-state">
          <div class="empty-text">对方的浏览足迹不对外展示~</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-page {
  background: #ffffff;
  min-height: calc(100vh - 48px);
}

/* 顶部信息：白色卡片 + 渐变封面，头像上叠 */
.profile-header {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 16px;
}
.profile-main {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px;
}
.avatar-large {
  width: 64px;
  height: 64px;
  border-radius: 14px;
  background: linear-gradient(135deg, #6c5cff, #5b8cff);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  font-weight: 600;
  flex-shrink: 0;
}
.profile-info {
  flex: 1;
  min-width: 0;
  padding-bottom: 2px;
}
.profile-name {
  font-size: 20px;
  font-weight: 700;
  color: #1f2329;
  margin-bottom: 8px;
}
.profile-stats {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 13px;
  color: #646a73;
  margin-bottom: 8px;
}
.profile-stats b {
  color: #1f2329;
  font-weight: 700;
  margin-right: 4px;
  font-size: 15px;
}
.profile-stats .sep {
  color: #d1d5db;
}
.profile-bio {
  font-size: 13px;
  color: #8f959e;
}
.share-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  border-radius: 999px;
  background: #1f2329;
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  flex-shrink: 0;
  margin-bottom: 8px;
  transition: background 0.2s;
}
.share-btn:hover {
  background: #333a45;
}

/* 账号安全卡片 */
.security-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 18px 24px;
  margin-bottom: 16px;
}
.security-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
  margin-bottom: 14px;
}
.security-row {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}
.security-row + .security-row {
  border-top: 1px solid #f0f0f0;
  padding-top: 16px;
  margin-top: 4px;
}
.security-label {
  width: 72px;
  flex-shrink: 0;
  font-size: 14px;
  color: #646a73;
  padding-top: 8px;
}
.security-control {
  flex: 1;
  min-width: 0;
}
.security-bound {
  display: inline-block;
  font-size: 14px;
  color: #1f2329;
  background: #f2f4f8;
  border-radius: 6px;
  padding: 6px 12px;
  margin-bottom: 10px;
}
.security-inputs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}
.security-input {
  flex: 1;
  min-width: 220px;
  padding: 9px 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
}
.security-input:focus {
  border-color: #3b6ef7;
}
.security-code-row {
  display: flex;
  gap: 8px;
}
.security-code-input {
  width: 140px;
  padding: 9px 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
}
.security-code-input:focus {
  border-color: #3b6ef7;
}
.security-code-btn {
  padding: 9px 14px;
  background: #3b6ef7;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.2s;
}
.security-code-btn:hover:not(:disabled) {
  background: #2f5ce0;
}
.security-code-btn:disabled {
  background: #c3cbe8;
  cursor: not-allowed;
}
.security-bind-btn {
  padding: 9px 20px;
  background: #1f2329;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}
.security-bind-btn:hover:not(:disabled) {
  background: #333a45;
}
.security-bind-btn:disabled {
  background: #c3cbe8;
  cursor: not-allowed;
}
.security-tip {
  margin-top: 10px;
  font-size: 12px;
  color: #9ca3af;
}

/* 标签栏：白底卡片 + 胶囊 Tab */
.tab-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 12px 16px;
  margin-bottom: 16px;
}
.tabs {
  display: flex;
  background: #fff;
  border: 1px solid #e4e5e7;
  border-radius: 8px;
  padding: 4px;
}
.tab {
  padding: 7px 16px;
  border-radius: 6px;
  font-size: 14px;
  color: #646a73;
  cursor: pointer;
  user-select: none;
  white-space: nowrap;
  transition: 0.2s;
}
.tab:hover {
  color: #1f2329;
}
.tab.active {
  background: #1f2329;
  color: #fff;
  font-weight: 500;
}

/* 筛选下拉 */
.filter-wrap {
  position: relative;
}
.filter-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
  font-size: 14px;
  color: #1f2329;
  cursor: pointer;
  transition: background 0.15s;
}
.filter-btn:hover {
  background: #f8fafc;
}
.caret {
  font-size: 10px;
  color: #9ca3af;
}
.filter-menu {
  position: absolute;
  right: 0;
  top: calc(100% + 4px);
  min-width: 130px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08);
  z-index: 10;
  padding: 4px;
}
.filter-item {
  padding: 8px 12px;
  font-size: 14px;
  border-radius: 6px;
  cursor: pointer;
  color: #1f2329;
}
.filter-item:hover {
  background: #f2f4f8;
}
.filter-item.active {
  background: #eef2ff;
  color: #4096ff;
}

/* 内容区 */
.content-area {
  padding: 4px 0 24px;
  min-height: 400px;
}
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 48px 0;
}
.empty-illustration {
  width: 200px;
  height: 180px;
}
.empty-text {
  font-size: 14px;
  color: #9ca3af;
}

/* 赞过的 / 足迹 帖子列表 */
.post-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 200px;
}

.post-card {
  background: #fff;
  border: 1px solid #eef0f4;
  border-radius: 12px;
  padding: 14px 16px;
  cursor: pointer;
  transition: box-shadow 0.18s ease, border-color 0.18s ease;
}

.post-card:hover {
  border-color: #d8ddf5;
  box-shadow: 0 4px 14px rgba(28, 36, 80, 0.08);
}

.post-card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.post-card-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.post-card-name {
  font-size: 13px;
  color: #6b7280;
}

.post-card-title {
  font-size: 15px;
  font-weight: 500;
  color: #1f2329;
  margin-bottom: 6px;
  line-height: 1.4;
}

.post-card-content {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 10px;
}

.post-card-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #9ca3af;
}

.post-card-like {
  color: #e35d6a;
}

.load-more-wrap {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.load-more-btn {
  padding: 7px 28px;
  border: 1px solid #e5e7eb;
  background: #fff;
  border-radius: 999px;
  font-size: 13px;
  color: #4a4f63;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease;
}

.load-more-btn:hover:not(:disabled) {
  background: #f4f6fb;
  border-color: #667eea;
  color: #667eea;
}

.load-more-btn:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}
</style>

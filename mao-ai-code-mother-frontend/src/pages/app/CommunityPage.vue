<template>
  <div class="community-wrap">
    <!-- 顶部区域 -->
    <div class="community-header">
      <div class="header-left">
        <h1>社区</h1>
        <div class="tab-bar">
          <div
            v-for="tab in topTabs"
            :key="tab.key"
            class="tab-item"
            :class="{ active: activeTopTab === tab.key }"
            @click="switchTopTab(tab.key)"
          >
            {{ tab.label }}
          </div>
        </div>
      </div>
      <div class="header-right-area">
        <div v-if="showBanner" class="banner-notice">
          秒哒推广大使招募，赢实习解锁专属好礼!
          <span class="banner-close" @click="showBanner = false">×</span>
        </div>
        <div class="search-action-row">
          <div class="search-input-wrap">
            <input v-model="searchKey" class="search-input" placeholder="搜索话题" @keyup.enter="loadPosts(1)" />
            <span class="search-icon" @click="loadPosts(1)">🔍</span>
          </div>
          <button class="btn-post" @click="goToPostEdit">+ 发布帖子</button>
        </div>
      </div>
    </div>

    <!-- 第二层胶囊标签 -->
    <div v-if="showSecondTabs" class="second-tab-wrap">
      <div class="tab-bottom-group">
        <div
          v-for="cat in categories"
          :key="cat.key"
          class="tab-bottom-item"
          :class="{ active: activeCategory === cat.key }"
          @click="switchCategory(cat.key)"
        >
          {{ cat.label }}
        </div>
      </div>
    </div>

    <!-- 帖子列表 -->
    <div v-loading="loading" class="post-list">
      <div v-if="postList.length === 0 && !loading" class="empty-wrap">
        <div class="empty-icon-box">📦</div>
        <div class="empty-text">没有符合的内容，更换搜索词再试试吧</div>
      </div>
      <div v-for="post in postList" :key="post.id" class="post-card" @click="viewPost(post)">
        <div class="post-header">
          <div class="post-author">
            <div class="avatar" @click.stop="goUserHome(post.userId)">
              {{ post.user?.userName ? post.user.userName.charAt(0) : 'U' }}
            </div>
            <div class="author-info">
              <div class="author-name" @click.stop="goUserHome(post.userId)">{{ post.user?.userName || '匿名用户' }}</div>
              <div class="post-time">{{ formatTime(post.createTime) }}</div>
            </div>
          </div>
          <div v-if="post.category" class="post-category">{{ getCategoryLabel(post.category) }}</div>
        </div>
        <div class="post-title" @click="viewPost(post)">{{ post.title }}</div>
        <div class="post-content" @click="viewPost(post)">{{ post.content }}</div>
        <div v-if="post.tags && post.tags.length > 0" class="post-tags">
          <span v-for="tag in post.tags" :key="tag" class="post-tag">#{{ tag }}</span>
        </div>
        <div class="post-footer">
          <div class="post-action" :class="{ liked: post.isLiked }" @click.stop="handleLike(post)">
            <span class="action-icon">{{ post.isLiked ? '❤️' : '🤍' }}</span>
            <span class="action-count">{{ post.likeCount || 0 }}</span>
          </div>
          <div class="post-action">
            <span class="action-icon">💬</span>
            <span class="action-count">{{ post.commentCount || 0 }}</span>
          </div>
          <div class="post-action">
            <span class="action-icon">👁️</span>
            <span class="action-count">{{ post.viewCount || 0 }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="total > 0" class="pagination-wrap">
      <a-pagination
        v-model:current="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :show-size-changer="false"
        :show-total="(t) => `共 ${t} 条`"
        @change="loadPosts"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  listCommunityPostVoByPage,
  listMyCommunityPostVoByPage,
  toggleCommunityPostLike,
} from '@/api/communityPostController'
import { useLoginUserStore } from '@/stores/loginUser'

const router = useRouter()

// 跳转到指定用户的个人主页（点其他玩家的头像/昵称）
const goUserHome = (userId?: number | string) => {
  if (userId == null) return
  router.push(`/user/profile/${userId}`)
}
const loginUserStore = useLoginUserStore()

const activeTopTab = ref('recommend')
const activeCategory = ref('all')
const searchKey = ref('')
const showBanner = ref(true)
const loading = ref(false)
const postList = ref<API.CommunityPostVO[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const topTabs = [
  { key: 'recommend', label: '推荐' },
  { key: 'hot', label: '最热' },
  { key: 'latest', label: '最新' },
  { key: 'myPost', label: '我发布的' },
  { key: 'myLike', label: '我点赞的' },
]

const categories = [
  { key: 'all', label: '全部' },
  { key: 'official', label: '官方公告' },
  { key: 'tips', label: '经验技巧' },
  { key: 'suggest', label: '产品建议' },
  { key: 'feedback', label: '问题反馈' },
  { key: 'other', label: '其他' },
]

const showSecondTabs = computed(() => {
  return ['recommend', 'hot', 'latest'].includes(activeTopTab.value)
})

const getCategoryLabel = (key: string) => {
  const cat = categories.find((c) => c.key === key)
  return cat ? cat.label : key
}

const formatTime = (time?: string) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString()
}

const switchTopTab = (key: string) => {
  activeTopTab.value = key
  loadPosts(1)
}

const switchCategory = (key: string) => {
  activeCategory.value = key
  loadPosts(1)
}

const loadPosts = async (page = currentPage.value) => {
  loading.value = true
  try {
    let sortField = 'createTime'
    let sortOrder = 'descend'

    if (activeTopTab.value === 'hot') {
      sortField = 'likeCount'
    } else if (activeTopTab.value === 'latest') {
      sortField = 'createTime'
    }

    const params: API.CommunityPostQueryRequest = {
      pageNum: page,
      pageSize: pageSize.value,
      sortField,
      sortOrder,
    }

    if (activeCategory.value !== 'all') {
      params.category = activeCategory.value
    }

    if (searchKey.value.trim()) {
      params.title = searchKey.value.trim()
    }

    let res
    if (activeTopTab.value === 'myPost') {
      res = await listMyCommunityPostVoByPage(params)
    } else {
      res = await listCommunityPostVoByPage(params)
    }

    if (res.data.code === 0 && res.data.data) {
      postList.value = res.data.data.records || []
      total.value = res.data.data.totalRow || 0
      currentPage.value = page
    }
  } catch (error) {
    console.error('加载帖子列表失败', error)
    message.error('加载失败，请重试')
  } finally {
    loading.value = false
  }
}

const goToPostEdit = () => {
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    router.push('/user/login')
    return
  }
  router.push('/community/post')
}

const handleLike = async (post: API.CommunityPostVO) => {
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    router.push('/user/login')
    return
  }
  if (!post.id) return

  try {
    const res = await toggleCommunityPostLike({ postId: post.id })
    if (res.data.code === 0) {
      const liked = res.data.data
      post.isLiked = liked
      post.likeCount = (post.likeCount || 0) + (liked ? 1 : -1)
    }
  } catch (error) {
    console.error('点赞失败', error)
  }
}

const viewPost = (post: API.CommunityPostVO) => {
  if (post.id) {
    router.push(`/community/post/${post.id}`)
  }
}

onMounted(() => {
  loadPosts(1)
})

// keep-alive 恢复（Tab 切回）时刷新帖子列表；初次激活 onMounted 已加载，跳过
let keepAliveActivatedOnce = false
onActivated(() => {
  if (keepAliveActivatedOnce) {
    loadPosts(1)
  }
  keepAliveActivatedOnce = true
})
</script>

<style scoped>
.community-wrap {
  padding: 24px 28px;
  background: linear-gradient(180deg, #f3e8ff 0%, #ffffff 100%);
  min-height: 100vh;
}

.community-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.header-left h1 {
  font-size: 22px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #1f2329;
}

.tab-bar {
  display: flex;
  gap: 24px;
  font-size: 15px;
}

.tab-item {
  cursor: pointer;
  color: #444;
  padding-bottom: 6px;
  position: relative;
}

.tab-item.active {
  color: #000;
  font-weight: 600;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: 0;
  width: 100%;
  height: 2px;
  background: #000;
}

.header-right-area {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}

.banner-notice {
  display: flex;
  align-items: center;
  background: #f0f4ff;
  padding: 4px 10px;
  border-radius: 16px;
  font-size: 13px;
  color: #4056d8;
  gap: 6px;
}

.banner-close {
  cursor: pointer;
  font-size: 14px;
}

.search-action-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.search-input-wrap {
  position: relative;
}

.search-input {
  width: 240px;
  padding: 7px 32px 7px 12px;
  border: 1px solid #dcdde0;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
}

.search-input:focus {
  border-color: #4056d8;
}

.search-icon {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 14px;
  color: #888;
  cursor: pointer;
}

.btn-post {
  padding: 7px 14px;
  background: #000;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.2s;
}

.btn-post:hover {
  background: #222;
}

/* 第二层胶囊标签 */
.second-tab-wrap {
  margin-bottom: 24px;
}

.tab-bottom-group {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.tab-bottom-item {
  padding: 8px 24px;
  border-radius: 999px;
  border: 1px solid #e2e2e8;
  background: #f7f7fa;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-bottom-item:hover {
  background: #eee;
}

.tab-bottom-item.active {
  background: #000000;
  color: #ffffff;
  border-color: #000;
}

/* 帖子列表 */
.post-list {
  max-width: 900px;
  margin: 0 auto;
}

.post-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.2s, transform 0.2s;
  cursor: pointer;
}

.post-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-1px);
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.post-author {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 500;
}

.author-info {
  display: flex;
  flex-direction: column;
}

.author-name {
  font-size: 14px;
  font-weight: 500;
  color: #1f2329;
}

.post-time {
  font-size: 12px;
  color: #868c96;
}

.post-category {
  padding: 4px 10px;
  background: #f0f4ff;
  color: #4056d8;
  border-radius: 4px;
  font-size: 12px;
}

.post-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2329;
  margin-bottom: 8px;
  line-height: 1.4;
}

.post-content {
  font-size: 14px;
  color: #4e5969;
  line-height: 1.6;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.post-tag {
  font-size: 12px;
  color: #4056d8;
  background: #f0f4ff;
  padding: 2px 8px;
  border-radius: 4px;
}

.post-footer {
  display: flex;
  gap: 24px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.post-action {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #868c96;
  font-size: 13px;
  cursor: pointer;
  transition: color 0.2s;
}

.post-action:hover {
  color: #4056d8;
}

.post-action.liked {
  color: #f53f3f;
}

.action-icon {
  font-size: 16px;
}

.action-count {
  min-width: 16px;
}

/* 空状态 */
.empty-wrap {
  text-align: center;
  padding: 80px 0;
}

.empty-icon-box {
  font-size: 52px;
  margin-bottom: 14px;
}

.empty-text {
  font-size: 13px;
  color: #868c96;
}

/* 分页 */
.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}
</style>

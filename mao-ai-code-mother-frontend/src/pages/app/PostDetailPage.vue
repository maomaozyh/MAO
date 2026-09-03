<template>
  <div class="post-detail-page">
    <!-- 返回导航 -->
    <div class="nav-bar">
      <span class="back-link" @click="goBack">
        <span class="back-icon">&lt;</span>
        返回社区
      </span>
    </div>

    <!-- 帖子内容卡片 -->
    <div v-loading="postLoading" class="post-detail-card">
      <template v-if="postDetail">
        <!-- 帖子头部 -->
        <div class="post-header">
          <div class="post-author">
            <div class="avatar" @click.stop="goUserHome(postDetail.userId)">
              {{ postDetail.user?.userName ? postDetail.user.userName.charAt(0) : 'U' }}
            </div>
            <div class="author-info">
              <div class="author-name" @click.stop="goUserHome(postDetail.userId)">{{ postDetail.user?.userName || '匿名用户' }}</div>
              <div class="post-time">{{ formatTime(postDetail.createTime) }}</div>
            </div>
          </div>
          <div v-if="postDetail.category" class="post-category">{{ getCategoryLabel(postDetail.category) }}</div>
        </div>

        <!-- 标题 -->
        <h1 class="post-title">{{ postDetail.title }}</h1>

        <!-- 标签 -->
        <div v-if="postDetail.tags && postDetail.tags.length > 0" class="post-tags">
          <span v-for="tag in postDetail.tags" :key="tag" class="post-tag">#{{ tag }}</span>
        </div>

        <!-- 内容（富文本：用 DOMPurify 净化后渲染） -->
        <div class="post-content" v-html="sanitizedPostContent"></div>

        <!-- 底部操作栏 -->
        <div class="post-actions">
          <div class="action-item" :class="{ liked: postDetail.isLiked }" @click="handleLike">
            <span class="action-icon">{{ postDetail.isLiked ? '❤️' : '🤍' }}</span>
            <span>{{ postDetail.likeCount || 0 }}</span>
          </div>
          <div class="action-item">
            <span class="action-icon">💬</span>
            <span>{{ postDetail.commentCount || 0 }} 评论</span>
          </div>
          <div class="action-item">
            <span class="action-icon">👁️</span>
            <span>{{ postDetail.viewCount || 0 }}</span>
          </div>
          <a-dropdown v-if="canDelete" trigger="click">
            <div class="action-item more-btn">
              <span class="action-icon">⋯</span>
            </div>
            <template #overlay>
              <a-menu>
                <a-menu-item @click="handleDeletePost" class="delete-item">
                  删除帖子
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </template>
    </div>

    <!-- 评论区 -->
    <div class="comment-section">
      <div class="comment-header">
        <h3>评论 ({{ commentTotal }})</h3>
      </div>

      <!-- 评论输入框 -->
      <div v-if="loginUserStore.loginUser.id" class="comment-input-wrap">
        <div class="comment-avatar">
          {{ loginUserStore.loginUser.userName?.charAt(0) || 'U' }}
        </div>
        <div class="comment-input-box">
          <textarea
            v-model="commentContent"
            class="comment-textarea"
            placeholder="写下你的评论..."
            rows="3"
            maxlength="1000"
            @keydown.ctrl.enter="submitComment"
          ></textarea>
          <div class="comment-input-footer">
            <span class="char-count">{{ commentContent.length }}/1000</span>
            <button
              class="btn-comment-submit"
              :disabled="!commentContent.trim() || commentSubmitting"
              @click="submitComment"
            >
              {{ commentSubmitting ? '发布中...' : '发布评论' }}
            </button>
          </div>
        </div>
      </div>
      <div v-else class="comment-login-tip">
        登录后可以发表评论，<span class="login-link" @click="goToLogin">立即登录</span>
      </div>

      <!-- 评论列表 -->
      <div v-loading="commentLoading" class="comment-list">
        <div v-if="commentList.length === 0 && !commentLoading" class="empty-comment">
          暂无评论，快来抢沙发吧～
        </div>
        <div v-for="comment in commentList" :key="comment.id" class="comment-item">
          <div class="comment-avatar" @click.stop="goUserHome(comment.userId)">
            {{ comment.user?.userName ? comment.user.userName.charAt(0) : 'U' }}
          </div>
          <div class="comment-body">
            <div class="comment-meta">
              <span class="comment-author" @click.stop="goUserHome(comment.userId)">{{ comment.user?.userName || '匿名用户' }}</span>
              <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
            </div>
            <div class="comment-content">{{ comment.content }}</div>
            <div class="comment-actions">
              <span class="comment-action" @click="replyTo(comment)">回复</span>
              <a-dropdown v-if="canDeleteComment(comment)" trigger="click">
                <span class="comment-action more-link">⋯</span>
                <template #overlay>
                  <a-menu>
                    <a-menu-item @click="handleDeleteComment(comment.id)" class="delete-item">
                      删除
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </div>
          </div>
        </div>
      </div>

      <!-- 评论分页 -->
      <div v-if="commentTotal > commentPageSize" class="comment-pagination">
        <a-pagination
          v-model:current="commentPage"
          v-model:page-size="commentPageSize"
          :total="commentTotal"
          :show-size-changer="false"
          :show-total="false"
          @change="loadComments"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { getCommunityPostDetail, toggleCommunityPostLike, deleteCommunityPost } from '@/api/communityPostController'
import { addCommunityComment, listCommunityCommentByPage, deleteCommunityComment } from '@/api/communityCommentController'
import { useLoginUserStore } from '@/stores/loginUser'
import DOMPurify from 'dompurify'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 雪花 ID 是 19 位数字，超过 JS Number 安全整数（2^53），必须保持字符串传递，否则精度丢失导致查不到帖子
const postId = computed(() => String(route.params.id))
const postLoading = ref(false)
const postDetail = ref<API.CommunityPostVO | null>(null)

/** 富文本内容用 DOMPurify 净化后渲染（v-html 的 XSS 防护） */
const sanitizedPostContent = computed(() =>
  DOMPurify.sanitize(postDetail.value?.content || '')
)

const commentLoading = ref(false)
const commentList = ref<API.CommunityCommentVO[]>([])
const commentPage = ref(1)
const commentPageSize = ref(10)
const commentTotal = ref(0)

const commentContent = ref('')
const commentSubmitting = ref(false)

const categories = [
  { key: 'official', label: '官方公告' },
  { key: 'tips', label: '经验技巧' },
  { key: 'suggest', label: '产品建议' },
  { key: 'feedback', label: '问题反馈' },
  { key: 'other', label: '其他' },
]

const getCategoryLabel = (key: string) => {
  const cat = categories.find((c) => c.key === key)
  return cat ? cat.label : key
}

const canDelete = computed(() => {
  if (!postDetail.value || !loginUserStore.loginUser.id) return false
  const isAuthor = postDetail.value.userId === loginUserStore.loginUser.id
  const isAdmin = loginUserStore.loginUser.userRole === 'admin'
  return isAuthor || isAdmin
})

const canDeleteComment = (comment: API.CommunityCommentVO) => {
  if (!loginUserStore.loginUser.id) return false
  const isAuthor = comment.userId === loginUserStore.loginUser.id
  const isAdmin = loginUserStore.loginUser.userRole === 'admin'
  return isAuthor || isAdmin
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

const goBack = () => {
  router.back()
}

const goToLogin = () => {
  router.push('/user/login')
}

const loadPostDetail = async () => {
  postLoading.value = true
  try {
    const res = await getCommunityPostDetail({ id: postId.value })
    if (res.data.code === 0 && res.data.data) {
      postDetail.value = res.data.data
    }
  } catch (error) {
    console.error('加载帖子详情失败', error)
    message.error('加载失败，请重试')
  } finally {
    postLoading.value = false
  }
}

const loadComments = async (page = commentPage.value) => {
  commentLoading.value = true
  try {
    const res = await listCommunityCommentByPage({
      postId: postId.value,
      pageNum: page,
      pageSize: commentPageSize.value,
    })
    if (res.data.code === 0 && res.data.data) {
      commentList.value = res.data.data.records || []
      commentTotal.value = res.data.data.totalRow || 0
      commentPage.value = page
    }
  } catch (error) {
    console.error('加载评论失败', error)
  } finally {
    commentLoading.value = false
  }
}

// 跳转到指定用户的个人主页（点作者/评论者头像、昵称）
const goUserHome = (userId?: number | string) => {
  if (userId == null) return
  router.push(`/user/profile/${userId}`)
}

const handleLike = async () => {
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    router.push('/user/login')
    return
  }
  if (!postDetail.value?.id) return

  try {
    const res = await toggleCommunityPostLike({ postId: postDetail.value.id })
    if (res.data.code === 0) {
      const liked = res.data.data
      postDetail.value.isLiked = liked
      postDetail.value.likeCount = (postDetail.value.likeCount || 0) + (liked ? 1 : -1)
    }
  } catch (error) {
    console.error('点赞失败', error)
  }
}

const submitComment = async () => {
  if (!commentContent.value.trim()) {
    message.warning('请输入评论内容')
    return
  }

  commentSubmitting.value = true
  try {
    const res = await addCommunityComment({
      postId: postId.value,
      content: commentContent.value.trim(),
    })
    if (res.data.code === 0) {
      message.success('评论成功')
      commentContent.value = ''
      // 更新帖子评论数
      if (postDetail.value) {
        postDetail.value.commentCount = (postDetail.value.commentCount || 0) + 1
      }
      // 重新加载评论
      loadComments(1)
    } else {
      message.error(res.data.message || '评论失败')
    }
  } catch (error) {
    console.error('评论失败', error)
    message.error('评论失败，请重试')
  } finally {
    commentSubmitting.value = false
  }
}

const replyTo = (comment: API.CommunityCommentVO) => {
  // 简单实现：聚焦到输入框并 @ 用户
  const userName = comment.user?.userName || ''
  commentContent.value = `@${userName} `
  // 滚动到评论输入框
  const textarea = document.querySelector('.comment-textarea') as HTMLTextAreaElement
  if (textarea) {
    textarea.focus()
    textarea.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
}

const handleDeletePost = () => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这篇帖子吗？删除后无法恢复。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res = await deleteCommunityPost({ id: postId.value })
        if (res.data.code === 0) {
          message.success('删除成功')
          router.replace('/community')
        } else {
          message.error(res.data.message || '删除失败')
        }
      } catch (error) {
        console.error('删除帖子失败', error)
        message.error('删除失败，请重试')
      }
    },
  })
}

const handleDeleteComment = (commentId?: number) => {
  if (!commentId) return
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这条评论吗？',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res = await deleteCommunityComment({ id: commentId })
        if (res.data.code === 0) {
          message.success('删除成功')
          // 更新帖子评论数
          if (postDetail.value) {
            postDetail.value.commentCount = Math.max(0, (postDetail.value.commentCount || 0) - 1)
          }
          // 重新加载评论
          loadComments()
        } else {
          message.error(res.data.message || '删除失败')
        }
      } catch (error) {
        console.error('删除评论失败', error)
        message.error('删除失败，请重试')
      }
    },
  })
}

onMounted(() => {
  if (postId.value) {
    loadPostDetail()
    loadComments(1)
  }
})
</script>

<style scoped>
.post-detail-page {
  min-height: 100vh;
  background: #f5f6f8;
  padding: 24px;
}

/* 导航栏 */
.nav-bar {
  max-width: 900px;
  margin: 0 auto 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.back-link {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #1f2329;
  cursor: pointer;
  transition: color 0.2s;
}

.back-link:hover {
  color: #4056d8;
}

.back-icon {
  font-size: 12px;
}

/* 帖子详情卡片 */
.post-detail-card {
  max-width: 900px;
  margin: 0 auto 24px;
  background: #fff;
  border-radius: 12px;
  padding: 28px 32px;
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.post-author {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  cursor: pointer;
  transition: transform 0.15s ease, filter 0.15s ease;
}

.avatar:hover {
  transform: scale(1.06);
  filter: brightness(1.1);
}

.author-info {
  display: flex;
  flex-direction: column;
}

.author-name {
  font-size: 15px;
  font-weight: 500;
  color: #1f2329;
  cursor: pointer;
}

.author-name:hover {
  color: #667eea;
}

.post-time {
  font-size: 12px;
  color: #868c96;
  margin-top: 2px;
}

.post-category {
  padding: 4px 12px;
  background: #f0f4ff;
  color: #4056d8;
  border-radius: 4px;
  font-size: 13px;
}

.post-title {
  font-size: 26px;
  font-weight: 700;
  color: #1f2329;
  margin: 0 0 12px 0;
  line-height: 1.4;
}

.post-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.post-tag {
  font-size: 13px;
  color: #4056d8;
  background: #f0f4ff;
  padding: 3px 10px;
  border-radius: 4px;
}

.post-content {
  font-size: 15px;
  color: #1f2329;
  line-height: 1.8;
  word-break: break-word;
  margin-bottom: 24px;
}

/* 富文本元素渲染样式（帖子内容是 HTML） */
.post-content :deep(p) { margin: 0 0 12px 0; }
.post-content :deep(h1),
.post-content :deep(h2),
.post-content :deep(h3) { font-weight: 600; margin: 20px 0 12px 0; line-height: 1.4; }
.post-content :deep(h1) { font-size: 22px; }
.post-content :deep(h2) { font-size: 20px; }
.post-content :deep(h3) { font-size: 17px; }
.post-content :deep(ul),
.post-content :deep(ol) { margin: 0 0 12px 0; padding-left: 24px; }
.post-content :deep(li) { margin-bottom: 4px; }
.post-content :deep(blockquote) {
  margin: 12px 0; padding: 8px 14px;
  border-left: 3px solid #dcdde0; color: #4e5969; background: #fafbfc;
}
.post-content :deep(a) { color: #4056d8; text-decoration: underline; }
.post-content :deep(img) { max-width: 100%; height: auto; border-radius: 4px; margin: 8px 0; }
.post-content :deep(strong) { font-weight: 700; }
.post-content :deep(em) { font-style: italic; }
.post-content :deep(s) { text-decoration: line-through; }

/* 操作栏 */
.post-actions {
  display: flex;
  align-items: center;
  gap: 32px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #868c96;
  cursor: pointer;
  transition: color 0.2s;
}

.action-item:hover {
  color: #4056d8;
}

.action-item.liked {
  color: #f53f3f;
}

.action-icon {
  font-size: 18px;
}

.more-btn {
  margin-left: auto;
  font-size: 20px;
}

.delete-item {
  color: #f53f3f !important;
}

/* 评论区 */
.comment-section {
  max-width: 900px;
  margin: 0 auto;
  background: #fff;
  border-radius: 12px;
  padding: 24px 32px;
}

.comment-header {
  margin-bottom: 20px;
}

.comment-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #1f2329;
  margin: 0;
}

/* 评论输入框 */
.comment-input-wrap {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid #f0f0f0;
}

.comment-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.15s ease, filter 0.15s ease;
}

.comment-avatar:hover {
  transform: scale(1.06);
  filter: brightness(1.1);
}

.comment-input-box {
  flex: 1;
  min-width: 0;
}

.comment-textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.6;
  resize: vertical;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
  font-family: inherit;
}

.comment-textarea:focus {
  border-color: #4056d8;
}

.comment-input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.char-count {
  font-size: 12px;
  color: #868c96;
}

.btn-comment-submit {
  padding: 6px 20px;
  background: #1f2329;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-comment-submit:hover:not(:disabled) {
  background: #333;
}

.btn-comment-submit:disabled {
  background: #a9aeb8;
  cursor: not-allowed;
}

.comment-login-tip {
  text-align: center;
  padding: 24px;
  color: #868c96;
  font-size: 14px;
  margin-bottom: 24px;
  border-bottom: 1px solid #f0f0f0;
}

.login-link {
  color: #4056d8;
  cursor: pointer;
}

/* 评论列表 */
.comment-list {
  margin-top: 16px;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid #f7f7fa;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.comment-author {
  font-size: 14px;
  font-weight: 500;
  color: #1f2329;
  cursor: pointer;
}

.comment-author:hover {
  color: #667eea;
}

.comment-time {
  font-size: 12px;
  color: #868c96;
}

.comment-content {
  font-size: 14px;
  color: #1f2329;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
  margin-bottom: 8px;
}

.comment-actions {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #868c96;
}

.comment-action {
  cursor: pointer;
  transition: color 0.2s;
}

.comment-action:hover {
  color: #4056d8;
}

.more-link {
  font-size: 16px;
  line-height: 1;
}

.empty-comment {
  text-align: center;
  padding: 48px 0;
  color: #868c96;
  font-size: 14px;
}

/* 评论分页 */
.comment-pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding-top: 16px;
}
</style>

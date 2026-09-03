<template>
  <div class="app-card" :class="{ 'app-card--featured': featured }" @click="handleCardClick">
    <div class="app-cover">
      <img v-if="app.cover" :src="app.cover" :alt="app.appName" />
      <div v-else class="app-placeholder">
        <span class="placeholder-icon">🤖</span>
      </div>
      <div v-if="isTemplate" class="template-badge">
        <span>模板</span>
      </div>
    </div>
    <div class="app-meta">
      <div class="app-title-row">
        <h3 class="app-title">{{ app.appName || '未命名应用' }}</h3>
      </div>
      <div class="app-stats-row">
        <div class="app-author">
          <span class="author-avatar">
            {{ app.user?.userName?.charAt(0) || 'U' }}
          </span>
          <span class="author-name">{{ app.user?.userName || (featured ? '官方' : '未知用户') }}</span>
        </div>
        <div class="app-stats">
          <span class="stat-item">
            <svg class="stat-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
              <circle cx="12" cy="12" r="3"/>
            </svg>
            {{ formatNumber(viewCount) }}
          </span>
          <span class="stat-item">
            <svg class="stat-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
            </svg>
            {{ formatNumber(likeCount) }}
          </span>
        </div>
      </div>
    </div>
    <!-- hover 操作层 -->
    <div class="app-overlay">
      <div class="overlay-buttons">
        <button class="overlay-btn primary" @click.stop="handleViewChat">查看对话</button>
        <button v-if="app.deployKey" class="overlay-btn" @click.stop="handleViewWork">查看作品</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  app: API.AppVO
  featured?: boolean
}

interface Emits {
  (e: 'view-chat', appId: string | number | undefined): void
  (e: 'view-work', app: API.AppVO): void
}

const props = withDefaults(defineProps<Props>(), {
  featured: false,
})

const emit = defineEmits<Emits>()

// 模拟数据（实际项目中应从后端获取）
const viewCount = computed(() => {
  // 用 id 生成一个伪随机的浏览量用于展示
  const id = props.app.id || 1
  return Math.floor((id * 137 + 500) % 50000) + 100
})

const likeCount = computed(() => {
  const id = props.app.id || 1
  return Math.floor((id * 53 + 20) % 5000) + 10
})

const isTemplate = computed(() => {
  // priority > 0 的标记为模板（可根据实际业务调整）
  return (props.app.priority || 0) > 0
})

const formatNumber = (num: number): string => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'W'
  } else if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'K'
  }
  return String(num)
}

const handleCardClick = () => {
  emit('view-chat', props.app.id)
}

const handleViewChat = () => {
  emit('view-chat', props.app.id)
}

const handleViewWork = () => {
  emit('view-work', props.app)
}
</script>

<style scoped>
.app-card {
  background: #ffffff;
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  position: relative;
  border: 1px solid rgba(0, 0, 0, 0.04);
}

.app-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12);
}

/* 封面区域 */
.app-cover {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 10;
  background: linear-gradient(135deg, #f5f5f7 0%, #e8e8ed 100%);
  overflow: hidden;
  border-radius: 16px 16px 0 0;
}

.app-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}

.app-card:hover .app-cover img {
  transform: scale(1.05);
}

.app-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.placeholder-icon {
  font-size: 64px;
  opacity: 0.4;
}

/* 模板标签 */
.template-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: #fff;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.4);
}

/* 元信息区域 */
.app-meta {
  padding: 14px 16px 16px;
}

.app-title-row {
  margin-bottom: 10px;
}

.app-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
}

.app-stats-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.app-author {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  max-width: 50%;
}

.author-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1a1a1a 0%, #333 100%);
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-weight: 500;
}

.author-name {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.app-stats {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #888;
}

.stat-icon {
  width: 14px;
  height: 14px;
}

/* hover 遮罩层 */
.app-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
  border-radius: 20px;
}

.app-card:hover .app-overlay {
  opacity: 1;
}

.overlay-buttons {
  display: flex;
  gap: 10px;
}

.overlay-btn {
  padding: 10px 20px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-weight: 500;
}

.overlay-btn:hover {
  background: rgba(255, 255, 255, 0.25);
}

.overlay-btn.primary {
  background: #fff;
  color: #1a1a1a;
  border-color: #fff;
}

.overlay-btn.primary:hover {
  background: #f0f0f0;
}

/* featured 样式微调 */
.app-card--featured .app-cover {
  aspect-ratio: 16 / 10;
}
</style>

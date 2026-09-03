<template>
  <div class="app-layout">
    <Sidebar />
    <main class="content">
      <!-- keep-alive 缓存高频 Tab 页：切换回来不重新挂载、保留滚动位置；
           SkillCenterPage 有生成轮询（onBeforeUnmount 清理），缓存后 deactivate 不触发清理，故排除 -->
      <router-view v-slot="{ Component }">
        <keep-alive :include="keepAlivePages">
          <component :is="Component" />
        </keep-alive>
      </router-view>
    </main>
  </div>
</template>

<script setup lang="ts">
import Sidebar from '@/components/Sidebar.vue'

// 只缓存无轮询/无定时器的纯列表页；数据新鲜度由各页 onActivated 刷新保证
const keepAlivePages = ['HomePage', 'ProjectPage', 'CommunityPage', 'MaterialPage']
</script>

<style scoped>
.app-layout {
  display: flex;
  align-items: flex-start;
  min-height: 100vh;
}
.content {
  flex: 1;
  min-width: 0;
  padding: 24px;
}
</style>

import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getRecentProjects, markProjectOpened } from '@/api/appController'

export interface RecentProject {
  id: string
  name: string
  emoji: string
  ts: number
}

/**
 * 最近项目（侧边栏"最近项目"）
 * - fetchRecent：拉取当前用户最近打开的应用列表（GET /app/recent）
 * - recordOpen：记录某应用被打开（进入对话页自动触发），并更新列表，让侧边栏实时刷新
 * 放在 Pinia 中是因为对话页（/app/chat/:id）是独立整页路由、不带侧边栏，
 * 而侧边栏在 BasicLayout 下被 keep-alive 缓存不会重挂载；用共享 store 才能在对话结束后
 * 回首页时让"最近项目"自动出现新记录，无需依赖组件重挂载。
 */
export const useRecentProjectsStore = defineStore('recentProjects', () => {
  const projects = ref<RecentProject[]>([])
  const loaded = ref(false)

  function mapProject(app: any): RecentProject {
    const ts = app.lastOpenTime
      ? Date.parse(app.lastOpenTime)
      : app.createTime
        ? Date.parse(app.createTime)
        : Date.now()
    return {
      // 应用 ID 是 19 位雪花 ID，必须保持字符串，严禁用 Number()（否则精度丢失导致跳转/打点错乱）
      id: String(app.id),
      name: app.appName,
      emoji: '📄',
      ts: Number.isNaN(ts) ? Date.now() : ts,
    }
  }

  async function fetchRecent() {
    try {
      const res = await getRecentProjects()
      if (res.data?.code === 0 && res.data.data) {
        projects.value = res.data.data.map(mapProject)
      }
    } catch (e) {
      // 忽略：最近项目为非关键路径
    } finally {
      loaded.value = true
    }
  }

  // 记录某应用被打开，并刷新列表（best-effort，失败不影响体验）
  async function recordOpen(appId: number | string) {
    // 保持雪花 ID 为字符串，禁止 Number() 转换（避免精度丢失）
    const id = String(appId)
    if (!id) return
    try {
      await markProjectOpened(id as any)
    } catch (e) {
      // 忽略同步失败
    }
    await fetchRecent()
  }

  return { projects, loaded, fetchRecent, recordOpen, mapProject }
})

/**
 * 后台管理菜单的单一配置源
 *
 * 之前菜单硬编码在两处：AdminLayout.vue（左侧菜单 + 面包屑）和 GlobalHeader.vue
 * （侧边栏 adminOnly 入口），新增一个管理页要同时改两个文件。
 * 现在统一到这里：新增管理页 = 这里加一行 + router/index.ts 注册路由。
 */
export interface AdminMenuItem {
  /** 路由路径，需与 router/index.ts 中的 /admin/* 保持一致 */
  path: string
  /** 菜单显示名 */
  label: string
  /** 菜单图标（emoji） */
  icon: string
}

export interface AdminMenuGroup {
  title: string
  items: AdminMenuItem[]
}

export const adminMenuGroups: AdminMenuGroup[] = [
  {
    title: '概览',
    items: [{ path: '/admin/dashboard', label: '仪表盘', icon: '📊' }],
  },
  {
    title: '管理',
    items: [
      { path: '/admin/userManage', label: '用户管理', icon: '👥' },
      { path: '/admin/postManage', label: '内容管理', icon: '📝' },
      { path: '/admin/appManage', label: '上传技能', icon: '📱' },
      { path: '/admin/chatManage', label: '对话管理', icon: '💬' },
      { path: '/admin/skillManage', label: '技能管理', icon: '🧩' },
      { path: '/admin/orderManage', label: '订单管理', icon: '💰' },
      { path: '/admin/materialManage', label: '素材管理', icon: '🗂️' },
      { path: '/admin/pointsManage', label: '积分流水', icon: '🪙' },
    ],
  },
  {
    title: '系统',
    items: [
      { path: '/admin/sensitiveWord', label: '敏感词管理', icon: '🚫' },
      { path: '/admin/roleManage', label: '角色管理', icon: '🎭' },
      { path: '/admin/permissionManage', label: '权限管理', icon: '🔐' },
      { path: '/admin/menuManage', label: '菜单管理', icon: '📋' },
      { path: '/admin/settings', label: '系统设置', icon: '⚙️' },
      { path: '/admin/logs', label: '操作日志', icon: '📜' },
    ],
  },
]

/** 打平后的菜单项，供 GlobalHeader 侧边栏等场景使用 */
export const adminMenuItems: AdminMenuItem[] = adminMenuGroups.flatMap(
  (group) => group.items
)

/** 根据当前路径反查菜单项（AdminLayout 面包屑用） */
export function findAdminMenuItem(path: string) {
  for (const group of adminMenuGroups) {
    const item = group.items.find((i) => path.startsWith(i.path))
    if (item) {
      return { group, item }
    }
  }
  return null
}

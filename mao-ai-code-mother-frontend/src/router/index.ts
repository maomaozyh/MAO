import { createRouter, createWebHistory } from 'vue-router'
import { message } from 'ant-design-vue'
import BasicLayout from '@/layouts/BasicLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // 主应用区：带常驻侧边栏的布局
    {
      path: '/',
      component: BasicLayout,
      children: [
        {
          path: '',
          name: '主页',
          component: () => import('@/pages/HomePage.vue'),
        },
        {
          path: 'user/profile',
          name: '个人主页',
          component: () => import('@/pages/user/UserProfilePage.vue'),
        },
        {
          path: 'user/profile/:userId',
          name: '他人主页',
          component: () => import('@/pages/user/UserProfilePage.vue'),
          props: true,
        },
        {
          path: 'app/edit/:id',
          name: '编辑应用',
          component: () => import('@/pages/app/AppEditPage.vue'),
        },
        {
          path: 'app',
          name: '项目',
          component: () => import('@/pages/app/ProjectPage.vue'),
        },
        {
          path: 'skills',
          name: '技能中心',
          component: () => import('@/pages/app/SkillCenterPage.vue'),
        },
        {
          path: 'materials',
          name: '素材库',
          component: () => import('@/pages/app/MaterialPage.vue'),
        },
        {
          path: 'community',
          name: '社区',
          component: () => import('@/pages/app/CommunityPage.vue'),
        },
        {
          path: 'community/post',
          name: '发布帖子',
          component: () => import('@/pages/app/PostEditPage.vue'),
        },
        {
          path: 'community/post/:id',
          name: '帖子详情',
          component: () => import('@/pages/app/PostDetailPage.vue'),
        },
        {
          path: 'membership',
          name: '会员中心',
          component: () => import('@/pages/member/MemberCenterPage.vue'),
        },
      ],
    },
    // 登录 / 注册 / 对话：独立整页，不带侧边栏
    {
      path: '/user/login',
      name: '用户登录',
      component: () => import('@/pages/user/UserLoginPage.vue'),
    },
    {
      path: '/user/register',
      name: '用户注册',
      component: () => import('@/pages/user/UserRegisterPage.vue'),
    },
    // 对话相关：独立整页，无侧边栏（与登录/注册一致）
    {
      path: '/chat',
      name: 'AI对话',
      component: () => import('@/pages/app/AiChatPage.vue'),
    },
    {
      path: '/app/chat/:id',
      name: '应用对话',
      component: () => import('@/pages/app/AppChatPage.vue'),
    },
    {
      path: '/app/stop',
      name: '对话停止',
      component: () => import('@/pages/app/ConversationStopPage.vue'),
    },
    // 后台管理路由（独立布局）
    {
      path: '/admin',
      component: AdminLayout,
      redirect: '/admin/dashboard',
      children: [
        {
          path: 'dashboard',
          name: '仪表盘',
          component: () => import('@/pages/admin/DashboardPage.vue'),
        },
        {
          path: 'userManage',
          name: '用户管理',
          component: () => import('@/pages/admin/UserManagePage.vue'),
        },
        {
          path: 'postManage',
          name: '内容管理',
          component: () => import('@/pages/admin/PostManagePage.vue'),
        },
        {
          path: 'appManage',
          name: '应用管理',
          component: () => import('@/pages/admin/AppManagePage.vue'),
        },
        {
          path: 'chatManage',
          name: '对话管理',
          component: () => import('@/pages/admin/ChatManagePage.vue'),
        },
        {
          path: 'skillManage',
          name: '技能管理',
          component: () => import('@/pages/admin/SkillManagePage.vue'),
        },
        {
          path: 'orderManage',
          name: '订单管理',
          component: () => import('@/pages/admin/OrderManagePage.vue'),
        },
        {
          path: 'materialManage',
          name: '素材管理',
          component: () => import('@/pages/admin/MaterialManagePage.vue'),
        },
        {
          path: 'pointsManage',
          name: '积分流水',
          component: () => import('@/pages/admin/PointsManagePage.vue'),
        },
        {
          path: 'settings',
          name: '系统设置',
          component: () => import('@/pages/admin/SettingsPage.vue'),
        },
        {
          path: 'logs',
          name: '操作日志',
          component: () => import('@/pages/admin/LogsPage.vue'),
        },
        {
          path: 'roleManage',
          name: '角色管理',
          component: () => import('@/pages/admin/RoleManagePage.vue'),
        },
        {
          path: 'permissionManage',
          name: '权限管理',
          component: () => import('@/pages/admin/PermissionManagePage.vue'),
        },
        {
          path: 'menuManage',
          name: '菜单管理',
          component: () => import('@/pages/admin/MenuManagePage.vue'),
        },
        {
          path: 'sensitiveWord',
          name: '敏感词管理',
          component: () => import('@/pages/admin/SensitiveWordPage.vue'),
        },
      ],
    },
  ],
})

// 路由级错误兜底：懒加载（如 HomePage/DashboardPage）在被访问时才向 dev server 拉取 chunk。
// 若 dev server 断开、网络异常或 chunk hash 失效导致 import() 失败，默认会整页白屏且无提示。
// 这里捕获此类错误，给出明确提示并自动重载一次，避免"登录成功但页面空白"这类静默失败。
let chunkErrorReloading = false
router.onError((error: unknown) => {
  const msg = (error as Error)?.message || String(error)
  const isChunkError =
    /Failed to fetch dynamically imported module|Importing a module script failed|Loading chunk .* failed|timeout of/i.test(
      msg,
    )
  if (isChunkError) {
    if (!chunkErrorReloading) {
      chunkErrorReloading = true
      message.error('页面资源加载失败，正在自动刷新…')
      setTimeout(() => location.reload(), 1500)
    }
  } else {
    console.error('[router] 路由导航出错：', error)
  }
})

export default router

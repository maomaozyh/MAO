import { useLoginUserStore } from '@/stores/loginUser'
import { usePermissionStore } from '@/stores/permission'
import { message } from 'ant-design-vue'
import router from '@/router'

// 是否为首次获取登录用户
let firstFetchLoginUser = true

/**
 * 全局权限校验
 */
router.beforeEach(async (to, from, next) => {
  const loginUserStore = useLoginUserStore()
  const permissionStore = usePermissionStore()
  let loginUser = loginUserStore.loginUser
  // 确保页面刷新，首次加载时，能够等后端返回用户信息后再校验权限
  if (firstFetchLoginUser) {
    await loginUserStore.fetchLoginUser()
    loginUser = loginUserStore.loginUser
    firstFetchLoginUser = false
  }
  const toUrl = to.fullPath
  // 管理端：区分「未登录」和「已登录但非管理员」，两者处理方式不同
  if (toUrl.startsWith('/admin')) {
    const isLogin = !!loginUser?.id
    // 未登录：去登录页，登录后跳回原地址
    if (!isLogin) {
      message.warning('请先登录')
      next(`/user/login?redirect=${to.fullPath}`)
      return
    }
    // 已登录但不是管理员：不踢去登录页，直接回首页
    if (loginUser.userRole !== 'admin') {
      message.error('没有权限访问管理后台')
      next('/')
      return
    }
    // 管理员加载权限和菜单（仅首次进入后台时加载）
    if (!permissionStore.hasLoaded) {
      await permissionStore.loadPermissionAndMenu()
    }
  }
  next()
})

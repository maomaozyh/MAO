import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getMyPermissionCodes } from '@/api/sysPermissionController.ts'
import { getUserMenuTree } from '@/api/sysMenuController.ts'

/**
 * 权限与菜单 store
 */
export const usePermissionStore = defineStore('permission', () => {
  // 权限编码列表
  const permissionCodes = ref<string[]>([])
  // 菜单树
  const menuTree = ref<any[]>([])
  // 是否已加载
  const hasLoaded = ref(false)

  // 加载当前用户的权限和菜单
  async function loadPermissionAndMenu() {
    try {
      // 加载权限编码
      const permRes = await getMyPermissionCodes()
      if (permRes.data.code === 0 && permRes.data.data) {
        permissionCodes.value = permRes.data.data
      }

      // 加载菜单树
      const menuRes = await getUserMenuTree()
      if (menuRes.data.code === 0 && menuRes.data.data) {
        menuTree.value = menuRes.data.data
      }

      hasLoaded.value = true
    } catch (e) {
      console.error('加载权限和菜单失败', e)
    }
  }

  // 检查是否有指定权限
  function hasPermission(code: string): boolean {
    if (!code) return true
    // admin 角色拥有所有权限（后端也会放行）
    if (permissionCodes.value.includes('*')) return true
    return permissionCodes.value.includes(code)
  }

  // 检查是否有任一权限
  function hasAnyPermission(codes: string[]): boolean {
    if (!codes || codes.length === 0) return true
    return codes.some((code) => hasPermission(code))
  }

  // 重置权限
  function reset() {
    permissionCodes.value = []
    menuTree.value = []
    hasLoaded.value = false
  }

  return {
    permissionCodes,
    menuTree,
    hasLoaded,
    loadPermissionAndMenu,
    hasPermission,
    hasAnyPermission,
    reset,
  }
})

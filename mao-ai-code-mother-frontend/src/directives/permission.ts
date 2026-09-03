import type { Directive, DirectiveBinding } from 'vue'
import { usePermissionStore } from '@/stores/permission'

/**
 * 按钮级权限指令 v-permission
 * 用法：
 *   v-permission="'user:add'"              // 单个权限
 *   v-permission="['user:add', 'user:edit']"  // 多个权限（满足任一即可）
 */
const permissionDirective: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    checkPermission(el, binding)
  },
  updated(el: HTMLElement, binding: DirectiveBinding) {
    checkPermission(el, binding)
  },
}

function checkPermission(el: HTMLElement, binding: DirectiveBinding) {
  const { value } = binding
  const permissionStore = usePermissionStore()

  if (!value) {
    return
  }

  let hasPermission = false

  if (typeof value === 'string') {
    hasPermission = permissionStore.hasPermission(value)
  } else if (Array.isArray(value)) {
    hasPermission = permissionStore.hasAnyPermission(value)
  }

  if (!hasPermission) {
    // 移除元素
    el.parentNode?.removeChild(el)
  }
}

export default permissionDirective

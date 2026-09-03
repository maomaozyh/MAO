import request from '@/request'

/**
 * 权限管理 API
 */

// 获取权限树
export async function getPermissionTree() {
  return request.get<any>('/admin/system/permission/tree')
}

// 获取所有权限列表
export async function listAllPermissions() {
  return request.get<any>('/admin/system/permission/list/all')
}

// 获取权限详情
export async function getPermissionById(id: number) {
  return request.get<any>(`/admin/system/permission/get?id=${id}`)
}

// 新增权限
export async function addPermission(params: any) {
  return request.post<any>('/admin/system/permission/add', params)
}

// 更新权限
export async function updatePermission(params: any) {
  return request.post<any>('/admin/system/permission/update', params)
}

// 删除权限
export async function deletePermission(params: any) {
  return request.post<any>('/admin/system/permission/delete', params)
}

// 获取当前用户权限编码列表
export async function getMyPermissionCodes() {
  return request.get<any>('/admin/system/permission/my/codes')
}

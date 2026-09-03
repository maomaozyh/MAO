import request from '@/request'

/**
 * 角色管理 API
 */

// 分页查询角色
export async function listRoleByPage(params: any) {
  return request.post<any>('/admin/system/role/list/page', params)
}

// 获取所有角色
export async function listAllRoles() {
  return request.get<any>('/admin/system/role/list/all')
}

// 获取角色详情
export async function getRoleById(id: number) {
  return request.get<any>(`/admin/system/role/get?id=${id}`)
}

// 新增角色
export async function addRole(params: any) {
  return request.post<any>('/admin/system/role/add', params)
}

// 更新角色
export async function updateRole(params: any) {
  return request.post<any>('/admin/system/role/update', params)
}

// 删除角色
export async function deleteRole(params: any) {
  return request.post<any>('/admin/system/role/delete', params)
}

// 获取角色的权限ID列表
export async function getRolePermissionIds(roleId: number) {
  return request.get<any>(`/admin/system/role/permission/list?roleId=${roleId}`)
}

// 分配权限给角色
export async function assignPermissionsToRole(params: any) {
  return request.post<any>('/admin/system/role/permission/assign', params)
}

// 获取角色的菜单ID列表
export async function getRoleMenuIds(roleId: number) {
  return request.get<any>(`/admin/system/role/menu/list?roleId=${roleId}`)
}

// 分配菜单给角色
export async function assignMenusToRole(params: any) {
  return request.post<any>('/admin/system/role/menu/assign', params)
}

// 获取用户的角色ID列表
export async function getUserRoleIds(userId: number) {
  return request.get<any>(`/admin/system/role/user/roles?userId=${userId}`)
}

// 分配角色给用户
export async function assignUserRoles(params: any) {
  return request.post<any>('/admin/system/role/user/assign', params)
}

import request from '@/request'

/**
 * 菜单管理 API
 */

// 获取菜单树
export async function getMenuTree() {
  return request.get<any>('/admin/system/menu/tree')
}

// 获取当前用户菜单树
export async function getUserMenuTree() {
  return request.get<any>('/admin/system/menu/user/tree')
}

// 获取所有菜单列表
export async function listAllMenus() {
  return request.get<any>('/admin/system/menu/list/all')
}

// 获取菜单详情
export async function getMenuById(id: number) {
  return request.get<any>(`/admin/system/menu/get?id=${id}`)
}

// 新增菜单
export async function addMenu(params: any) {
  return request.post<any>('/admin/system/menu/add', params)
}

// 更新菜单
export async function updateMenu(params: any) {
  return request.post<any>('/admin/system/menu/update', params)
}

// 删除菜单
export async function deleteMenu(params: any) {
  return request.post<any>('/admin/system/menu/delete', params)
}

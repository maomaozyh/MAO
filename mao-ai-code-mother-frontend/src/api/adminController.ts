// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 获取仪表盘统计数据 GET /admin/dashboard/stats */
export async function getDashboardStats(options?: { [key: string]: any }) {
  return request<API.BaseResponseDashboardStatsVO>('/admin/dashboard/stats', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 管理员更新帖子 POST /community/post/admin/update */
export async function adminUpdatePost(
  body: API.CommunityPostUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/community/post/admin/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 管理员删除帖子 POST /community/post/admin/delete */
export async function adminDeletePost(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/community/post/admin/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 管理员分页获取帖子列表 POST /community/post/admin/list/vo/page */
export async function adminListPostVoByPage(
  body: API.CommunityPostQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageCommunityPostVO>('/community/post/admin/list/vo/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 分页查询操作日志 POST /admin/log/list/page */
export async function listOperationLogByPage(
  body: API.OperationLogQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageOperationLog>('/admin/log/list/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 获取系统配置列表 GET /admin/config/list */
export async function listSysConfig(options?: { [key: string]: any }) {
  return request<API.BaseResponseListSysConfig>('/admin/config/list', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 更新系统配置 POST /admin/config/update */
export async function updateSysConfig(
  body: API.SysConfigUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/admin/config/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

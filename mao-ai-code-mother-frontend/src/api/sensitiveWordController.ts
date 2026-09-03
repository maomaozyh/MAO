// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 分页查询敏感词 POST /admin/sensitive/page */
export async function pageSensitiveWords(
  body: API.SensitiveWordQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageSysSensitiveWord>('/admin/sensitive/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 获取所有敏感词列表 GET /admin/sensitive/list */
export async function listSensitiveWords(options?: { [key: string]: any }) {
  return request<API.BaseResponseListSysSensitiveWord>('/admin/sensitive/list', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 新增敏感词 POST /admin/sensitive/add */
export async function addSensitiveWord(
  body: API.SensitiveWordAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseString>('/admin/sensitive/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 更新敏感词 POST /admin/sensitive/update */
export async function updateSensitiveWord(
  body: API.SensitiveWordAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/admin/sensitive/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 删除敏感词 POST /admin/sensitive/delete */
export async function deleteSensitiveWord(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/admin/sensitive/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 批量导入敏感词 POST /admin/sensitive/batch/add */
export async function batchAddSensitiveWords(
  body: { words: string[]; category?: string },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseInt>('/admin/sensitive/batch/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 刷新敏感词缓存 POST /admin/sensitive/refresh */
export async function refreshSensitiveWords(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/admin/sensitive/refresh', {
    method: 'POST',
    ...(options || {}),
  })
}

/** 检测文本敏感词 POST /admin/sensitive/check */
export async function checkSensitiveText(text: string, options?: { [key: string]: any }) {
  return request<API.BaseResponseListString>('/admin/sensitive/check', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: text,
    ...(options || {}),
  })
}

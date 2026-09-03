// @ts-ignore
// API 更新时间：
// API 唯一标识：
import request from '@/request'

/** 创建技能（管理员） POST /skill/add */
export async function addSkill(body: API.SkillAddRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseString>('/skill/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 更新技能（管理员 / 创建人） POST /skill/update */
export async function updateSkill(body: API.SkillUpdateRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/skill/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 删除技能（管理员） POST /skill/delete */
export async function deleteSkill(body: API.DeleteRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/skill/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 根据 id 获取技能详情（公开） GET /skill/get/vo */
export async function getSkillVOById(params: { id: number }, options?: { [key: string]: any }) {
  return request<API.BaseResponseSkillVO>('/skill/get/vo', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 分页获取技能列表（公开） POST /skill/list/page/vo */
export async function listSkillVOByPage(body: API.SkillQueryRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponsePageSkillVO>('/skill/list/page/vo', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 上传技能文件并自动解析创建（.json / .zip / .skill） POST /skill/upload */
export async function uploadSkill(formData: FormData, options?: { [key: string]: any }) {
  return request<API.BaseResponseString>('/skill/upload', {
    method: 'POST',
    // 上传文件用 multipart/form-data，axios 会在 data 为 FormData 时自动设置 boundary
    data: formData,
    timeout: 120000,
    ...(options || {}),
  })
}

/** 获取技能中心公开配置（Banner/分类/额度，无需登录） GET /config/skill-center */
export async function getSkillCenterConfig(options?: { [key: string]: any }) {
  return request<API.BaseResponseSkillCenterConfig>('/config/skill-center', {
    method: 'GET',
    ...(options || {}),
  })
}

// @ts-ignore
// API 更新时间：
// API 唯一标识：
import request from '@/request'

/** 上传素材（保存到 COS 并落库） POST /material/upload */
export async function uploadMaterial(body: FormData, options?: { [key: string]: any }) {
  return request<API.BaseResponseString>('/material/upload', {
    method: 'POST',
    data: body,
    ...(options || {}),
  })
}

/** 删除素材 POST /material/delete */
export async function deleteMaterial(body: API.DeleteRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/material/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 根据 id 获取素材详情（公开） GET /material/get/vo */
export async function getMaterialVOById(params: { id: number | string }, options?: { [key: string]: any }) {
  return request<API.BaseResponseMaterialVO>('/material/get/vo', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 分页获取我的素材列表 POST /material/list/page/vo */
export async function listMyMaterialVoByPage(body: API.MaterialQueryRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponsePageMaterialVO>('/material/list/page/vo', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 新建文件夹 POST /material/folder/add */
export async function addMaterialFolder(body: API.MaterialFolderAddRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseString>('/material/folder/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 查询当前用户的文件夹列表 GET /material/folder/list */
export async function listMaterialFolder(options?: { [key: string]: any }) {
  return request<API.BaseResponseListMaterialFolderVO>('/material/folder/list', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 删除文件夹（素材不会被删除，仅解除归属） POST /material/folder/delete */
export async function deleteMaterialFolder(body: API.DeleteRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/material/folder/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

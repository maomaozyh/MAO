// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 添加评论 POST /community/comment/add */
export async function addCommunityComment(body: API.CommunityCommentAddRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseString>('/community/comment/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 删除评论 POST /community/comment/delete */
export async function deleteCommunityComment(body: API.DeleteRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/community/comment/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 分页获取评论列表 GET /community/comment/list/page */
export async function listCommunityCommentByPage(
  params: API.listCommunityCommentByPageParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageCommunityCommentVO>('/community/comment/list/page', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

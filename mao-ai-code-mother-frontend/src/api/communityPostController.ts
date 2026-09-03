// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 发布帖子 POST /community/post/add */
export async function addCommunityPost(body: API.CommunityPostAddRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseString>('/community/post/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 删除帖子 POST /community/post/delete */
export async function deleteCommunityPost(body: API.DeleteRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/community/post/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 根据 id 获取帖子 GET /community/post/get/vo */
export async function getCommunityPostVoById(
  params: API.getCommunityPostVOByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseCommunityPostVO>('/community/post/get/vo', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 获取帖子详情（浏览量+1） GET /community/post/get/detail */
export async function getCommunityPostDetail(
  params: API.getCommunityPostVOByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseCommunityPostVO>('/community/post/get/detail', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 分页获取帖子列表 POST /community/post/list/vo/page */
export async function listCommunityPostVoByPage(
  body: API.CommunityPostQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageCommunityPostVO>('/community/post/list/vo/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 获取我发布的帖子 POST /community/post/my/list/vo/page */
export async function listMyCommunityPostVoByPage(
  body: API.CommunityPostQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageCommunityPostVO>('/community/post/my/list/vo/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 点赞 / 取消点赞 POST /community/post/like/toggle */
export async function toggleCommunityPostLike(
  params: API.toggleCommunityPostLikeParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/community/post/like/toggle', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 我赞过的帖子列表 GET /community/post/my/liked/page */
export async function listMyLikedCommunityPostByPage(
  params: { pageNum?: number; pageSize?: number },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageCommunityPostVO>('/community/post/my/liked/page', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 我的足迹（浏览记录）列表 GET /community/post/my/footprint/page */
export async function listMyFootprintCommunityPostByPage(
  params: { pageNum?: number; pageSize?: number },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageCommunityPostVO>('/community/post/my/footprint/page', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

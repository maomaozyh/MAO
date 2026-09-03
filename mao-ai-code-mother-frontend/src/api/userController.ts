// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 POST /user/add */
export async function addUser(body: API.UserAddRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseString>('/user/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /user/delete */
export async function deleteUser(body: API.DeleteRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/user/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /user/get */
export async function getUserById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getUserByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseUser>('/user/get', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /user/get/login */
export async function getLoginUser(options?: { [key: string]: any }) {
  return request<API.BaseResponseLoginUserVO>('/user/get/login', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /user/get/vo */
export async function getUserVoById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getUserVOByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseUserVO>('/user/get/vo', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /user/list/page/vo */
export async function listUserVoByPage(
  body: API.UserQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageUserVO>('/user/list/page/vo', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /user/login */
export async function userLogin(body: API.UserLoginRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseLoginUserVO>('/user/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 发送短信验证码 POST /user/login/sms/code */
export async function sendSmsCode(body: { phone: string; captchaKey?: string; captcha?: string }, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/user/login/sms/code', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 获取图形验证码 GET /user/captcha */
export async function getCaptcha(options?: { [key: string]: any }) {
  return request<API.BaseResponseCaptchaVO>('/user/captcha', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 短信验证码登录 POST /user/login/sms */
export async function userLoginBySms(body: { phone: string; code: string }, options?: { [key: string]: any }) {
  return request<API.BaseResponseLoginUserVO>('/user/login/sms', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 发送「找回密码」短信验证码 POST /user/password/reset/sms/code */
export async function sendResetSmsCode(body: { phone: string; captchaKey?: string; captcha?: string }, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/user/password/reset/sms/code', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 手机号 + 验证码找回密码 POST /user/password/reset/phone */
export async function resetPasswordByPhone(
  body: API.UserResetPasswordByPhoneRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/user/password/reset/phone', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 发送「找回密码」邮箱验证码 POST /user/password/reset/email/code */
export async function sendResetEmailCode(body: { email: string; captchaKey?: string; captcha?: string }, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/user/password/reset/email/code', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 邮箱 + 验证码找回密码 POST /user/password/reset/email */
export async function resetPasswordByEmail(
  body: API.UserResetPasswordByEmailRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/user/password/reset/email', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 发送「绑定邮箱」验证码 POST /user/bind/email/code */
export async function sendBindEmailCode(body: { email: string }, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/user/bind/email/code', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 绑定邮箱 POST /user/bind/email */
export async function bindEmail(body: API.UserBindEmailRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/user/bind/email', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /user/logout */
export async function userLogout(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/user/logout', {
    method: 'POST',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /user/register */
export async function userRegister(
  body: API.UserRegisterRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseString>('/user/register', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /user/update */
export async function updateUser(body: API.UserUpdateRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/user/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 修改密码（登录态，需校验原密码）POST /user/update/password */
export async function updateMyPassword(
  body: API.UserUpdatePasswordRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/user/update/password', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 管理员重置用户密码 POST /user/reset/password */
export async function resetUserPassword(body: { userId: number; newPassword: string }, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/user/reset/password', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 管理员调整用户积分 POST /user/adjust/balance */
export async function adjustUserBalance(body: { userId: number; amount: number; reason?: string }, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/user/adjust/balance', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 管理员修改用户会员等级 POST /user/update/membership */
export async function updateUserMembership(body: { userId: number; membershipTier: string; membershipExpireTime?: string }, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/user/update/membership', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 管理员批量删除用户 POST /user/batch/delete */
export async function batchDeleteUsers(body: { ids: number[] }, options?: { [key: string]: any }) {
  return request<API.BaseResponseInt>('/user/batch/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

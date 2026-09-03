/**
 * 通用表单校验工具。
 * 手机号正则与后端 UserServiceImpl.PHONE_REGEX（^1[3-9]\d{9}$）保持一致，
 * 作为前端唯一来源，避免两端漂移。
 */

export const PHONE_REGEX = /^1[3-9]\d{9}$/

/**
 * 校验中国大陆手机号。自动 trim，空值返回 false。
 */
export const isValidPhone = (v?: string | null): boolean =>
  typeof v === 'string' && v.trim().length > 0 && PHONE_REGEX.test(v.trim())

/**
 * 邮箱正则（与后端 UserServiceImpl.EMAIL_REGEX 保持一致）。
 */
export const EMAIL_REGEX = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/

/**
 * 校验邮箱格式。自动 trim，空值返回 false。
 */
export const isValidEmail = (v?: string | null): boolean =>
  typeof v === 'string' && v.trim().length > 0 && EMAIL_REGEX.test(v.trim())

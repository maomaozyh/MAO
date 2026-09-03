import myAxios from '@/request'

/**
 * 积分余额（购买余额 + 赠送额度）
 */
export function getPointsBalance() {
  return myAxios.get('/points/balance')
}

/**
 * 分页查询我的积分流水
 */
export function listMyPointsByPage(params: { pageNum?: number; pageSize?: number }) {
  return myAxios.get('/points/list/page/vo', { params })
}

/**
 * 每日签到（每天一次，送积分计入赠送额度）
 */
export function checkinPoints() {
  return myAxios.post('/points/checkin')
}

/**
 * 查询今日签到状态
 */
export function getCheckinStatus() {
  return myAxios.get('/points/checkin/status')
}

/**
 * 管理后台分页查询全部积分流水（仅管理员）
 */
export function listAllPointsByPage(params: {
  pageNum?: number
  pageSize?: number
  userId?: string | number
  bizType?: string
}) {
  return myAxios.post('/points/admin/list/page/vo', params)
}

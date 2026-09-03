import { useLoginUserStore } from '@/stores/loginUser'

/**
 * 积分消费（扩写 / 代码自查 / 生成代码 / 语义搜索等）后刷新登录用户，
 * 让侧边栏额度卡、会员中心余额等所有共用 loginUserStore 的地方实时更新。
 * 后端在 deduct/refund 时已清 Redis 登录缓存，这里只需重新拉取最新登录态。
 */
export function refreshBalance() {
  return useLoginUserStore().fetchLoginUser()
}

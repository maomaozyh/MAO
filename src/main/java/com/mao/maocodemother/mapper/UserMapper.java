package com.mao.maocodemother.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mao.maocodemother.model.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户 映射层。
 *
 * @author <a href="https://github.com/liyupi">程序员mao</a>
 */
public interface UserMapper extends BaseMapper<User> {

    /**
     * CAS（Compare-And-Set）原子更新两级积分余额。
     *
     * <p>仅当数据库中两级余额仍等于读取时的期望值（NULL 安全比较 &lt;=&gt;）时才写入新值，
     * 用于替代「读-改-整行覆盖写」的丢失更新竞态：并发扣费/签到/入账时，
     * 后提交的事务若基于过期快照，本条 UPDATE 影响 0 行，由调用方重试。
     *
     * @param userId         用户 id
     * @param expectedGift   读取时的赠送额度（用于乐观校验）
     * @param expectedBalance 读取时的购买余额（用于乐观校验）
     * @param newGift        新赠送额度
     * @param newBalance     新购买余额
     * @return 影响行数（0 = 并发冲突，需重读重试）
     */
    @Update("UPDATE user SET giftSecondsBalance = #{newGift}, secondsBalance = #{newBalance}, updateTime = NOW() "
            + "WHERE id = #{userId} AND (giftSecondsBalance <=> #{expectedGift}) AND (secondsBalance <=> #{expectedBalance})")
    int casUpdateBalance(@Param("userId") Long userId,
                         @Param("expectedGift") Long expectedGift,
                         @Param("expectedBalance") Long expectedBalance,
                         @Param("newGift") long newGift,
                         @Param("newBalance") long newBalance);

    /**
     * CAS 原子签到：置签到日期 + 赠送额度加奖励。
     *
     * <p>WHERE 里同时校验「上次签到日期仍是读取时的值」，两个并发签到请求只有一个能成功，
     * 另一个 CAS 失败后重读会发现今日已签，走幂等拒绝分支。
     *
     * @param userId             用户 id
     * @param expectedGift       读取时的赠送额度
     * @param expectedCheckinDate 读取时的签到日期（可空）
     * @param newGift            新赠送额度
     * @param today              今日日期（yyyy-MM-dd）
     * @return 影响行数（0 = 并发冲突或已签到）
     */
    @Update("UPDATE user SET giftSecondsBalance = #{newGift}, lastCheckinDate = #{today}, updateTime = NOW() "
            + "WHERE id = #{userId} AND (giftSecondsBalance <=> #{expectedGift}) AND (lastCheckinDate <=> #{expectedCheckinDate})")
    int casCheckin(@Param("userId") Long userId,
                   @Param("expectedGift") Long expectedGift,
                   @Param("expectedCheckinDate") String expectedCheckinDate,
                   @Param("newGift") long newGift,
                   @Param("today") String today);

    /**
     * 原子发放会员月度赠送额度：仅当本月未发放过（lastGiftMonth 与当前月不同或为空）时生效。
     *
     * <p>条件内置于 WHERE，天然幂等：并发/重复调度只有一个请求影响 1 行，
     * 同时用列自增表达式避免「读-改-写」覆盖并发签到/扣费的更新。
     *
     * @param userId 用户 id
     * @param amount 发放额度
     * @param month  当前月份（yyyy-MM）
     * @return 影响行数（0 = 本月已发放）
     */
    @Update("UPDATE user SET giftSecondsBalance = IFNULL(giftSecondsBalance, 0) + #{amount}, lastGiftMonth = #{month}, updateTime = NOW() "
            + "WHERE id = #{userId} AND (lastGiftMonth IS NULL OR lastGiftMonth <> #{month})")
    int grantMonthlyGift(@Param("userId") Long userId,
                         @Param("amount") long amount,
                         @Param("month") String month);
}

package com.mao.maocodemother.schedule;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mao.maocodemother.mapper.SecondsRecordMapper;
import com.mao.maocodemother.mapper.UserMapper;
import com.mao.maocodemother.model.entity.SecondsRecord;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.enums.SecondsBizTypeEnum;
import com.mao.maocodemother.service.SysConfigService;
import com.mao.maocodemother.service.UserService;
import com.mao.maocodemother.utils.RedisUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 付费会员每月赠送积分发放。
 *
 * <p>每小时检查一次：会员等级非 FREE 且会员未过期的用户，
 * 当月（按 lastGiftMonth 判断）未发放过的，发放 user.giftSeconds 配置的积分。
 * lastGiftMonth 保证每月只发一次（幂等）。
 */
@Component
@Slf4j
public class GiftSecondsScheduler {

    private static final String LOGIN_USER_KEY_PREFIX = "login:user:";

    @Resource
    private UserService userService;

    @Resource
    private SysConfigService sysConfigService;

    @Resource
    private SecondsRecordMapper secondsRecordMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private RedisUtil redisUtil;

    @Scheduled(fixedRate = 3600_000)
    public void grantMonthlyGift() {
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        long giftAmount;
        try {
            giftAmount = Long.parseLong(sysConfigService.getConfigValue("user.giftSeconds", "100").trim());
        } catch (NumberFormatException e) {
            giftAmount = 100L;
        }
        if (giftAmount <= 0) {
            return;
        }
        long granted = 0;
        // 付费会员（非 FREE 且未过期）且本月未发放
        QueryColumn giftMonthCol = new QueryColumn("lastGiftMonth");
        List<User> paidUsers = userService.list(QueryWrapper.create()
                .ne(User::getMembershipTier, "FREE")
                .gt(User::getMembershipExpireTime, LocalDateTime.now())
                .and(giftMonthCol.isNull().or(giftMonthCol.ne(month))));
        if (paidUsers.isEmpty()) {
            return;
        }
        for (User user : paidUsers) {
            try {
                // 原子条件发放：WHERE 内置「本月未发放」判断，天然幂等且不会覆盖并发的扣费/签到更新
                int rows = userMapper.grantMonthlyGift(user.getId(), giftAmount, month);
                if (rows == 0) {
                    continue;
                }
                redisUtil.delete(LOGIN_USER_KEY_PREFIX + user.getId());
                secondsRecordMapper.insert(SecondsRecord.builder()
                        .userId(user.getId())
                        .amount(giftAmount)
                        .balanceAfter(user.getSecondsBalance())
                        .giftAfter((user.getGiftSecondsBalance() == null ? 0L : user.getGiftSecondsBalance()) + giftAmount)
                        .bizType(SecondsBizTypeEnum.GIFT.getValue())
                        .bizDesc("会员月度额度发放（" + month + "）")
                        .status(0)
                        .build(), true);
                granted++;
            } catch (Exception e) {
                log.warn("[积分] 会员月度额度发放失败，userId={}", user.getId(), e);
            }
        }
        log.info("[积分] 会员月度额度发放完成，month={}，发放 {} 人，每人 {} 点", month, granted, giftAmount);
    }
}

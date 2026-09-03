package com.mao.maocodemother.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mao.maocodemother.exception.BusinessException;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.exception.ThrowUtils;
import com.mao.maocodemother.mapper.SecondsRecordMapper;
import com.mao.maocodemother.mapper.UserMapper;
import com.mao.maocodemother.model.dto.seconds.SecondsRecordQueryRequest;
import com.mao.maocodemother.model.entity.SecondsRecord;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.enums.SecondsBizTypeEnum;
import com.mao.maocodemother.model.vo.SecondsBalanceVO;
import com.mao.maocodemother.model.vo.SecondsRecordVO;
import com.mao.maocodemother.service.SecondsService;
import com.mao.maocodemother.service.SysConfigService;
import com.mao.maocodemother.service.UserService;
import com.mao.maocodemother.utils.RedisUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 积分服务实现
 *
 * <p>扣费顺序：先扣赠送额度 giftSecondsBalance，不足部分再扣购买余额 secondsBalance。
 * <p>失败退回通过 refund 完成，以流水的 status 字段保证幂等。
 */
@Service
@Slf4j
public class SecondsServiceImpl extends ServiceImpl<SecondsRecordMapper, SecondsRecord> implements SecondsService {

    /**
     * 计费单价配置键（sys_config）
     */
    private static final String PRICE_CONFIG_KEY = "seconds.price";

    /**
     * 计费单价默认值（配置缺失时兜底）
     */
    private static final String DEFAULT_PRICE_JSON =
            "{\"genCode\":10,\"image\":20,\"video\":100,\"model3d\":150,\"ppt\":30,"
                    + "\"expand\":2,\"semanticSearch\":2,\"selfCheck\":2}";

    /**
     * 登录态缓存前缀（需与 UserServiceImpl 保持一致），权益变更后必须清除
     */
    private static final String LOGIN_USER_KEY_PREFIX = "login:user:";

    /**
     * CAS 乐观更新最大重试次数（并发冲突时重读-重算-重写）
     */
    private static final int CAS_MAX_RETRY = 3;

    @Resource
    private UserService userService;

    @Resource
    private SysConfigService sysConfigService;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private UserMapper userMapper;

    @Resource
    private SecondsRecordMapper secondsRecordMapper;

    @Override
    public long getPrice(SecondsBizTypeEnum bizType) {
        if (bizType == null || !bizType.isConsume()) {
            return 0L;
        }
        String raw = sysConfigService.getConfigValue(PRICE_CONFIG_KEY, DEFAULT_PRICE_JSON);
        try {
            JSONObject json = JSONUtil.parseObj(raw);
            return json.getLong(bizType.getPriceKey(), 10L);
        } catch (Exception e) {
            log.warn("[积分] 计费单价配置解析失败，使用默认值 10，key={}", PRICE_CONFIG_KEY, e);
            return 10L;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long deduct(Long userId, SecondsBizTypeEnum bizType, String bizDesc, Long appId) {
        long cost = getPrice(bizType);
        if (cost <= 0) {
            return 0L;
        }
        for (int attempt = 1; attempt <= CAS_MAX_RETRY; attempt++) {
            User user = userService.getById(userId);
            if (user == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            }
            Long giftRaw = user.getGiftSecondsBalance();
            Long balanceRaw = user.getSecondsBalance();
            long gift = giftRaw == null ? 0L : giftRaw;
            long balance = balanceRaw == null ? 0L : balanceRaw;
            long total = gift + balance;
            if (total < cost) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR,
                        "积分不足，本次需要 " + cost + " 积分，当前可用 " + total + " 积分，请先充值");
            }
            // 先扣赠送额度，不足部分再扣购买余额
            long useGift = Math.min(gift, cost);
            long useBalance = cost - useGift;
            long newGift = gift - useGift;
            long newBalance = balance - useBalance;
            // CAS 原子更新：仅当两级余额仍等于本次读取值时生效，影响 0 行 = 并发冲突 → 重读重试
            int rows = userMapper.casUpdateBalance(userId, giftRaw, balanceRaw, newGift, newBalance);
            if (rows > 0) {
                evictLoginCache(userId);
                SecondsRecord record = SecondsRecord.builder()
                        .userId(userId)
                        .amount(-cost)
                        .balanceAfter(newBalance)
                        .giftAfter(newGift)
                        .bizType(bizType.getValue())
                        .bizDesc(bizDesc == null ? bizType.getText() : bizDesc)
                        .appId(appId)
                        .status(0)
                        .build();
                this.save(record);
                return record.getId();
            }
            log.warn("[积分] 扣费 CAS 冲突（第 {}/{} 次），userId={}，重读重试", attempt, CAS_MAX_RETRY, userId);
        }
        throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作太频繁，请稍后重试");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(Long recordId) {
        if (recordId == null || recordId <= 0) {
            return;
        }
        SecondsRecord record = this.getById(recordId);
        if (record == null) {
            return;
        }
        // 幂等：非消耗记录不再处理；已退回的走下方原子抢占分支
        if (record.getAmount() == null || record.getAmount() >= 0) {
            return;
        }
        // 原子抢占退款权：并发对同一流水退款时只有一个请求影响 1 行，其余直接返回
        if (secondsRecordMapper.markRefunded(recordId) == 0) {
            return;
        }
        long back = -record.getAmount();
        Long userId = record.getUserId();
        for (int attempt = 1; attempt <= CAS_MAX_RETRY; attempt++) {
            User user = userService.getById(userId);
            if (user == null) {
                return;
            }
            Long giftRaw = user.getGiftSecondsBalance();
            Long balanceRaw = user.getSecondsBalance();
            long gift = giftRaw == null ? 0L : giftRaw;
            long balance = balanceRaw == null ? 0L : balanceRaw;
            // 与扣费顺序一致，优先退回赠送额度，保证总额正确
            int rows = userMapper.casUpdateBalance(userId, giftRaw, balanceRaw, gift + back, balance);
            if (rows > 0) {
                evictLoginCache(userId);
                SecondsRecord refundRecord = SecondsRecord.builder()
                        .userId(userId)
                        .amount(back)
                        .balanceAfter(balance)
                        .giftAfter(gift + back)
                        .bizType(SecondsBizTypeEnum.REFUND.getValue())
                        .bizDesc("生成失败退回：" + record.getBizDesc())
                        .appId(record.getAppId())
                        .status(0)
                        .build();
                this.save(refundRecord);
                return;
            }
            log.warn("[积分] 退款 CAS 冲突（第 {}/{} 次），userId={}，重读重试", attempt, CAS_MAX_RETRY, userId);
        }
        // 重试耗尽：抛异常回滚（含上面的流水 status 置 1），保证余额与流水一致
        throw new BusinessException(ErrorCode.OPERATION_ERROR, "退款操作冲突，请稍后重试");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void credit(Long userId, long amount, SecondsBizTypeEnum bizType, String bizDesc) {
        if (userId == null || amount <= 0) {
            return;
        }
        for (int attempt = 1; attempt <= CAS_MAX_RETRY; attempt++) {
            User user = userService.getById(userId);
            if (user == null) {
                return;
            }
            Long giftRaw = user.getGiftSecondsBalance();
            Long balanceRaw = user.getSecondsBalance();
            long gift = giftRaw == null ? 0L : giftRaw;
            long balance = balanceRaw == null ? 0L : balanceRaw;
            // 购买计入 secondsBalance；赠送发放计入 giftSecondsBalance
            boolean purchase = SecondsBizTypeEnum.PURCHASE.equals(bizType);
            long newGift = purchase ? gift : gift + amount;
            long newBalance = purchase ? balance + amount : balance;
            int rows = userMapper.casUpdateBalance(userId, giftRaw, balanceRaw, newGift, newBalance);
            if (rows > 0) {
                evictLoginCache(userId);
                SecondsRecord record = SecondsRecord.builder()
                        .userId(userId)
                        .amount(amount)
                        .balanceAfter(newBalance)
                        .giftAfter(newGift)
                        .bizType(bizType.getValue())
                        .bizDesc(bizDesc == null ? bizType.getText() : bizDesc)
                        .status(0)
                        .build();
                this.save(record);
                return;
            }
            log.warn("[积分] 入账 CAS 冲突（第 {}/{} 次），userId={}，重读重试", attempt, CAS_MAX_RETRY, userId);
        }
        throw new BusinessException(ErrorCode.OPERATION_ERROR, "入账操作冲突，请稍后重试");
    }

    @Override
    public SecondsBalanceVO getBalance(Long userId) {
        SecondsBalanceVO vo = new SecondsBalanceVO();
        User user = userService.getById(userId);
        if (user == null) {
            vo.setSecondsBalance(0L);
            vo.setGiftSecondsBalance(0L);
            vo.setTotalSeconds(0L);
            return vo;
        }
        long balance = user.getSecondsBalance() == null ? 0L : user.getSecondsBalance();
        long gift = user.getGiftSecondsBalance() == null ? 0L : user.getGiftSecondsBalance();
        vo.setSecondsBalance(balance);
        vo.setGiftSecondsBalance(gift);
        vo.setTotalSeconds(balance + gift);
        return vo;
    }

    @Override
    public Page<SecondsRecordVO> listMyRecords(Long userId, long pageNum, long pageSize) {
        long current = pageNum < 1 ? 1 : pageNum;
        long size = pageSize < 1 ? 10 : Math.min(pageSize, 50);
        Page<SecondsRecord> page = this.page(Page.of(current, size),
                QueryWrapper.create().eq(SecondsRecord::getUserId, userId)
                        .orderBy(SecondsRecord::getCreateTime, false));
        Page<SecondsRecordVO> voPage = new Page<>();
        voPage.setPageNumber(page.getPageNumber());
        voPage.setPageSize(page.getPageSize());
        voPage.setTotalRow(page.getTotalRow());
        List<SecondsRecordVO> voList = page.getRecords().stream().map(this::toVO).toList();
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public Page<SecondsRecordVO> listRecordsByPage(SecondsRecordQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        long current = request.getPageNum() < 1 ? 1 : request.getPageNum();
        long size = request.getPageSize() < 1 ? 10 : Math.min(request.getPageSize(), 100);
        String bizType = request.getBizType();
        QueryWrapper wrapper = QueryWrapper.create()
                .eq(SecondsRecord::getUserId, request.getUserId(), request.getUserId() != null)
                .eq(SecondsRecord::getBizType, bizType, bizType != null && !bizType.isEmpty())
                .orderBy(SecondsRecord::getCreateTime, false);
        Page<SecondsRecord> page = this.page(Page.of(current, size), wrapper);
        Page<SecondsRecordVO> voPage = new Page<>();
        voPage.setPageNumber(page.getPageNumber());
        voPage.setPageSize(page.getPageSize());
        voPage.setTotalRow(page.getTotalRow());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    private SecondsRecordVO toVO(SecondsRecord r) {
        SecondsRecordVO vo = new SecondsRecordVO();
        vo.setId(r.getId());
        vo.setUserId(r.getUserId());
        vo.setAmount(r.getAmount());
        vo.setBalanceAfter(r.getBalanceAfter());
        vo.setGiftAfter(r.getGiftAfter());
        vo.setBizType(r.getBizType());
        SecondsBizTypeEnum bizEnum = SecondsBizTypeEnum.getEnumByValue(r.getBizType());
        vo.setBizTypeText(bizEnum == null ? r.getBizType() : bizEnum.getText());
        vo.setBizDesc(r.getBizDesc());
        vo.setAppId(r.getAppId());
        vo.setStatus(r.getStatus());
        vo.setCreateTime(r.getCreateTime());
        return vo;
    }

    /**
     * 清除登录态缓存（权益变更后必须调用，否则 getLoginUser 一直返回旧余额）。
     *
     * <p>失败重试一次并打 error 级日志：Redis 持续不可用时缓存清不掉，前端余额会滞后展示，
     * 需要能从日志/告警发现，而不是静默吞掉。
     */
    private void evictLoginCache(Long userId) {
        String key = LOGIN_USER_KEY_PREFIX + userId;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                redisUtil.delete(key);
                return;
            } catch (Exception e) {
                log.error("[积分] 清除登录态缓存失败（第 {} 次），key={}", attempt, key, e);
            }
        }
        log.error("[积分] 清除登录态缓存重试耗尽，登录态缓存可能滞后展示旧余额，userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long checkin(Long userId) {
        String today = java.time.LocalDate.now().toString();
        long reward;
        try {
            reward = Long.parseLong(sysConfigService.getConfigValue("seconds.checkinReward", "200").trim());
        } catch (NumberFormatException e) {
            reward = 200L;
        }
        if (reward <= 0) {
            reward = 200L;
        }
        for (int attempt = 1; attempt <= CAS_MAX_RETRY; attempt++) {
            User user = userService.getById(userId);
            if (user == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            }
            if (today.equals(user.getLastCheckinDate())) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "今日已签到，明天再来吧");
            }
            Long giftRaw = user.getGiftSecondsBalance();
            long gift = giftRaw == null ? 0L : giftRaw;
            // CAS 原子签到：WHERE 同时校验旧签到日期与旧赠送额度，并发签到只有一个成功
            int rows = userMapper.casCheckin(userId, giftRaw, user.getLastCheckinDate(), gift + reward, today);
            if (rows > 0) {
                evictLoginCache(userId);
                SecondsRecord record = SecondsRecord.builder()
                        .userId(userId)
                        .amount(reward)
                        .balanceAfter(user.getSecondsBalance())
                        .giftAfter(gift + reward)
                        .bizType(SecondsBizTypeEnum.GIFT.getValue())
                        .bizDesc("每日签到")
                        .status(0)
                        .build();
                this.save(record);
                return reward;
            }
            // CAS 失败：大概率是并发签到已把日期置为今日，重读后走上面的幂等分支
            log.warn("[积分] 签到 CAS 冲突（第 {}/{} 次），userId={}，重读重试", attempt, CAS_MAX_RETRY, userId);
        }
        throw new BusinessException(ErrorCode.OPERATION_ERROR, "今日已签到或操作冲突，请刷新后重试");
    }

    @Override
    public boolean isCheckedInToday(Long userId) {
        User user = userService.getById(userId);
        return user != null && java.time.LocalDate.now().toString().equals(user.getLastCheckinDate());
    }
}

package com.mao.maocodemother.service.impl;

import com.mao.maocodemother.service.LoginAttemptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 登录尝试服务实现（基于 Redis）
 * <p>
 * 防暴力破解策略：
 * - 连续失败 5 次后，临时锁定 15 分钟
 * - 失败计数窗口：5 分钟
 * - 锁定维度：账号 + IP 组合（防止针对同一账号从不同 IP 爆破，也防止同一 IP 爆破不同账号）
 * - 登录成功后自动清除失败计数
 */
@Service
@Slf4j
public class LoginAttemptServiceImpl implements LoginAttemptService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 最大连续失败次数
     */
    private static final int MAX_FAILURE_ATTEMPTS = 5;

    /**
     * 失败计数过期时间（秒）- 5 分钟
     */
    private static final long FAILURE_COUNT_TTL_SECONDS = 300;

    /**
     * 锁定时间（秒）- 15 分钟
     */
    private static final long LOCK_DURATION_SECONDS = 900;

    /**
     * 失败计数 key 前缀
     */
    private static final String FAILURE_COUNT_KEY_PREFIX = "login:failure:";

    /**
     * 锁定 key 前缀
     */
    private static final String LOCK_KEY_PREFIX = "login:lock:";

    @Override
    public void recordFailure(String userAccount, String clientIp) {
        String key = buildKey(userAccount, clientIp);
        String countKey = FAILURE_COUNT_KEY_PREFIX + key;

        // 自增失败计数
        Long count = stringRedisTemplate.opsForValue().increment(countKey);
        // 首次失败时设置过期时间
        if (count != null && count == 1) {
            stringRedisTemplate.expire(countKey, FAILURE_COUNT_TTL_SECONDS, TimeUnit.SECONDS);
        }

        // 达到阈值，锁定
        if (count != null && count >= MAX_FAILURE_ATTEMPTS) {
            String lockKey = LOCK_KEY_PREFIX + key;
            stringRedisTemplate.opsForValue().set(lockKey, "1", LOCK_DURATION_SECONDS, TimeUnit.SECONDS);
            log.warn("[登录锁定] 账号 {} IP {} 连续失败 {} 次，已锁定 {} 分钟",
                    userAccount, clientIp, count, LOCK_DURATION_SECONDS / 60);
        }
    }

    @Override
    public void recordSuccess(String userAccount, String clientIp) {
        String key = buildKey(userAccount, clientIp);
        stringRedisTemplate.delete(FAILURE_COUNT_KEY_PREFIX + key);
        stringRedisTemplate.delete(LOCK_KEY_PREFIX + key);
    }

    @Override
    public boolean isLocked(String userAccount, String clientIp) {
        String key = buildKey(userAccount, clientIp);
        String lockKey = LOCK_KEY_PREFIX + key;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(lockKey));
    }

    @Override
    public long getRemainingLockSeconds(String userAccount, String clientIp) {
        String key = buildKey(userAccount, clientIp);
        String lockKey = LOCK_KEY_PREFIX + key;
        Long ttl = stringRedisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
        return ttl != null && ttl > 0 ? ttl : 0;
    }

    @Override
    public int getFailureCount(String userAccount, String clientIp) {
        String key = buildKey(userAccount, clientIp);
        String countKey = FAILURE_COUNT_KEY_PREFIX + key;
        String count = stringRedisTemplate.opsForValue().get(countKey);
        try {
            return count != null ? Integer.parseInt(count) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 构建 key（账号 + IP）
     */
    private String buildKey(String userAccount, String clientIp) {
        // 都转小写，避免大小写问题
        String account = userAccount != null ? userAccount.toLowerCase() : "unknown";
        String ip = clientIp != null ? clientIp : "unknown";
        return account + ":" + ip;
    }
}

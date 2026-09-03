package com.mao.maocodemother.service;

/**
 * 登录尝试服务
 * 防止暴力破解：连续失败达到阈值后临时锁定账号 + IP
 */
public interface LoginAttemptService {

    /**
     * 记录登录失败
     *
     * @param userAccount 账号
     * @param clientIp    客户端 IP
     */
    void recordFailure(String userAccount, String clientIp);

    /**
     * 记录登录成功（清除失败计数）
     *
     * @param userAccount 账号
     * @param clientIp    客户端 IP
     */
    void recordSuccess(String userAccount, String clientIp);

    /**
     * 检查是否被锁定
     *
     * @param userAccount 账号
     * @param clientIp    客户端 IP
     * @return true=被锁定
     */
    boolean isLocked(String userAccount, String clientIp);

    /**
     * 获取剩余锁定时间（秒）
     *
     * @param userAccount 账号
     * @param clientIp    客户端 IP
     * @return 剩余秒数，未锁定返回 0
     */
    long getRemainingLockSeconds(String userAccount, String clientIp);

    /**
     * 获取当前连续失败次数
     *
     * @param userAccount 账号
     * @param clientIp    客户端 IP
     * @return 失败次数
     */
    int getFailureCount(String userAccount, String clientIp);
}

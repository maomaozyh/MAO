package com.mao.maocodemother.utils;

import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 通用工具类
 * <p>
 * 基于 {@link StringRedisTemplate} 封装常用操作，value 统一以 JSON 字符串存储。
 * 用于直接操作 Redis 的场景（如登录态缓存、计数器、分布式锁前缀等），
 * 与 Spring Cache（@Cacheable）互补。
 */
@Component
public class RedisUtil {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 写入键值（JSON 序列化），带过期时间
     *
     * @param key       键
     * @param value     值（任意对象，序列化为 JSON）
     * @param ttlSeconds 过期秒数，<= 0 表示不过期
     */
    public void set(String key, Object value, long ttlSeconds) {
        String json = JSONUtil.toJsonStr(value);
        if (ttlSeconds > 0) {
            stringRedisTemplate.opsForValue().set(key, json, ttlSeconds, TimeUnit.SECONDS);
        } else {
            stringRedisTemplate.opsForValue().set(key, json);
        }
    }

    /**
     * 读取并反序列化为指定类型
     *
     * @param key  键
     * @param clazz 目标类型
     * @param <T>  类型
     * @return 反序列化后的对象，不存在或解析失败返回 null
     */
    public <T> T get(String key, Class<T> clazz) {
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        try {
            return JSONUtil.toBean(value, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 读取原始字符串值
     */
    public String getString(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 判断 key 是否存在
     */
    public Boolean hasKey(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    /**
     * 删除 key
     */
    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     * 设置过期时间
     *
     * @return 是否设置成功
     */
    public Boolean expire(String key, long ttlSeconds) {
        return stringRedisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * 原子自增（不存在则初始化为 0 后 +delta）
     *
     * @return 自增后的值
     */
    public Long increment(String key, long delta) {
        return stringRedisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 原子自增（步长 1）
     */
    public Long increment(String key) {
        return stringRedisTemplate.opsForValue().increment(key);
    }

    /**
     * 分布式锁式写入：仅当 key 不存在时设置，返回是否设置成功（用于频控）
     *
     * @return true 表示设置成功（首次），false 表示已存在
     */
    public Boolean setIfAbsent(String key, String value, long ttlSeconds) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue()
                .setIfAbsent(key, value, ttlSeconds, TimeUnit.SECONDS));
    }
}

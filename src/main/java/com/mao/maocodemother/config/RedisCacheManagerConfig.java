package com.mao.maocodemother.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 缓存管理器配置
 * <p>
 * 统一使用 JSON 序列化（带类型信息），避免 JDK 序列化要求所有缓存对象实现 Serializable 的限制，
 * 同时兼容任意返回类型的 @Cacheable 缓存。
 */
@Configuration
public class RedisCacheManagerConfig {

    @Resource
    private RedisConnectionFactory redisConnectionFactory;

    @Bean
    public CacheManager cacheManager() {
        // 配置 ObjectMapper 支持 Java8 时间类型
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        // 关键：必须显式开启 default typing，否则 GenericJackson2JsonRedisSerializer 写入的 JSON 不带 @class，
        // 反序列化时只能还原成 LinkedHashMap，@Cacheable 方法会抛 ClassCastException（接口 500）。
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);

        // 默认配置：key 用 String，value 用 JSON（带 @class 类型信息，可反序列化为任意类型）
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // 默认 30 分钟过期
                .disableCachingNullValues() // 禁用 null 值缓存
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                // 精选应用列表：5 分钟
                .withCacheConfiguration("good_app_page",
                        defaultConfig.entryTtl(Duration.ofMinutes(5)))
                // 应用详情：10 分钟
                .withCacheConfiguration("app_detail",
                        defaultConfig.entryTtl(Duration.ofMinutes(10)))
                // 管理员看板统计：1 分钟（统计类数据变化较慢，可较短缓存）
                .withCacheConfiguration("admin_dashboard",
                        defaultConfig.entryTtl(Duration.ofMinutes(1)))
                .build();
    }

    /**
     * 清理旧序列化格式的缓存，避免反序列化失败导致接口 500。
     * <p>
     * 缓存序列化格式变更（如补充 default typing）后，旧格式数据在反序列化时会变成
     * LinkedHashMap，导致 @Cacheable 方法抛 ClassCastException。启动时统一清理业务缓存前缀，
     * 保证缓存与当前序列化器一致。Redis 不可用时静默忽略，不影响启动。
     */
    @PostConstruct
    public void clearLegacyCaches() {
        String[] cachePatterns = {"good_app_page:*", "app_detail:*", "admin_dashboard:*"};
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            for (String pattern : cachePatterns) {
                ScanOptions options = ScanOptions.scanOptions().match(pattern).count(200).build();
                try (var cursor = connection.scan(options)) {
                    while (cursor.hasNext()) {
                        connection.del(cursor.next());
                    }
                }
            }
        } catch (Exception ignored) {
            // Redis 不可用时忽略
        }
    }
}

package com.mao.maocodemother.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * 全局跨域配置
 * <p>
 * 安全策略：
 * - 开发环境（未配置 cors.allowed-origins）：默认允许所有来源，便于本地开发
 * - 生产环境：必须在配置中指定允许的域名白名单，防止 CSRF
 */
@Configuration
@Slf4j
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:}")
    private String allowedOrigins;

    @Value("${cors.allow-all:false}")
    private boolean allowAll;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 覆盖所有请求
        var registration = registry.addMapping("/**")
                // 允许发送 Cookie
                .allowCredentials(true)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*")
                // 预检请求缓存时间（秒），减少 OPTIONS 请求次数
                .maxAge(3600);

        if (allowAll || (allowedOrigins == null || allowedOrigins.trim().isEmpty())) {
            // 开发模式：允许所有来源（用 patterns 兼容 allowCredentials）
            registration.allowedOriginPatterns("*");
            log.warn("⚠️ CORS 配置为允许所有来源（开发模式），生产环境请设置 cors.allowed-origins 白名单");
        } else {
            // 生产模式：白名单域名
            List<String> origins = Arrays.asList(allowedOrigins.split(","));
            // 去掉首尾空格
            origins = origins.stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            registration.allowedOriginPatterns(origins.toArray(new String[0]));
            log.info("✅ CORS 白名单已配置，允许来源：{}", origins);
        }
    }
}

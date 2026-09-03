package com.mao.maocodemother.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Session Cookie 配置
 * 解决跨端口、新开页面登录态共享问题
 */
@Configuration
public class SessionConfig {

    @Bean
    public DefaultCookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        // Cookie 名称
        serializer.setCookieName("SESSION");
        // Cookie 路径设为 /，确保全站共享（默认是 context-path /api）
        serializer.setCookiePath("/");
        // 使用 lax 模式，保证跨端口/新开页面时 cookie 能正常携带
        // 注意：SameSite=None 必须配合 Secure=true，本地 HTTP 环境下无法使用
        serializer.setSameSite("lax");
        // 开启 httpOnly，防止 XSS 窃取 cookie
        serializer.setUseHttpOnlyCookie(true);
        // Cookie 最大存活时间（秒），与 session 过期时间一致（30 天）
        serializer.setCookieMaxAge(2592000);
        return serializer;
    }
}

package com.mao.maocodemother.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密配置
 * 使用 BCrypt 算法（自带盐值、自适应强度）替代原 MD5 + 固定盐
 * <p>
 * BCrypt 优势：
 * 1. 自动生成随机盐，无需手动管理
 * 2. 可配置计算成本，抵御暴力破解
 * 3. 标准算法，行业广泛使用
 */
@Configuration
public class PasswordConfig {

    /**
     * BCrypt 密码编码器
     * strength 默认 10，可通过配置调整（数值越大越慢、越安全）
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}

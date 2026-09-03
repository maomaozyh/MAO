package com.mao.maocodemother.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 邮箱验证码配置（用于「邮箱找回密码」）。
 * <p>
 * 与 {@link SmsProperties} 保持一致的设计：默认 mock 模式，仅把验证码打印到 DEBUG 日志，
 * 不引入 {@code spring-boot-starter-mail} 依赖，保证离线环境可编译可运行；
 * 生产环境接入真实邮件服务时把 {@code mail.enabled} 置为 true 并在 {@code MailServiceImpl} 中补全发送逻辑。
 */
@Data
@Component
@ConfigurationProperties(prefix = "mail")
public class MailProperties {

    /**
     * 是否启用真实邮件发送，默认 false（mock 模式：验证码只打印日志）
     */
    private boolean enabled = false;
}

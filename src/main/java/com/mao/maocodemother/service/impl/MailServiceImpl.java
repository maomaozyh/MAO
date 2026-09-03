package com.mao.maocodemother.service.impl;

import com.mao.maocodemother.config.MailProperties;
import com.mao.maocodemother.service.MailService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 邮件服务实现（mock 模式）。
 * <p>
 * 默认 {@code mail.enabled=false}：验证码仅打印到 DEBUG 日志，供本地联调；
 * 真实邮件服务未接入前不阻塞业务，符合与短信同样的「mock 先行」策略。
 */
@Slf4j
@Service
public class MailServiceImpl implements MailService {

    @Resource
    private MailProperties mailProperties;

    @Override
    public void sendResetCodeMail(String email, String code) {
        if (mailProperties.isEnabled()) {
            // TODO: 接入真实邮件服务（JavaMailSender / 第三方 SMTP），发送包含验证码的找回密码邮件
            log.info("[MAIL] 已向 {} 发送重置密码验证码（真实邮件服务启用，待接入）", maskEmail(email));
        } else {
            // mock 模式：仅 DEBUG 级别打印验证码，供本地联调，生产不会输出
            log.debug("[MAIL][mock] 邮箱 {} 重置密码验证码 {}", maskEmail(email), code);
        }
    }

    /**
     * 日志脱敏：邮箱首字符保留、@ 之后全保留，中间打码（a***@qq.com），避免明文邮箱进入日志
     */
    private static String maskEmail(String email) {
        if (email == null) {
            return email;
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return email;
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}

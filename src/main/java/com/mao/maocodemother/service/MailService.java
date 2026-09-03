package com.mao.maocodemother.service;

/**
 * 邮件服务（用于「邮箱验证码找回密码」）。
 * <p>
 * 当前为 mock 实现：验证码只打印到 DEBUG 日志，不引入 JavaMailSender 依赖，保证离线可编译运行。
 * 生产接入真实邮件服务时，在 {@code MailServiceImpl#sendResetCodeMail} 中补全 SMTP / 第三方邮件发送逻辑。
 */
public interface MailService {

    /**
     * 发送「找回密码」验证码邮件。
     *
     * @param email 收件邮箱
     * @param code  6 位验证码
     */
    void sendResetCodeMail(String email, String code);
}

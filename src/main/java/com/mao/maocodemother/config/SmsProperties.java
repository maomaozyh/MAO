package com.mao.maocodemother.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 短信验证码配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {

    /**
     * 是否启用真实短信发送，默认 false（mock 模式：验证码只打印日志）
     */
    private boolean enabled = false;

    /**
     * 阿里云短信访问密钥 ID（真实发送时必填，对应环境变量 SMS_ACCESS_KEY_ID）
     */
    private String accessKeyId;

    /**
     * 阿里云短信访问密钥 Secret（真实发送时必填，对应环境变量 SMS_ACCESS_KEY_SECRET）
     */
    private String accessKeySecret;

    /**
     * 短信签名（阿里云控制台已审核签名，如「元知AI」，对应环境变量 SMS_SIGN_NAME）
     */
    private String signName;

    /**
     * 验证码短信模板 CODE（如 SMS_**********，对应环境变量 SMS_TEMPLATE_CODE）
     */
    private String templateCode;

    /**
     * 阿里云短信服务接入点，默认杭州公网接入点
     */
    private String endpoint = "dysmsapi.aliyuncs.com";
}

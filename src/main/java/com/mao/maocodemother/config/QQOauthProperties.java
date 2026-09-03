package com.mao.maocodemother.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * QQ 互联 OAuth2 扫码登录配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "oauth.qq")
public class QQOauthProperties {

    /**
     * QQ 互联应用 AppID
     */
    private String appId;

    /**
     * QQ 互联应用 AppSecret
     */
    private String appSecret;

    /**
     * 授权回调地址（需与 QQ 互联平台配置一致）
     */
    private String redirectUri;
}

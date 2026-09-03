package com.mao.maocodemother.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信开放平台 OAuth2 扫码登录配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "oauth.wechat")
public class WechatOauthProperties {

    /**
     * 微信开放平台应用 AppID
     */
    private String appId;

    /**
     * 微信开放平台应用 AppSecret
     */
    private String appSecret;

    /**
     * 授权回调地址（需与微信开放平台配置一致）
     */
    private String redirectUri;
}

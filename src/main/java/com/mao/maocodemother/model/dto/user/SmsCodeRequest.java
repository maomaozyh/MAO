package com.mao.maocodemother.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 发送短信验证码请求
 */
@Data
public class SmsCodeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 图形验证码标识（人机校验，公开发码接口必填）
     */
    private String captchaKey;

    /**
     * 图形验证码（用户输入）
     */
    private String captcha;
}

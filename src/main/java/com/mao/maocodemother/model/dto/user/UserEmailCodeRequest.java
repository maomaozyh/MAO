package com.mao.maocodemother.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 发送邮箱验证码（找回密码）请求
 */
@Data
public class UserEmailCodeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 图形验证码标识（人机校验，公开发码接口必填）
     */
    private String captchaKey;

    /**
     * 图形验证码（用户输入）
     */
    private String captcha;
}

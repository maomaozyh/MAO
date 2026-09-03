package com.mao.maocodemother.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 短信验证码登录请求
 */
@Data
public class SmsLoginRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 验证码
     */
    private String code;
}

package com.mao.maocodemother.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 手机号 + 验证码找回密码请求
 */
@Data
public class UserResetPasswordByPhoneRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 短信验证码
     */
    private String code;

    /**
     * 新密码
     */
    private String newPassword;

    /**
     * 确认新密码
     */
    private String checkPassword;
}

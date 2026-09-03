package com.mao.maocodemother.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 邮箱 + 验证码找回密码请求
 */
@Data
public class UserResetPasswordByEmailRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 邮箱验证码
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

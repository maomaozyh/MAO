package com.mao.maocodemother.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 绑定邮箱请求（登录态下设置用于找回密码的邮箱）
 */
@Data
public class UserBindEmailRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 邮箱验证码
     */
    private String code;
}

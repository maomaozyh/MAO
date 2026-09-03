package com.mao.maocodemother.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 手机号（登录二次验证用，与下方验证码配套）
     */
    private String phone;

    /**
     * 短信验证码（登录强制二次验证：已绑定手机则校验绑定号，未绑定则凭此验证码并自动绑定）
     */
    private String code;
}
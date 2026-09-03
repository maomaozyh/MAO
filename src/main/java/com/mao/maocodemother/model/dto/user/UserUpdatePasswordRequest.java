package com.mao.maocodemother.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户修改自己的密码请求（需要校验原密码）
 *
 * <p>与「管理员重置他人密码」不同：重置走 /user/reset/password，不需要原密码；
 * 本请求用于「系统设置 - 安全设置 - 修改密码」，必须先用原密码验证身份。
 */
@Data
public class UserUpdatePasswordRequest implements Serializable {

    /**
     * 原密码
     */
    private String oldPassword;

    /**
     * 新密码
     */
    private String newPassword;

    /**
     * 确认新密码
     */
    private String checkPassword;

    private static final long serialVersionUID = 1L;
}

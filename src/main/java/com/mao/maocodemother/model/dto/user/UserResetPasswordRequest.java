package com.mao.maocodemother.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理员重置用户密码请求
 */
@Data
public class UserResetPasswordRequest implements Serializable {

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 新密码
     */
    private String newPassword;

    private static final long serialVersionUID = 1L;
}

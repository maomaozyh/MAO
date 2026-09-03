package com.mao.maocodemother.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理员调整用户积分请求
 */
@Data
public class UserAdjustBalanceRequest implements Serializable {

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 调整积分数额（正数增加，负数扣除）
     */
    private Long amount;

    /**
     * 调整原因
     */
    private String reason;

    private static final long serialVersionUID = 1L;
}

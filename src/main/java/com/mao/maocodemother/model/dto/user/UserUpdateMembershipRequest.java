package com.mao.maocodemother.model.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理员修改用户会员等级请求
 */
@Data
public class UserUpdateMembershipRequest implements Serializable {

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 会员等级
     */
    private String membershipTier;

    /**
     * 会员到期时间
     */
    private LocalDateTime membershipExpireTime;

    private static final long serialVersionUID = 1L;
}

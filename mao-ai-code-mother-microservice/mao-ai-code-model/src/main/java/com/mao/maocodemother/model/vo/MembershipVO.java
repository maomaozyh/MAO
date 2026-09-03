package com.mao.maocodemother.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 当前用户会员与秒点信息
 */
@Data
public class MembershipVO implements Serializable {

    private String membershipTier;

    private String membershipTierName;

    private Long secondsBalance;

    private LocalDateTime membershipExpireTime;

    private static final long serialVersionUID = 1L;
}

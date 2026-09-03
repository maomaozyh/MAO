package com.mao.maocodemother.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 脱敏后的登录用户信息
 */
@Data
public class LoginUserVO implements Serializable {

    /**
     * 用户 id
     */
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 手机号（脱敏展示用，仅本人可见；不存在则为 null）
     */
    private String userPhone;

    /**
     * 邮箱（用于邮箱验证码找回密码，仅本人可见；不存在则为 null）
     */
    private String userEmail;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    /**
     * 会员等级：FREE / PROFESSIONAL / FLAGSHIP / ENTERPRISE_STANDARD / ENTERPRISE_SEAT
     */
    private String membershipTier;

    /**
     * 积分余额（购买所得）
     */
    private Long secondsBalance;

    /**
     * 赠送积分额度（注册赠送 / 会员每月发放），消费时优先扣除
     */
    private Long giftSecondsBalance;

    /**
     * 会员到期时间
     */
    private LocalDateTime membershipExpireTime;

    /**
     * 是否曾经付费购买过会员：用于「首购 8 折」判断。
     * true = 已买过会员（后续下单恢复原价）；false/null = 仍是首购，可享 8 折。
     */
    private Boolean hasPaidMembership;

    /**
     * 是否曾经付费购买过积分（增购积分 / SECONDS）：用于「首购 8 折」判断。
     * true = 已买过积分（后续下单恢复原价）；false/null = 仍是首购，可享 8 折。
     */
    private Boolean hasPaidPoints;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}
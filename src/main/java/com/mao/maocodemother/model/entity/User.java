package com.mao.maocodemother.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

import java.io.Serial;

import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户 实体类。
 *
 * @author <a href="https://github.com/liyupi">程序员mao</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 账号
     */
    @Column("userAccount")
    private String userAccount;

    /**
     * 密码
     */
    @Column("userPassword")
    private String userPassword;

    /**
     * 用户昵称
     */
    @Column("userName")
    private String userName;

    /**
     * 用户头像
     */
    @Column("userAvatar")
    private String userAvatar;

    /**
     * 用户简介
     */
    @Column("userProfile")
    private String userProfile;

    /**
     * 手机号
     */
    @Column("userPhone")
    private String userPhone;

    /**
     * 邮箱（用于邮箱验证码找回密码，可空）
     */
    @Column("userEmail")
    private String userEmail;

    /**
     * 微信 OpenID
     */
    @Column("wechatOpenId")
    private String wechatOpenId;

    /**
     * QQ OpenID
     */
    @Column("qqOpenId")
    private String qqOpenId;

    /**
     * 用户角色：user/admin
     */
    @Column("userRole")
    private String userRole;

    /**
     * 会员等级：FREE / PROFESSIONAL / FLAGSHIP / ENTERPRISE_STANDARD / ENTERPRISE_SEAT
     */
    @Column("membershipTier")
    private String membershipTier;

    /**
     * 积分余额
     */
    @Column("secondsBalance")
    private Long secondsBalance;

    /**
     * 会员到期时间
     */
    @Column("membershipExpireTime")
    private LocalDateTime membershipExpireTime;

    /**
     * 赠送积分额度（注册赠送 / 会员每月发放），消费时优先于 secondsBalance 扣除
     */
    @Column("giftSecondsBalance")
    private Long giftSecondsBalance;

    /**
     * 上次发放赠送积分的月份（yyyy-MM），保证每月只发放一次
     */
    @Column("lastGiftMonth")
    private String lastGiftMonth;

    /**
     * 上次签到日期（yyyy-MM-dd），每日签到幂等
     */
    @Column("lastCheckinDate")
    private String lastCheckinDate;

    /**
     * 编辑时间
     */
    @Column("editTime")
    private LocalDateTime editTime;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column("updateTime")
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;

}

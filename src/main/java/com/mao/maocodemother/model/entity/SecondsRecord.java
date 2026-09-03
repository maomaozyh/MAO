package com.mao.maocodemother.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 积分流水（购买入账 / 赠送发放 / AI 能力扣费 / 失败退回）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("seconds_record")
public class SecondsRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 用户 id
     */
    @Column("userId")
    private Long userId;

    /**
     * 变动积分（正数=获取，负数=消耗）
     */
    @Column("amount")
    private Long amount;

    /**
     * 变动后的购买余额
     */
    @Column("balanceAfter")
    private Long balanceAfter;

    /**
     * 变动后的赠送额度
     */
    @Column("giftAfter")
    private Long giftAfter;

    /**
     * 业务类型（见 SecondsBizTypeEnum）
     */
    @Column("bizType")
    private String bizType;

    /**
     * 业务描述
     */
    @Column("bizDesc")
    private String bizDesc;

    /**
     * 关联应用 id（可空）
     */
    @Column("appId")
    private Long appId;

    /**
     * 状态：0-有效 1-已退回（用于退款幂等）
     */
    @Column("status")
    private Integer status;

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

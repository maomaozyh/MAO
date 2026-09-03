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
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易订单（支付：渠道可插拔 MOCK 沙箱 / REAL 真实渠道）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("trade_order")
public class Order implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    @Column("orderNo")
    private String orderNo;

    @Column("userId")
    private Long userId;

    @Column("productType")
    private String productType;

    @Column("productCode")
    private String productCode;

    @Column("productName")
    private String productName;

    @Column("quantity")
    private Integer quantity;

    @Column("amount")
    private BigDecimal amount;

    @Column("currency")
    private String currency;

    @Column("status")
    private String status;

    @Column("channel")
    private String channel;

    @Column("payTradeNo")
    private String payTradeNo;

    @Column("expireTime")
    private LocalDateTime expireTime;

    @Column("payTime")
    private LocalDateTime payTime;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}

package com.mao.maocodemother.payment;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 支付渠道异步回调的标准化结果。
 */
@Data
public class OrderNotify implements Serializable {

    /**
     * 商户订单号
     */
    private String orderNo;

    /**
     * 渠道交易流水号
     */
    private String tradeNo;

    /**
     * 是否支付成功
     */
    private boolean success;

    /**
     * 实际支付金额（用于金额校验）
     */
    private BigDecimal amount;
}

package com.mao.maocodemother.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 积分余额视图（购买余额 + 赠送额度）
 */
@Data
public class SecondsBalanceVO implements Serializable {

    /**
     * 购买的积分余额
     */
    private Long secondsBalance;

    /**
     * 赠送的积分额度
     */
    private Long giftSecondsBalance;

    /**
     * 可用总积分
     */
    private Long totalSeconds;

    private static final long serialVersionUID = 1L;
}

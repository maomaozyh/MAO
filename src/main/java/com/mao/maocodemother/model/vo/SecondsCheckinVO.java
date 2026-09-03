package com.mao.maocodemother.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 签到结果视图
 */
@Data
public class SecondsCheckinVO implements Serializable {

    /**
     * 本次送出的积分数
     */
    private Long reward;

    /**
     * 今天是否已签到
     */
    private Boolean checkedToday;

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

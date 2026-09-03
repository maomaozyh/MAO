package com.mao.maocodemother.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 积分流水视图
 */
@Data
public class SecondsRecordVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 变动积分（正数=获取，负数=消耗）
     */
    private Long amount;

    /**
     * 变动后的购买余额
     */
    private Long balanceAfter;

    /**
     * 变动后的赠送额度
     */
    private Long giftAfter;

    /**
     * 业务类型（见 SecondsBizTypeEnum）
     */
    private String bizType;

    /**
     * 业务类型文案
     */
    private String bizTypeText;

    /**
     * 业务描述
     */
    private String bizDesc;

    /**
     * 关联应用 id
     */
    private Long appId;

    /**
     * 状态：0-有效 1-已退回
     */
    private Integer status;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    private static final long serialVersionUID = 1L;
}

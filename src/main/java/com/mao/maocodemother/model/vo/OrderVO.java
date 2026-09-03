package com.mao.maocodemother.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 订单视图对象
 */
@Data
public class OrderVO implements Serializable {

    private Long id;

    private String orderNo;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 用户账号（管理后台展示用）
     */
    private String userAccount;

    private String productType;

    private String productName;

    private Integer quantity;

    private BigDecimal amount;

    private String currency;

    private String status;

    private String channel;

    private LocalDateTime createTime;

    private LocalDateTime expireTime;

    private LocalDateTime payTime;

    /**
     * 收银台跳转地址（真实渠道创建支付后返回）
     */
    private String payUrl;

    /**
     * 支付二维码内容（扫码支付场景）
     */
    private String qrCode;

    /**
     * 渠道透传参数（如自动提交表单、预支付 id 等）
     */
    private Map<String, Object> channelParams;
}

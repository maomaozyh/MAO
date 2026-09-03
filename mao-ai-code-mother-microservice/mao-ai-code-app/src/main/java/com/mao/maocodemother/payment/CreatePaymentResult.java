package com.mao.maocodemother.payment;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 创建支付（下单）的返回结果，供前端拉起收银台。
 */
@Data
public class CreatePaymentResult implements Serializable {

    /**
     * 收银台跳转地址（H5 / PC 收银台 URL）
     */
    private String payUrl;

    /**
     * 支付二维码内容（扫码支付场景）
     */
    private String qrCode;

    /**
     * 渠道透传参数（如自动提交的 form 表单、签名串、预支付 id 等）
     */
    private Map<String, Object> params;

    /**
     * 渠道原始返回，便于调试 / 前端自定义跳转
     */
    private String raw;
}

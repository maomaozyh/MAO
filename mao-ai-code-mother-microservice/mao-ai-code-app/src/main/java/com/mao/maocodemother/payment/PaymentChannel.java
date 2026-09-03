package com.mao.maocodemother.payment;

import com.mao.maocodemother.model.entity.Order;

import java.util.Map;

/**
 * 支付渠道抽象。
 * 通过配置 {@code payment.channel} 切换实现：
 * - MOCK：沙箱，直接标记支付成功（默认）
 * - REAL：接真实支付 API（支付宝 / 微信 / Stripe 等），见 {@link RealPaymentChannel}
 */
public interface PaymentChannel {

    /**
     * 渠道标识（写入订单 channel 字段）。
     */
    String channel();

    /**
     * 创建支付（下单），返回收银台地址 / 二维码 / 透传参数等。
     * 对于 MOCK 沙箱可不返回跳转地址，由前端调用 mock-pay 完成。
     */
    CreatePaymentResult createPayment(Order order);

    /**
     * 解析并校验支付渠道异步回调，返回标准化的通知结果。
     */
    OrderNotify parseNotify(Map<String, String> params);

    /**
     * 校验异步回调签名。MOCK 直接返回 true，真实渠道需实现验签。
     */
    boolean verifyNotify(Map<String, String> params);
}

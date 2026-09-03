package com.mao.maocodemother.payment;

import com.mao.maocodemother.model.entity.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 真实支付渠道桩（接支付宝 / 微信支付 / Stripe 等）。
 * <p>
 * 启用方式：在 application.yml / 环境变量设置 {@code payment.channel=REAL}，
 * 并补全下方 {@code TODO} 三处方法（创建支付、解析回调、验签）。
 * 渠道所需的 appId / 密钥 / 网关地址等已通过 {@code payment.real.*} 注入，直接调用官方 SDK 即可。
 */
@Component
@ConditionalOnProperty(name = "payment.channel", havingValue = "REAL")
public class RealPaymentChannel implements PaymentChannel {

    @Value("${payment.real.appId:}")
    private String appId;

    @Value("${payment.real.appKey:}")
    private String appKey;

    @Value("${payment.real.gatewayUrl:}")
    private String gatewayUrl;

    @Value("${payment.real.notifyUrl:}")
    private String notifyUrl;

    @Override
    public String channel() {
        return "REAL";
    }

    @Override
    public CreatePaymentResult createPayment(Order order) {
        // TODO: 调用支付渠道「创建支付 / 统一下单」接口，返回收银台地址或二维码
        // 示例伪代码：
        //   SomePayClient client = SomePayClient.builder().appId(appId).appKey(appKey).gatewayUrl(gatewayUrl).build();
        //   PrepayResponse resp = client.createOrder(order.getOrderNo(), order.getAmount(), order.getProductName(), notifyUrl);
        //   CreatePaymentResult result = new CreatePaymentResult();
        //   result.setPayUrl(resp.getCashierUrl());   // PC / H5 跳转收银台
        //   result.setQrCode(resp.getCodeUrl());      // 扫码支付二维码
        //   result.setParams(resp.getExtra());        // 透传参数（如自动提交表单）
        //   return result;
        throw new UnsupportedOperationException("请在此接入真实支付渠道：实现 createPayment()");
    }

    @Override
    public OrderNotify parseNotify(Map<String, String> params) {
        // TODO: 解析渠道异步回调，返回标准化 OrderNotify（orderNo / tradeNo / success / amount）
        throw new UnsupportedOperationException("请在此接入真实支付渠道：实现 parseNotify()");
    }

    @Override
    public boolean verifyNotify(Map<String, String> params) {
        // TODO: 使用 appKey 校验回调签名（RSA / HMAC 等），验签失败返回 false
        throw new UnsupportedOperationException("请在此接入真实支付渠道：实现 verifyNotify()");
    }
}

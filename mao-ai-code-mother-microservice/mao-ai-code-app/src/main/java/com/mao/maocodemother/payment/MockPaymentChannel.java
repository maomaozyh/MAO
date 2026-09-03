package com.mao.maocodemother.payment;

import com.mao.maocodemother.model.entity.Order;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 沙箱支付渠道（默认）。
 * 不下发真实收银台，前端直接调用 {@code /payment/order/mock-pay} 完成支付。
 */
@Component
@ConditionalOnProperty(name = "payment.channel", havingValue = "MOCK", matchIfMissing = true)
public class MockPaymentChannel implements PaymentChannel {

    @Override
    public String channel() {
        return "MOCK";
    }

    @Override
    public CreatePaymentResult createPayment(Order order) {
        // 沙箱无需跳转，返回空结果；前端走 mock-pay 完成支付
        return new CreatePaymentResult();
    }

    @Override
    public OrderNotify parseNotify(Map<String, String> params) {
        OrderNotify notify = new OrderNotify();
        notify.setOrderNo(params.get("orderNo"));
        notify.setTradeNo("MOCK" + System.currentTimeMillis());
        notify.setSuccess(true);
        return notify;
    }

    @Override
    public boolean verifyNotify(Map<String, String> params) {
        return true;
    }
}

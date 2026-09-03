package com.mao.maocodemother.schedule;

import com.mao.maocodemother.service.OrderService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 扫描并关闭超时未支付的订单。
 */
@Component
public class OrderExpireScheduler {

    @Resource
    private OrderService orderService;

    @Scheduled(fixedRate = 60_000)
    public void expirePendingOrders() {
        orderService.expirePendingOrders();
    }
}

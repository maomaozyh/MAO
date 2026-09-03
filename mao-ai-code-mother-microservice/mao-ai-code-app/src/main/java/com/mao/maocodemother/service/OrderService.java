package com.mao.maocodemother.service;

import com.mao.maocodemother.model.dto.order.CreateOrderRequest;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.vo.OrderVO;

import java.util.List;
import java.util.Map;

/**
 * 订单服务
 */
public interface OrderService {

    /**
     * 创建订单（下单，状态 PENDING），并返回支付渠道的收银台参数
     */
    OrderVO createOrder(CreateOrderRequest request, User loginUser);

    /**
     * 模拟支付（沙箱：直接标记订单为已支付）
     */
    OrderVO mockPay(Long orderId, User loginUser);

    /**
     * 查询我的订单
     */
    OrderVO getOrder(Long orderId, User loginUser);

    /**
     * 分页查询我的订单
     */
    List<OrderVO> listMyOrders(User loginUser, long pageNum, long pageSize);

    /**
     * 处理支付渠道异步回调（验签 + 解析 + 幂等标记已支付）
     */
    OrderVO handleNotify(Map<String, String> params);

    /**
     * 关闭超时未支付订单
     */
    void expirePendingOrders();
}

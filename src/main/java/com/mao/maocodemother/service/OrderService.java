package com.mao.maocodemother.service;

import com.mybatisflex.core.paginate.Page;
import com.mao.maocodemother.model.dto.order.CreateOrderRequest;
import com.mao.maocodemother.model.dto.order.OrderQueryRequest;
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

    /**
     * 管理后台分页查询订单（支持按订单号 / 用户 / 商品类型 / 状态过滤）
     *
     * @param request 查询条件（含分页参数）
     * @return 订单分页
     */
    Page<OrderVO> listOrdersByPage(OrderQueryRequest request);

    /**
     * 管理后台查看订单详情
     *
     * @param orderId 订单 id
     * @return 订单详情
     */
    OrderVO adminGetOrder(Long orderId);

    /**
     * 管理后台取消订单（仅待支付状态可取消，取消后状态为 EXPIRED）
     *
     * @param orderId 订单 id
     * @return 是否成功
     */
    boolean adminCancelOrder(Long orderId);

    /**
     * 管理后台强制标记订单为已支付（线下转账等场景）
     *
     * @param orderId 订单 id
     * @return 订单详情
     */
    OrderVO adminMarkPaid(Long orderId);
}

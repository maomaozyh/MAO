package com.mao.maocodemother.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.exception.ThrowUtils;
import com.mao.maocodemother.innerservice.InnerUserService;
import com.mao.maocodemother.mapper.OrderMapper;
import com.mao.maocodemother.model.dto.order.CreateOrderRequest;
import com.mao.maocodemother.model.entity.Order;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.vo.OrderVO;
import com.mao.maocodemother.payment.CreatePaymentResult;
import com.mao.maocodemother.payment.OrderNotify;
import com.mao.maocodemother.payment.PaymentChannel;
import com.mao.maocodemother.product.ProductCatalog;
import com.mao.maocodemother.service.OrderService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * 订单服务实现（支付渠道可插拔：MOCK 沙箱 / REAL 真实渠道）
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_EXPIRED = "EXPIRED";

    @Resource
    private ProductCatalog productCatalog;

    @Resource
    private PaymentChannel paymentChannel;

    @DubboReference
    private InnerUserService innerUserService;

    @Override
    public OrderVO createOrder(CreateOrderRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null || request.getProductType() == null || request.getProductCode() == null,
                ErrorCode.PARAMS_ERROR, "商品信息不能为空");
        ProductCatalog.ProductInfo info = productCatalog.resolve(request.getProductType(), request.getProductCode());
        ThrowUtils.throwIf(info == null, ErrorCode.PARAMS_ERROR, "未知的商品");

        int quantity = request.getQuantity() == null || request.getQuantity() < 1 ? 1 : request.getQuantity();
        BigDecimal amount = info.amount().multiply(BigDecimal.valueOf(quantity));

        Order order = Order.builder()
                .orderNo("NO" + System.currentTimeMillis() + loginUser.getId())
                .userId(loginUser.getId())
                .productType(request.getProductType())
                .productCode(request.getProductCode())
                .productName(info.name())
                .quantity(quantity)
                .amount(amount)
                .currency("CNY")
                .status(STATUS_PENDING)
                .channel(paymentChannel.channel())
                .expireTime(LocalDateTime.now().plus(15, ChronoUnit.MINUTES))
                .build();
        boolean saved = save(order);
        ThrowUtils.throwIf(!saved, ErrorCode.SYSTEM_ERROR, "创建订单失败");

        OrderVO vo = toVO(order);
        CreatePaymentResult result = paymentChannel.createPayment(order);
        if (result != null) {
            vo.setPayUrl(result.getPayUrl());
            vo.setQrCode(result.getQrCode());
            vo.setChannelParams(result.getParams());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO mockPay(Long orderId, User loginUser) {
        Order order = getOrderEntity(orderId, loginUser);
        ThrowUtils.throwIf(STATUS_PAID.equals(order.getStatus()), ErrorCode.OPERATION_ERROR, "订单已支付");
        ThrowUtils.throwIf(STATUS_EXPIRED.equals(order.getStatus()), ErrorCode.OPERATION_ERROR, "订单已过期");
        markPaid(order, "MOCK" + System.currentTimeMillis());
        updateById(order);
        return toVO(order);
    }

    @Override
    public OrderVO getOrder(Long orderId, User loginUser) {
        return toVO(getOrderEntity(orderId, loginUser));
    }

    @Override
    public List<OrderVO> listMyOrders(User loginUser, long pageNum, long pageSize) {
        long currentPage = pageNum < 1 ? 1 : pageNum;
        long size = pageSize < 1 ? 10 : Math.min(pageSize, 50);
        List<Order> orders = list(QueryWrapper.create()
                .eq("userId", loginUser.getId())
                .orderBy("createTime", false)
                .limit((currentPage - 1) * size, size));
        return orders.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO handleNotify(Map<String, String> params) {
        ThrowUtils.throwIf(params == null || params.isEmpty(), ErrorCode.PARAMS_ERROR, "回调参数为空");
        if (!paymentChannel.verifyNotify(params)) {
            throw new com.mao.maocodemother.exception.BusinessException(ErrorCode.OPERATION_ERROR, "回调验签失败");
        }
        OrderNotify notify = paymentChannel.parseNotify(params);
        ThrowUtils.throwIf(notify == null || notify.getOrderNo() == null, ErrorCode.PARAMS_ERROR, "回调订单号缺失");
        Order order = getOne(QueryWrapper.create().eq(Order::getOrderNo, notify.getOrderNo()));
        ThrowUtils.throwIf(order == null, ErrorCode.NOT_FOUND_ERROR, "订单不存在");
        if (notify.getAmount() != null && order.getAmount().compareTo(notify.getAmount()) != 0) {
            throw new com.mao.maocodemother.exception.BusinessException(ErrorCode.OPERATION_ERROR, "回调金额不一致");
        }
        if (!notify.isSuccess()) {
            return toVO(order);
        }
        if (!STATUS_PAID.equals(order.getStatus())) {
            markPaid(order, notify.getTradeNo());
            updateById(order);
        }
        return toVO(order);
    }

    @Override
    public void expirePendingOrders() {
        List<Order> expiredOrders = list(QueryWrapper.create()
                .eq("status", STATUS_PENDING)
                .lt("expireTime", LocalDateTime.now()));
        if (expiredOrders.isEmpty()) {
            return;
        }
        for (Order order : expiredOrders) {
            order.setStatus(STATUS_EXPIRED);
        }
        updateBatch(expiredOrders);
    }

    private void markPaid(Order order, String tradeNo) {
        order.setStatus(STATUS_PAID);
        order.setPayTime(LocalDateTime.now());
        order.setPayTradeNo(tradeNo);
        innerUserService.fulfillOrder(
                order.getUserId(),
                order.getProductType(),
                order.getProductCode(),
                order.getQuantity() == null ? 1 : order.getQuantity());
    }

    private Order getOrderEntity(Long orderId, User loginUser) {
        Order order = getById(orderId);
        ThrowUtils.throwIf(order == null, ErrorCode.NOT_FOUND_ERROR, "订单不存在");
        ThrowUtils.throwIf(!loginUser.getId().equals(order.getUserId()), ErrorCode.NO_AUTH_ERROR, "无权操作该订单");
        return order;
    }

    private OrderVO toVO(Order o) {
        OrderVO vo = new OrderVO();
        vo.setId(o.getId());
        vo.setOrderNo(o.getOrderNo());
        vo.setProductType(o.getProductType());
        vo.setProductName(o.getProductName());
        vo.setQuantity(o.getQuantity());
        vo.setAmount(o.getAmount());
        vo.setCurrency(o.getCurrency());
        vo.setStatus(o.getStatus());
        vo.setChannel(o.getChannel());
        vo.setCreateTime(o.getCreateTime());
        vo.setExpireTime(o.getExpireTime());
        vo.setPayTime(o.getPayTime());
        return vo;
    }
}

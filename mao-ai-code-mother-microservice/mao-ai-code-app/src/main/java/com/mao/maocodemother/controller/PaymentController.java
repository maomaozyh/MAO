package com.mao.maocodemother.controller;

import com.mao.maocodemother.common.BaseResponse;
import com.mao.maocodemother.common.ResultUtils;
import com.mao.maocodemother.exception.BusinessException;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.innerservice.InnerUserService;
import com.mao.maocodemother.model.dto.order.CreateOrderRequest;
import com.mao.maocodemother.model.dto.order.MockPayRequest;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.vo.OrderVO;
import com.mao.maocodemother.service.OrderService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 支付（沙箱）接口
 */
@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Resource
    private OrderService orderService;

    @Value("${payment.channel:MOCK}")
    private String paymentChannel;

    /**
     * 创建订单
     */
    @PostMapping("/order/create")
    public BaseResponse<OrderVO> createOrder(@RequestBody CreateOrderRequest request, HttpServletRequest httpRequest) {
        User loginUser = InnerUserService.getLoginUser(httpRequest);
        return ResultUtils.success(orderService.createOrder(request, loginUser));
    }

    /**
     * 模拟支付（沙箱：直接标记已支付）
     * 仅在 MOCK 渠道下可用，防止生产环境被滥用
     */
    @PostMapping("/order/mock-pay")
    public BaseResponse<OrderVO> mockPay(@RequestBody MockPayRequest request, HttpServletRequest httpRequest) {
        if (!"MOCK".equalsIgnoreCase(paymentChannel)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前支付渠道不支持模拟支付");
        }
        User loginUser = InnerUserService.getLoginUser(httpRequest);
        return ResultUtils.success(orderService.mockPay(request.getId(), loginUser));
    }

    /**
     * 查询订单
     */
    @GetMapping("/order/get")
    public BaseResponse<OrderVO> getOrder(@RequestParam("id") Long id, HttpServletRequest httpRequest) {
        User loginUser = InnerUserService.getLoginUser(httpRequest);
        return ResultUtils.success(orderService.getOrder(id, loginUser));
    }

    /**
     * 我的订单列表
     */
    @GetMapping("/order/list")
    public BaseResponse<java.util.List<OrderVO>> listOrders(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
            HttpServletRequest httpRequest) {
        User loginUser = InnerUserService.getLoginUser(httpRequest);
        return ResultUtils.success(orderService.listMyOrders(loginUser, pageNum, pageSize));
    }

    /**
     * 支付渠道异步回调（验签 + 解析 + 幂等标记已支付，渠道由 payment.channel 决定）
     * /notify/mock 仅在 MOCK 渠道下开放，生产环境（REAL 渠道）走 /notify 由 RealPaymentChannel 验签
     */
    @PostMapping({"/notify", "/notify/mock"})
    public BaseResponse<Boolean> notify(@RequestBody Map<String, String> body,
                                        HttpServletRequest httpRequest) {
        // /notify/mock 端点仅在 MOCK 渠道下可用，防止生产环境被伪造支付回调
        String requestUri = httpRequest.getRequestURI();
        if (requestUri.endsWith("/notify/mock") && !"MOCK".equalsIgnoreCase(paymentChannel)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前支付渠道不支持模拟回调");
        }
        orderService.handleNotify(body);
        return ResultUtils.success(true);
    }
}

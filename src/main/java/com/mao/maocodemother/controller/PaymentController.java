package com.mao.maocodemother.controller;

import com.mybatisflex.core.paginate.Page;
import com.mao.maocodemother.annotation.AuthCheck;
import com.mao.maocodemother.common.BaseResponse;
import com.mao.maocodemother.common.ResultUtils;
import com.mao.maocodemother.constant.UserConstant;
import com.mao.maocodemother.exception.BusinessException;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.model.dto.order.CreateOrderRequest;
import com.mao.maocodemother.model.dto.order.MockPayRequest;
import com.mao.maocodemother.model.dto.order.OrderQueryRequest;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.vo.OrderVO;
import com.mao.maocodemother.service.OrderService;
import com.mao.maocodemother.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支付接口
 */
@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Resource
    private OrderService orderService;

    @Resource
    private UserService userService;

    @Value("${payment.channel:MOCK}")
    private String paymentChannel;

    /**
     * 创建订单
     */
    @PostMapping("/order/create")
    public BaseResponse<OrderVO> createOrder(@RequestBody CreateOrderRequest request, HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
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
        User loginUser = userService.getLoginUser(httpRequest);
        return ResultUtils.success(orderService.mockPay(request.getId(), loginUser));
    }

    /**
     * 查询订单
     */
    @GetMapping("/order/get")
    public BaseResponse<OrderVO> getOrder(@RequestParam("id") Long id, HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
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
        User loginUser = userService.getLoginUser(httpRequest);
        return ResultUtils.success(orderService.listMyOrders(loginUser, pageNum, pageSize));
    }

    /**
     * 管理后台分页查询全部订单（支持订单号 / 用户 / 商品类型 / 状态过滤，仅管理员）
     */
    @PostMapping("/order/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<OrderVO>> listOrdersByPage(@RequestBody OrderQueryRequest request) {
        return ResultUtils.success(orderService.listOrdersByPage(request));
    }

    /**
     * 管理后台查看订单详情（仅管理员）
     */
    @GetMapping("/order/admin/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<OrderVO> adminGetOrder(@RequestParam("id") Long id) {
        return ResultUtils.success(orderService.adminGetOrder(id));
    }

    /**
     * 管理后台取消订单（仅管理员，仅待支付状态可取消）
     */
    @PostMapping("/order/admin/cancel")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> adminCancelOrder(@RequestBody Map<String, Long> body) {
        Long id = body.get("id");
        return ResultUtils.success(orderService.adminCancelOrder(id));
    }

    /**
     * 管理后台强制标记订单已支付（仅管理员，线下转账等场景）
     */
    @PostMapping("/order/admin/mark-paid")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<OrderVO> adminMarkPaid(@RequestBody Map<String, Long> body) {
        Long id = body.get("id");
        return ResultUtils.success(orderService.adminMarkPaid(id));
    }

    /**
     * 支付渠道异步回调（验签 + 解析 + 幂等标记已支付，渠道由 payment.channel 决定）
     * /notify/mock 仅在 MOCK 渠道下开放，生产环境（REAL 渠道）走 /notify 由 RealPaymentChannel 验签。
     * 支付宝等渠道以 application/x-www-form-urlencoded 回调，故从 HttpServletRequest 读取参数。
     */
    @PostMapping({"/notify", "/notify/mock"})
    public BaseResponse<Boolean> notify(HttpServletRequest httpRequest) {
        // /notify/mock 端点仅在 MOCK 渠道下可用，防止生产环境被伪造支付回调
        String requestUri = httpRequest.getRequestURI();
        if (requestUri.endsWith("/notify/mock") && !"MOCK".equalsIgnoreCase(paymentChannel)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前支付渠道不支持模拟回调");
        }
        Map<String, String> body = new LinkedHashMap<>();
        httpRequest.getParameterMap().forEach((k, v) -> body.put(k, v != null && v.length > 0 ? v[0] : ""));
        orderService.handleNotify(body);
        return ResultUtils.success(true);
    }
}

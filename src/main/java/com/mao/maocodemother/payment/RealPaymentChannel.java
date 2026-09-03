package com.mao.maocodemother.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mao.maocodemother.exception.BusinessException;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.model.entity.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 真实支付渠道：支付宝（当面付 alipay.trade.precreate 扫码支付 + 异步回调验签）。
 * <p>
 * 启用方式：在 application.yml / 环境变量设置 {@code payment.channel=REAL}，并补全下方 {@code payment.alipay.*} 配置。
 * 采用手写 RSA2（SHA256withRSA）签名 / 验签（见 {@link AlipaySignatureUtil}），不依赖支付宝 SDK，便于离线编译。
 * 下单时同步调用支付宝预创建接口，直接返回可渲染的扫码二维码内容（qr_code）。
 */
@Component
@ConditionalOnProperty(name = "payment.channel", havingValue = "REAL")
public class RealPaymentChannel implements PaymentChannel {

    private static final String METHOD = "alipay.trade.precreate";
    private static final String SIGN_TYPE = "RSA2";
    private static final String VERSION = "1.0";
    private static final DateTimeFormatter ALIPAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${payment.alipay.appId:}")
    private String appId;

    @Value("${payment.alipay.appPrivateKey:}")
    private String appPrivateKey;

    @Value("${payment.alipay.alipayPublicKey:}")
    private String alipayPublicKey;

    @Value("${payment.alipay.gatewayUrl:https://openapi.alipay.com/gateway.do}")
    private String gatewayUrl;

    @Value("${payment.alipay.notifyUrl:}")
    private String notifyUrl;

    @Override
    public String channel() {
        return "REAL";
    }

    @Override
    public CreatePaymentResult createPayment(Order order) {
        checkConfigured();
        try {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("app_id", appId);
            params.put("method", METHOD);
            params.put("format", "JSON");
            params.put("charset", "utf-8");
            params.put("sign_type", SIGN_TYPE);
            params.put("timestamp", LocalDateTime.now().format(ALIPAY_TIME));
            params.put("version", VERSION);
            if (notifyUrl != null && !notifyUrl.isEmpty()) {
                params.put("notify_url", notifyUrl);
            }
            String totalAmount = order.getAmount() == null
                    ? "0.00"
                    : order.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
            Map<String, Object> biz = new LinkedHashMap<>();
            biz.put("out_trade_no", order.getOrderNo());
            biz.put("total_amount", totalAmount);
            biz.put("subject", order.getProductName() == null ? "秒哒订单" : order.getProductName());
            params.put("biz_content", objectMapper.writeValueAsString(biz));

            String sign = AlipaySignatureUtil.sign(params, appPrivateKey);
            params.put("sign", sign);

            // 当面付预创建：同步 POST 表单到网关，返回可渲染的扫码二维码
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<String> entity = new HttpEntity<>(toQueryString(params), headers);
            ResponseEntity<String> resp = restTemplate.postForEntity(gatewayUrl, entity, String.class);
            String qrCode = parseQrCode(resp.getBody());
            if (qrCode == null || qrCode.isEmpty()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "支付宝未返回扫码二维码");
            }

            CreatePaymentResult result = new CreatePaymentResult();
            result.setQrCode(qrCode);
            result.setRaw(resp.getBody());
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "支付宝下单失败：" + e.getMessage());
        }
    }

    /**
     * 解析支付宝预创建响应中的 qr_code：{"alipay_trade_precreate_response":{"code":"10000","qr_code":"...",...},"sign":"..."}
     */
    private String parseQrCode(String respBody) throws Exception {
        if (respBody == null || respBody.isEmpty()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "支付宝返回为空");
        }
        JsonNode root = objectMapper.readTree(respBody);
        JsonNode pre = root.get("alipay_trade_precreate_response");
        if (pre == null || !"10000".equals(pre.get("code").asText())) {
            String subMsg = pre == null ? "空响应" : (pre.get("sub_msg") != null ? pre.get("sub_msg").asText()
                    : pre.get("msg") != null ? pre.get("msg").asText() : "未知错误");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "支付宝预创建订单失败：" + subMsg);
        }
        return pre.get("qr_code").asText();
    }

    @Override
    public OrderNotify parseNotify(Map<String, String> params) {
        OrderNotify notify = new OrderNotify();
        notify.setOrderNo(params.get("out_trade_no"));
        notify.setTradeNo(params.get("trade_no"));
        String status = params.get("trade_status");
        notify.setSuccess("TRADE_SUCCESS".equals(status) || "TRADE_FINISHED".equals(status));
        String amount = params.get("total_amount");
        if (amount != null && !amount.isEmpty()) {
            try {
                notify.setAmount(new BigDecimal(amount));
            } catch (NumberFormatException ignored) {
                // 金额解析失败不影响订单号与成功状态
            }
        }
        return notify;
    }

    @Override
    public boolean verifyNotify(Map<String, String> params) {
        if (alipayPublicKey == null || alipayPublicKey.isEmpty()) {
            return false;
        }
        try {
            return AlipaySignatureUtil.verify(params, alipayPublicKey, params.get("sign"));
        } catch (Exception e) {
            return false;
        }
    }

    private void checkConfigured() {
        if (appId == null || appId.isEmpty() || appPrivateKey == null || appPrivateKey.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "支付宝支付未配置：请在 application.yml 补全 payment.alipay.appId / appPrivateKey");
        }
    }

    /**
     * 将参数拼接为 URL 编码的查询串（含 sign），用于表单提交到支付宝网关。
     */
    private String toQueryString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                sb.append('&');
            }
            sb.append(entry.getKey()).append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            first = false;
        }
        return sb.toString();
    }
}

package com.mao.maocodemother.product;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 商品目录（价格以服务端为准，下单时不信任前端传参）。
 * 覆盖：会员升级（个人/企业）、增购秒点、选购卡券。
 */
@Component
public class ProductCatalog {

    private final Map<String, ProductInfo> CATALOG = new HashMap<>();

    public ProductCatalog() {
        // 会员升级
        put("MEMBERSHIP", "PROFESSIONAL", "专业版会员", "14.90");
        put("MEMBERSHIP", "FLAGSHIP", "旗舰版会员", "65.00");
        put("MEMBERSHIP", "ENTERPRISE_STANDARD_MONTH", "企业标准版(月)", "299.00");
        put("MEMBERSHIP", "ENTERPRISE_STANDARD_YEAR", "企业标准版(年)", "3410.00");
        put("MEMBERSHIP", "ENTERPRISE_SEAT_MONTH", "企业成员席位(月)", "34.90");
        put("MEMBERSHIP", "ENTERPRISE_SEAT_YEAR", "企业成员席位(年)", "399.00");

        // 增购秒点
        put("SECONDS", "SEC_1000", "1000 秒点", "22");
        put("SECONDS", "SEC_2000", "2000 秒点", "44");
        put("SECONDS", "SEC_4000", "4000 秒点", "88");
        put("SECONDS", "SEC_8000", "8000 秒点", "176");
        put("SECONDS", "SEC_16000", "16000 秒点", "320");
        put("SECONDS", "SEC_28000", "28000 秒点", "560");
        put("SECONDS", "SEC_48000", "48000 秒点", "960");
        put("SECONDS", "SEC_72000", "72000 秒点", "1440");
        put("SECONDS", "SEC_100000", "100000 秒点", "2000");
        put("SECONDS", "SEC_124500", "124500 秒点", "2490");
        put("SECONDS", "SEC_190000", "190000 秒点", "3800");
        put("SECONDS", "SEC_260000", "260000 秒点", "5200");

        // 选购卡券 - 会员卡券
        put("CARD", "VIP_PERSONAL_PRO", "个人专业版卡券", "30");
        put("CARD", "VIP_PERSONAL_FLAGSHIP", "个人旗舰版卡券", "70");
        put("CARD", "ENTERPRISE_STANDARD_MONTH", "企业标准版卡券(月)", "299");
        put("CARD", "ENTERPRISE_STANDARD_YEAR", "企业标准版卡券(年)", "3410");
        put("CARD", "ENTERPRISE_SEAT_MONTH", "企业成员席位卡券(月)", "34.90");
        put("CARD", "ENTERPRISE_SEAT_YEAR", "企业成员席位卡券(年)", "399");
        // 选购卡券 - 秒点卡券
        put("CARD", "POINT_1000", "1000 秒点卡券", "22");
        put("CARD", "POINT_2000", "2000 秒点卡券", "44");
        put("CARD", "POINT_4000", "4000 秒点卡券", "88");
        put("CARD", "POINT_8000", "8000 秒点卡券", "176");
        put("CARD", "POINT_16000", "16000 秒点卡券", "320");
        put("CARD", "POINT_28000", "28000 秒点卡券", "560");
        put("CARD", "POINT_48000", "48000 秒点卡券", "960");
        put("CARD", "POINT_72000", "72000 秒点卡券", "1440");
    }

    public ProductInfo resolve(String productType, String productCode) {
        return CATALOG.get(productType + ":" + productCode);
    }

    public record ProductInfo(String name, BigDecimal amount) {
    }

    private void put(String type, String code, String name, String amount) {
        CATALOG.put(type + ":" + code, new ProductInfo(name, new BigDecimal(amount)));
    }
}

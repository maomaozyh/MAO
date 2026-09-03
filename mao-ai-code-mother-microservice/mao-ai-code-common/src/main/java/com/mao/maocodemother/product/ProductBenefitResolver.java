package com.mao.maocodemother.product;

import cn.hutool.core.util.StrUtil;

/**
 * 根据商品编码解析应发放的权益（会员等级、秒点数、有效期天数）。
 */
public final class ProductBenefitResolver {

    private ProductBenefitResolver() {
    }

    public static long resolveSeconds(String productCode) {
        if (StrUtil.isBlank(productCode)) {
            return 0;
        }
        if (productCode.startsWith("SEC_")) {
            return parseNumericSuffix(productCode.substring(4));
        }
        if (productCode.startsWith("POINT_")) {
            return parseNumericSuffix(productCode.substring(6));
        }
        return 0;
    }

    public static String resolveMembershipTier(String productCode) {
        if (StrUtil.isBlank(productCode)) {
            return "FREE";
        }
        return switch (productCode) {
            case "PROFESSIONAL", "VIP_PERSONAL_PRO" -> "PROFESSIONAL";
            case "FLAGSHIP", "VIP_PERSONAL_FLAGSHIP" -> "FLAGSHIP";
            case "ENTERPRISE_STANDARD_MONTH", "ENTERPRISE_STANDARD_YEAR" -> "ENTERPRISE_STANDARD";
            case "ENTERPRISE_SEAT_MONTH", "ENTERPRISE_SEAT_YEAR" -> "ENTERPRISE_SEAT";
            default -> "FREE";
        };
    }

    public static int resolveMembershipDays(String productCode) {
        if (StrUtil.isBlank(productCode)) {
            return 0;
        }
        if (productCode.endsWith("_YEAR")) {
            return 365;
        }
        if (productCode.endsWith("_MONTH")
                || "PROFESSIONAL".equals(productCode)
                || "FLAGSHIP".equals(productCode)
                || productCode.startsWith("VIP_")) {
            return 30;
        }
        return 0;
    }

    public static String resolveMembershipTierName(String tier) {
        if (StrUtil.isBlank(tier) || "FREE".equals(tier)) {
            return "个人免费版";
        }
        return switch (tier) {
            case "PROFESSIONAL" -> "专业版";
            case "FLAGSHIP" -> "旗舰版";
            case "ENTERPRISE_STANDARD" -> "企业标准版";
            case "ENTERPRISE_SEAT" -> "企业成员席位";
            default -> tier;
        };
    }

    private static long parseNumericSuffix(String suffix) {
        try {
            return Long.parseLong(suffix);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

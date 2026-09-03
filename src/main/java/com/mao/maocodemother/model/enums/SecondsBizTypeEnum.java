package com.mao.maocodemother.model.enums;

import lombok.Getter;

/**
 * 积分流水业务类型
 *
 * <p>priceKey 对应 sys_config 中 seconds.price 这个 JSON 配置里的键，
 * 为 null 表示该类型不参与扣费（购买 / 赠送 / 退回）。
 */
@Getter
public enum SecondsBizTypeEnum {

    PURCHASE("PURCHASE", "购买积分", null),
    GIFT("GIFT", "赠送积分", null),
    REFUND("REFUND", "失败退回", null),
    ADMIN_ADD("ADMIN_ADD", "管理员增加", null),
    ADMIN_DEDUCT("ADMIN_DEDUCT", "管理员扣除", null),

    GEN_CODE("GEN_CODE", "AI 生成应用", "genCode"),
    GEN_IMAGE("GEN_IMAGE", "AI 生成图片", "image"),
    GEN_VIDEO("GEN_VIDEO", "AI 生成视频", "video"),
    GEN_3D("GEN_3D", "AI 生成 3D 模型", "model3d"),
    GEN_PPT("GEN_PPT", "AI 生成 PPT", "ppt"),
    EXPAND("EXPAND", "描述扩写", "expand"),
    SEMANTIC_SEARCH("SEMANTIC_SEARCH", "语义搜索", "semanticSearch"),
    SELF_CHECK("SELF_CHECK", "代码自查", "selfCheck");

    private final String value;
    private final String text;
    private final String priceKey;

    SecondsBizTypeEnum(String value, String text, String priceKey) {
        this.value = value;
        this.text = text;
        this.priceKey = priceKey;
    }

    /**
     * 是否属于消耗类（需要扣费）
     */
    public boolean isConsume() {
        return priceKey != null;
    }

    public static SecondsBizTypeEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }
        for (SecondsBizTypeEnum anEnum : SecondsBizTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}

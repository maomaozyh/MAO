package com.mao.maocodemother.model.enums;

import lombok.Getter;

/**
 * 生成的外部素材类型（与 ExternalAssetType 对应，用于持久化到 generated_asset 表）
 *
 * @author mao
 */
@Getter
public enum AssetTypeEnum {

    IMAGE("图像", "image"),
    VIDEO("视频", "video"),
    MODEL_3D("3D 模型", "model_3d"),
    PPT("PPT 演示文稿", "ppt");

    private final String text;
    private final String value;

    AssetTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static AssetTypeEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }
        for (AssetTypeEnum anEnum : AssetTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}

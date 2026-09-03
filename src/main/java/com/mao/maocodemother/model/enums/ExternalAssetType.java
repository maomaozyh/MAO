package com.mao.maocodemother.model.enums;

import lombok.Getter;

/**
 * 应用分类对应的「外部素材生成类型」
 * <p>
 * 用于区分：该分类是走现有 DeepSeek 代码生成链路（NONE），
 * 还是需要调用外部 AI 服务产出真实素材文件（IMAGE / VIDEO / MODEL_3D 等）。
 * 后续接入视频、3D 时在此扩展即可。
 *
 * @author mao
 */
@Getter
public enum ExternalAssetType {

    NONE("无（走代码生成链路）", "none"),
    IMAGE("图像", "image"),
    VIDEO("视频", "video"),
    MODEL_3D("3D 模型", "model_3d"),
    PPT("PPT 演示文稿", "ppt");

    private final String text;
    private final String value;

    ExternalAssetType(String text, String value) {
        this.text = text;
        this.value = value;
    }
}

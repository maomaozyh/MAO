package com.mao.maocodemother.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用创建请求
 */
@Data
public class AppAddRequest implements Serializable {

    /**
     * 应用初始化的 prompt
     */
    private String initPrompt;

    /**
     * 应用分类（对应前端快捷入口，如 miniprogram/image/ppt 等），可选
     */
    private String category;

    /**
     * 使用的技能ID，可选
     */
    private Long skillId;

    /**
     * 是否公开：1-公开，0-私密（可选，默认公开）
     */
    private Integer isPublic;

    private static final long serialVersionUID = 1L;
} 
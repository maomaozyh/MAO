package com.mao.maocodemother.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新应用请求
 */
@Data
public class AppUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 是否公开：1-公开，0-私密（不传则保持原值）
     */
    private Integer isPublic;

    private static final long serialVersionUID = 1L;
} 
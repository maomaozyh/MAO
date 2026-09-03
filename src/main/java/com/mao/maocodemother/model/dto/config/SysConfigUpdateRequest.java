package com.mao.maocodemother.model.dto.config;

import lombok.Data;

import java.io.Serializable;

/**
 * 系统配置更新请求
 */
@Data
public class SysConfigUpdateRequest implements Serializable {

    /**
     * 配置键
     */
    private String configKey;

    /**
     * 配置值
     */
    private String configValue;

    private static final long serialVersionUID = 1L;
}

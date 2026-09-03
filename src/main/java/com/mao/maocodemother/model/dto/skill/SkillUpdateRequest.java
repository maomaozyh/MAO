package com.mao.maocodemother.model.dto.skill;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class SkillUpdateRequest implements Serializable {

    private Long id;

    private String skillName;

    private String skillCode;

    private String skillDesc;

    private String icon;

    private String category;

    private String price;

    private String originalPrice;

    private String priceUnit;

    private String tags;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 模型类型
     */
    private String modelType;

    /**
     * 采样温度
     */
    private BigDecimal temperature;

    /**
     * 功能介绍
     */
    private String featureDesc;

    /**
     * 使用说明
     */
    private String usageDesc;

    /**
     * MCP 服务配置（JSON 数组）
     */
    private String mcpServers;

    private Integer status;

    private static final long serialVersionUID = 1L;
}

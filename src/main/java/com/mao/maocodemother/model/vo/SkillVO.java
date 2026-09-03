package com.mao.maocodemother.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SkillVO implements Serializable {

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
     * 系统提示词（技能专属角色设定）
     */
    private String systemPrompt;

    /**
     * 模型类型：DEFAULT-默认 REASONING-推理
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

    private Long usageCount;

    private Integer status;

    private Long userId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private UserVO user;

    private static final long serialVersionUID = 1L;
}

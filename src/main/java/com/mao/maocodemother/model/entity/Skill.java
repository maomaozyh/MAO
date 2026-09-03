package com.mao.maocodemother.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("skill")
public class Skill implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    @Column("skillName")
    private String skillName;

    @Column("skillCode")
    private String skillCode;

    @Column("skillDesc")
    private String skillDesc;

    private String icon;

    private String category;

    private String price;

    @Column("originalPrice")
    private String originalPrice;

    @Column("priceUnit")
    private String priceUnit;

    private String tags;

    /**
     * 系统提示词（技能专属角色设定）
     */
    @Column("systemPrompt")
    private String systemPrompt;

    /**
     * 模型类型：DEFAULT-默认模型 REASONING-推理模型
     */
    @Column("modelType")
    private String modelType;

    /**
     * 采样温度 0-1，null 用模型默认值
     */
    private BigDecimal temperature;

    /**
     * 功能介绍
     */
    @Column("featureDesc")
    private String featureDesc;

    /**
     * 使用说明
     */
    @Column("usageDesc")
    private String usageDesc;

    /**
     * MCP 服务配置（JSON 数组，每个元素含 name/type/url/headers 等）
     */
    @Column("mcpServers")
    private String mcpServers;

    @Column("usageCount")
    private Long usageCount;

    private Integer status;

    @Column("userId")
    private Long userId;

    @Column("editTime")
    private LocalDateTime editTime;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}

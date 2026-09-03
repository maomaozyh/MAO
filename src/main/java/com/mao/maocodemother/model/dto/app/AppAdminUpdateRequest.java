package com.mao.maocodemother.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理员更新应用请求
 */
@Data
public class AppAdminUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用封面
     */
    private String cover;

    /**
     * 应用初始化的 prompt
     */
    private String initPrompt;

    /**
     * 代码生成类型（枚举）
     */
    private String codeGenType;

    /**
     * 技能ID
     */
    private Long skillId;

    /**
     * 应用分类
     */
    private String category;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 应用状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 是否公开：1-公开，0-私密（管理员可在后台切换）
     */
    private Integer isPublic;

    private static final long serialVersionUID = 1L;
} 

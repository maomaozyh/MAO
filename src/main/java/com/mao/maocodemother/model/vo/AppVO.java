package com.mao.maocodemother.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用封装类
 */
@Data
public class AppVO implements Serializable {

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
     * 使用的技能ID
     */
    private Long skillId;

    /**
     * 应用分类
     */
    private String category;

    /**
     * 部署标识
     */
    private String deployKey;

    /**
     * 部署时间
     */
    private LocalDateTime deployedTime;

    /**
     * 最近打开时间（用于"最近项目"排序与同步）
     */
    private LocalDateTime lastOpenTime;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 应用状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 是否公开：1-公开，0-私密
     */
    private Integer isPublic;

    /**
     * 创建用户id
     */
    private Long userId;

    /**
     * 编辑时间
     */
    private LocalDateTime editTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建用户信息
     */
    private UserVO user;

    /**
     * 绑定的技能信息（精简）
     */
    private SkillVO skill;

    private static final long serialVersionUID = 1L;
} 

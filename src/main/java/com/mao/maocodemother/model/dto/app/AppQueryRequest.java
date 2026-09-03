package com.mao.maocodemother.model.dto.app;

import com.mao.maocodemother.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class AppQueryRequest extends PageRequest implements Serializable {

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
     * 应用分类
     */
    private String category;

    /**
     * 技能ID
     */
    private Long skillId;

    /**
     * 部署标识
     */
    private String deployKey;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 应用状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 是否公开：1-公开，0-私密（用于广场/发现等只查公开应用的场景）
     */
    private Integer isPublic;

    /**
     * 创建用户id
     */
    private Long userId;

    /**
     * 创建时间-起始
     */
    private LocalDateTime createTimeStart;

    /**
     * 创建时间-截止
     */
    private LocalDateTime createTimeEnd;

    private static final long serialVersionUID = 1L;
} 

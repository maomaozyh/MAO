package com.mao.maocodemother.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;

import java.io.Serializable;
import java.time.LocalDateTime;

import java.io.Serial;

import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 应用 实体类。
 *
 * @author <a href="https://github.com/liyupi">程序员mao</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("app")
public class App implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 应用名称
     */
    @Column("appName")
    private String appName;

    /**
     * 应用封面
     */
    private String cover;

    /**
     * 应用初始化的 prompt
     */
    @Column("initPrompt")
    private String initPrompt;

    /**
     * 代码生成类型（枚举）
     */
    @Column("codeGenType")
    private String codeGenType;

    /**
     * 使用的技能ID
     */
    @Column("skillId")
    private Long skillId;

    /**
     * 应用分类（对应前端快捷入口，如 miniprogram/image/ppt 等）
     */
    @Column("category")
    private String category;

    /**
     * 部署标识
     */
    @Column("deployKey")
    private String deployKey;

    /**
     * 部署时间
     */
    @Column("deployedTime")
    private LocalDateTime deployedTime;

    /**
     * 最近打开时间（用于"最近项目"排序与同步，进入对话页时更新）
     */
    @Column("lastOpenTime")
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
     * 是否公开：1-公开，0-私密（私密应用仅本人与管理员可见，不出现在广场/发现）
     */
    @Column("isPublic")
    private Integer isPublic;

    /**
     * 创建用户id
     */
    @Column("userId")
    private Long userId;

    /**
     * 编辑时间
     */
    @Column("editTime")
    private LocalDateTime editTime;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column("updateTime")
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;

}

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

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统权限 实体类。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_permission")
public class SysPermission implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 权限ID
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 权限编码
     */
    @Column("permissionCode")
    private String permissionCode;

    /**
     * 权限名称
     */
    @Column("permissionName")
    private String permissionName;

    /**
     * 权限描述
     */
    @Column("description")
    private String description;

    /**
     * 类型：menu-菜单 button-按钮
     */
    @Column("type")
    private String type;

    /**
     * 父级ID
     */
    @Column("parentId")
    private Long parentId;

    /**
     * 排序
     */
    @Column("sortOrder")
    private Integer sortOrder;

    /**
     * 状态：0-禁用 1-启用
     */
    @Column("status")
    private Integer status;

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

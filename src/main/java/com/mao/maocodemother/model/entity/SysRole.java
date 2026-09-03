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
 * 系统角色 实体类。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_role")
public class SysRole implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 角色ID
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 角色编码
     */
    @Column("roleCode")
    private String roleCode;

    /**
     * 角色名称
     */
    @Column("roleName")
    private String roleName;

    /**
     * 角色描述
     */
    @Column("description")
    private String description;

    /**
     * 状态：0-禁用 1-启用
     */
    @Column("status")
    private Integer status;

    /**
     * 排序
     */
    @Column("sortOrder")
    private Integer sortOrder;

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

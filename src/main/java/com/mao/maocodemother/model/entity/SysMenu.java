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
 * 系统菜单 实体类。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_menu")
public class SysMenu implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 菜单ID
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 菜单名称
     */
    @Column("menuName")
    private String menuName;

    /**
     * 路由路径
     */
    @Column("menuPath")
    private String menuPath;

    /**
     * 组件路径
     */
    @Column("menuComponent")
    private String menuComponent;

    /**
     * 父级ID
     */
    @Column("parentId")
    private Long parentId;

    /**
     * 菜单图标
     */
    @Column("icon")
    private String icon;

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
     * 是否显示：0-隐藏 1-显示
     */
    @Column("visible")
    private Integer visible;

    /**
     * 重定向地址
     */
    @Column("redirect")
    private String redirect;

    /**
     * 关联权限编码
     */
    @Column("permissionCode")
    private String permissionCode;

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

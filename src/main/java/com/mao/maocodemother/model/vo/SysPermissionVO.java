package com.mao.maocodemother.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 权限 VO（支持树形结构）
 */
@Data
public class SysPermissionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String permissionCode;

    private String permissionName;

    private String description;

    private String type;

    private Long parentId;

    private Integer sortOrder;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 子权限
     */
    private List<SysPermissionVO> children;
}

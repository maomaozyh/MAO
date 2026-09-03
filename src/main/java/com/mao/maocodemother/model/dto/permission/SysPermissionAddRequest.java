package com.mao.maocodemother.model.dto.permission;

import lombok.Data;

import java.io.Serializable;

/**
 * 权限新增/编辑请求
 */
@Data
public class SysPermissionAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String permissionCode;

    private String permissionName;

    private String description;

    private String type;

    private Long parentId;

    private Integer sortOrder;

    private Integer status;
}

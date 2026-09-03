package com.mao.maocodemother.model.dto.role;

import lombok.Data;

import java.io.Serializable;

/**
 * 角色新增/编辑请求
 */
@Data
public class SysRoleAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String roleCode;

    private String roleName;

    private String description;

    private Integer status;

    private Integer sortOrder;
}

package com.mao.maocodemother.model.dto.role;

import lombok.Data;

import java.io.Serializable;

/**
 * 角色查询请求
 */
@Data
public class SysRoleQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String roleCode;

    private String roleName;

    private Integer status;

    private int pageNum = 1;

    private int pageSize = 10;

    private String sortField = "sortOrder";

    private String sortOrder = "ascend";
}

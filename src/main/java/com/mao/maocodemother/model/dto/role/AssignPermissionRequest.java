package com.mao.maocodemother.model.dto.role;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分配权限请求
 */
@Data
public class AssignPermissionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long roleId;

    private List<Long> permissionIds;
}

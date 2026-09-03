package com.mao.maocodemother.model.dto.role;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分配用户角色请求
 */
@Data
public class AssignUserRoleRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private List<Long> roleIds;
}

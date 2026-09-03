package com.mao.maocodemother.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色 VO
 */
@Data
public class SysRoleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String roleCode;

    private String roleName;

    private String description;

    private Integer status;

    private Integer sortOrder;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

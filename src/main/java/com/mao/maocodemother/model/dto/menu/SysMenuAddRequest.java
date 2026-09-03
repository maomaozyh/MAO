package com.mao.maocodemother.model.dto.menu;

import lombok.Data;

import java.io.Serializable;

/**
 * 菜单新增/编辑请求
 */
@Data
public class SysMenuAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String menuName;

    private String menuPath;

    private String menuComponent;

    private Long parentId;

    private String icon;

    private Integer sortOrder;

    private Integer status;

    private Integer visible;

    private String redirect;

    private String permissionCode;
}

package com.mao.maocodemother.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单 VO（支持树形结构）
 */
@Data
public class SysMenuVO implements Serializable {

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

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 子菜单
     */
    private List<SysMenuVO> children;
}

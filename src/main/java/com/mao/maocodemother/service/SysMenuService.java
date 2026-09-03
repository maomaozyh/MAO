package com.mao.maocodemother.service;

import com.mybatisflex.core.service.IService;
import com.mao.maocodemother.model.entity.SysMenu;

import java.util.List;

public interface SysMenuService extends IService<SysMenu> {

    /**
     * 获取所有菜单（树形结构）
     */
    List<SysMenu> getMenuTree();

    /**
     * 根据角色ID列表获取菜单列表
     */
    List<SysMenu> getMenusByRoleIds(List<Long> roleIds);

    /**
     * 根据用户ID获取菜单树
     */
    List<SysMenu> getMenuTreeByUserId(Long userId);
}

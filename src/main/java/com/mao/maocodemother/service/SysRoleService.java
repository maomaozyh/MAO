package com.mao.maocodemother.service;

import com.mybatisflex.core.service.IService;
import com.mao.maocodemother.model.entity.SysRole;
import com.mao.maocodemother.model.vo.SysRoleVO;

import java.util.List;

public interface SysRoleService extends IService<SysRole> {

    /**
     * 根据用户ID获取角色列表
     */
    List<SysRole> getRolesByUserId(Long userId);

    /**
     * 根据用户ID获取角色编码列表
     */
    List<String> getRoleCodesByUserId(Long userId);

    /**
     * 分配角色给用户
     */
    boolean assignRolesToUser(Long userId, List<Long> roleIds);

    /**
     * 获取角色的权限ID列表
     */
    List<Long> getPermissionIdsByRoleId(Long roleId);

    /**
     * 分配权限给角色
     */
    boolean assignPermissionsToRole(Long roleId, List<Long> permissionIds);

    /**
     * 获取角色的菜单ID列表
     */
    List<Long> getMenuIdsByRoleId(Long roleId);

    /**
     * 分配菜单给角色
     */
    boolean assignMenusToRole(Long roleId, List<Long> menuIds);

    /**
     * 转换为VO
     */
    SysRoleVO toVO(SysRole role);
}

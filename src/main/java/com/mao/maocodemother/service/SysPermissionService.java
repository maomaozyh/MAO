package com.mao.maocodemother.service;

import com.mybatisflex.core.service.IService;
import com.mao.maocodemother.model.entity.SysPermission;

import java.util.List;

public interface SysPermissionService extends IService<SysPermission> {

    /**
     * 根据角色ID列表获取权限列表
     */
    List<SysPermission> getPermissionsByRoleIds(List<Long> roleIds);

    /**
     * 根据用户ID获取权限编码列表
     */
    List<String> getPermissionCodesByUserId(Long userId);

    /**
     * 获取树形结构的权限列表
     */
    List<SysPermission> getPermissionTree();
}

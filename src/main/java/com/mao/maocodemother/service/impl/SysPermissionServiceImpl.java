package com.mao.maocodemother.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mao.maocodemother.mapper.SysPermissionMapper;
import com.mao.maocodemother.mapper.SysRolePermissionMapper;
import com.mao.maocodemother.mapper.SysUserRoleMapper;
import com.mao.maocodemother.model.entity.SysPermission;
import com.mao.maocodemother.model.entity.SysRolePermission;
import com.mao.maocodemother.model.entity.SysUserRole;
import com.mao.maocodemother.service.SysPermissionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission> implements SysPermissionService {

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Resource
    private SysRolePermissionMapper sysRolePermissionMapper;

    @Override
    public List<SysPermission> getPermissionsByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<SysRolePermission> rolePermissions = sysRolePermissionMapper.selectListByQuery(
            QueryWrapper.create().in("roleId", roleIds)
        );
        if (rolePermissions.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> permissionIds = rolePermissions.stream()
            .map(SysRolePermission::getPermissionId)
            .distinct()
            .collect(Collectors.toList());
        return this.listByIds(permissionIds);
    }

    @Override
    public List<String> getPermissionCodesByUserId(Long userId) {
        List<SysUserRole> userRoles = sysUserRoleMapper.selectListByQuery(
            QueryWrapper.create().eq("userId", userId)
        );
        if (userRoles.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> roleIds = userRoles.stream()
            .map(SysUserRole::getRoleId)
            .collect(Collectors.toList());
        List<SysPermission> permissions = getPermissionsByRoleIds(roleIds);
        return permissions.stream()
            .map(SysPermission::getPermissionCode)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public List<SysPermission> getPermissionTree() {
        List<SysPermission> all = this.list(
            QueryWrapper.create().orderBy("sortOrder", true).orderBy("id", true)
        );
        return buildTree(all);
    }

    private List<SysPermission> buildTree(List<SysPermission> all) {
        Map<Long, List<SysPermission>> parentMap = all.stream()
            .collect(Collectors.groupingBy(p -> p.getParentId() == null ? 0L : p.getParentId()));
        return parentMap.getOrDefault(0L, new ArrayList<>());
    }
}

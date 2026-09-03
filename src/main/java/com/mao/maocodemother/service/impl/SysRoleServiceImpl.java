package com.mao.maocodemother.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mao.maocodemother.mapper.SysPermissionMapper;
import com.mao.maocodemother.mapper.SysRoleMapper;
import com.mao.maocodemother.mapper.SysRoleMenuMapper;
import com.mao.maocodemother.mapper.SysRolePermissionMapper;
import com.mao.maocodemother.mapper.SysUserRoleMapper;
import com.mao.maocodemother.model.entity.SysPermission;
import com.mao.maocodemother.model.entity.SysRole;
import com.mao.maocodemother.model.entity.SysRoleMenu;
import com.mao.maocodemother.model.entity.SysRolePermission;
import com.mao.maocodemother.model.entity.SysUserRole;
import com.mao.maocodemother.model.vo.SysRoleVO;
import com.mao.maocodemother.service.SysRoleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Resource
    private SysRolePermissionMapper sysRolePermissionMapper;

    @Resource
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Resource
    private SysPermissionMapper sysPermissionMapper;

    @Override
    public List<SysRole> getRolesByUserId(Long userId) {
        List<SysUserRole> userRoles = sysUserRoleMapper.selectListByQuery(
            QueryWrapper.create().eq("userId", userId)
        );
        if (userRoles.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> roleIds = userRoles.stream()
            .map(SysUserRole::getRoleId)
            .collect(Collectors.toList());
        return this.listByIds(roleIds);
    }

    @Override
    public List<String> getRoleCodesByUserId(Long userId) {
        List<SysRole> roles = getRolesByUserId(userId);
        return roles.stream()
            .map(SysRole::getRoleCode)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignRolesToUser(Long userId, List<Long> roleIds) {
        sysUserRoleMapper.deleteByQuery(
            QueryWrapper.create().eq("userId", userId)
        );
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                SysUserRole userRole = SysUserRole.builder()
                    .userId(userId)
                    .roleId(roleId)
                    .build();
                sysUserRoleMapper.insert(userRole);
            }
        }
        return true;
    }

    @Override
    public List<Long> getPermissionIdsByRoleId(Long roleId) {
        List<SysRolePermission> rolePermissions = sysRolePermissionMapper.selectListByQuery(
            QueryWrapper.create().eq("roleId", roleId)
        );
        return rolePermissions.stream()
            .map(SysRolePermission::getPermissionId)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignPermissionsToRole(Long roleId, List<Long> permissionIds) {
        sysRolePermissionMapper.deleteByQuery(
            QueryWrapper.create().eq("roleId", roleId)
        );
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long permissionId : permissionIds) {
                SysRolePermission rolePermission = SysRolePermission.builder()
                    .roleId(roleId)
                    .permissionId(permissionId)
                    .build();
                sysRolePermissionMapper.insert(rolePermission);
            }
        }
        return true;
    }

    @Override
    public List<Long> getMenuIdsByRoleId(Long roleId) {
        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectListByQuery(
            QueryWrapper.create().eq("roleId", roleId)
        );
        return roleMenus.stream()
            .map(SysRoleMenu::getMenuId)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignMenusToRole(Long roleId, List<Long> menuIds) {
        sysRoleMenuMapper.deleteByQuery(
            QueryWrapper.create().eq("roleId", roleId)
        );
        if (menuIds != null && !menuIds.isEmpty()) {
            for (Long menuId : menuIds) {
                SysRoleMenu roleMenu = SysRoleMenu.builder()
                    .roleId(roleId)
                    .menuId(menuId)
                    .build();
                sysRoleMenuMapper.insert(roleMenu);
            }
        }
        return true;
    }

    @Override
    public SysRoleVO toVO(SysRole role) {
        if (role == null) {
            return null;
        }
        SysRoleVO vo = new SysRoleVO();
        vo.setId(role.getId());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleName(role.getRoleName());
        vo.setDescription(role.getDescription());
        vo.setStatus(role.getStatus());
        vo.setSortOrder(role.getSortOrder());
        vo.setCreateTime(role.getCreateTime());
        vo.setUpdateTime(role.getUpdateTime());
        return vo;
    }
}

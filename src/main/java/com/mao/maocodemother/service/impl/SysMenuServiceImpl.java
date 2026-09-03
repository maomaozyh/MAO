package com.mao.maocodemother.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mao.maocodemother.mapper.SysMenuMapper;
import com.mao.maocodemother.mapper.SysRoleMenuMapper;
import com.mao.maocodemother.mapper.SysUserRoleMapper;
import com.mao.maocodemother.model.entity.SysMenu;
import com.mao.maocodemother.model.entity.SysRoleMenu;
import com.mao.maocodemother.model.entity.SysUserRole;
import com.mao.maocodemother.model.vo.SysMenuVO;
import com.mao.maocodemother.service.SysMenuService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Resource
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Override
    public List<SysMenu> getMenuTree() {
        List<SysMenu> all = this.list(
            QueryWrapper.create()
                .eq("visible", 1)
                .orderBy("sortOrder", true)
                .orderBy("id", true)
        );
        return buildMenuTree(all);
    }

    @Override
    public List<SysMenu> getMenusByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectListByQuery(
            QueryWrapper.create().in("roleId", roleIds)
        );
        if (roleMenus.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> menuIds = roleMenus.stream()
            .map(SysRoleMenu::getMenuId)
            .distinct()
            .collect(Collectors.toList());
        return this.listByIds(menuIds);
    }

    @Override
    public List<SysMenu> getMenuTreeByUserId(Long userId) {
        List<SysUserRole> userRoles = sysUserRoleMapper.selectListByQuery(
            QueryWrapper.create().eq("userId", userId)
        );
        if (userRoles.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> roleIds = userRoles.stream()
            .map(SysUserRole::getRoleId)
            .collect(Collectors.toList());
        List<SysMenu> menus = getMenusByRoleIds(roleIds);
        menus = menus.stream()
            .filter(m -> m.getStatus() != null && m.getStatus() == 1)
            .filter(m -> m.getVisible() == null || m.getVisible() == 1)
            .collect(Collectors.toList());
        return buildMenuTree(menus);
    }

    private List<SysMenu> buildMenuTree(List<SysMenu> all) {
        Map<Long, List<SysMenu>> parentMap = all.stream()
            .collect(Collectors.groupingBy(m -> m.getParentId() == null ? 0L : m.getParentId()));
        return parentMap.getOrDefault(0L, new ArrayList<>());
    }

    public SysMenuVO toVO(SysMenu menu) {
        if (menu == null) return null;
        SysMenuVO vo = new SysMenuVO();
        vo.setId(menu.getId());
        vo.setMenuName(menu.getMenuName());
        vo.setMenuPath(menu.getMenuPath());
        vo.setMenuComponent(menu.getMenuComponent());
        vo.setParentId(menu.getParentId());
        vo.setIcon(menu.getIcon());
        vo.setSortOrder(menu.getSortOrder());
        vo.setStatus(menu.getStatus());
        vo.setVisible(menu.getVisible());
        vo.setRedirect(menu.getRedirect());
        vo.setPermissionCode(menu.getPermissionCode());
        vo.setCreateTime(menu.getCreateTime());
        vo.setUpdateTime(menu.getUpdateTime());
        return vo;
    }
}

package com.mao.maocodemother.controller;

import com.mybatisflex.core.query.QueryWrapper;
import com.mao.maocodemother.annotation.AuthCheck;
import com.mao.maocodemother.common.BaseResponse;
import com.mao.maocodemother.common.DeleteRequest;
import com.mao.maocodemother.common.ResultUtils;
import com.mao.maocodemother.constant.UserConstant;
import com.mao.maocodemother.exception.BusinessException;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.model.dto.menu.SysMenuAddRequest;
import com.mao.maocodemother.model.entity.SysMenu;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.vo.SysMenuVO;
import com.mao.maocodemother.service.SysMenuService;
import com.mao.maocodemother.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统菜单控制器
 */
@RestController
@RequestMapping("/admin/system/menu")
public class SysMenuController {

    @Resource
    private SysMenuService sysMenuService;

    @Resource
    private UserService userService;

    /**
     * 获取菜单树
     */
    @GetMapping("/tree")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<SysMenuVO>> getMenuTree() {
        List<SysMenu> all = sysMenuService.list(
            QueryWrapper.create()
                .eq("visible", 1)
                .orderBy("sortOrder", true)
                .orderBy("id", true)
        );
        List<SysMenuVO> allVO = all.stream().map(this::toVO).collect(Collectors.toList());
        return ResultUtils.success(buildVOTree(allVO));
    }

    /**
     * 获取当前用户的菜单树（动态菜单）
     */
    @GetMapping("/user/tree")
    public BaseResponse<List<SysMenuVO>> getUserMenuTree(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            return ResultUtils.success(new ArrayList<>());
        }
        // 管理员返回全部菜单
        if (UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            List<SysMenu> all = sysMenuService.list(
                QueryWrapper.create()
                    .eq("visible", 1)
                    .eq("status", 1)
                    .orderBy("sortOrder", true)
                    .orderBy("id", true)
            );
            List<SysMenuVO> allVO = all.stream().map(this::toVO).collect(Collectors.toList());
            return ResultUtils.success(buildVOTree(allVO));
        }
        // 普通用户按角色返回菜单
        List<SysMenu> menus = sysMenuService.getMenuTreeByUserId(loginUser.getId());
        List<SysMenuVO> voList = menus.stream().map(this::toVO).collect(Collectors.toList());
        return ResultUtils.success(voList);
    }

    /**
     * 获取所有菜单列表（平铺）
     */
    @GetMapping("/list/all")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<SysMenuVO>> listAllMenus() {
        List<SysMenu> list = sysMenuService.list(
            QueryWrapper.create().orderBy("sortOrder", true).orderBy("id", true)
        );
        List<SysMenuVO> voList = list.stream()
            .map(this::toVO)
            .collect(Collectors.toList());
        return ResultUtils.success(voList);
    }

    /**
     * 获取菜单详情
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<SysMenuVO> getMenuById(Long id) {
        SysMenu menu = sysMenuService.getById(id);
        return ResultUtils.success(toVO(menu));
    }

    /**
     * 新增菜单
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> addMenu(@RequestBody SysMenuAddRequest request) {
        if (request == null || request.getMenuName() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SysMenu menu = new SysMenu();
        BeanUtils.copyProperties(request, menu);
        if (menu.getStatus() == null) menu.setStatus(1);
        if (menu.getVisible() == null) menu.setVisible(1);
        if (menu.getSortOrder() == null) menu.setSortOrder(0);
        if (menu.getParentId() == null) menu.setParentId(0L);
        sysMenuService.save(menu);
        // 雪花 ID 超过 JS 安全整数范围，转字符串返回避免前端精度丢失
        return ResultUtils.success(String.valueOf(menu.getId()));
    }

    /**
     * 更新菜单
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateMenu(@RequestBody SysMenuAddRequest request) {
        if (request == null || request.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SysMenu old = sysMenuService.getById(request.getId());
        if (old == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        SysMenu update = new SysMenu();
        BeanUtils.copyProperties(request, update);
        boolean result = sysMenuService.updateById(update);
        return ResultUtils.success(result);
    }

    /**
     * 删除菜单
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteMenu(@RequestBody DeleteRequest request) {
        if (request == null || request.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long childCount = sysMenuService.count(
            QueryWrapper.create().eq("parentId", request.getId())
        );
        if (childCount > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "存在子菜单，无法删除");
        }
        boolean result = sysMenuService.removeById(request.getId());
        return ResultUtils.success(result);
    }

    private SysMenuVO toVO(SysMenu menu) {
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

    private List<SysMenuVO> buildVOTree(List<SysMenuVO> all) {
        Map<Long, List<SysMenuVO>> parentMap = all.stream()
            .collect(Collectors.groupingBy(m -> m.getParentId() == null ? 0L : m.getParentId()));
        List<SysMenuVO> roots = parentMap.getOrDefault(0L, new ArrayList<>());
        setChildren(roots, parentMap);
        return roots;
    }

    private void setChildren(List<SysMenuVO> nodes, Map<Long, List<SysMenuVO>> parentMap) {
        for (SysMenuVO node : nodes) {
            List<SysMenuVO> children = parentMap.get(node.getId());
            if (children != null && !children.isEmpty()) {
                node.setChildren(children);
                setChildren(children, parentMap);
            }
        }
    }
}

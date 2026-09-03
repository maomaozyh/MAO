package com.mao.maocodemother.controller;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mao.maocodemother.annotation.AuthCheck;
import com.mao.maocodemother.common.BaseResponse;
import com.mao.maocodemother.common.DeleteRequest;
import com.mao.maocodemother.common.ResultUtils;
import com.mao.maocodemother.constant.UserConstant;
import com.mao.maocodemother.exception.BusinessException;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.model.dto.role.AssignMenuRequest;
import com.mao.maocodemother.model.dto.role.AssignPermissionRequest;
import com.mao.maocodemother.model.dto.role.AssignUserRoleRequest;
import com.mao.maocodemother.model.dto.role.SysRoleAddRequest;
import com.mao.maocodemother.model.dto.role.SysRoleQueryRequest;
import com.mao.maocodemother.model.entity.SysRole;
import com.mao.maocodemother.model.vo.SysRoleVO;
import com.mao.maocodemother.service.SysRoleService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统角色控制器
 */
@RestController
@RequestMapping("/admin/system/role")
public class SysRoleController {

    @Resource
    private SysRoleService sysRoleService;

    /**
     * 分页查询角色
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<SysRoleVO>> listRoleByPage(@RequestBody SysRoleQueryRequest request) {
        QueryWrapper wrapper = QueryWrapper.create();
        if (request.getRoleCode() != null && !request.getRoleCode().isEmpty()) {
            wrapper.like("roleCode", request.getRoleCode());
        }
        if (request.getRoleName() != null && !request.getRoleName().isEmpty()) {
            wrapper.like("roleName", request.getRoleName());
        }
        if (request.getStatus() != null) {
            wrapper.eq("status", request.getStatus());
        }
        wrapper.orderBy("sortOrder", true).orderBy("id", false);
        Page<SysRole> page = sysRoleService.page(new Page<>(request.getPageNum(), request.getPageSize()), wrapper);
        Page<SysRoleVO> voPage = new Page<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow());
        voPage.setRecords(page.getRecords().stream()
            .map(sysRoleService::toVO)
            .collect(Collectors.toList()));
        return ResultUtils.success(voPage);
    }

    /**
     * 获取所有角色列表
     */
    @GetMapping("/list/all")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<SysRoleVO>> listAllRoles() {
        List<SysRole> roles = sysRoleService.list(
            QueryWrapper.create().orderBy("sortOrder", true).orderBy("id", true)
        );
        List<SysRoleVO> voList = roles.stream()
            .map(sysRoleService::toVO)
            .collect(Collectors.toList());
        return ResultUtils.success(voList);
    }

    /**
     * 获取角色详情
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<SysRoleVO> getRoleById(Long id) {
        SysRole role = sysRoleService.getById(id);
        return ResultUtils.success(sysRoleService.toVO(role));
    }

    /**
     * 新增角色
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> addRole(@RequestBody SysRoleAddRequest request) {
        if (request == null || request.getRoleCode() == null || request.getRoleName() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long count = sysRoleService.count(
            QueryWrapper.create().eq("roleCode", request.getRoleCode())
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "角色编码已存在");
        }
        SysRole role = new SysRole();
        BeanUtils.copyProperties(request, role);
        if (role.getStatus() == null) role.setStatus(1);
        if (role.getSortOrder() == null) role.setSortOrder(0);
        sysRoleService.save(role);
        // 雪花 ID 超过 JS 安全整数范围，转字符串返回避免前端精度丢失
        return ResultUtils.success(String.valueOf(role.getId()));
    }

    /**
     * 更新角色
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateRole(@RequestBody SysRoleAddRequest request) {
        if (request == null || request.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SysRole role = sysRoleService.getById(request.getId());
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (!role.getRoleCode().equals(request.getRoleCode())) {
            long count = sysRoleService.count(
                QueryWrapper.create().eq("roleCode", request.getRoleCode())
            );
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "角色编码已存在");
            }
        }
        SysRole updateRole = new SysRole();
        BeanUtils.copyProperties(request, updateRole);
        boolean result = sysRoleService.updateById(updateRole);
        return ResultUtils.success(result);
    }

    /**
     * 删除角色
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteRole(@RequestBody DeleteRequest request) {
        if (request == null || request.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SysRole role = sysRoleService.getById(request.getId());
        if (role != null && ("admin".equals(role.getRoleCode()) || "user".equals(role.getRoleCode()))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内置角色不可删除");
        }
        boolean result = sysRoleService.removeById(request.getId());
        return ResultUtils.success(result);
    }

    /**
     * 获取角色的权限ID列表
     */
    @GetMapping("/permission/list")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<Long>> getRolePermissionIds(Long roleId) {
        List<Long> permissionIds = sysRoleService.getPermissionIdsByRoleId(roleId);
        return ResultUtils.success(permissionIds);
    }

    /**
     * 分配权限给角色
     */
    @PostMapping("/permission/assign")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> assignPermissions(@RequestBody AssignPermissionRequest request) {
        if (request == null || request.getRoleId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = sysRoleService.assignPermissionsToRole(request.getRoleId(), request.getPermissionIds());
        return ResultUtils.success(result);
    }

    /**
     * 获取角色的菜单ID列表
     */
    @GetMapping("/menu/list")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<Long>> getRoleMenuIds(Long roleId) {
        List<Long> menuIds = sysRoleService.getMenuIdsByRoleId(roleId);
        return ResultUtils.success(menuIds);
    }

    /**
     * 分配菜单给角色
     */
    @PostMapping("/menu/assign")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> assignMenus(@RequestBody AssignMenuRequest request) {
        if (request == null || request.getRoleId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = sysRoleService.assignMenusToRole(request.getRoleId(), request.getMenuIds());
        return ResultUtils.success(result);
    }

    /**
     * 获取用户的角色ID列表
     */
    @GetMapping("/user/roles")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<Long>> getUserRoleIds(Long userId) {
        List<SysRole> roles = sysRoleService.getRolesByUserId(userId);
        List<Long> roleIds = roles.stream()
            .map(SysRole::getId)
            .collect(Collectors.toList());
        return ResultUtils.success(roleIds);
    }

    /**
     * 分配角色给用户
     */
    @PostMapping("/user/assign")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> assignUserRoles(@RequestBody AssignUserRoleRequest request) {
        if (request == null || request.getUserId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = sysRoleService.assignRolesToUser(request.getUserId(), request.getRoleIds());
        return ResultUtils.success(result);
    }
}

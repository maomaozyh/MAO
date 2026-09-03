package com.mao.maocodemother.controller;

import com.mybatisflex.core.query.QueryWrapper;
import com.mao.maocodemother.annotation.AuthCheck;
import com.mao.maocodemother.common.BaseResponse;
import com.mao.maocodemother.common.DeleteRequest;
import com.mao.maocodemother.common.ResultUtils;
import com.mao.maocodemother.constant.UserConstant;
import com.mao.maocodemother.exception.BusinessException;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.model.dto.permission.SysPermissionAddRequest;
import com.mao.maocodemother.model.entity.SysPermission;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.vo.SysPermissionVO;
import com.mao.maocodemother.service.SysPermissionService;
import com.mao.maocodemother.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统权限控制器
 */
@RestController
@RequestMapping("/admin/system/permission")
public class SysPermissionController {

    @Resource
    private SysPermissionService sysPermissionService;

    @Resource
    private UserService userService;

    /**
     * 获取当前登录用户的权限编码列表
     */
    @GetMapping("/my/codes")
    public BaseResponse<List<String>> getMyPermissionCodes(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            return ResultUtils.success(new ArrayList<>());
        }
        List<String> codes = sysPermissionService.getPermissionCodesByUserId(loginUser.getId());
        return ResultUtils.success(codes);
    }

    /**
     * 获取权限树
     */
    @GetMapping("/tree")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<SysPermission>> getPermissionTree() {
        List<SysPermission> tree = sysPermissionService.getPermissionTree();
        return ResultUtils.success(tree);
    }

    /**
     * 获取所有权限列表（平铺）
     */
    @GetMapping("/list/all")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<SysPermissionVO>> listAllPermissions() {
        List<SysPermission> list = sysPermissionService.list(
            QueryWrapper.create().orderBy("sortOrder", true).orderBy("id", true)
        );
        List<SysPermissionVO> voList = list.stream()
            .map(this::toVO)
            .collect(Collectors.toList());
        return ResultUtils.success(voList);
    }

    /**
     * 获取权限详情
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<SysPermissionVO> getPermissionById(Long id) {
        SysPermission permission = sysPermissionService.getById(id);
        return ResultUtils.success(toVO(permission));
    }

    /**
     * 新增权限
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> addPermission(@RequestBody SysPermissionAddRequest request) {
        if (request == null || request.getPermissionCode() == null || request.getPermissionName() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long count = sysPermissionService.count(
            QueryWrapper.create().eq("permissionCode", request.getPermissionCode())
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "权限编码已存在");
        }
        SysPermission permission = new SysPermission();
        BeanUtils.copyProperties(request, permission);
        if (permission.getStatus() == null) permission.setStatus(1);
        if (permission.getSortOrder() == null) permission.setSortOrder(0);
        if (permission.getParentId() == null) permission.setParentId(0L);
        if (permission.getType() == null) permission.setType("button");
        sysPermissionService.save(permission);
        // 雪花 ID 超过 JS 安全整数范围，转字符串返回避免前端精度丢失
        return ResultUtils.success(String.valueOf(permission.getId()));
    }

    /**
     * 更新权限
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePermission(@RequestBody SysPermissionAddRequest request) {
        if (request == null || request.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SysPermission old = sysPermissionService.getById(request.getId());
        if (old == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (!old.getPermissionCode().equals(request.getPermissionCode())) {
            long count = sysPermissionService.count(
                QueryWrapper.create().eq("permissionCode", request.getPermissionCode())
            );
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "权限编码已存在");
            }
        }
        SysPermission update = new SysPermission();
        BeanUtils.copyProperties(request, update);
        boolean result = sysPermissionService.updateById(update);
        return ResultUtils.success(result);
    }

    /**
     * 删除权限
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deletePermission(@RequestBody DeleteRequest request) {
        if (request == null || request.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long childCount = sysPermissionService.count(
            QueryWrapper.create().eq("parentId", request.getId())
        );
        if (childCount > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "存在子权限，无法删除");
        }
        boolean result = sysPermissionService.removeById(request.getId());
        return ResultUtils.success(result);
    }

    private SysPermissionVO toVO(SysPermission p) {
        if (p == null) return null;
        SysPermissionVO vo = new SysPermissionVO();
        vo.setId(p.getId());
        vo.setPermissionCode(p.getPermissionCode());
        vo.setPermissionName(p.getPermissionName());
        vo.setDescription(p.getDescription());
        vo.setType(p.getType());
        vo.setParentId(p.getParentId());
        vo.setSortOrder(p.getSortOrder());
        vo.setStatus(p.getStatus());
        vo.setCreateTime(p.getCreateTime());
        vo.setUpdateTime(p.getUpdateTime());
        return vo;
    }
}

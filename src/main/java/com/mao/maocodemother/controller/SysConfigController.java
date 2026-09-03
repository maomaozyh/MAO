package com.mao.maocodemother.controller;

import com.mao.maocodemother.annotation.AuthCheck;
import com.mao.maocodemother.common.BaseResponse;
import com.mao.maocodemother.common.ResultUtils;
import com.mao.maocodemother.constant.UserConstant;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.exception.ThrowUtils;
import com.mao.maocodemother.model.dto.config.SysConfigUpdateRequest;
import com.mao.maocodemother.model.entity.SysConfig;
import com.mao.maocodemother.service.SysConfigService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统设置 控制层。
 */
@RestController
@RequestMapping("/admin/config")
public class SysConfigController {

    @Resource
    private SysConfigService sysConfigService;

    /**
     * 列出所有系统配置（管理员）
     *
     * @return 配置列表
     */
    @GetMapping("/list")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<SysConfig>> listConfig() {
        return ResultUtils.success(sysConfigService.listAllConfig());
    }

    /**
     * 更新系统配置（管理员）
     *
     * @param sysConfigUpdateRequest 更新请求
     * @return 是否更新成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateConfig(@RequestBody SysConfigUpdateRequest sysConfigUpdateRequest) {
        ThrowUtils.throwIf(sysConfigUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        sysConfigService.updateConfig(sysConfigUpdateRequest.getConfigKey(), sysConfigUpdateRequest.getConfigValue());
        return ResultUtils.success(true);
    }
}

package com.mao.maocodemother.controller;

import com.mao.maocodemother.common.BaseResponse;
import com.mao.maocodemother.common.DeleteRequest;
import com.mao.maocodemother.common.ResultUtils;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.exception.ThrowUtils;
import com.mao.maocodemother.innerservice.InnerUserService;
import com.mao.maocodemother.model.dto.material.MaterialFolderAddRequest;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.vo.MaterialFolderVO;
import com.mao.maocodemother.service.MaterialFolderService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 素材文件夹 接口。
 */
@RestController
@RequestMapping("/materialFolder")
@Slf4j
public class MaterialFolderController {

    @Resource
    private MaterialFolderService materialFolderService;

    /**
     * 新建文件夹。
     */
    @PostMapping("/add")
    public BaseResponse<Long> addFolder(@RequestBody MaterialFolderAddRequest materialFolderAddRequest, HttpServletRequest request) {
        User loginUser = InnerUserService.getLoginUser(request);
        long id = materialFolderService.addFolder(materialFolderAddRequest.getName(), loginUser);
        return ResultUtils.success(id);
    }

    /**
     * 查询当前用户的文件夹列表。
     */
    @PostMapping("/list")
    public BaseResponse<List<MaterialFolderVO>> listMyFolders(HttpServletRequest request) {
        User loginUser = InnerUserService.getLoginUser(request);
        List<MaterialFolderVO> list = materialFolderService.listMyFolders(loginUser);
        return ResultUtils.success(list);
    }

    /**
     * 删除文件夹（素材不会被删除，仅解除归属）。
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteFolder(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        User loginUser = InnerUserService.getLoginUser(request);
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        boolean result = materialFolderService.deleteFolder(deleteRequest.getId(), loginUser);
        return ResultUtils.success(result);
    }
}

package com.mao.maocodemother.controller;

import com.mybatisflex.core.paginate.Page;
import com.mao.maocodemother.annotation.AuthCheck;
import com.mao.maocodemother.common.BaseResponse;
import com.mao.maocodemother.common.DeleteRequest;
import com.mao.maocodemother.common.ResultUtils;
import com.mao.maocodemother.constant.UserConstant;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.exception.ThrowUtils;
import com.mao.maocodemother.innerservice.InnerUserService;
import com.mao.maocodemother.model.dto.material.MaterialQueryRequest;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.vo.MaterialVO;
import com.mao.maocodemother.service.MaterialService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 素材接口。
 */
@RestController
@RequestMapping("/material")
public class MaterialController {

    @Resource
    private MaterialService materialService;

    /**
     * 上传素材（保存到 COS 并落库，仅本人）
     */
    @PostMapping("/upload")
    public BaseResponse<Long> uploadMaterial(@RequestParam("file") MultipartFile file,
                                             @RequestParam(value = "name", required = false) String name,
                                             @RequestParam(value = "folderId", required = false) Long folderId,
                                             HttpServletRequest request) {
        User loginUser = InnerUserService.getLoginUser(request);
        return ResultUtils.success(materialService.uploadMaterial(file, name, folderId, loginUser));
    }

    /**
     * 删除素材（本人 / 管理员）
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteMaterial(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = InnerUserService.getLoginUser(request);
        return ResultUtils.success(materialService.deleteMaterial(deleteRequest.getId(), loginUser));
    }

    /**
     * 根据 id 获取素材详情（公开）
     */
    @GetMapping("/get/vo")
    public BaseResponse<MaterialVO> getMaterialVOById(@RequestParam("id") Long id) {
        return ResultUtils.success(materialService.getMaterialVOById(id));
    }

    /**
     * 分页获取我的素材列表
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<MaterialVO>> listMaterialVOByPage(@RequestBody MaterialQueryRequest materialQueryRequest,
                                                               HttpServletRequest request) {
        User loginUser = InnerUserService.getLoginUser(request);
        materialQueryRequest.setUserId(loginUser.getId());
        return ResultUtils.success(materialService.listMaterialVOByPage(materialQueryRequest));
    }
}

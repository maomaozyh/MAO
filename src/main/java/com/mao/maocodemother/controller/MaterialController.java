package com.mao.maocodemother.controller;

import com.mybatisflex.core.paginate.Page;
import com.mao.maocodemother.common.BaseResponse;
import com.mao.maocodemother.common.DeleteRequest;
import com.mao.maocodemother.common.ResultUtils;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.exception.ThrowUtils;
import com.mao.maocodemother.model.dto.material.MaterialFolderAddRequest;
import com.mao.maocodemother.model.dto.material.MaterialQueryRequest;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.vo.MaterialFolderVO;
import com.mao.maocodemother.model.vo.MaterialVO;
import com.mao.maocodemother.service.MaterialFolderService;
import com.mao.maocodemother.service.MaterialService;
import com.mao.maocodemother.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 素材库接口（上传到 COS 并落库 / 我的素材分页 / 文件夹 CRUD）
 */
@RestController
@RequestMapping("/material")
public class MaterialController {

    @Resource
    private MaterialService materialService;

    @Resource
    private MaterialFolderService materialFolderService;

    @Resource
    private UserService userService;

    /**
     * 上传素材（multipart/form-data：file 必填，name/folderId 可选）
     */
    @PostMapping("/upload")
    public BaseResponse<String> uploadMaterial(@RequestPart("file") MultipartFile file,
                                             @RequestParam(value = "name", required = false) String name,
                                             @RequestParam(value = "folderId", required = false) Long folderId,
                                             HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long id = materialService.uploadMaterial(file, name, folderId, loginUser.getId());
        // 雪花 ID 超过 JS 安全整数范围，转字符串返回避免前端精度丢失
        return ResultUtils.success(String.valueOf(id));
    }

    /**
     * 删除素材
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteMaterial(@RequestBody DeleteRequest deleteRequest,
                                                HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null,
                ErrorCode.PARAMS_ERROR, "素材 id 不能为空");
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(materialService.deleteMaterial(deleteRequest.getId(), loginUser.getId()));
    }

    /**
     * 查询素材详情（本人或管理员）
     */
    @GetMapping("/get/vo")
    public BaseResponse<MaterialVO> getMaterialVOById(@RequestParam Long id,
                                                      HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(materialService.getMaterialVO(id, loginUser.getId()));
    }

    /**
     * 分页查询素材（默认查本人的）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<MaterialVO>> listMaterialVOByPage(@RequestBody MaterialQueryRequest queryRequest,
                                                               HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(materialService.listMaterialVOPage(queryRequest, loginUser.getId()));
    }

    /**
     * 新建文件夹
     */
    @PostMapping("/folder/add")
    public BaseResponse<String> addMaterialFolder(@RequestBody MaterialFolderAddRequest addRequest,
                                                HttpServletRequest request) {
        ThrowUtils.throwIf(addRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        User loginUser = userService.getLoginUser(request);
        Long id = materialFolderService.addFolder(addRequest.getName(), loginUser.getId());
        // 雪花 ID 超过 JS 安全整数范围，转字符串返回避免前端精度丢失
        return ResultUtils.success(String.valueOf(id));
    }

    /**
     * 查询我的文件夹列表
     */
    @GetMapping("/folder/list")
    public BaseResponse<List<MaterialFolderVO>> listMaterialFolder(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(materialFolderService.listUserFolders(loginUser.getId()));
    }

    /**
     * 删除文件夹
     */
    @PostMapping("/folder/delete")
    public BaseResponse<Boolean> deleteMaterialFolder(@RequestBody DeleteRequest deleteRequest,
                                                      HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null,
                ErrorCode.PARAMS_ERROR, "文件夹 id 不能为空");
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(materialFolderService.deleteFolder(deleteRequest.getId(), loginUser.getId()));
    }
}

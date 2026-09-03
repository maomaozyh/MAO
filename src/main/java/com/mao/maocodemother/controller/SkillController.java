package com.mao.maocodemother.controller;

import com.mybatisflex.core.paginate.Page;
import com.mao.maocodemother.annotation.AuthCheck;
import com.mao.maocodemother.common.BaseResponse;
import com.mao.maocodemother.common.DeleteRequest;
import com.mao.maocodemother.common.ResultUtils;
import com.mao.maocodemother.constant.UserConstant;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.exception.ThrowUtils;
import com.mao.maocodemother.model.dto.skill.SkillAddRequest;
import com.mao.maocodemother.model.dto.skill.SkillQueryRequest;
import com.mao.maocodemother.model.dto.skill.SkillUpdateRequest;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.vo.SkillVO;
import com.mao.maocodemother.service.SkillService;
import com.mao.maocodemother.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 技能接口（单体版，复用本地 UserService 取登录态）。
 */
@RestController
@RequestMapping("/skill")
public class SkillController {

    @Resource
    private SkillService skillService;

    @Resource
    private UserService userService;

    /**
     * 创建技能（登录用户均可创建）
     */
    @PostMapping("/add")
    public BaseResponse<String> addSkill(@RequestBody SkillAddRequest skillAddRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        // 雪花 ID 超过 JS 安全整数范围，转字符串返回避免前端精度丢失
        return ResultUtils.success(String.valueOf(skillService.createSkill(skillAddRequest, loginUser)));
    }

    /**
     * 上传技能文件并自动解析创建（.json / .zip / .skill）。
     * 解析失败会返回具体原因，便于用户修正后重新上传。
     */
    @PostMapping("/upload")
    public BaseResponse<String> uploadSkill(@RequestPart("file") MultipartFile file, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        // 雪花 ID 超过 JS 安全整数范围，转字符串返回避免前端精度丢失
        return ResultUtils.success(String.valueOf(skillService.uploadSkill(file, loginUser)));
    }

    /**
     * 更新技能（管理员 / 创建人）
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateSkill(@RequestBody SkillUpdateRequest skillUpdateRequest,
                                             HttpServletRequest request) {
        ThrowUtils.throwIf(skillUpdateRequest == null || skillUpdateRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(skillService.updateSkill(skillUpdateRequest, loginUser));
    }

    /**
     * 删除技能（管理员 / 创建人）
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSkill(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(skillService.deleteSkill(deleteRequest.getId(), loginUser));
    }

    /**
     * 根据 id 获取技能详情（公开）
     */
    @GetMapping("/get/vo")
    public BaseResponse<SkillVO> getSkillVOById(@RequestParam("id") Long id) {
        return ResultUtils.success(skillService.getSkillVOById(id));
    }

    /**
     * 分页获取技能列表（公开）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SkillVO>> listSkillVOByPage(@RequestBody SkillQueryRequest skillQueryRequest) {
        return ResultUtils.success(skillService.listSkillVOByPage(skillQueryRequest));
    }
}

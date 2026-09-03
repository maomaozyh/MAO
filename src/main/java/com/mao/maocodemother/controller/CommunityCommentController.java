package com.mao.maocodemother.controller;

import com.mybatisflex.core.paginate.Page;
import com.mao.maocodemother.common.BaseResponse;
import com.mao.maocodemother.common.DeleteRequest;
import com.mao.maocodemother.common.ResultUtils;
import com.mao.maocodemother.exception.ThrowUtils;
import com.mao.maocodemother.model.dto.community.CommunityCommentAddRequest;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.vo.CommunityCommentVO;
import com.mao.maocodemother.service.CommunityCommentService;
import com.mao.maocodemother.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/**
 * 社区评论 控制层。
 */
@RestController
@RequestMapping("/community/comment")
public class CommunityCommentController {

    @Resource
    private CommunityCommentService communityCommentService;

    @Resource
    private UserService userService;

    /**
     * 添加评论
     *
     * @param commentAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<String> addComment(@RequestBody CommunityCommentAddRequest commentAddRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long commentId = communityCommentService.addComment(commentAddRequest, loginUser);
        // 雪花 ID 超过 JS 安全整数范围，转字符串返回避免前端精度丢失
        return ResultUtils.success(String.valueOf(commentId));
    }

    /**
     * 删除评论
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteComment(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null, com.mao.maocodemother.exception.ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        boolean result = communityCommentService.deleteComment(deleteRequest.getId(), loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 分页获取帖子的评论列表
     *
     * @param postId
     * @param pageNum
     * @param pageSize
     * @param request
     * @return
     */
    @GetMapping("/list/page")
    public BaseResponse<Page<CommunityCommentVO>> listCommentByPage(
            @RequestParam Long postId,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            HttpServletRequest request) {
        ThrowUtils.throwIf(pageSize > 50, com.mao.maocodemother.exception.ErrorCode.PARAMS_ERROR, "每页最多 50 条");
        User loginUser = null;
        try {
            loginUser = userService.getLoginUser(request);
        } catch (Exception e) {
            // 未登录也可以查看
        }
        Page<CommunityCommentVO> commentVOPage = communityCommentService.getCommentVOPage(postId, pageNum, pageSize, loginUser);
        return ResultUtils.success(commentVOPage);
    }
}

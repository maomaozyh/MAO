package com.mao.maocodemother.controller;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mao.maocodemother.common.BaseResponse;
import com.mao.maocodemother.common.DeleteRequest;
import com.mao.maocodemother.common.ResultUtils;
import com.mao.maocodemother.exception.ThrowUtils;
import com.mao.maocodemother.model.dto.community.CommunityPostAddRequest;
import com.mao.maocodemother.model.dto.community.CommunityPostQueryRequest;
import com.mao.maocodemother.model.entity.CommunityPost;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.vo.CommunityPostVO;
import com.mao.maocodemother.service.CommunityPostService;
import com.mao.maocodemother.service.OperationLogService;
import com.mao.maocodemother.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import com.mao.maocodemother.annotation.AuthCheck;
import com.mao.maocodemother.constant.UserConstant;
import com.mao.maocodemother.model.dto.community.CommunityPostUpdateRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 社区帖子 控制层。
 */
@RestController
@RequestMapping("/community/post")
public class CommunityPostController {

    @Resource
    private CommunityPostService communityPostService;

    @Resource
    private UserService userService;

    @Resource
    private OperationLogService operationLogService;

    /**
     * 发布帖子
     *
     * @param postAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<String> addPost(@RequestBody CommunityPostAddRequest postAddRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long postId = communityPostService.createPost(postAddRequest, loginUser);
        // 雪花 ID 超过 JS 安全整数范围，转字符串返回避免前端精度丢失
        return ResultUtils.success(String.valueOf(postId));
    }

    /**
     * 删除帖子
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePost(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null, com.mao.maocodemother.exception.ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        CommunityPost oldPost = communityPostService.getById(deleteRequest.getId());
        boolean result = communityPostService.deletePost(deleteRequest.getId(), loginUser);
        // 记录操作日志
        try {
            operationLogService.recordSuccess(loginUser.getId(), loginUser.getUserName(), "post", "删除",
                    String.valueOf(deleteRequest.getId()),
                    "删除帖子：" + (oldPost != null ? oldPost.getTitle() : deleteRequest.getId()), request);
        } catch (Exception ignore) {
        }
        return ResultUtils.success(result);
    }

    /**
     * 获取帖子详情（浏览量 +1）
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get/detail")
    public BaseResponse<CommunityPostVO> getPostDetail(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, com.mao.maocodemother.exception.ErrorCode.PARAMS_ERROR);
        User loginUser = null;
        try {
            loginUser = userService.getLoginUser(request);
        } catch (Exception e) {
            // 未登录也可以查看
        }
        CommunityPostVO postVO = communityPostService.getPostDetail(id, loginUser);
        return ResultUtils.success(postVO);
    }

    /**
     * 根据 id 获取帖子
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<CommunityPostVO> getPostVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, com.mao.maocodemother.exception.ErrorCode.PARAMS_ERROR);
        CommunityPost post = communityPostService.getById(id);
        ThrowUtils.throwIf(post == null, com.mao.maocodemother.exception.ErrorCode.NOT_FOUND_ERROR);
        User loginUser = null;
        try {
            loginUser = userService.getLoginUser(request);
        } catch (Exception e) {
            // 未登录也可以查看
        }
        return ResultUtils.success(communityPostService.getPostVO(post, loginUser));
    }

    /**
     * 分页获取帖子列表（封装类）
     *
     * @param postQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/vo/page")
    public BaseResponse<Page<CommunityPostVO>> listPostVOByPage(@RequestBody CommunityPostQueryRequest postQueryRequest, HttpServletRequest request) {
        long current = postQueryRequest.getPageNum();
        long size = postQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 50, com.mao.maocodemother.exception.ErrorCode.PARAMS_ERROR, "每页最多 50 条");
        QueryWrapper queryWrapper = communityPostService.getQueryWrapper(postQueryRequest);
        Page<CommunityPost> postPage = communityPostService.page(new Page<>(current, size), queryWrapper);

        User loginUser = null;
        try {
            loginUser = userService.getLoginUser(request);
        } catch (Exception e) {
            // 未登录也可以查看
        }

        List<CommunityPostVO> postVOList = communityPostService.getPostVOList(postPage.getRecords(), loginUser);
        Page<CommunityPostVO> postVOPage = new Page<>(current, size, postPage.getTotalRow());
        postVOPage.setRecords(postVOList);
        return ResultUtils.success(postVOPage);
    }

    /**
     * 获取我发布的帖子
     *
     * @param postQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/vo/page")
    public BaseResponse<Page<CommunityPostVO>> listMyPostVOByPage(@RequestBody CommunityPostQueryRequest postQueryRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        postQueryRequest.setUserId(loginUser.getId());
        return listPostVOByPage(postQueryRequest, request);
    }

    /**
     * 我赞过的帖子列表（按点赞时间倒序）
     *
     * @param pageNum
     * @param pageSize
     * @param request
     * @return
     */
    @GetMapping("/my/liked/page")
    public BaseResponse<Page<CommunityPostVO>> listMyLikedPostVOByPage(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<CommunityPostVO> postVOPage = communityPostService.listMyLikedPostVOByPage(loginUser, pageNum, pageSize);
        return ResultUtils.success(postVOPage);
    }

    /**
     * 我的足迹列表（按浏览时间倒序）
     *
     * @param pageNum
     * @param pageSize
     * @param request
     * @return
     */
    @GetMapping("/my/footprint/page")
    public BaseResponse<Page<CommunityPostVO>> listMyFootprintPostVOByPage(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<CommunityPostVO> postVOPage = communityPostService.listMyFootprintPostVOByPage(loginUser, pageNum, pageSize);
        return ResultUtils.success(postVOPage);
    }

    /**
     * 点赞 / 取消点赞
     *
     * @param postId
     * @param request
     * @return
     */
    @PostMapping("/like/toggle")
    public BaseResponse<Boolean> toggleLike(@RequestParam Long postId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        boolean liked = communityPostService.toggleLike(postId, loginUser);
        return ResultUtils.success(liked);
    }

    // ==================== 管理员接口 ====================

    /**
     * 管理员更新帖子
     */
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> adminUpdatePost(@RequestBody CommunityPostUpdateRequest updateRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(updateRequest == null || updateRequest.getId() == null,
                com.mao.maocodemother.exception.ErrorCode.PARAMS_ERROR);
        boolean result = communityPostService.adminUpdatePost(updateRequest);
        // 记录操作日志（含审核：status 变更）
        try {
            User loginUser = userService.getLoginUser(request);
            String operation = updateRequest.getStatus() != null ? "审核" : "编辑";
            operationLogService.recordSuccess(loginUser.getId(), loginUser.getUserName(), "post", operation,
                    String.valueOf(updateRequest.getId()),
                    "管理员" + operation + "帖子：" + updateRequest.getTitle(), request);
        } catch (Exception ignore) {
        }
        return ResultUtils.success(result);
    }

    /**
     * 管理员删除帖子
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> adminDeletePost(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null,
                com.mao.maocodemother.exception.ErrorCode.PARAMS_ERROR);
        CommunityPost oldPost = communityPostService.getById(deleteRequest.getId());
        boolean result = communityPostService.removeById(deleteRequest.getId());
        // 记录操作日志
        try {
            User loginUser = userService.getLoginUser(request);
            operationLogService.recordSuccess(loginUser.getId(), loginUser.getUserName(), "post", "管理员删除",
                    String.valueOf(deleteRequest.getId()),
                    "管理员删除帖子：" + (oldPost != null ? oldPost.getTitle() : deleteRequest.getId()), request);
        } catch (Exception ignore) {
        }
        return ResultUtils.success(result);
    }

    /**
     * 管理员分页获取帖子列表
     */
    @PostMapping("/admin/list/vo/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<CommunityPostVO>> adminListPostVOByPage(
            @RequestBody CommunityPostQueryRequest postQueryRequest,
            HttpServletRequest request) {
        long current = postQueryRequest.getPageNum();
        long size = postQueryRequest.getPageSize();
        // 管理员查询所有状态的帖子
        postQueryRequest.setAllStatus(true);
        QueryWrapper queryWrapper = communityPostService.getQueryWrapper(postQueryRequest);
        Page<CommunityPost> postPage = communityPostService.page(new Page<>(current, size), queryWrapper);

        User loginUser = null;
        try {
            loginUser = userService.getLoginUser(request);
        } catch (Exception e) {
            // ignore
        }

        List<CommunityPostVO> postVOList = communityPostService.getPostVOList(postPage.getRecords(), loginUser);
        Page<CommunityPostVO> postVOPage = new Page<>(current, size, postPage.getTotalRow());
        postVOPage.setRecords(postVOList);
        return ResultUtils.success(postVOPage);
    }
}

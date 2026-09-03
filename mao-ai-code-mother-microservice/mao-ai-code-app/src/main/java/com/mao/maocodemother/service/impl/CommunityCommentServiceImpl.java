package com.mao.maocodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mao.maocodemother.exception.BusinessException;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.exception.ThrowUtils;
import com.mao.maocodemother.innerservice.InnerUserService;
import com.mao.maocodemother.mapper.CommunityCommentMapper;
import com.mao.maocodemother.mapper.CommunityPostMapper;
import com.mao.maocodemother.model.dto.community.CommunityCommentAddRequest;
import com.mao.maocodemother.model.entity.CommunityComment;
import com.mao.maocodemother.model.entity.CommunityPost;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.enums.UserRoleEnum;
import com.mao.maocodemother.model.vo.CommunityCommentVO;
import com.mao.maocodemother.model.vo.UserVO;
import com.mao.maocodemother.service.CommunityCommentService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 社区评论 服务层实现。
 */
@Service
@Slf4j
public class CommunityCommentServiceImpl extends ServiceImpl<CommunityCommentMapper, CommunityComment> implements CommunityCommentService {

    @DubboReference
    private InnerUserService userService;

    @Resource
    private CommunityPostMapper communityPostMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addComment(CommunityCommentAddRequest commentAddRequest, User loginUser) {
        ThrowUtils.throwIf(commentAddRequest == null, ErrorCode.PARAMS_ERROR);
        Long postId = commentAddRequest.getPostId();
        String content = commentAddRequest.getContent();

        ThrowUtils.throwIf(postId == null || postId <= 0, ErrorCode.PARAMS_ERROR, "帖子 ID 错误");
        ThrowUtils.throwIf(StrUtil.isBlank(content), ErrorCode.PARAMS_ERROR, "评论内容不能为空");
        ThrowUtils.throwIf(content.length() > 1000, ErrorCode.PARAMS_ERROR, "评论内容不能超过 1000 字");

        // 检查帖子是否存在
        CommunityPost post = communityPostMapper.selectOneById(postId);
        ThrowUtils.throwIf(post == null, ErrorCode.NOT_FOUND_ERROR, "帖子不存在");

        // 如果有 parentId，检查父评论是否存在
        Long parentId = commentAddRequest.getParentId();
        if (parentId != null && parentId > 0) {
            CommunityComment parentComment = this.getById(parentId);
            ThrowUtils.throwIf(parentComment == null, ErrorCode.NOT_FOUND_ERROR, "父评论不存在");
        } else {
            // 一级评论默认 parentId = 0
            parentId = 0L;
        }

        // 构造评论
        CommunityComment comment = CommunityComment.builder()
                .postId(postId)
                .content(content.trim())
                .parentId(parentId)
                .userId(loginUser.getId())
                .createTime(LocalDateTime.now())
                .build();

        boolean result = this.save(comment);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "评论失败");

        // 更新帖子评论数
        post.setCommentCount(post.getCommentCount() + 1);
        communityPostMapper.update(post);

        log.info("评论发布成功，ID: {}, 帖子ID: {}", comment.getId(), postId);
        return comment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteComment(Long commentId, User loginUser) {
        ThrowUtils.throwIf(commentId == null || commentId <= 0, ErrorCode.PARAMS_ERROR, "评论 ID 错误");
        CommunityComment comment = this.getById(commentId);
        ThrowUtils.throwIf(comment == null, ErrorCode.NOT_FOUND_ERROR, "评论不存在");

        // 仅本人或管理员可删除
        if (!comment.getUserId().equals(loginUser.getId()) && !UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限删除");
        }

        boolean result = this.removeById(commentId);
        if (result) {
            // 更新帖子评论数
            CommunityPost post = communityPostMapper.selectOneById(comment.getPostId());
            if (post != null) {
                post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
                communityPostMapper.update(post);
            }
        }
        return result;
    }

    @Override
    public Page<CommunityCommentVO> getCommentVOPage(Long postId, long pageNum, long pageSize, User loginUser) {
        ThrowUtils.throwIf(postId == null || postId <= 0, ErrorCode.PARAMS_ERROR, "帖子 ID 错误");

        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("postId", postId)
                .eq("parentId", 0) // 只查一级评论，回复可以后续展开
                .orderBy("createTime", false);

        Page<CommunityComment> commentPage = this.page(new Page<>(pageNum, pageSize), queryWrapper);
        List<CommunityCommentVO> commentVOList = getCommentVOList(commentPage.getRecords(), loginUser);

        Page<CommunityCommentVO> result = new Page<>(pageNum, pageSize, commentPage.getTotalRow());
        result.setRecords(commentVOList);
        return result;
    }

    @Override
    public List<CommunityCommentVO> getCommentVOList(List<CommunityComment> commentList, User loginUser) {
        if (commentList == null || commentList.isEmpty()) {
            return new ArrayList<>();
        }
        // 批量查询用户信息
        Set<Long> userIds = commentList.stream()
                .map(CommunityComment::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> {
                    UserVO userVO = new UserVO();
                    BeanUtil.copyProperties(user, userVO);
                    return userVO;
                }));

        return commentList.stream().map(comment -> {
            CommunityCommentVO commentVO = new CommunityCommentVO();
            BeanUtil.copyProperties(comment, commentVO);
            commentVO.setUser(userVOMap.get(comment.getUserId()));
            return commentVO;
        }).collect(Collectors.toList());
    }
}

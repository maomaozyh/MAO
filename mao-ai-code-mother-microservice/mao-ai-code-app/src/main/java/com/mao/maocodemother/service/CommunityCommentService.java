package com.mao.maocodemother.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.mao.maocodemother.model.dto.community.CommunityCommentAddRequest;
import com.mao.maocodemother.model.entity.CommunityComment;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.vo.CommunityCommentVO;

import java.util.List;

/**
 * 社区评论 服务层。
 */
public interface CommunityCommentService extends IService<CommunityComment> {

    /**
     * 添加评论
     *
     * @param commentAddRequest
     * @param loginUser
     * @return
     */
    Long addComment(CommunityCommentAddRequest commentAddRequest, User loginUser);

    /**
     * 删除评论
     *
     * @param commentId
     * @param loginUser
     * @return
     */
    boolean deleteComment(Long commentId, User loginUser);

    /**
     * 根据帖子ID分页查询评论
     *
     * @param postId
     * @param pageNum
     * @param pageSize
     * @param loginUser
     * @return
     */
    Page<CommunityCommentVO> getCommentVOPage(Long postId, long pageNum, long pageSize, User loginUser);

    /**
     * 获取评论封装类列表
     *
     * @param commentList
     * @param loginUser
     * @return
     */
    List<CommunityCommentVO> getCommentVOList(List<CommunityComment> commentList, User loginUser);
}

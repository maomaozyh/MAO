package com.mao.maocodemother.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.mao.maocodemother.model.dto.community.CommunityPostAddRequest;
import com.mao.maocodemother.model.dto.community.CommunityPostQueryRequest;
import com.mao.maocodemother.model.dto.community.CommunityPostUpdateRequest;
import com.mao.maocodemother.model.entity.CommunityPost;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.vo.CommunityPostVO;

import java.util.List;

/**
 * 社区帖子 服务层。
 */
public interface CommunityPostService extends IService<CommunityPost> {

    /**
     * 创建帖子
     *
     * @param postAddRequest
     * @param loginUser
     * @return
     */
    Long createPost(CommunityPostAddRequest postAddRequest, User loginUser);

    /**
     * 删除帖子
     *
     * @param postId
     * @param loginUser
     * @return
     */
    boolean deletePost(Long postId, User loginUser);

    /**
     * 点赞 / 取消点赞
     *
     * @param postId
     * @param loginUser
     * @return
     */
    boolean toggleLike(Long postId, User loginUser);

    /**
     * 获取帖子详情（浏览量+1）
     *
     * @param postId
     * @param loginUser
     * @return
     */
    CommunityPostVO getPostDetail(Long postId, User loginUser);

    /**
     * 记录浏览足迹（已登录用户浏览帖子详情时调用）
     *
     * @param userId
     * @param postId
     */
    void recordFootprint(Long userId, Long postId);

    /**
     * 分页获取我赞过的帖子列表（按点赞时间倒序）
     *
     * @param loginUser
     * @param pageNum
     * @param pageSize
     * @return
     */
    Page<CommunityPostVO> listMyLikedPostVOByPage(User loginUser, long pageNum, long pageSize);

    /**
     * 分页获取我的浏览足迹帖子列表（按浏览时间倒序）
     *
     * @param loginUser
     * @param pageNum
     * @param pageSize
     * @return
     */
    Page<CommunityPostVO> listMyFootprintPostVOByPage(User loginUser, long pageNum, long pageSize);

    /**
     * 获取帖子封装类
     *
     * @param post
     * @param loginUser
     * @return
     */
    CommunityPostVO getPostVO(CommunityPost post, User loginUser);

    /**
     * 获取帖子封装类列表
     *
     * @param postList
     * @param loginUser
     * @return
     */
    List<CommunityPostVO> getPostVOList(List<CommunityPost> postList, User loginUser);

    /**
     * 构造帖子查询条件
     *
     * @param postQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(CommunityPostQueryRequest postQueryRequest);

    /**
     * 管理员更新帖子
     *
     * @param updateRequest
     * @return
     */
    boolean adminUpdatePost(CommunityPostUpdateRequest updateRequest);
}

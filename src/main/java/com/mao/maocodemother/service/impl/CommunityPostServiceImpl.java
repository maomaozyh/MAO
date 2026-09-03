package com.mao.maocodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mao.maocodemother.exception.BusinessException;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.exception.ThrowUtils;
import com.mao.maocodemother.mapper.CommunityFootprintMapper;
import com.mao.maocodemother.mapper.CommunityLikeMapper;
import com.mao.maocodemother.mapper.CommunityPostMapper;
import com.mao.maocodemother.model.dto.community.CommunityPostAddRequest;
import com.mao.maocodemother.model.dto.community.CommunityPostQueryRequest;
import com.mao.maocodemother.model.dto.community.CommunityPostUpdateRequest;
import com.mao.maocodemother.model.entity.CommunityFootprint;
import com.mao.maocodemother.model.entity.CommunityLike;
import com.mao.maocodemother.model.entity.CommunityPost;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.enums.UserRoleEnum;
import com.mao.maocodemother.model.vo.CommunityPostVO;
import com.mao.maocodemother.model.vo.UserVO;
import com.mao.maocodemother.service.CommunityPostService;
import com.mao.maocodemother.service.SensitiveWordService;
import com.mao.maocodemother.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 社区帖子 服务层实现。
 */
@Service
@Slf4j
public class CommunityPostServiceImpl extends ServiceImpl<CommunityPostMapper, CommunityPost> implements CommunityPostService {

    @Resource
    private UserService userService;

    @Resource
    private CommunityLikeMapper communityLikeMapper;

    @Resource
    private CommunityFootprintMapper communityFootprintMapper;

    @Resource
    private SensitiveWordService sensitiveWordService;

    @Override
    public Long createPost(CommunityPostAddRequest postAddRequest, User loginUser) {
        // 参数校验
        ThrowUtils.throwIf(postAddRequest == null, ErrorCode.PARAMS_ERROR);
        String title = postAddRequest.getTitle();
        String content = postAddRequest.getContent();
        ThrowUtils.throwIf(StrUtil.isBlank(title), ErrorCode.PARAMS_ERROR, "标题不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(content), ErrorCode.PARAMS_ERROR, "内容不能为空");
        ThrowUtils.throwIf(title.length() > 100, ErrorCode.PARAMS_ERROR, "标题不能超过 100 字");
        ThrowUtils.throwIf(content.length() > 5000, ErrorCode.PARAMS_ERROR, "内容不能超过 5000 字");

        // 敏感词检测
        String fullText = title + " " + content;
        if (sensitiveWordService.containsSensitiveWord(fullText)) {
            java.util.Set<String> words = sensitiveWordService.findSensitiveWords(fullText);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "内容包含敏感词：" + String.join("、", words));
        }

        // 构造入库对象
        CommunityPost post = new CommunityPost();
        BeanUtil.copyProperties(postAddRequest, post);
        // tags 列表转 JSON 字符串存储
        if (postAddRequest.getTags() != null && !postAddRequest.getTags().isEmpty()) {
            post.setTags(JSONUtil.toJsonStr(postAddRequest.getTags()));
        } else {
            post.setTags("");
        }
        // 可选字段默认值
        if (StrUtil.isBlank(post.getCategory())) {
            post.setCategory("");
        }
        if (StrUtil.isBlank(post.getCoverImage())) {
            post.setCoverImage("");
        }
        post.setUserId(loginUser.getId());
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setStatus(1); // 1-正常

        // 插入数据库
        boolean result = this.save(post);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "发布失败");
        log.info("帖子发布成功，ID: {}", post.getId());
        return post.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletePost(Long postId, User loginUser) {
        ThrowUtils.throwIf(postId == null || postId <= 0, ErrorCode.PARAMS_ERROR, "帖子 ID 错误");
        CommunityPost post = this.getById(postId);
        ThrowUtils.throwIf(post == null, ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
        // 仅本人或管理员可删除
        if (!post.getUserId().equals(loginUser.getId()) && !UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限删除");
        }
        return this.removeById(postId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleLike(Long postId, User loginUser) {
        ThrowUtils.throwIf(postId == null || postId <= 0, ErrorCode.PARAMS_ERROR, "帖子 ID 错误");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        CommunityPost post = this.getById(postId);
        ThrowUtils.throwIf(post == null, ErrorCode.NOT_FOUND_ERROR, "帖子不存在");

        // 查询是否已点赞
        CommunityLike existLike = communityLikeMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("postId", postId)
                        .eq("userId", loginUser.getId())
        );

        if (existLike != null) {
            // 取消点赞
            communityLikeMapper.deleteById(existLike.getId());
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        } else {
            // 点赞
            CommunityLike like = CommunityLike.builder()
                    .postId(postId)
                    .userId(loginUser.getId())
                    .createTime(LocalDateTime.now())
                    .build();
            communityLikeMapper.insert(like);
            post.setLikeCount(post.getLikeCount() + 1);
        }
        this.updateById(post);
        return existLike == null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommunityPostVO getPostDetail(Long postId, User loginUser) {
        ThrowUtils.throwIf(postId == null || postId <= 0, ErrorCode.PARAMS_ERROR, "帖子 ID 错误");
        CommunityPost post = this.getById(postId);
        ThrowUtils.throwIf(post == null, ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
        // 浏览量 +1
        post.setViewCount(post.getViewCount() + 1);
        this.updateById(post);
        // 已登录用户记录浏览足迹
        if (loginUser != null) {
            recordFootprint(loginUser.getId(), postId);
        }
        return getPostVO(post, loginUser);
    }

    @Override
    public void recordFootprint(Long userId, Long postId) {
        if (userId == null || postId == null) {
            return;
        }
        CommunityFootprint footprint = communityFootprintMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("postId", postId)
                        .eq("userId", userId)
        );
        LocalDateTime now = LocalDateTime.now();
        if (footprint != null) {
            // 重复浏览则更新时间戳，保证足迹按最近浏览排序
            footprint.setCreateTime(now);
            communityFootprintMapper.update(footprint);
        } else {
            CommunityFootprint newFootprint = CommunityFootprint.builder()
                    .postId(postId)
                    .userId(userId)
                    .createTime(now)
                    .build();
            communityFootprintMapper.insert(newFootprint);
        }
    }

    @Override
    public Page<CommunityPostVO> listMyLikedPostVOByPage(User loginUser, long pageNum, long pageSize) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        Page<CommunityLike> likePage = communityLikeMapper.paginate(pageNum, pageSize,
                QueryWrapper.create()
                        .eq("userId", loginUser.getId())
                        .orderBy("createTime", false)
        );
        List<Long> postIds = likePage.getRecords().stream()
                .map(CommunityLike::getPostId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<CommunityPostVO> postVOList = getPostVOListByPostIds(postIds, loginUser);
        Page<CommunityPostVO> postVOPage = new Page<>(pageNum, pageSize, likePage.getTotalRow());
        postVOPage.setRecords(postVOList);
        return postVOPage;
    }

    @Override
    public Page<CommunityPostVO> listMyFootprintPostVOByPage(User loginUser, long pageNum, long pageSize) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        Page<CommunityFootprint> footprintPage = communityFootprintMapper.paginate(pageNum, pageSize,
                QueryWrapper.create()
                        .eq("userId", loginUser.getId())
                        .orderBy("createTime", false)
        );
        List<Long> postIds = footprintPage.getRecords().stream()
                .map(CommunityFootprint::getPostId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<CommunityPostVO> postVOList = getPostVOListByPostIds(postIds, loginUser);
        Page<CommunityPostVO> postVOPage = new Page<>(pageNum, pageSize, footprintPage.getTotalRow());
        postVOPage.setRecords(postVOList);
        return postVOPage;
    }

    /**
     * 按给定的帖子 ID 顺序（时间倒序）查询帖子并封装 VO，避免 listByIds 打乱顺序
     */
    private List<CommunityPostVO> getPostVOListByPostIds(List<Long> postIds, User loginUser) {
        if (postIds.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, CommunityPost> postMap = this.listByIds(postIds).stream()
                .collect(Collectors.toMap(CommunityPost::getId, post -> post));
        List<CommunityPost> postList = postIds.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return getPostVOList(postList, loginUser);
    }

    @Override
    public CommunityPostVO getPostVO(CommunityPost post, User loginUser) {
        if (post == null) {
            return null;
        }
        CommunityPostVO postVO = new CommunityPostVO();
        BeanUtil.copyProperties(post, postVO);
        // tags JSON 字符串转列表
        if (StrUtil.isNotBlank(post.getTags())) {
            try {
                postVO.setTags(JSONUtil.toList(post.getTags(), String.class));
            } catch (Exception e) {
                postVO.setTags(List.of());
            }
        } else {
            postVO.setTags(List.of());
        }

        // 关联用户信息
        Long userId = post.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = new UserVO();
            if (user != null) {
                BeanUtil.copyProperties(user, userVO);
            }
            postVO.setUser(userVO);
        }

        // 是否已点赞
        if (loginUser != null) {
            Long count = communityLikeMapper.selectCountByQuery(
                    QueryWrapper.create()
                            .eq("postId", post.getId())
                            .eq("userId", loginUser.getId())
            );
            postVO.setIsLiked(count != null && count > 0);
        } else {
            postVO.setIsLiked(false);
        }

        return postVO;
    }

    @Override
    public List<CommunityPostVO> getPostVOList(List<CommunityPost> postList, User loginUser) {
        if (postList == null || postList.isEmpty()) {
            return new ArrayList<>();
        }
        // 批量查询用户信息
        Set<Long> userIds = postList.stream()
                .map(CommunityPost::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> {
                    UserVO userVO = new UserVO();
                    BeanUtil.copyProperties(user, userVO);
                    return userVO;
                }));

        // 批量查询点赞状态
        Set<Long> postIds = postList.stream()
                .map(CommunityPost::getId)
                .collect(Collectors.toSet());
        Set<Long> likedPostIds = Set.of();
        if (loginUser != null) {
            List<CommunityLike> myLikes = communityLikeMapper.selectListByQuery(
                    QueryWrapper.create()
                            .in("postId", postIds)
                            .eq("userId", loginUser.getId())
            );
            likedPostIds = myLikes.stream()
                    .map(CommunityLike::getPostId)
                    .collect(Collectors.toSet());
        }

        // 组装 VO
        Set<Long> finalLikedPostIds = likedPostIds;
        return postList.stream().map(post -> {
            CommunityPostVO postVO = new CommunityPostVO();
            BeanUtil.copyProperties(post, postVO);
            // tags JSON 字符串转列表
            if (StrUtil.isNotBlank(post.getTags())) {
                try {
                    postVO.setTags(JSONUtil.toList(post.getTags(), String.class));
                } catch (Exception e) {
                    postVO.setTags(List.of());
                }
            } else {
                postVO.setTags(List.of());
            }
            postVO.setUser(userVOMap.get(post.getUserId()));
            postVO.setIsLiked(finalLikedPostIds.contains(post.getId()));
            return postVO;
        }).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(CommunityPostQueryRequest postQueryRequest) {
        if (postQueryRequest == null) {
            return QueryWrapper.create();
        }
        Long id = postQueryRequest.getId();
        String title = postQueryRequest.getTitle();
        String content = postQueryRequest.getContent();
        String category = postQueryRequest.getCategory();
        String tags = postQueryRequest.getTags();
        Integer status = postQueryRequest.getStatus();
        Long userId = postQueryRequest.getUserId();
        Boolean allStatus = postQueryRequest.getAllStatus();
        String sortField = postQueryRequest.getSortField();
        String sortOrder = postQueryRequest.getSortOrder();

        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("id", id)
                .like("title", title)
                .like("content", content)
                .eq("category", category)
                .like("tags", tags)
                .eq("status", status)
                .eq("userId", userId);

        // 默认状态为正常（管理员查询所有状态时跳过）
        if (status == null && !Boolean.TRUE.equals(allStatus)) {
            queryWrapper.eq("status", 1);
        }

        // 排序
        boolean isAsc = "ascend".equals(sortOrder);
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, isAsc);
        } else {
            queryWrapper.orderBy("createTime", false);
        }

        return queryWrapper;
    }

    @Override
    public boolean adminUpdatePost(CommunityPostUpdateRequest updateRequest) {
        ThrowUtils.throwIf(updateRequest == null || updateRequest.getId() == null,
                ErrorCode.PARAMS_ERROR, "参数错误");
        CommunityPost post = this.getById(updateRequest.getId());
        ThrowUtils.throwIf(post == null, ErrorCode.NOT_FOUND_ERROR, "帖子不存在");

        CommunityPost updatePost = new CommunityPost();
        updatePost.setId(updateRequest.getId());
        if (StrUtil.isNotBlank(updateRequest.getTitle())) {
            updatePost.setTitle(updateRequest.getTitle());
        }
        if (StrUtil.isNotBlank(updateRequest.getContent())) {
            updatePost.setContent(updateRequest.getContent());
        }
        if (updateRequest.getCategory() != null) {
            updatePost.setCategory(updateRequest.getCategory());
        }
        if (updateRequest.getTags() != null) {
            updatePost.setTags(JSONUtil.toJsonStr(updateRequest.getTags()));
        }
        if (updateRequest.getCoverImage() != null) {
            updatePost.setCoverImage(updateRequest.getCoverImage());
        }
        if (updateRequest.getStatus() != null) {
            updatePost.setStatus(updateRequest.getStatus());
        }

        return this.updateById(updatePost);
    }
}

package com.mao.maocodemother.model.dto.community;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 帖子更新请求（管理员用）
 */
@Data
public class CommunityPostUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 封面图
     */
    private String coverImage;

    /**
     * 状态：0-待审核，1-已发布，2-已下架
     */
    private Integer status;
}

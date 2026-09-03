package com.mao.maocodemother.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommunityPostVO implements Serializable {

    private Long id;

    private String title;

    private String content;

    private String category;

    private List<String> tags;

    private String coverImage;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    private Integer status;

    private Long userId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private UserVO user;

    private Boolean isLiked;

    private static final long serialVersionUID = 1L;
}

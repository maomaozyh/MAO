package com.mao.maocodemother.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class CommunityCommentVO implements Serializable {

    private Long id;

    private String content;

    private Long postId;

    private Long parentId;

    private Long userId;

    private LocalDateTime createTime;

    private UserVO user;

    private static final long serialVersionUID = 1L;
}

package com.mao.maocodemother.model.dto.community;

import lombok.Data;

import java.io.Serializable;

@Data
public class CommunityCommentAddRequest implements Serializable {

    private Long postId;

    private String content;

    private Long parentId;

    private static final long serialVersionUID = 1L;
}

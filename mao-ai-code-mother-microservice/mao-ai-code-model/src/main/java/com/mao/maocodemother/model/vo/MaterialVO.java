package com.mao.maocodemother.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class MaterialVO implements Serializable {

    private Long id;

    private String name;

    private String type;

    private String url;

    private Long size;

    private Long userId;

    private Long folderId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private UserVO user;

    private static final long serialVersionUID = 1L;
}

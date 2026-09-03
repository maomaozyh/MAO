package com.mao.maocodemother.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 素材 VO（字段名与前端 typings 的 MaterialVO 对齐：name/type/url/size）
 */
@Data
public class MaterialVO implements Serializable {

    private Long id;

    private String name;

    private String type;

    private String url;

    private Long size;

    private Long folderId;

    private Long userId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private UserVO user;

    private static final long serialVersionUID = 1L;
}

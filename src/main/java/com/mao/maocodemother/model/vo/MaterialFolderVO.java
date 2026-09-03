package com.mao.maocodemother.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 素材文件夹 VO（字段名与前端 typings 的 MaterialFolderVO 对齐：name）
 */
@Data
public class MaterialFolderVO implements Serializable {

    private Long id;

    private String name;

    private Long userId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}

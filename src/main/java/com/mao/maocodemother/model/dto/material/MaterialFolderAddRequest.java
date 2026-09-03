package com.mao.maocodemother.model.dto.material;

import lombok.Data;

import java.io.Serializable;

/**
 * 新建素材文件夹请求（字段名与前端 typings 的 MaterialFolderAddRequest 对齐：name）
 */
@Data
public class MaterialFolderAddRequest implements Serializable {

    private String name;

    private static final long serialVersionUID = 1L;
}

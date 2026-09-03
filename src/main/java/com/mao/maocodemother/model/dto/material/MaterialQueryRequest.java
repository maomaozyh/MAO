package com.mao.maocodemother.model.dto.material;

import com.mao.maocodemother.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 素材分页查询请求（字段名与前端 typings 的 MaterialQueryRequest 对齐：name/type/folderId）
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MaterialQueryRequest extends PageRequest implements Serializable {

    private String name;

    private String type;

    private Long folderId;

    private Long userId;

    private static final long serialVersionUID = 1L;
}

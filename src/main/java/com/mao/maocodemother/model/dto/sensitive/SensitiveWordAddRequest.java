package com.mao.maocodemother.model.dto.sensitive;

import lombok.Data;

import java.io.Serializable;

/**
 * 敏感词新增/更新请求
 */
@Data
public class SensitiveWordAddRequest implements Serializable {

    /**
     * id（更新时传）
     */
    private Long id;

    /**
     * 敏感词
     */
    private String word;

    /**
     * 分类
     */
    private String category;

    /**
     * 是否启用
     */
    private Integer enabled;

    /**
     * 备注
     */
    private String remark;

    private static final long serialVersionUID = 1L;
}

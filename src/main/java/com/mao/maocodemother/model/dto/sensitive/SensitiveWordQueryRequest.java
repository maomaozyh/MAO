package com.mao.maocodemother.model.dto.sensitive;

import lombok.Data;

import java.io.Serializable;

/**
 * 敏感词查询请求
 */
@Data
public class SensitiveWordQueryRequest implements Serializable {

    /**
     * 关键词
     */
    private String keyword;

    /**
     * 分类
     */
    private String category;

    /**
     * 是否启用
     */
    private Integer enabled;

    /**
     * 页码
     */
    private long pageNum = 1;

    /**
     * 每页大小
     */
    private long pageSize = 10;

    private static final long serialVersionUID = 1L;
}

package com.mao.maocodemother.model.dto.sensitive;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 敏感词批量导入请求
 */
@Data
public class SensitiveWordBatchRequest implements Serializable {

    /**
     * 敏感词列表（每行一个）
     */
    private List<String> words;

    /**
     * 分类
     */
    private String category;

    private static final long serialVersionUID = 1L;
}

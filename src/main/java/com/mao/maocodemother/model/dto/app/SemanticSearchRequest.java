package com.mao.maocodemother.model.dto.app;

import com.mao.maocodemother.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 语义搜索请求（AI 查询扩展）
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SemanticSearchRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 自然语言搜索词
     */
    private String keyword;
}

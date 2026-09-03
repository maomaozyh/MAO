package com.mao.maocodemother.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 代码错误自检结果
 */
@Data
public class SelfCheckResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否存在问题
     */
    private Boolean hasIssue;

    /**
     * 问题列表
     */
    private List<String> issues;

    /**
     * 修正后的完整代码（仅当有可修复问题时返回）
     */
    private String fixedCode;
}

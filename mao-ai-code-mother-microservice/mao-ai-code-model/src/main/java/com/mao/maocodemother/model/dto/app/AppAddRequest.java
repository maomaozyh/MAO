package com.mao.maocodemother.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用创建请求
 */
@Data
public class AppAddRequest implements Serializable {

    /**
     * 应用初始化的 prompt
     */
    private String initPrompt;

    /**
     * 应用名称（可选，不传则自动取 prompt 前 12 位）
     */
    private String appName;

    /**
     * 代码生成类型（可选，不传则由 AI 智能选择；取值：html / multi_file / vue_project）
     */
    private String codeGenType;

    private static final long serialVersionUID = 1L;
} 
package com.mao.maocodemother.model.dto.app;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 描述智能扩写请求
 */
@Data
public class PromptExpandRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 一句话需求描述
     */
    private String prompt;
}

package com.mao.maocodemother.model.dto.app;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 代码错误自检请求
 */
@Data
public class SelfCheckRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 应用 ID
     */
    private Long appId;
}

package com.mao.maocodemother.model.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量删除用户请求
 */
@Data
public class UserBatchDeleteRequest implements Serializable {

    /**
     * 用户 id 列表
     */
    private List<Long> ids;

    private static final long serialVersionUID = 1L;
}

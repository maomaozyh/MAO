package com.mao.maocodemother.model.dto.community;

import com.mao.maocodemother.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class CommunityPostQueryRequest extends PageRequest implements Serializable {

    private Long id;

    private String title;

    private String content;

    private String category;

    private String tags;

    private Integer status;

    private Long userId;

    /**
     * 是否查询所有状态（管理员用，不默认过滤 status=1）
     */
    private Boolean allStatus;

    private static final long serialVersionUID = 1L;
}

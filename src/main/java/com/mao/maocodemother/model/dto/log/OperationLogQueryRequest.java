package com.mao.maocodemother.model.dto.log;

import com.mao.maocodemother.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class OperationLogQueryRequest extends PageRequest implements Serializable {

    /**
     * 操作人 ID
     */
    private Long userId;

    /**
     * 模块名
     */
    private String module;

    /**
     * 操作类型
     */
    private String operation;

    /**
     * 关键词，模糊搜索操作详情或操作人昵称
     */
    private String keyword;

    /**
     * 起始时间
     */
    @EqualsAndHashCode.Exclude
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @EqualsAndHashCode.Exclude
    private LocalDateTime endTime;

    private static final long serialVersionUID = 1L;
}

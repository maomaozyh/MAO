package com.mao.maocodemother.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 操作日志
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("operation_log")
public class OperationLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 操作人 ID
     */
    @Column("userId")
    private Long userId;

    /**
     * 操作人昵称
     */
    @Column("userName")
    private String userName;

    /**
     * 模块名，如 user/app/post
     */
    private String module;

    /**
     * 操作类型，如 新增/删除/审核
     */
    private String operation;

    /**
     * 操作对象 ID
     */
    @Column("targetId")
    private String targetId;

    /**
     * 操作详情
     */
    private String detail;

    /**
     * 操作 IP
     */
    private String ip;

    /**
     * 0 失败 1 成功
     */
    private Integer status;

    /**
     * 错误信息
     */
    @Column("errorMsg")
    private String errorMsg;

    @Column("createTime")
    private LocalDateTime createTime;
}

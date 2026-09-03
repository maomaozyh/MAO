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

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 安全审计日志实体
 * 记录登录、敏感操作等安全相关事件，用于追溯和合规
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("audit_log")
public class AuditLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 操作类型：LOGIN / LOGOUT / PASSWORD_CHANGE / ADMIN_OP / API_CALL 等
     */
    @Column("actionType")
    private String actionType;

    /**
     * 操作描述
     */
    @Column("actionDesc")
    private String actionDesc;

    /**
     * 操作人用户 ID（未登录时为 null）
     */
    @Column("userId")
    private Long userId;

    /**
     * 操作人账号
     */
    @Column("userAccount")
    private String userAccount;

    /**
     * 客户端 IP
     */
    @Column("clientIp")
    private String clientIp;

    /**
     * User-Agent
     */
    @Column("userAgent")
    private String userAgent;

    /**
     * 请求方法（GET/POST 等）
     */
    @Column("requestMethod")
    private String requestMethod;

    /**
     * 请求 URI
     */
    @Column("requestUri")
    private String requestUri;

    /**
     * 请求参数（JSON 格式，脱敏后存储）
     */
    @Column("requestParams")
    private String requestParams;

    /**
     * 操作结果：SUCCESS / FAIL
     */
    @Column("resultStatus")
    private String resultStatus;

    /**
     * 失败原因（失败时记录）
     */
    @Column("failReason")
    private String failReason;

    /**
     * 操作耗时（毫秒）
     */
    @Column("costMs")
    private Long costMs;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;
}

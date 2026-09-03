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
 * 系统配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_config")
public class SysConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 配置键
     */
    @Column("configKey")
    private String configKey;

    /**
     * 配置值
     */
    @Column("configValue")
    private String configValue;

    /**
     * 配置名称
     */
    @Column("configName")
    private String configName;

    /**
     * 值类型：string/number/boolean
     */
    @Column("configType")
    private String configType;

    /**
     * 说明
     */
    private String description;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;
}

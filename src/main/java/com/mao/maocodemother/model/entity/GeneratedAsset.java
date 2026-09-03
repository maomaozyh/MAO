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
 * 生成的外部素材（图片/视频/3D 模型/PPT 等）持久化实体
 *
 * @author mao
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("generated_asset")
public class GeneratedAsset implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id（雪花 ID）
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 所属应用 id
     */
    @Column("appId")
    private Long appId;

    /**
     * 生成用户 id
     */
    @Column("userId")
    private Long userId;

    /**
     * 素材类型（image/video/model_3d/ppt）
     */
    @Column("assetType")
    private String assetType;

    /**
     * 素材可访问 URL（外部服务返回的地址）
     */
    @Column("url")
    private String url;

    /**
     * 本地/COS 存储路径（预留，当前直接用外部 URL）
     */
    @Column("localPath")
    private String localPath;

    /**
     * 生成该素材时使用的最终提示词
     */
    @Column("prompt")
    private String prompt;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column("updateTime")
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}

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
 * 素材（对齐库表列：name/type/url/size/folderId）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("material")
public class Material implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 素材名称
     */
    @Column("name")
    private String name;

    /**
     * 素材类型(image/video/audio/3d/other)
     */
    @Column("type")
    private String type;

    /**
     * 素材访问地址（COS URL）
     */
    @Column("url")
    private String url;

    /**
     * 文件大小(字节)
     */
    @Column("size")
    private Long size;

    /**
     * 上传用户 id
     */
    @Column("userId")
    private Long userId;

    /**
     * 所属文件夹 id（可空）
     */
    @Column("folderId")
    private Long folderId;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}

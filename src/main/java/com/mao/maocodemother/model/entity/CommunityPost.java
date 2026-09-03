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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("community_post")
public class CommunityPost implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    private String title;

    private String content;

    private String category;

    private String tags;

    @Column("coverImage")
    private String coverImage;

    @Column("viewCount")
    private Integer viewCount;

    @Column("likeCount")
    private Integer likeCount;

    @Column("commentCount")
    private Integer commentCount;

    private Integer status;

    @Column("userId")
    private Long userId;

    @Column("editTime")
    private LocalDateTime editTime;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}

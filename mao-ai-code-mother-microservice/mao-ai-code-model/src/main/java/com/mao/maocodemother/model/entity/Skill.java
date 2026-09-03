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
@Table("skill")
public class Skill implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    @Column("skillName")
    private String skillName;

    @Column("skillDesc")
    private String skillDesc;

    @Column("featureDesc")
    private String featureDesc;

    @Column("usageDesc")
    private String usageDesc;

    private String icon;

    private String category;

    private String price;

    @Column("originalPrice")
    private String originalPrice;

    @Column("priceUnit")
    private String priceUnit;

    private String tags;

    @Column("usageCount")
    private Long usageCount;

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

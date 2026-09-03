package com.mao.maocodemother.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class SkillVO implements Serializable {

    private Long id;

    private String skillName;

    private String skillDesc;

    private String featureDesc;

    private String usageDesc;

    private String icon;

    private String category;

    private String price;

    private String originalPrice;

    private String priceUnit;

    private String tags;

    private Long usageCount;

    private Integer status;

    private Long userId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private UserVO user;

    private static final long serialVersionUID = 1L;
}

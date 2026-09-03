package com.mao.maocodemother.model.dto.skill;

import lombok.Data;

import java.io.Serializable;

@Data
public class SkillUpdateRequest implements Serializable {

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

    private Integer status;

    private static final long serialVersionUID = 1L;
}

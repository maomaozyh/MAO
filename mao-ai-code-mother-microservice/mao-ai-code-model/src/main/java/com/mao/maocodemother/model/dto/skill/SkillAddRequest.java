package com.mao.maocodemother.model.dto.skill;

import lombok.Data;

import java.io.Serializable;

@Data
public class SkillAddRequest implements Serializable {

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

    private static final long serialVersionUID = 1L;
}

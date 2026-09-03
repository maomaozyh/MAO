package com.mao.maocodemother.model.dto.skill;

import com.mao.maocodemother.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class SkillQueryRequest extends PageRequest implements Serializable {

    private Long id;

    private String skillName;

    private String category;

    private String tags;

    private Integer status;

    private Long userId;

    private static final long serialVersionUID = 1L;
}

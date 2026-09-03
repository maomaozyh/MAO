package com.mao.maocodemother.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class MaterialFolderVO implements Serializable {

    private Long id;

    private String name;

    private Long userId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}

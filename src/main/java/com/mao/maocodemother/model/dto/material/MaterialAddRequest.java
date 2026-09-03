package com.mao.maocodemother.model.dto.material;

import lombok.Data;

import java.io.Serializable;

@Data
public class MaterialAddRequest implements Serializable {

    private String materialName;

    private String fileUrl;

    private String fileType;

    private Long fileSize;

    private Long folderId;

    private String tags;

    private static final long serialVersionUID = 1L;
}

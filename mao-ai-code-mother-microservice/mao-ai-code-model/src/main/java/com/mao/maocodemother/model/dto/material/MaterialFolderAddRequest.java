package com.mao.maocodemother.model.dto.material;

import lombok.Data;

import java.io.Serializable;

@Data
public class MaterialFolderAddRequest implements Serializable {

    private String name;

    private static final long serialVersionUID = 1L;
}

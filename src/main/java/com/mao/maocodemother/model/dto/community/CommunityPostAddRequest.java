package com.mao.maocodemother.model.dto.community;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CommunityPostAddRequest implements Serializable {

    private String title;

    private String content;

    private String category;

    private List<String> tags;

    private String coverImage;

    private static final long serialVersionUID = 1L;
}

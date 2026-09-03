package com.mao.maocodemother.model.dto.material;

import com.mao.maocodemother.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class MaterialQueryRequest extends PageRequest implements Serializable {

    private String name;

    /**
     * 素材类型：image / video / audio / 3d / other
     */
    private String type;

    private Long userId;

    /**
     * 文件夹 id，为空表示查询未归类的素材
     */
    private Long folderId;

    private static final long serialVersionUID = 1L;
}

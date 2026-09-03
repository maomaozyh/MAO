package com.mao.maocodemother.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mao.maocodemother.model.dto.material.MaterialQueryRequest;
import com.mao.maocodemother.model.entity.Material;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.vo.MaterialVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 素材 服务层。
 */
public interface MaterialService {

    /**
     * 上传素材（保存到 COS 并落库）
     */
    Long uploadMaterial(MultipartFile file, String name, Long folderId, User loginUser);

    /**
     * 删除素材（仅本人或管理员）
     */
    Boolean deleteMaterial(Long id, User loginUser);

    MaterialVO getMaterialVOById(Long id);

    Page<MaterialVO> listMaterialVOByPage(MaterialQueryRequest materialQueryRequest);

    MaterialVO getMaterialVO(Material material);

    List<MaterialVO> getMaterialVOList(List<Material> materialList);

    QueryWrapper getQueryWrapper(MaterialQueryRequest materialQueryRequest);
}

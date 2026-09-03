package com.mao.maocodemother.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.mao.maocodemother.model.dto.material.MaterialQueryRequest;
import com.mao.maocodemother.model.entity.Material;
import com.mao.maocodemother.model.vo.MaterialVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 素材 服务层。
 */
public interface MaterialService extends IService<Material> {

    /**
     * 上传素材：上传到 COS 并落库
     *
     * @param file     文件
     * @param name     素材名称（为空用原文件名）
     * @param folderId 所属文件夹 id（可空）
     * @param userId   上传用户 id
     * @return 素材 id
     */
    Long uploadMaterial(MultipartFile file, String name, Long folderId, Long userId);

    /**
     * 查询素材 VO（校验归属：本人或管理员）
     */
    MaterialVO getMaterialVO(Long id, Long loginUserId);

    /**
     * 分页查询我的素材（本人 userId 兜底，管理员可查任意 userId）
     */
    Page<MaterialVO> listMaterialVOPage(MaterialQueryRequest request, Long loginUserId);

    /**
     * 删除素材（校验归属：本人或管理员）
     *
     * @return 是否删除成功
     */
    boolean deleteMaterial(Long id, Long loginUserId);
}

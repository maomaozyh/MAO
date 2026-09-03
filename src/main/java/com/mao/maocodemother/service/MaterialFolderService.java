package com.mao.maocodemother.service;

import com.mybatisflex.core.service.IService;
import com.mao.maocodemother.model.entity.MaterialFolder;
import com.mao.maocodemother.model.vo.MaterialFolderVO;

import java.util.List;

/**
 * 素材文件夹 服务层。
 */
public interface MaterialFolderService extends IService<MaterialFolder> {

    /**
     * 新建文件夹
     *
     * @param name   文件夹名称
     * @param userId 用户 id
     * @return 文件夹 id
     */
    Long addFolder(String name, Long userId);

    /**
     * 查询某用户的全部文件夹（按创建时间正序）
     */
    List<MaterialFolderVO> listUserFolders(Long userId);

    /**
     * 删除文件夹（校验归属；同时清空其下素材的 folderId）
     */
    boolean deleteFolder(Long id, Long loginUserId);
}

package com.mao.maocodemother.service;

import com.mao.maocodemother.model.entity.MaterialFolder;
import com.mao.maocodemother.model.vo.MaterialFolderVO;

import java.util.List;

/**
 * 素材文件夹 服务接口。
 */
public interface MaterialFolderService {

    /**
     * 新建文件夹。
     *
     * @param name      文件夹名称
     * @param loginUser 登录用户
     * @return 新建的文件夹 id
     */
    long addFolder(String name, com.mao.maocodemother.model.entity.User loginUser);

    /**
     * 查询当前用户的文件夹列表。
     *
     * @param loginUser 登录用户
     * @return 文件夹视图列表
     */
    List<MaterialFolderVO> listMyFolders(com.mao.maocodemother.model.entity.User loginUser);

    /**
     * 删除文件夹（同时解除文件夹下素材的归属，素材不会被删除）。
     *
     * @param id        文件夹 id
     * @param loginUser 登录用户
     * @return 是否成功
     */
    boolean deleteFolder(long id, com.mao.maocodemother.model.entity.User loginUser);
}

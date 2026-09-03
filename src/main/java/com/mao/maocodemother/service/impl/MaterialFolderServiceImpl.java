package com.mao.maocodemother.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.exception.ThrowUtils;
import com.mao.maocodemother.mapper.MaterialFolderMapper;
import com.mao.maocodemother.model.entity.Material;
import com.mao.maocodemother.model.entity.MaterialFolder;
import com.mao.maocodemother.model.enums.UserRoleEnum;
import com.mao.maocodemother.model.vo.MaterialFolderVO;
import com.mao.maocodemother.service.MaterialFolderService;
import com.mao.maocodemother.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 素材文件夹 服务层实现。
 */
@Service
public class MaterialFolderServiceImpl extends ServiceImpl<MaterialFolderMapper, MaterialFolder>
        implements MaterialFolderService {

    @Resource
    private UserService userService;

    @Override
    public Long addFolder(String name, Long userId) {
        ThrowUtils.throwIf(StrUtil.isBlank(name), ErrorCode.PARAMS_ERROR, "文件夹名称不能为空");
        ThrowUtils.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        MaterialFolder folder = MaterialFolder.builder()
                .name(name.trim())
                .userId(userId)
                .build();
        this.save(folder);
        return folder.getId();
    }

    @Override
    public List<MaterialFolderVO> listUserFolders(Long userId) {
        List<MaterialFolder> folders = this.list(QueryWrapper.create()
                .eq(MaterialFolder::getUserId, userId)
                .orderBy(MaterialFolder::getCreateTime, true));
        return folders.stream().map(this::toVO).toList();
    }

    @Override
    public boolean deleteFolder(Long id, Long loginUserId) {
        MaterialFolder folder = this.getById(id);
        ThrowUtils.throwIf(folder == null, ErrorCode.NOT_FOUND_ERROR, "文件夹不存在");
        boolean own = folder.getUserId().equals(loginUserId);
        boolean admin = UserRoleEnum.ADMIN.getValue().equals(userService.getById(loginUserId).getUserRole());
        ThrowUtils.throwIf(!own && !admin, ErrorCode.NO_AUTH_ERROR, "无权操作该文件夹");
        // 先清空其下素材的 folderId，避免素材变成「挂在已删除文件夹下」查不到
        UpdateChain.of(Material.class)
                .set(Material::getFolderId, null)
                .where(Material::getFolderId).eq(id)
                .update();
        return this.removeById(id);
    }

    private MaterialFolderVO toVO(MaterialFolder folder) {
        MaterialFolderVO vo = new MaterialFolderVO();
        vo.setId(folder.getId());
        vo.setName(folder.getName());
        vo.setUserId(folder.getUserId());
        vo.setCreateTime(folder.getCreateTime());
        vo.setUpdateTime(folder.getUpdateTime());
        return vo;
    }
}

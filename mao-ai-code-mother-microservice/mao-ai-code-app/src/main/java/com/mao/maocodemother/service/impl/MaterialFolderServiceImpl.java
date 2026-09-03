package com.mao.maocodemother.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.exception.ThrowUtils;
import com.mao.maocodemother.mapper.MaterialFolderMapper;
import com.mao.maocodemother.mapper.MaterialMapper;
import com.mao.maocodemother.model.entity.Material;
import com.mao.maocodemother.model.entity.MaterialFolder;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.vo.MaterialFolderVO;
import com.mao.maocodemother.service.MaterialFolderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 素材文件夹 服务实现。
 */
@Service
@Slf4j
public class MaterialFolderServiceImpl implements MaterialFolderService {

    @Resource
    private MaterialFolderMapper materialFolderMapper;

    @Resource
    private MaterialMapper materialMapper;

    @Override
    public long addFolder(String name, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(StrUtil.isBlank(name), ErrorCode.PARAMS_ERROR, "文件夹名称不能为空");
        name = name.trim();
        ThrowUtils.throwIf(name.length() > 30, ErrorCode.PARAMS_ERROR, "文件夹名称不能超过 30 个字符");

        MaterialFolder folder = new MaterialFolder();
        folder.setName(name);
        folder.setUserId(loginUser.getId());
        folder.setCreateTime(LocalDateTime.now());
        folder.setUpdateTime(LocalDateTime.now());
        boolean result = materialFolderMapper.insert(folder) > 0;
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建文件夹失败");
        return folder.getId();
    }

    @Override
    public List<MaterialFolderVO> listMyFolders(User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        QueryWrapper query = QueryWrapper.create()
                .where("userId = ?", loginUser.getId())
                .orderBy("createTime", true);
        List<MaterialFolder> folders = materialFolderMapper.selectListByQuery(query);
        return folders.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public boolean deleteFolder(long id, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        MaterialFolder folder = materialFolderMapper.selectOneById(id);
        ThrowUtils.throwIf(folder == null, ErrorCode.NOT_FOUND_ERROR, "文件夹不存在");
        ThrowUtils.throwIf(!folder.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR);

        // 解除该文件夹下素材的归属（素材本身不删除）
        Material updater = new Material();
        updater.setFolderId(null);
        QueryWrapper updateQuery = QueryWrapper.create()
                .where("userId = ?", loginUser.getId())
                .and("folderId = ?", id);
        materialMapper.updateByQuery(updater, updateQuery);

        boolean result = materialFolderMapper.deleteById(id) > 0;
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除文件夹失败");
        return true;
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

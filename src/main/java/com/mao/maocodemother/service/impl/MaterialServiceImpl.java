package com.mao.maocodemother.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mao.maocodemother.exception.BusinessException;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.exception.ThrowUtils;
import com.mao.maocodemother.manager.CosManager;
import com.mao.maocodemother.mapper.MaterialMapper;
import com.mao.maocodemother.model.dto.material.MaterialQueryRequest;
import com.mao.maocodemother.model.entity.Material;
import com.mao.maocodemother.model.enums.UserRoleEnum;
import com.mao.maocodemother.model.vo.MaterialVO;
import com.mao.maocodemother.model.vo.UserVO;
import com.mao.maocodemother.service.MaterialService;
import com.mao.maocodemother.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 素材 服务层实现。
 */
@Service
@Slf4j
public class MaterialServiceImpl extends ServiceImpl<MaterialMapper, Material> implements MaterialService {

    /**
     * COS 素材目录前缀
     */
    private static final String COS_MATERIAL_DIR = "material";

    @Resource
    private CosManager cosManager;

    @Resource
    private UserService userService;

    @Override
    public Long uploadMaterial(MultipartFile file, String name, Long folderId, Long userId) {
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        ThrowUtils.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR, "请先登录");

        String originalName = file.getOriginalFilename();
        String materialName = StrUtil.isBlank(name) ? (originalName == null ? "未命名素材" : originalName) : name.trim();
        String ext = StrUtil.subAfter(originalName == null ? "" : originalName, ".", true);
        // 扩展名白名单，防注入 COS key
        String safeExt = (StrUtil.isBlank(ext) || !ext.matches("[a-zA-Z0-9]{1,10}")) ? "bin" : ext.toLowerCase();
        String type = detectType(originalName, file.getContentType());

        // 1. 落临时文件 → 上传 COS
        File tempFile = null;
        try {
            tempFile = File.createTempFile("material_", "." + safeExt);
            file.transferTo(tempFile);
            String key = COS_MATERIAL_DIR + "/" + userId + "/" + IdUtil.getSnowflakeNextId() + "." + safeExt;
            String url = cosManager.uploadFile(key, tempFile);
            ThrowUtils.throwIf(StrUtil.isBlank(url), ErrorCode.OPERATION_ERROR, "文件上传失败，请稍后重试");

            // 2. 落库
            Material material = Material.builder()
                    .name(materialName)
                    .type(type)
                    .url(url)
                    .size(file.getSize())
                    .userId(userId)
                    .folderId(folderId)
                    .build();
            this.save(material);
            return material.getId();
        } catch (IOException e) {
            log.error("[素材] 上传文件失败，userId={}, name={}", userId, materialName, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文件上传失败，请稍后重试");
        } finally {
            if (tempFile != null) {
                FileUtil.del(tempFile);
            }
        }
    }

    @Override
    public MaterialVO getMaterialVO(Long id, Long loginUserId) {
        Material material = this.getById(id);
        ThrowUtils.throwIf(material == null, ErrorCode.NOT_FOUND_ERROR, "素材不存在");
        checkOwner(material, loginUserId);
        return toVO(material);
    }

    @Override
    public Page<MaterialVO> listMaterialVOPage(MaterialQueryRequest request, Long loginUserId) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        // 本人查自己的；管理员可通过 userId 字段查任意用户
        Long queryUserId = request.getUserId();
        if (!UserRoleEnum.ADMIN.getValue().equals(userService.getById(loginUserId).getUserRole())) {
            queryUserId = loginUserId;
        }
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(Material::getUserId, queryUserId, queryUserId != null)
                .eq(Material::getType, request.getType(), StrUtil.isNotBlank(request.getType()))
                .like(Material::getName, request.getName(), StrUtil.isNotBlank(request.getName()));
        // folderId：null 表示全部（含未分类），传了则精确过滤
        if (request.getFolderId() != null) {
            queryWrapper.eq(Material::getFolderId, request.getFolderId());
        }
        queryWrapper.orderBy(Material::getCreateTime, !"ascend".equalsIgnoreCase(request.getSortOrder()));

        Page<Material> page = this.page(Page.of(request.getPageNum(), request.getPageSize()), queryWrapper);
        Page<MaterialVO> voPage = new Page<>();
        voPage.setPageNumber(page.getPageNumber());
        voPage.setPageSize(page.getPageSize());
        voPage.setTotalRow(page.getTotalRow());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public boolean deleteMaterial(Long id, Long loginUserId) {
        Material material = this.getById(id);
        ThrowUtils.throwIf(material == null, ErrorCode.NOT_FOUND_ERROR, "素材不存在");
        checkOwner(material, loginUserId);
        return this.removeById(id);
    }

    /**
     * 归属校验：本人或管理员可操作
     */
    private void checkOwner(Material material, Long loginUserId) {
        if (material.getUserId().equals(loginUserId)) {
            return;
        }
        String role = userService.getById(loginUserId).getUserRole();
        ThrowUtils.throwIf(!UserRoleEnum.ADMIN.getValue().equals(role), ErrorCode.NO_AUTH_ERROR, "无权操作该素材");
    }

    private MaterialVO toVO(Material material) {
        MaterialVO vo = new MaterialVO();
        vo.setId(material.getId());
        vo.setName(material.getName());
        vo.setType(material.getType());
        vo.setUrl(material.getUrl());
        vo.setSize(material.getSize());
        vo.setFolderId(material.getFolderId());
        vo.setUserId(material.getUserId());
        vo.setCreateTime(material.getCreateTime());
        vo.setUpdateTime(material.getUpdateTime());
        return vo;
    }

    /**
     * 按文件名/ContentType 判定素材类型（与前端 tab 的 image/video/audio/3d/other 对齐）
     */
    private String detectType(String filename, String contentType) {
        String lower = filename == null ? "" : filename.toLowerCase();
        if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".bmp")
                || lower.endsWith(".svg") || lower.endsWith(".ico") || lower.endsWith(".avif")) {
            return "image";
        }
        if (lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".avi")
                || lower.endsWith(".mkv") || lower.endsWith(".webm") || lower.endsWith(".flv")
                || lower.endsWith(".wmv") || lower.endsWith(".m4v")) {
            return "video";
        }
        if (lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".flac")
                || lower.endsWith(".aac") || lower.endsWith(".ogg") || lower.endsWith(".m4a")
                || lower.endsWith(".wma")) {
            return "audio";
        }
        if (lower.endsWith(".glb") || lower.endsWith(".gltf") || lower.endsWith(".fbx")
                || lower.endsWith(".obj") || lower.endsWith(".stl") || lower.endsWith(".usdz")
                || lower.endsWith(".3ds")) {
            return "3d";
        }
        return "other";
    }
}

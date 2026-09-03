package com.mao.maocodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mao.maocodemother.constant.UserConstant;
import com.mao.maocodemother.exception.BusinessException;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.exception.ThrowUtils;
import com.mao.maocodemother.innerservice.InnerUserService;
import com.mao.maocodemother.manager.CosManager;
import com.mao.maocodemother.mapper.MaterialMapper;
import com.mao.maocodemother.model.dto.material.MaterialQueryRequest;
import com.mao.maocodemother.model.entity.Material;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.vo.MaterialVO;
import com.mao.maocodemother.model.vo.UserVO;
import com.mao.maocodemother.service.MaterialService;
import com.mao.maocodemother.ai.service.VectorSearchService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 素材 服务层实现。
 */
@Service
@Slf4j
public class MaterialServiceImpl extends ServiceImpl<MaterialMapper, Material> implements MaterialService {

    @DubboReference
    private InnerUserService userService;

    @Resource
    private CosManager cosManager;

    @Autowired(required = false)
    private VectorSearchService vectorSearchService;

    private static final long MAX_SIZE = 200L * 1024 * 1024;

    @Override
    public Long uploadMaterial(MultipartFile file, String name, Long folderId, User loginUser) {
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "文件不能为空");
        ThrowUtils.throwIf(file.getSize() > MAX_SIZE, ErrorCode.PARAMS_ERROR, "文件大小不能超过 200MB");
        String originalFilename = file.getOriginalFilename();
        String type = resolveType(file.getContentType(), originalFilename);
        String finalName = StrUtil.isBlank(name) ? (originalFilename == null ? "未命名素材" : originalFilename) : name.trim();
        File tempFile = null;
        try {
            tempFile = File.createTempFile("material_", "_" + (originalFilename == null ? "" : originalFilename));
            file.transferTo(tempFile);
            String dateDir = DateUtil.format(DateUtil.date(), "yyyyMMdd");
            String key = String.format("materials/%s/%s/%s_%s", loginUser.getId(), dateDir,
                    UUID.randomUUID().toString(), originalFilename == null ? "" : originalFilename);
            String url = cosManager.uploadFile(key, tempFile);
            ThrowUtils.throwIf(StrUtil.isBlank(url), ErrorCode.OPERATION_ERROR, "文件上传失败");
            Material material = new Material();
            material.setName(finalName);
            material.setType(type);
            material.setUrl(url);
            material.setSize(file.getSize());
            material.setUserId(loginUser.getId());
            material.setFolderId(folderId);
            boolean result = this.save(material);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            ingestMaterialVector(material);
            return material.getId();
        } catch (IOException e) {
            log.error("素材上传异常", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文件上传失败");
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private String resolveType(String contentType, String filename) {
        String ct = StrUtil.isBlank(contentType) ? "" : contentType.toLowerCase();
        if (ct.startsWith("image/")) {
            return "image";
        }
        if (ct.startsWith("video/")) {
            return "video";
        }
        if (ct.startsWith("audio/")) {
            return "audio";
        }
        String lower = filename == null ? "" : filename.toLowerCase();
        if (lower.endsWith(".glb") || lower.endsWith(".gltf") || lower.endsWith(".obj")
                || lower.endsWith(".fbx") || lower.endsWith(".stl") || lower.endsWith(".blend")) {
            return "3d";
        }
        return "other";
    }

    @Override
    public Boolean deleteMaterial(Long id, User loginUser) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Material old = this.getById(id);
        ThrowUtils.throwIf(old == null, ErrorCode.NOT_FOUND_ERROR);
        if (!old.getUserId().equals(loginUser.getId())
                && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限删除该素材");
        }
        boolean result = this.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return true;
    }

    @Override
    public MaterialVO getMaterialVOById(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Material material = this.getById(id);
        ThrowUtils.throwIf(material == null, ErrorCode.NOT_FOUND_ERROR);
        return this.getMaterialVO(material);
    }

    @Override
    public Page<MaterialVO> listMaterialVOByPage(MaterialQueryRequest materialQueryRequest) {
        ThrowUtils.throwIf(materialQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = materialQueryRequest.getPageNum();
        long pageSize = materialQueryRequest.getPageSize();
        QueryWrapper queryWrapper = this.getQueryWrapper(materialQueryRequest);
        Page<Material> materialPage = this.page(Page.of(pageNum, pageSize), queryWrapper);
        Page<MaterialVO> materialVOPage = new Page<>(pageNum, pageSize, materialPage.getTotalRow());
        materialVOPage.setRecords(this.getMaterialVOList(materialPage.getRecords()));
        return materialVOPage;
    }

    @Override
    public MaterialVO getMaterialVO(Material material) {
        if (material == null) {
            return null;
        }
        MaterialVO materialVO = new MaterialVO();
        BeanUtil.copyProperties(material, materialVO);
        Long userId = material.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            materialVO.setUser(userVO);
        }
        return materialVO;
    }

    @Override
    public List<MaterialVO> getMaterialVOList(List<Material> materialList) {
        if (CollUtil.isEmpty(materialList)) {
            return new ArrayList<>();
        }
        Set<Long> userIds = materialList.stream()
                .map(Material::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return materialList.stream().map(material -> {
            MaterialVO materialVO = this.getMaterialVO(material);
            materialVO.setUser(userVOMap.get(material.getUserId()));
            return materialVO;
        }).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(MaterialQueryRequest materialQueryRequest) {
        if (materialQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        String name = materialQueryRequest.getName();
        String type = materialQueryRequest.getType();
        Long userId = materialQueryRequest.getUserId();
        Long folderId = materialQueryRequest.getFolderId();
        QueryWrapper queryWrapper = QueryWrapper.create()
                .like("name", name)
                .eq("type", type)
                .eq("userId", userId)
                .orderBy("createTime", false);
        if (folderId != null) {
            queryWrapper.eq("folderId", folderId);
        }
        return queryWrapper;
    }

    private void ingestMaterialVector(Material material) {
        if (vectorSearchService == null) {
            return;
        }
        try {
            vectorSearchService.ingest(
                    "material-" + material.getId(),
                    material.getName(),
                    Map.of(
                            "type", "material",
                            "materialId", material.getId(),
                            "userId", material.getUserId(),
                            "materialType", material.getType()
                    ));
        } catch (Exception e) {
            log.warn("素材向量入库失败, materialId={}", material.getId(), e);
        }
    }
}

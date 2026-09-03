package com.mao.maocodemother.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mao.maocodemother.mapper.GeneratedAssetMapper;
import com.mao.maocodemother.model.entity.GeneratedAsset;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 生成的外部素材（图片/视频/3D/PPT）持久化服务
 *
 * @author mao
 */
@Service
@Slf4j
public class GeneratedAssetService {

    @Resource
    private GeneratedAssetMapper generatedAssetMapper;

    /**
     * 保存一条生成素材记录
     */
    public GeneratedAsset saveAsset(Long appId, Long userId, String assetType, String url, String prompt) {
        GeneratedAsset asset = GeneratedAsset.builder()
                .appId(appId)
                .userId(userId)
                .assetType(assetType)
                .url(url)
                .prompt(prompt)
                .build();
        generatedAssetMapper.insert(asset);
        return asset;
    }

    /**
     * 查询某应用下全部生成素材（按创建时间倒序）
     */
    public List<GeneratedAsset> listByAppId(Long appId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId)
                .orderBy("createTime", false);
        return generatedAssetMapper.selectListByQuery(queryWrapper);
    }
}

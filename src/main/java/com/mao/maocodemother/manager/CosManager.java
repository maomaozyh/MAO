package com.mao.maocodemother.manager;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.CannedAccessControlList;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import cn.hutool.core.util.StrUtil;
import com.mao.maocodemother.config.CosClientConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * COS 对象存储管理器
 */
@Component
@Slf4j
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 启动时把桶设为公有读：项目所有直链 URL（素材/截图封面/图表）都直接暴露给前端 <img>，
     * 私有读会导致公网访问 403、图片加载不出来。幂等，重复执行无副作用。
     */
    @PostConstruct
    public void ensureBucketPublicRead() {
        try {
            cosClient.setBucketAcl(cosClientConfig.getBucket(), CannedAccessControlList.PublicRead);
            log.info("COS 桶已设置为公有读：{}", cosClientConfig.getBucket());
        } catch (Exception e) {
            log.error("设置 COS 桶公有读失败，直链 URL 可能 403 无法访问，bucket={}", cosClientConfig.getBucket(), e);
        }
    }

    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     * @return 上传结果
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 上传文件到 COS 并返回访问 URL
     *
     * @param key  COS对象键（完整路径）
     * @param file 要上传的文件
     * @return 文件的访问URL，失败返回null
     */
    public String uploadFile(String key, File file) {
        PutObjectResult result = putObject(key, file);
        if (result != null) {
            // host 配置可能不带协议前缀（如 maomao-xxx.cos.ap-guangzhou.myqcloud.com），补全 https:// 和末尾斜杠
            String host = cosClientConfig.getHost();
            if (StrUtil.isNotBlank(host)
                    && !host.startsWith("http://") && !host.startsWith("https://")) {
                host = "https://" + host;
            }
            if (StrUtil.isNotBlank(host) && !host.endsWith("/")) {
                host = host + "/";
            }
            String url = String.format("%s%s", host, key);
            log.info("文件上传到 COS 成功：{} -> {}", file.getName(), url);
            return url;
        } else {
            log.error("文件上传到 COS 失败：{}，返回结果为空", file.getName());
            return null;
        }
    }
}

package com.mao.maocodemother.service;

import com.mao.maocodemother.model.entity.SysConfig;

import java.util.List;

/**
 * 系统配置 服务。
 */
public interface SysConfigService {

    /**
     * 列出所有配置
     *
     * @return 配置列表
     */
    List<SysConfig> listAllConfig();

    /**
     * 按配置键更新配置值
     *
     * @param configKey   配置键
     * @param configValue 配置值
     */
    void updateConfig(String configKey, String configValue);

    /**
     * 读取配置值
     *
     * @param key          配置键
     * @param defaultValue 配置不存在时的默认值
     * @return 配置值
     */
    String getConfigValue(String key, String defaultValue);
}

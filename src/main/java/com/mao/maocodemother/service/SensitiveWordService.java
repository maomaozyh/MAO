package com.mao.maocodemother.service;

import java.util.List;
import java.util.Set;

/**
 * 敏感词过滤服务
 */
public interface SensitiveWordService {

    /**
     * 判断文本是否包含敏感词
     *
     * @param text 待检测文本
     * @return 是否包含敏感词
     */
    boolean containsSensitiveWord(String text);

    /**
     * 获取文本中包含的所有敏感词
     *
     * @param text 待检测文本
     * @return 敏感词列表
     */
    Set<String> findSensitiveWords(String text);

    /**
     * 过滤敏感词（替换为 *）
     *
     * @param text 待过滤文本
     * @return 过滤后的文本
     */
    String filter(String text);

    /**
     * 刷新敏感词缓存
     */
    void refreshCache();

    /**
     * 获取所有启用的敏感词
     *
     * @return 敏感词列表
     */
    List<String> getAllEnabledWords();
}

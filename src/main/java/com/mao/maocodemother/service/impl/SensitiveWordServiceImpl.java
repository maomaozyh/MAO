package com.mao.maocodemother.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mao.maocodemother.mapper.SysSensitiveWordMapper;
import com.mao.maocodemother.model.entity.SysSensitiveWord;
import com.mao.maocodemother.service.SensitiveWordService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 敏感词过滤服务实现（基于 DFA 算法）
 */
@Slf4j
@Service
public class SensitiveWordServiceImpl implements SensitiveWordService {

    @Resource
    private SysSensitiveWordMapper sensitiveWordMapper;

    /**
     * DFA 敏感词字典树
     */
    private final Map<Character, Map> sensitiveWordMap = new ConcurrentHashMap<>();

    /**
     * 所有启用的敏感词列表缓存
     */
    private volatile List<String> enabledWordCache = new ArrayList<>();

    @PostConstruct
    public void init() {
        refreshCache();
    }

    @Override
    public boolean containsSensitiveWord(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return findSensitiveWords(text).size() > 0;
    }

    @Override
    public Set<String> findSensitiveWords(String text) {
        Set<String> result = new HashSet<>();
        if (text == null || text.isEmpty()) {
            return result;
        }
        for (int i = 0; i < text.length(); i++) {
            String word = checkSensitiveWord(text, i);
            if (word != null) {
                result.add(word);
            }
        }
        return result;
    }

    @Override
    public String filter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        Set<String> sensitiveWords = findSensitiveWords(text);
        String result = text;
        for (String word : sensitiveWords) {
            String mask = "*".repeat(word.length());
            result = result.replace(word, mask);
        }
        return result;
    }

    @Override
    public void refreshCache() {
        try {
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq("enabled", 1)
                    .eq("isDelete", 0);
            List<SysSensitiveWord> words = sensitiveWordMapper.selectListByQuery(queryWrapper);

            // 重建 DFA 字典树
            Map<Character, Map> newMap = new HashMap<>();
            List<String> wordList = new ArrayList<>();

            for (SysSensitiveWord word : words) {
                String w = word.getWord();
                if (w == null || w.isEmpty()) {
                    continue;
                }
                wordList.add(w);
                addWordToDFA(newMap, w);
            }

            sensitiveWordMap.clear();
            sensitiveWordMap.putAll(newMap);
            enabledWordCache = wordList;

            log.info("敏感词缓存刷新成功，共加载 {} 个敏感词", wordList.size());
        } catch (Exception e) {
            log.error("刷新敏感词缓存失败", e);
        }
    }

    @Override
    public List<String> getAllEnabledWords() {
        return new ArrayList<>(enabledWordCache);
    }

    /**
     * 将敏感词添加到 DFA 字典树
     */
    @SuppressWarnings("unchecked")
    private void addWordToDFA(Map<Character, Map> map, String word) {
        Map<Character, Map> currentMap = map;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            Map<Character, Map> subMap = currentMap.get(c);
            if (subMap == null) {
                subMap = new HashMap<>();
                currentMap.put(c, subMap);
            }
            currentMap = subMap;
            // 最后一个字符标记结束
            if (i == word.length() - 1) {
                currentMap.put('\0', Collections.emptyMap());
            }
        }
    }

    /**
     * 从指定位置开始检查是否存在敏感词
     *
     * @return 匹配到的敏感词，未匹配返回 null
     */
    @SuppressWarnings("unchecked")
    private String checkSensitiveWord(String text, int startIndex) {
        Map<Character, Map> currentMap = sensitiveWordMap;
        StringBuilder sb = new StringBuilder();
        boolean found = false;

        for (int i = startIndex; i < text.length(); i++) {
            char c = text.charAt(i);
            // 跳过空格等空白字符（可选）
            if (Character.isWhitespace(c)) {
                if (sb.length() > 0) {
                    sb.append(c);
                }
                continue;
            }

            Map<Character, Map> subMap = currentMap.get(c);
            if (subMap == null) {
                break;
            }

            sb.append(c);
            currentMap = subMap;

            // 检查是否到达敏感词末尾
            if (currentMap.containsKey('\0')) {
                found = true;
                break;
            }
        }

        return found ? sb.toString() : null;
    }
}

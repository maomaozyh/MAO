package com.mao.maocodemother.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 语义搜索查询词扩展服务
 * 将用户的自然语言搜索词扩展为 3~5 个相关关键词/同义词（JSON 数组字符串）
 */
public interface SemanticSearchExpandService {

    /**
     * 扩展搜索关键词
     *
     * @param keyword 自然语言搜索词
     * @return JSON 数组字符串，如 ["个人主页","简历","作品集"]
     */
    @SystemMessage(fromResource = "prompt/semantic-search-expand-system-prompt.txt")
    String expandKeywords(@UserMessage String keyword);
}

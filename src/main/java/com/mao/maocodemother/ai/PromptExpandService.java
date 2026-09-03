package com.mao.maocodemother.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 描述智能扩写服务
 * 将一句话需求扩写为包含功能点、页面结构、交互细节的详细需求描述
 */
public interface PromptExpandService {

    /**
     * 扩写用户的一句话需求
     *
     * @param prompt 一句话需求
     * @return 扩写后的详细需求描述（150-300 字）
     */
    @SystemMessage(fromResource = "prompt/prompt-expand-system-prompt.txt")
    String expandPrompt(@UserMessage String prompt);
}

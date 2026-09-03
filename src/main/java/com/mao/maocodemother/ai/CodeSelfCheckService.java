package com.mao.maocodemother.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 代码错误自检服务
 * 分析生成代码的常见问题并返回 JSON 字符串
 */
public interface CodeSelfCheckService {

    /**
     * 检查代码问题并尝试修复
     *
     * @param codeContent 代码内容（含文件路径标记）
     * @return JSON 字符串：{ "hasIssue": true/false, "issues": [...], "fixedCode": "..." }
     */
    @SystemMessage(fromResource = "prompt/code-selfcheck-system-prompt.txt")
    String checkAndFixCode(@UserMessage String codeContent);
}

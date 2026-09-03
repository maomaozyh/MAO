package com.mao.maocodemother.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 代码错误自检服务工厂
 * 复用项目已有的 openAiChatModel（DeepSeek 封装），不额外创建第二套模型配置
 */
@Configuration
public class CodeSelfCheckServiceFactory {

    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;

    @Bean
    public CodeSelfCheckService createCodeSelfCheckService() {
        return AiServices.builder(CodeSelfCheckService.class)
                .chatModel(chatModel)
                .build();
    }
}

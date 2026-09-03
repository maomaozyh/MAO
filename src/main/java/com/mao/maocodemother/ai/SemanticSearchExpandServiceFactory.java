package com.mao.maocodemother.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 语义搜索查询词扩展服务工厂
 * 复用项目已有的 openAiChatModel（DeepSeek 封装），不额外创建第二套模型配置
 */
@Configuration
public class SemanticSearchExpandServiceFactory {

    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;

    @Bean
    public SemanticSearchExpandService createSemanticSearchExpandService() {
        return AiServices.builder(SemanticSearchExpandService.class)
                .chatModel(chatModel)
                .build();
    }
}

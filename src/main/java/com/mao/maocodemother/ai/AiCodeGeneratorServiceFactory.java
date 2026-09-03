package com.mao.maocodemother.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mao.maocodemother.ai.guardrail.PromptSafetyInputGuardrail;
import com.mao.maocodemother.ai.guardrail.RetryOutputGuardrail;
import com.mao.maocodemother.ai.mcp.SkillMcpToolManager;
import com.mao.maocodemother.ai.mcp.SkillMcpToolProvider;
import com.mao.maocodemother.ai.tools.*;
import com.mao.maocodemother.exception.BusinessException;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.model.entity.Skill;
import com.mao.maocodemother.model.enums.CodeGenTypeEnum;
import com.mao.maocodemother.service.ChatHistoryService;
import com.mao.maocodemother.utils.SpringContextUtil;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AI 服务创建工厂
 */
@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ToolManager toolManager;

    @Resource
    private SkillMcpToolManager skillMcpToolManager;

    /**
     * AI 服务实例缓存
     * 缓存策略：
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，缓存键: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * 根据 appId 获取服务（为了兼容老逻辑）
     *
     * @param appId
     * @return
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }

    /**
     * 根据 appId 获取服务
     *
     * @param appId       应用 id
     * @param codeGenType 生成类型
     * @return
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        return getAiCodeGeneratorService(appId, codeGenType, null);
    }

    /**
     * 根据 appId 和技能获取服务（技能可携带专属 MCP 工具集）
     *
     * @param appId       应用 id
     * @param codeGenType 生成类型
     * @param skill       绑定的技能，可为 null
     * @return
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType, Skill skill) {
        String cacheKey = buildCacheKey(appId, codeGenType);
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenType, skill));
    }

    /**
     * 创建新的 AI 服务实例
     *
     * @param appId       应用 id
     * @param codeGenType 生成类型
     * @return
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        return createAiCodeGeneratorService(appId, codeGenType, null);
    }

    /**
     * 创建新的 AI 服务实例（支持技能专属 MCP 工具）
     *
     * @param appId       应用 id
     * @param codeGenType 生成类型
     * @param skill       绑定的技能，可为 null
     * @return
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType, Skill skill) {
        log.info("为 appId: {} 创建新的 AI 服务实例，技能：{}", appId,
                skill != null ? skill.getSkillName() : "无");
        // 根据 appId 构建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        // 从数据库中加载对话历史到记忆中
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);

        // 获取技能专属的 MCP 工具提供者（如有）
        SkillMcpToolProvider mcpToolProvider = null;
        if (skill != null) {
            mcpToolProvider = skillMcpToolManager.getMcpToolProvider(skill);
        }
        final SkillMcpToolProvider finalMcpToolProvider = mcpToolProvider;

        return switch (codeGenType) {
            // Vue 项目生成，使用工具调用和推理模型
            case VUE_PROJECT -> {
                // 使用多例模式的 StreamingChatModel 解决并发问题
                StreamingChatModel reasoningStreamingChatModel = SpringContextUtil.getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
                var builder = AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(reasoningStreamingChatModel)
                        .chatMemoryProvider(memoryId -> chatMemory)
                        .tools(toolManager.getAllTools())
                        // 处理工具调用幻觉问题
                        .hallucinatedToolNameStrategy(toolExecutionRequest ->
                                ToolExecutionResultMessage.from(toolExecutionRequest,
                                        "Error: there is no tool called " + toolExecutionRequest.name())
                        )
                        .maxSequentialToolsInvocations(20)  // 最多连续调用 20 次工具
                        .inputGuardrails(new PromptSafetyInputGuardrail()); // 添加输入护轨
                // 注入技能专属 MCP 工具（通过 ToolProvider 动态提供）
                if (finalMcpToolProvider != null) {
                    builder.toolProvider(finalMcpToolProvider);
                }
                yield builder.build();
            }
            // HTML 和 多文件生成，使用流式对话模型
            case HTML, MULTI_FILE -> {
                // 使用多例模式的 StreamingChatModel 解决并发问题
                StreamingChatModel openAiStreamingChatModel = SpringContextUtil.getBean("streamingChatModelPrototype", StreamingChatModel.class);
                var builder = AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(openAiStreamingChatModel)
                        .chatMemory(chatMemory)
                        .inputGuardrails(new PromptSafetyInputGuardrail()); // 添加输入护轨
                // 注入技能专属 MCP 工具（通过 ToolProvider 动态提供）
                if (finalMcpToolProvider != null) {
                    builder.toolProvider(finalMcpToolProvider);
                }
                yield builder.build();
            }
            default ->
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型: " + codeGenType.getValue());
        };
    }

    /**
     * 创建 AI 代码生成器服务
     *
     * @return
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return getAiCodeGeneratorService(0);
    }

    /**
     * 构造缓存键
     *
     * @param appId
     * @param codeGenType
     * @return
     */
    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }
}

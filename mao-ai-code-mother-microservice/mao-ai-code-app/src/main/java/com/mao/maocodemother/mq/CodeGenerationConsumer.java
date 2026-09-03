package com.mao.maocodemother.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mao.maocodemother.ai.service.VectorSearchService;
import com.mao.maocodemother.model.entity.App;
import com.mao.maocodemother.service.AppService;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 代码生成任务消费者。
 * 异步处理应用创建后的后置任务（向量入库、知识库准备等）。
 */
@Component
public class CodeGenerationConsumer {

    private static final Logger log = LoggerFactory.getLogger(CodeGenerationConsumer.class);

    private final ObjectMapper objectMapper;
    private final AppService appService;
    private final VectorSearchService vectorSearchService;

    public CodeGenerationConsumer(ObjectMapper objectMapper,
                                  AppService appService,
                                  @Autowired(required = false) VectorSearchService vectorSearchService) {
        this.objectMapper = objectMapper;
        this.appService = appService;
        this.vectorSearchService = vectorSearchService;
    }

    @RabbitListener(queues = "#{codegenTaskQueue.name}")
    public void handleGenerationTask(String payload) {
        GenerationTaskMessage message;
        try {
            message = objectMapper.readValue(payload, GenerationTaskMessage.class);
        } catch (Exception e) {
            // 消息本身非法，重投也无法恢复：抛异常后由容器转入死信队列
            log.error("[RabbitMQ] 代码生成任务消息格式非法，转入死信队列, payload={}", payload, e);
            throw new IllegalArgumentException("非法的代码生成任务消息", e);
        }
        log.info("[RabbitMQ] 开始处理代码生成任务, appId={}, appName={}, codeGenType={}",
                message.getAppId(), message.getAppName(), message.getCodeGenType());

        App app = appService.getById(message.getAppId());
        if (app == null) {
            // 应用已被删除，属于不可恢复场景：正常返回即确认消息，避免无意义重投
            log.warn("[RabbitMQ] 应用不存在，跳过任务并确认消息, appId={}", message.getAppId());
            return;
        }

        try {
            ingestInitPrompt(app, message);
            log.info("[RabbitMQ] 代码生成任务处理完成, appId={}", message.getAppId());
        } catch (Exception e) {
            // 处理失败必须抛出异常：否则容器会确认消息，导致任务永久丢失。
            // 抛出后由容器按重试策略重投，重试耗尽后经死信交换机进入死信队列。
            log.error("[RabbitMQ] 处理代码生成任务失败，等待重试或转入死信队列, appId={}", message.getAppId(), e);
            throw new IllegalStateException("代码生成任务处理失败, appId=" + message.getAppId(), e);
        }
    }

    private void ingestInitPrompt(App app, GenerationTaskMessage message) {
        if (vectorSearchService == null) {
            return;
        }
        String prompt = StrUtil.blankToDefault(message.getInitPrompt(), app.getInitPrompt());
        if (StrUtil.isBlank(prompt)) {
            return;
        }
        try {
            String text = StrUtil.blankToDefault(app.getAppName(), "未命名应用") + "\n" + prompt;
            vectorSearchService.ingest(
                    "app-" + app.getId(),
                    text,
                    Map.of(
                            "type", "app",
                            "appId", app.getId(),
                            "userId", app.getUserId(),
                            "codeGenType", StrUtil.blankToDefault(app.getCodeGenType(), "")
                    ));
            log.info("[RabbitMQ] 已将应用初始 prompt 写入向量库, appId={}", app.getId());
        } catch (Exception e) {
            log.warn("[RabbitMQ] 向量入库失败（Milvus 可能未启动）, appId={}", app.getId(), e);
        }
    }
}

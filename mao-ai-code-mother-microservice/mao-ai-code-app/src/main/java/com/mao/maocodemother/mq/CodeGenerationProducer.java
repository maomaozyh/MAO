package com.mao.maocodemother.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 代码生成任务生产者。
 * 在应用创建后将任务投递到 RabbitMQ，由消费者异步处理。
 */
@Component
public class CodeGenerationProducer {

    private static final Logger log = LoggerFactory.getLogger(CodeGenerationProducer.class);

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Value("${rabbitmq.exchange:mao-ai-code-exchange}")
    private String exchangeName;

    @Value("${rabbitmq.routing-key.codegen:codegen.task}")
    private String codegenRoutingKey;

    /**
     * 发送代码生成异步任务。
     *
     * @param message 任务消息
     */
    public void sendGenerationTask(GenerationTaskMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            rabbitTemplate.convertAndSend(exchangeName, codegenRoutingKey, payload);
            log.info("[RabbitMQ] 已发送代码生成任务, appId={}, userId={}", message.getAppId(), message.getUserId());
        } catch (Exception e) {
            log.error("[RabbitMQ] 发送代码生成任务失败, appId={}", message.getAppId(), e);
        }
    }
}

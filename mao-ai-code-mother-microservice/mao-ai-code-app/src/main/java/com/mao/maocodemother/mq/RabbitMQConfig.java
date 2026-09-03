package com.mao.maocodemother.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 队列/交换机声明。
 * 通过环境变量可覆盖名称，便于多环境隔离。
 *
 * <p>可靠性设计：业务队列统一绑定死信交换机。消费失败且重试耗尽后消息会转投死信队列，
 * 而不是被无限重投或直接丢弃，避免 AI 代码生成任务静默丢失。</p>
 *
 * <p>注意：如果 broker 中已存在同名队列且未配置死信参数，重新声明会因参数不一致
 * 触发 PRECONDITION_FAILED。上线前需先删除旧队列（或迁移）后再部署。</p>
 */
@Configuration
public class RabbitMQConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfig.class);

    @Value("${rabbitmq.exchange:mao-ai-code-exchange}")
    private String exchangeName;

    @Value("${rabbitmq.queue.codegen:codegen.task.queue}")
    private String codegenQueueName;

    @Value("${rabbitmq.routing-key.codegen:codegen.task}")
    private String codegenRoutingKey;

    @Value("${rabbitmq.dlx.exchange:mao-ai-code-exchange.dlx}")
    private String deadLetterExchangeName;

    @Value("${rabbitmq.dlx.queue.codegen:codegen.task.queue.dlq}")
    private String deadLetterQueueName;

    @Value("${rabbitmq.dlx.routing-key.codegen:codegen.task.dlq}")
    private String deadLetterRoutingKey;

    @Bean
    public TopicExchange yuAiCodeExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    /**
     * 死信交换机：接收消费失败且重试耗尽的消息，便于人工排查与重放。
     */
    @Bean
    public TopicExchange yuAiCodeDeadLetterExchange() {
        return new TopicExchange(deadLetterExchangeName, true, false);
    }

    /**
     * 业务队列：指定死信交换机与路由键，替换原先无死信的裸队列。
     */
    @Bean
    public Queue codegenTaskQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", deadLetterExchangeName);
        args.put("x-dead-letter-routing-key", deadLetterRoutingKey);
        return QueueBuilder.durable(codegenQueueName).withArguments(args).build();
    }

    @Bean
    public Queue codegenTaskDeadLetterQueue() {
        return QueueBuilder.durable(deadLetterQueueName).build();
    }

    @Bean
    public Binding codegenTaskBinding(Queue codegenTaskQueue, TopicExchange yuAiCodeExchange) {
        return BindingBuilder.bind(codegenTaskQueue).to(yuAiCodeExchange).with(codegenRoutingKey);
    }

    @Bean
    public Binding codegenTaskDeadLetterBinding(Queue codegenTaskDeadLetterQueue,
                                                TopicExchange yuAiCodeDeadLetterExchange) {
        return BindingBuilder.bind(codegenTaskDeadLetterQueue)
                .to(yuAiCodeDeadLetterExchange)
                .with(deadLetterRoutingKey);
    }

    /**
     * 生产者确认与回退回调：消息未到达交换机、或未路由到任何队列时打日志，
     * 便于快速发现"消息发出去了但没进队列"的静默失败。
     */
    @Bean
    public RabbitTemplateCustomizer rabbitTemplateCustomizer() {
        return new RabbitTemplateCustomizer() {
            @Override
            public void customize(RabbitTemplate template) {
                template.setConfirmCallback((correlationData, ack, cause) -> {
                    if (Boolean.FALSE.equals(ack)) {
                        log.error("[RabbitMQ] 消息未到达交换机, correlationData={}, cause={}", correlationData, cause);
                    }
                });
                template.setReturnsCallback(returned -> log.error(
                        "[RabbitMQ] 消息未路由到队列, exchange={}, routingKey={}, replyCode={}, replyText={}",
                        returned.getExchange(), returned.getRoutingKey(),
                        returned.getReplyCode(), returned.getReplyText()));
            }
        };
    }
}

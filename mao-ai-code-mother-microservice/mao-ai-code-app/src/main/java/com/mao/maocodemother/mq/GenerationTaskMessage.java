package com.mao.maocodemother.mq;

import java.io.Serializable;

/**
 * 代码生成异步任务消息。
 * 在应用创建后由生产者发送到 RabbitMQ，由消费者异步处理（如预生成、素材准备、知识库写入等）。
 */
public class GenerationTaskMessage implements Serializable {

    private Long appId;

    private Long userId;

    private String appName;

    private String codeGenType;

    private String initPrompt;

    private Long timestamp;

    public GenerationTaskMessage() {
    }

    public GenerationTaskMessage(Long appId, Long userId, String appName, String codeGenType, String initPrompt) {
        this.appId = appId;
        this.userId = userId;
        this.appName = appName;
        this.codeGenType = codeGenType;
        this.initPrompt = initPrompt;
        this.timestamp = System.currentTimeMillis();
    }

    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getCodeGenType() {
        return codeGenType;
    }

    public void setCodeGenType(String codeGenType) {
        this.codeGenType = codeGenType;
    }

    public String getInitPrompt() {
        return initPrompt;
    }

    public void setInitPrompt(String initPrompt) {
        this.initPrompt = initPrompt;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}

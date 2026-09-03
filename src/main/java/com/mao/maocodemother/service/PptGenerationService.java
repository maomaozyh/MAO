package com.mao.maocodemother.service;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mao.maocodemother.exception.BusinessException;
import com.mao.maocodemother.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PPT 生成服务
 * <p>
 * 复用 DashScope 的 OpenAI 兼容对话接口（qwen 系列）让大模型产出「结构化幻灯片 JSON」，
 * 前端再用 pptxgenjs 在浏览器端把这份 JSON 渲染成可下载的 .pptx 文件（无需后端 POI 依赖与文件存储）。
 * 复用 application.yml 中的 {@code dashscope.api-key}。
 *
 * @author mao
 */
@Service
@Slf4j
public class PptGenerationService {

    private static final String CHAT_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String SYSTEM_PROMPT = "你是一个专业的演示文稿大纲撰写助手。"
            + "请根据用户主题，输出一个结构化的幻灯片大纲，严格只用 JSON 返回，不要包含任何解释或 markdown 代码块标记。"
            + "JSON 结构：{\"title\":\"演示文稿标题\",\"slides\":[{\"title\":\"页标题\",\"bullets\":[\"要点1\",\"要点2\"],\"notes\":\"演讲备注\"}]}。"
            + "slides 数组包含 5 到 10 页，每页 bullets 为 3 到 6 条简洁要点。";

    @Value("${dashscope.api-key:}")
    private String apiKey;

    @Value("${dashscope.chat-model:qwen-turbo}")
    private String chatModel;

    private final RestTemplate restTemplate = buildRestTemplate();

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(120000);
        return new RestTemplate(factory);
    }

    /**
     * 根据主题生成结构化幻灯片 JSON 字符串（前端据此导出 .pptx）
     *
     * @param prompt 演示文稿主题
     * @return 幻灯片 JSON 字符串
     */
    public String generateSlidesJson(String prompt) {
        if (StrUtil.isBlank(prompt)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "演示文稿主题不能为空");
        }
        if (StrUtil.isBlank(apiKey)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "PPT 生成 API Key 未配置，请在 application.yml 设置 dashscope.api-key");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        messages.add(Map.of("role", "user", "content", prompt));

        Map<String, Object> body = new HashMap<>(8);
        body.put("model", chatModel);
        body.put("messages", messages);
        body.put("temperature", 0.7);
        body.put("response_format", Map.of("type", "json_object"));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<ChatCompletionResponse> response = restTemplate.exchange(
                    CHAT_URL, HttpMethod.POST, entity, ChatCompletionResponse.class);
            ChatCompletionResponse resp = response.getBody();
            if (resp == null || resp.getChoices() == null || resp.getChoices().isEmpty()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "PPT 大纲生成失败：未返回有效结果");
            }
            String content = resp.getChoices().get(0).getMessage().getContent();
            String json = extractJson(content);
            validateSlidesJson(json);
            return json;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("生成 PPT 大纲失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "PPT 大纲生成失败：" + e.getMessage());
        }
    }

    /**
     * 从模型返回中剔除可能的 ```json 代码块标记，提取纯 JSON
     */
    private String extractJson(String content) {
        if (content == null) {
            return null;
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    /**
     * 校验 JSON 结构是否含 title + slides 数组
     */
    private void validateSlidesJson(String json) {
        if (StrUtil.isBlank(json)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "PPT 大纲生成失败：结果为空");
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            if (!root.has("slides") || !root.get("slides").isArray() || root.get("slides").isEmpty()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "PPT 大纲生成失败：结构不合法");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "PPT 大纲生成失败：JSON 解析错误");
        }
    }

    // ===== 响应 DTO =====

    public static class ChatCompletionResponse {
        private List<ChatChoice> choices;

        public List<ChatChoice> getChoices() {
            return choices;
        }

        public void setChoices(List<ChatChoice> choices) {
            this.choices = choices;
        }
    }

    public static class ChatChoice {
        private ChatMessage message;

        public ChatMessage getMessage() {
            return message;
        }

        public void setMessage(ChatMessage message) {
            this.message = message;
        }
    }

    public static class ChatMessage {
        private String role;
        private String content;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}

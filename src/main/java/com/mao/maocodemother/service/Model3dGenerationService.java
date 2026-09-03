package com.mao.maocodemother.service;

import cn.hutool.core.util.StrUtil;
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

import java.util.HashMap;
import java.util.Map;

/**
 * 3D 模型生成服务
 * <p>
 * 抽象对接 Tripo3D 风格的文生 3D 接口（https://api.tripo3d.ai/v2/openapi）。
 * 通过 {@code tripo.api-key} 配置，未配置时抛清晰错误提示用户接入凭证。
 * 接口为异步任务模式：提交任务 → 轮询状态 → 成功后返回 GLB 模型地址。
 * 如需切换其它服务商（Meshy 等），替换 base-url 与响应字段解析即可。
 *
 * @author mao
 */
@Service
@Slf4j
public class Model3dGenerationService {

    @Value("${tripo.api-key:}")
    private String apiKey;

    @Value("${tripo.base-url:https://api.tripo3d.ai/v2/openapi}")
    private String baseUrl;

    @Value("${tripo.poll-interval-ms:5000}")
    private long pollIntervalMs;

    private static final int MAX_POLL_ATTEMPTS = 80;

    private final RestTemplate restTemplate = buildRestTemplate();

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(120000);
        return new RestTemplate(factory);
    }

    /**
     * 根据提示词生成 3D 模型，返回 GLB 模型地址
     *
     * @param prompt 模型描述（英文效果更佳，Tripo 对中文支持有限）
     * @return 模型 URL
     */
    public String generateModel(String prompt) {
        if (StrUtil.isBlank(prompt)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "模型描述不能为空");
        }
        if (StrUtil.isBlank(apiKey)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "3D 生成 API Key 未配置，请在 application.yml 设置 tripo.api-key");
        }
        String taskId = submitTask(prompt);
        return pollTask(taskId);
    }

    private String submitTask(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        Map<String, Object> body = new HashMap<>(2);
        body.put("type", "text_to_model");
        body.put("prompt", prompt);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<TripoCreateResponse> response = restTemplate.exchange(
                    baseUrl + "/task", HttpMethod.POST, entity, TripoCreateResponse.class);
            TripoCreateResponse resp = response.getBody();
            if (resp == null || resp.getData() == null || StrUtil.isBlank(resp.getData().getTaskId())) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "3D 生成任务提交失败：未返回 task_id");
            }
            if (resp.getCode() != null && resp.getCode() != 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "3D 生成任务提交失败：code=" + resp.getCode());
            }
            return resp.getData().getTaskId();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("提交 3D 生成任务失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "3D 生成任务提交失败：" + e.getMessage());
        }
    }

    private String pollTask(String taskId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        for (int i = 0; i < MAX_POLL_ATTEMPTS; i++) {
            try {
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "3D 生成轮询被中断");
            }
            try {
                ResponseEntity<TripoQueryResponse> response = restTemplate.exchange(
                        baseUrl + "/task/" + taskId, HttpMethod.GET, entity, TripoQueryResponse.class);
                TripoQueryResponse resp = response.getBody();
                if (resp == null || resp.getData() == null) {
                    continue;
                }
                String status = resp.getData().getStatus();
                if ("success".equals(status)) {
                    TripoOutput output = resp.getData().getOutput();
                    if (output != null) {
                        String url = StrUtil.isNotBlank(output.getPbrModel())
                                ? output.getPbrModel()
                                : output.getModel();
                        if (StrUtil.isNotBlank(url)) {
                            return url;
                        }
                    }
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "3D 生成成功但未返回模型地址");
                }
                if ("failed".equals(status)) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "3D 生成失败");
                }
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.warn("轮询 3D 任务异常，继续重试：{}", e.getMessage());
            }
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "3D 生成超时（任务未完成）");
    }

    // ===== 响应 DTO =====

    public static class TripoCreateResponse {
        private Integer code;
        private TripoTaskData data;

        public Integer getCode() {
            return code;
        }

        public void setCode(Integer code) {
            this.code = code;
        }

        public TripoTaskData getData() {
            return data;
        }

        public void setData(TripoTaskData data) {
            this.data = data;
        }
    }

    public static class TripoTaskData {
        private String taskId;
        private String type;
        private String status;

        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class TripoQueryResponse {
        private Integer code;
        private TripoQueryData data;

        public Integer getCode() {
            return code;
        }

        public void setCode(Integer code) {
            this.code = code;
        }

        public TripoQueryData getData() {
            return data;
        }

        public void setData(TripoQueryData data) {
            this.data = data;
        }
    }

    public static class TripoQueryData {
        private String taskId;
        private String type;
        private String status;
        private TripoOutput output;

        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public TripoOutput getOutput() {
            return output;
        }

        public void setOutput(TripoOutput output) {
            this.output = output;
        }
    }

    public static class TripoOutput {
        private String pbrModel;
        private String model;
        private String baseModel;

        public String getPbrModel() {
            return pbrModel;
        }

        public void setPbrModel(String pbrModel) {
            this.pbrModel = pbrModel;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getBaseModel() {
            return baseModel;
        }

        public void setBaseModel(String baseModel) {
            this.baseModel = baseModel;
        }
    }
}

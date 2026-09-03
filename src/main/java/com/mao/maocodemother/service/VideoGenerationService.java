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
 * 视频生成服务
 * <p>
 * 调用阿里云 DashScope 通义万相「文生视频」任务接口（wanx2.1-t2v 系列）。
 * 该接口为异步任务模式：先提交任务拿到 task_id，再轮询任务状态，成功后返回视频 URL。
 * 复用 application.yml 中的 {@code dashscope.api-key}。
 *
 * @author mao
 */
@Service
@Slf4j
public class VideoGenerationService {

    private static final String SUBMIT_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/aigc/video-generation/video-synthesis";
    private static final String TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/";
    private static final long POLL_INTERVAL_MS = 5000L;
    private static final int MAX_POLL_ATTEMPTS = 60;

    @Value("${dashscope.api-key:}")
    private String apiKey;

    @Value("${dashscope.video-model:wanx2.1-t2v-turbo}")
    private String videoModel;

    private final RestTemplate restTemplate = buildRestTemplate();

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(120000);
        return new RestTemplate(factory);
    }

    /**
     * 根据提示词生成视频，返回视频 URL（同步阻塞直到任务完成或超时）
     *
     * @param prompt 视频描述
     * @return 视频 URL
     */
    public String generateVideo(String prompt) {
        if (StrUtil.isBlank(prompt)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "视频描述不能为空");
        }
        if (StrUtil.isBlank(apiKey)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "视频生成 API Key 未配置，请在 application.yml 设置 dashscope.api-key");
        }
        String taskId = submitTask(prompt);
        return pollTask(taskId);
    }

    /**
     * 提交视频生成任务
     */
    private String submitTask(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        headers.set("X-DashScope-Async", "enable");

        Map<String, Object> input = new HashMap<>(2);
        input.put("prompt", prompt);
        Map<String, Object> parameters = new HashMap<>(2);
        parameters.put("size", "1280*720");
        parameters.put("duration", 5);
        Map<String, Object> body = new HashMap<>(4);
        body.put("model", videoModel);
        body.put("input", input);
        body.put("parameters", parameters);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<VideoSubmitResponse> response = restTemplate.exchange(
                    SUBMIT_URL, HttpMethod.POST, entity, VideoSubmitResponse.class);
            VideoSubmitResponse resp = response.getBody();
            if (resp == null || resp.getOutput() == null || StrUtil.isBlank(resp.getOutput().getTaskId())) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "视频生成任务提交失败：未返回 task_id");
            }
            return resp.getOutput().getTaskId();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("提交视频生成任务失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "视频生成任务提交失败：" + e.getMessage());
        }
    }

    /**
     * 轮询任务状态直到成功或超时
     */
    private String pollTask(String taskId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        for (int i = 0; i < MAX_POLL_ATTEMPTS; i++) {
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "视频生成轮询被中断");
            }
            try {
                ResponseEntity<VideoTaskResponse> response = restTemplate.exchange(
                        TASK_URL + taskId, HttpMethod.GET, entity, VideoTaskResponse.class);
                VideoTaskResponse resp = response.getBody();
                if (resp == null || resp.getOutput() == null) {
                    continue;
                }
                String status = resp.getOutput().getTaskStatus();
                if ("SUCCEEDED".equals(status)) {
                    String url = resp.getOutput().getVideoUrl();
                    if (StrUtil.isNotBlank(url)) {
                        return url;
                    }
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "视频生成成功但未返回视频地址");
                }
                if ("FAILED".equals(status)) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                            "视频生成失败：" + StrUtil.nullToEmpty(resp.getOutput().getMessage()));
                }
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.warn("轮询视频任务异常，继续重试：{}", e.getMessage());
            }
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "视频生成超时（任务未完成）");
    }

    // ===== 响应 DTO =====

    public static class VideoSubmitResponse {
        private VideoSubmitOutput output;
        private String requestId;

        public VideoSubmitOutput getOutput() {
            return output;
        }

        public void setOutput(VideoSubmitOutput output) {
            this.output = output;
        }

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }
    }

    public static class VideoSubmitOutput {
        private String taskId;
        private String taskStatus;

        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public String getTaskStatus() {
            return taskStatus;
        }

        public void setTaskStatus(String taskStatus) {
            this.taskStatus = taskStatus;
        }
    }

    public static class VideoTaskResponse {
        private VideoTaskOutput output;
        private String requestId;

        public VideoTaskOutput getOutput() {
            return output;
        }

        public void setOutput(VideoTaskOutput output) {
            this.output = output;
        }

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }
    }

    public static class VideoTaskOutput {
        private String taskStatus;
        private String videoUrl;
        private String message;

        public String getTaskStatus() {
            return taskStatus;
        }

        public void setTaskStatus(String taskStatus) {
            this.taskStatus = taskStatus;
        }

        public String getVideoUrl() {
            return videoUrl;
        }

        public void setVideoUrl(String videoUrl) {
            this.videoUrl = videoUrl;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}

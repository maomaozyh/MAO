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
import java.util.List;
import java.util.Map;

/**
 * 图像生成服务
 * <p>
 * 采用 OpenAI 兼容方式调用阿里云 DashScope 通义万相文生图接口
 * （端点：https://dashscope.aliyuncs.com/compatible-mode/v1/images/generations）。
 * 复用 application.yml 中已有的 dashscope.api-key / dashscope.image-model 配置，
 * 用户只需把 Key 填入 dashscope.api-key 即可（本项目已有的路由模型也在用同一个 Key）。
 *
 * <p>返回生成图片的可访问 URL；若接口返回 base64 则转换为 data URI。
 *
 * @author mao
 */
@Service
@Slf4j
public class ImageGenerationService {

    private static final String DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private static final int TIMEOUT_MS = 90000;

    @Value("${dashscope.api-key:}")
    private String apiKey;

    @Value("${dashscope.image-model:wan2.2-t2i-flash}")
    private String imageModel;

    private final RestTemplate restTemplate = buildRestTemplate();

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT_MS);
        factory.setReadTimeout(TIMEOUT_MS);
        return new RestTemplate(factory);
    }

    /**
     * 根据提示词生成一张图片
     *
     * @param prompt 图像描述（中文效果最佳）
     * @return 图片 URL 或 data URI
     */
    public String generateImage(String prompt) {
        if (StrUtil.isBlank(prompt)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图像描述不能为空");
        }
        if (StrUtil.isBlank(apiKey)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "图像生成 API Key 未配置，请在 application.yml 设置 dashscope.api-key");
        }
        String endpoint = DEFAULT_BASE_URL + "/images/generations";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        Map<String, Object> body = new HashMap<>(8);
        body.put("model", imageModel);
        body.put("prompt", prompt);
        body.put("n", 1);
        body.put("size", "1024x1024");
        body.put("response_format", "url");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ImageGenResponse resp;
        try {
            ResponseEntity<ImageGenResponse> responseEntity =
                    restTemplate.exchange(endpoint, HttpMethod.POST, entity, ImageGenResponse.class);
            resp = responseEntity.getBody();
        } catch (Exception e) {
            log.error("调用图像生成接口失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图像生成请求失败：" + e.getMessage());
        }
        if (resp == null || resp.getData() == null || resp.getData().isEmpty()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图像生成失败：未返回有效结果");
        }
        String imageUrl = resp.getData().get(0).getUrl();
        if (StrUtil.isBlank(imageUrl)) {
            String b64 = resp.getData().get(0).getB64Json();
            if (StrUtil.isNotBlank(b64)) {
                imageUrl = "data:image/png;base64," + b64;
            }
        }
        if (StrUtil.isBlank(imageUrl)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图像生成失败：未返回图片地址");
        }
        return imageUrl;
    }

    /**
     * DashScope / OpenAI 兼容图像生成响应
     */
    public static class ImageGenResponse {
        private List<ImageItem> data;

        public List<ImageItem> getData() {
            return data;
        }

        public void setData(List<ImageItem> data) {
            this.data = data;
        }
    }

    public static class ImageItem {
        private String url;
        private String b64Json;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getB64Json() {
            return b64Json;
        }

        public void setB64Json(String b64Json) {
            this.b64Json = b64Json;
        }
    }
}

package com.mao.maocodemother.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 图形验证码响应：前端拿到 captchaKey + 图片后，发码时把 key 和用户输入一并回传校验。
 */
@Data
public class CaptchaVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 验证码标识（Redis key 后缀，5 分钟过期）
     */
    private String captchaKey;

    /**
     * 验证码图片（base64 PNG data URL）
     */
    private String captchaImg;

    public CaptchaVO() {
    }

    public CaptchaVO(String captchaKey, String captchaImg) {
        this.captchaKey = captchaKey;
        this.captchaImg = captchaImg;
    }
}

package com.mao.maocodemother.gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查接口，供前端 /health/ 调用。
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping({"/", ""})
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("data", "ok");
        result.put("message", "ok");
        return result;
    }
}

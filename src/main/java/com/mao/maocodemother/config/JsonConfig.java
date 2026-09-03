package com.mao.maocodemother.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Spring MVC Json 配置
 */
@JsonComponent
public class JsonConfig {

    /**
     * Long 序列化策略：雪花 ID（id / 以 Id 结尾的字段）转字符串保精度，
     * 其余 Long（金额、计数）转数字，避免前端字符串拼接类 bug（如 "0"+"200"="0200"）。
     */
    @Bean
    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, new IdAwareLongSerializer());
        module.addSerializer(Long.TYPE, new IdAwareLongSerializer());
        objectMapper.registerModule(module);
        return objectMapper;
    }
}
package com.mao.maocodemother;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@MapperScan("com.mao.maocodemother.mapper")
@EnableDubbo
@EnableCaching
@EnableScheduling
public class YuAiCodeAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(YuAiCodeAppApplication.class, args);
    }
}
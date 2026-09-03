package com.mao.maocodemother.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 网关服务：统一入口（端口 8123），将 /api 请求路由到各微服务。
 *
 * @author <a href="https://github.com/liyupi">程序员mao</a>
 */
@SpringBootApplication(scanBasePackages = "com.mao.maocodemother.gateway")
public class YuAiCodeGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(YuAiCodeGatewayApplication.class, args);
    }
}

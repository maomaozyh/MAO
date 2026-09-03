package com.mao.maocodemother.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;

/**
 * XSS 防护配置
 * <p>
 * 通过 Jackson 全局 String 序列化器，在 JSON 输出时自动对字符串进行 HTML 转义，
 * 防止存储型 XSS 攻击。
 * <p>
 * 转义范围：所有通过接口返回的 String 类型字段
 * 转义规则：将 < > " ' & / ` = 等特殊字符转为 HTML 实体
 */
@Configuration
public class XssConfig implements WebMvcConfigurer {

    /**
     * 注册 XSS 转义序列化模块
     */
    @Bean
    public SimpleModule xssStringSerializerModule() {
        SimpleModule module = new SimpleModule("XssStringSerializer");
        module.addSerializer(String.class, new XssStringSerializer());
        return module;
    }

    /**
     * 自定义 String 序列化器，输出时自动 HTML 转义
     */
    public static class XssStringSerializer extends JsonSerializer<String> {

        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            if (value == null) {
                gen.writeNull();
                return;
            }
            gen.writeString(escapeHtml(value));
        }

        /**
         * HTML 转义，防止 XSS
         * 转义常见 XSS 攻击向量：< > " ' & / ` =
         */
        private String escapeHtml(String input) {
            StringBuilder sb = new StringBuilder(input.length() + 16);
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                switch (c) {
                    case '<':
                        sb.append("&lt;");
                        break;
                    case '>':
                        sb.append("&gt;");
                        break;
                    case '"':
                        sb.append("&quot;");
                        break;
                    case '\'':
                        sb.append("&#39;");
                        break;
                    case '&':
                        sb.append("&amp;");
                        break;
                    case '/':
                        sb.append("&#47;");
                        break;
                    case '`':
                        sb.append("&#96;");
                        break;
                    case '=':
                        sb.append("&#61;");
                        break;
                    default:
                        sb.append(c);
                }
            }
            return sb.toString();
        }
    }
}

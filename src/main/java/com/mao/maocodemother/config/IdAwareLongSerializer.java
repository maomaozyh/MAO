package com.mao.maocodemother.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;

/**
 * Long 序列化策略（根治余额被序列化成字符串后前端拼接出 "0200" 这类 bug）：
 * - 雪花 ID 字段（字段名为 id，或以 Id 结尾，如 userId / appId / postId）→ 序列化为字符串，保留 JS 精度；
 * - 其余 Long（金额、计数，如 secondsBalance / giftSecondsBalance / amount / totalUsers）→ 序列化为数字。
 */
public class IdAwareLongSerializer extends JsonSerializer<Long> implements ContextualSerializer {

    private final boolean asString;

    public IdAwareLongSerializer() {
        this(false);
    }

    public IdAwareLongSerializer(boolean asString) {
        this.asString = asString;
    }

    @Override
    public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        if (asString) {
            gen.writeString(value.toString());
        } else {
            gen.writeNumber(value);
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        if (property == null) {
            return this;
        }
        String name = property.getName();
        // 雪花 ID：字段名等于 id 或以 Id 结尾；其余 Long 当作普通数字
        boolean isId = name != null && (name.equals("id") || name.endsWith("Id"));
        return new IdAwareLongSerializer(isId);
    }
}

package com.mao.maocodemother.common;

import com.mao.maocodemother.exception.ErrorCode;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 通过响应类
 *
 * @param <T>
 */
@Data
/**
 * 必须提供无参构造：@Cacheable 会把本类序列化进 Redis（如 app_detail / good_app_page / admin_dashboard），
 * 缺少无参构造时 Jackson 无法反序列化，缓存命中会抛 InvalidDefinitionException 导致接口 500。
 */
@NoArgsConstructor
public class BaseResponse<T> implements Serializable {

    private int code;

    private T data;

    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}

package com.mao.maocodemother.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 安全审计日志注解
 * 标记需要记录审计日志的方法（如登录、修改密码、管理员操作等）
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /**
     * 操作类型（如 LOGIN, PASSWORD_CHANGE, ADMIN_UPDATE 等）
     */
    String actionType();

    /**
     * 操作描述
     */
    String actionDesc() default "";

    /**
     * 是否记录请求参数（默认记录，敏感操作可关闭）
     */
    boolean recordParams() default true;
}

package com.mao.maocodemother.aop;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mao.maocodemother.annotation.AuditLog;
import com.mao.maocodemother.mapper.AuditLogMapper;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 安全审计日志 AOP 切面
 * 自动拦截 @AuditLog 注解的方法，记录操作日志
 */
@Aspect
@Component
@Slf4j
public class AuditLogAspect {

    @Resource
    private AuditLogMapper auditLogMapper;

    @Resource
    private UserService userService;

    /**
     * 需要脱敏的参数名（不记录具体值）
     */
    private static final java.util.Set<String> SENSITIVE_PARAM_NAMES = java.util.Set.of(
            "userPassword", "password", "checkPassword", "newPassword",
            "oldPassword", "secret", "token", "apiKey", "secretKey"
    );

    @Around("@annotation(auditLogAnnotation)")
    public Object doAudit(ProceedingJoinPoint joinPoint, AuditLog auditLogAnnotation) throws Throwable {
        long startTime = System.currentTimeMillis();
        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        // 构建日志对象
        com.mao.maocodemother.model.entity.AuditLog auditLog =
                com.mao.maocodemother.model.entity.AuditLog.builder()
                .actionType(auditLogAnnotation.actionType())
                .actionDesc(auditLogAnnotation.actionDesc())
                .createTime(LocalDateTime.now())
                .build();

        if (request != null) {
            auditLog.setClientIp(getClientIp(request));
            auditLog.setUserAgent(request.getHeader("User-Agent"));
            auditLog.setRequestMethod(request.getMethod());
            auditLog.setRequestUri(request.getRequestURI());
            // 记录请求参数（脱敏）
            if (auditLogAnnotation.recordParams()) {
                auditLog.setRequestParams(getSanitizedParams(joinPoint));
            }
        }

        // 获取当前登录用户
        try {
            User loginUser = getLoginUserSafe(request);
            if (loginUser != null) {
                auditLog.setUserId(loginUser.getId());
                auditLog.setUserAccount(loginUser.getUserAccount());
            }
        } catch (Exception e) {
            // 未登录时忽略，登录方法本身就是未登录状态调用的
        }

        Object result;
        try {
            result = joinPoint.proceed();
            auditLog.setResultStatus("SUCCESS");
            return result;
        } catch (Throwable e) {
            auditLog.setResultStatus("FAIL");
            auditLog.setFailReason(e.getMessage() != null ? e.getMessage().substring(0, Math.min(500, e.getMessage().length())) : null);
            throw e;
        } finally {
            auditLog.setCostMs(System.currentTimeMillis() - startTime);
            // 异步写入数据库，不影响主流程性能
            saveAuditLogAsync(auditLog);
        }
    }

    /**
     * 异步保存审计日志
     */
    @Async
    public void saveAuditLogAsync(com.mao.maocodemother.model.entity.AuditLog auditLog) {
        try {
            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            log.error("[审计日志] 写入失败：{}", e.getMessage(), e);
        }
    }

    /**
     * 获取客户端真实 IP（支持反向代理）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 获取脱敏后的请求参数
     */
    private String getSanitizedParams(ProceedingJoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) {
                return "";
            }
            // 过滤掉 HttpServletRequest/Response/MultipartFile 等不可序列化对象
            var filtered = Arrays.stream(args)
                    .filter(arg -> arg != null
                            && !(arg instanceof HttpServletRequest)
                            && !(arg instanceof HttpServletResponse)
                            && !(arg instanceof org.springframework.web.multipart.MultipartFile))
                    .collect(Collectors.toList());
            String json = JSONUtil.toJsonStr(filtered);
            // 简单脱敏：替换敏感字段的值
            for (String sensitive : SENSITIVE_PARAM_NAMES) {
                json = json.replaceAll("(?i)\"" + sensitive + "\"\\s*:\\s*\"[^\"]*\"",
                        "\"" + sensitive + "\":\"***\"");
            }
            // 限制长度
            if (json.length() > 2000) {
                json = json.substring(0, 2000) + "...(truncated)";
            }
            return json;
        } catch (Exception e) {
            return "[参数解析失败]";
        }
    }

    /**
     * 安全获取当前登录用户（不抛异常）
     */
    private User getLoginUserSafe(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        try {
            return userService.getLoginUser(request);
        } catch (Exception e) {
            return null;
        }
    }
}

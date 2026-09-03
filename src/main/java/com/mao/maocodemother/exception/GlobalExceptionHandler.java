package com.mao.maocodemother.exception;

import cn.hutool.json.JSONUtil;
import com.mao.maocodemother.common.BaseResponse;
import com.mao.maocodemother.common.ResultUtils;
import com.mao.maocodemother.exception.BusinessException;
import com.mao.maocodemother.exception.ErrorCode;
import dev.langchain4j.guardrail.InputGuardrailException;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.util.Map;

@Hidden
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        // 尝试处理 SSE 请求
        if (handleSseError(e.getCode(), e.getMessage())) {
            return null;
        }
        // 对于普通请求，返回标准 JSON 响应
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        // 尝试处理 SSE 请求
        if (handleSseError(ErrorCode.SYSTEM_ERROR.getCode(), "系统错误")) {
            return null;
        }
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }

    /**
     * AI 输入护栏异常：把护栏内的友好中文原因（而非笼统的「系统错误」）透传给前端
     */
    @ExceptionHandler(InputGuardrailException.class)
    public BaseResponse<?> inputGuardrailExceptionHandler(InputGuardrailException e) {
        // 异常信息形如：The guardrail X failed with this message: <友好原因>
        String raw = e.getMessage() == null ? "" : e.getMessage();
        String friendly = raw;
        int idx = raw.lastIndexOf("this message:");
        if (idx >= 0) {
            friendly = raw.substring(idx + "this message:".length()).trim();
        }
        if (friendly.isBlank()) {
            friendly = "输入内容不合规，请修改后重试";
        }
        log.warn("InputGuardrailException: {}", friendly);
        // 护栏拦截属于用户输入问题，用 PARAMS_ERROR 透传友好原因
        if (handleSseError(ErrorCode.PARAMS_ERROR.getCode(), friendly)) {
            return null;
        }
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, friendly);
    }

    /**
     * 处理SSE请求的错误响应
     *
     * @param errorCode 错误码
     * @param errorMessage 错误信息
     * @return true表示是SSE请求并已处理，false表示不是SSE请求
     */
    private boolean handleSseError(int errorCode, String errorMessage) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return false;
        }
        HttpServletRequest request = attributes.getRequest();
        HttpServletResponse response = attributes.getResponse();
        // 判断是否是SSE请求（通过Accept头或URL路径）
        String accept = request.getHeader("Accept");
        String uri = request.getRequestURI();
        if ((accept != null && accept.contains("text/event-stream")) ||
                uri.contains("/chat/gen/code")) {
            try {
                // 设置SSE响应头
                response.setContentType("text/event-stream");
                response.setCharacterEncoding("UTF-8");
                response.setHeader("Cache-Control", "no-cache");
                response.setHeader("Connection", "keep-alive");
                // 构造错误消息的SSE格式
                Map<String, Object> errorData = Map.of(
                        "error", true,
                        "code", errorCode,
                        "message", errorMessage
                );
                String errorJson = JSONUtil.toJsonStr(errorData);
                // 发送业务错误事件（避免与标准error事件冲突）
                String sseData = "event: business-error\ndata: " + errorJson + "\n\n";
                response.getWriter().write(sseData);
                response.getWriter().flush();
                // 发送结束事件
                response.getWriter().write("event: done\ndata: {}\n\n");
                response.getWriter().flush();
                // 表示已处理SSE请求
                return true;
            } catch (IOException ioException) {
                log.error("Failed to write SSE error response", ioException);
                // 即使写入失败，也表示这是SSE请求
                return true;
            }
        }
        return false;
    }
}
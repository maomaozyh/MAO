package com.mao.maocodemother.ai.tools;

import cn.hutool.json.JSONObject;
import com.mao.maocodemother.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件读取工具
 * 支持 AI 通过工具调用的方式读取文件内容
 */
@Slf4j
@Component
public class FileReadTool extends BaseTool {

    private static final String PROJECT_DIR_PREFIX = "vue_project";

    @Tool("读取指定路径的文件内容")
    public String readFile(
            @P("文件的相对路径")
            String relativeFilePath,
            @ToolMemoryId Long appId
    ) {
        try {
            // 安全校验：防止路径遍历
            Path path = PathSecurityUtils.safeResolveFile(relativeFilePath, appId, PROJECT_DIR_PREFIX);
            if (!Files.exists(path)) {
                return "错误：文件不存在 - " + relativeFilePath;
            }
            // 读取大小限制：最大 100KB，防止 AI 读取过大文件耗尽上下文
            long size = Files.size(path);
            if (size > 100 * 1024) {
                return "错误：文件过大（" + (size / 1024) + "KB），超过 100KB 限制 - " + relativeFilePath;
            }
            return Files.readString(path);
        } catch (SecurityException e) {
            log.warn("文件读取安全拦截：appId={}, path={}, reason={}", appId, relativeFilePath, e.getMessage());
            return "错误：" + e.getMessage();
        } catch (IOException e) {
            String errorMessage = "读取文件失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    @Override
    public String getToolName() {
        return "readFile";
    }

    @Override
    public String getDisplayName() {
        return "读取文件";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath");
        return String.format("[工具调用] %s %s", getDisplayName(), relativeFilePath);
    }
}

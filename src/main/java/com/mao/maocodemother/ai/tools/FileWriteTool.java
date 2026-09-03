package com.mao.maocodemother.ai.tools;

import cn.hutool.core.io.FileUtil;
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
import java.nio.file.StandardOpenOption;

/**
 * 文件写入工具
 * 支持 AI 通过工具调用的方式写入文件
 */
@Slf4j
@Component
public class FileWriteTool extends BaseTool {

    private static final String PROJECT_DIR_PREFIX = "vue_project";

    /**
     * 单个文件写入大小限制（500KB）
     */
    private static final long MAX_FILE_SIZE = 500 * 1024;

    @Tool("写入文件到指定路径")
    public String writeFile(
            @P("文件的相对路径")
            String relativeFilePath,
            @P("要写入文件的内容")
            String content,
            @ToolMemoryId Long appId
    ) {
        try {
            // 安全校验：防止路径遍历
            Path path = PathSecurityUtils.safeResolve(relativeFilePath, appId, PROJECT_DIR_PREFIX);

            // 写入大小限制
            if (content != null && content.length() > MAX_FILE_SIZE) {
                return "错误：文件内容过大，超过 " + (MAX_FILE_SIZE / 1024) + "KB 限制";
            }

            // 校验文件扩展名（禁止写入可执行文件、配置文件等危险类型）
            String fileName = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase();
            if (isDangerousFileType(fileName)) {
                log.warn("危险文件类型写入被拦截：appId={}, file={}", appId, fileName);
                return "错误：不允许写入该类型的文件 - " + fileName;
            }

            // 创建父目录（如果不存在）
            Path parentDir = path.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            // 写入文件内容
            Files.write(path, content.getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            log.info("成功写入文件: {}", path.toAbsolutePath());
            // 注意要返回相对路径，不能让 AI 把文件绝对路径返回给用户
            return "文件写入成功: " + relativeFilePath;
        } catch (SecurityException e) {
            log.warn("文件写入安全拦截：appId={}, path={}, reason={}", appId, relativeFilePath, e.getMessage());
            return "错误：" + e.getMessage();
        } catch (IOException e) {
            String errorMessage = "文件写入失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    /**
     * 判断是否是危险文件类型（不允许 AI 写入）
     */
    private boolean isDangerousFileType(String fileName) {
        String[] dangerousExts = {
                ".exe", ".bat", ".cmd", ".sh", ".ps1", ".vbs",
                ".dll", ".so", ".dylib",
                ".env", ".pem", ".key", ".crt", ".p12", ".pfx",
        };
        for (String ext : dangerousExts) {
            if (fileName.endsWith(ext)) {
                return true;
            }
        }
        // 隐藏文件（以 . 开头的系统/配置文件）也不允许写入到根目录
        return false;
    }

    @Override
    public String getToolName() {
        return "writeFile";
    }

    @Override
    public String getDisplayName() {
        return "写入文件";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath");
        String suffix = FileUtil.getSuffix(relativeFilePath);
        String content = arguments.getStr("content");
        return String.format("""
                        [工具调用] %s %s
                        ```%s
                        %s
                        ```
                        """, getDisplayName(), relativeFilePath, suffix, content);
    }
}

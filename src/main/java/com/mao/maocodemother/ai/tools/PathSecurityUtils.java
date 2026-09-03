package com.mao.maocodemother.ai.tools;

import cn.hutool.core.util.StrUtil;
import com.mao.maocodemother.constant.AppConstant;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * AI 工具路径安全校验工具
 * 防止路径遍历攻击（Path Traversal），确保所有文件操作都限制在项目目录内
 * <p>
 * 五层防御：
 * 1. 禁止绝对路径
 * 2. 规范化路径（resolve + normalize）
 * 3. 校验路径以项目根目录开头
 * 4. 检查符号链接（防止 symlink 跳出沙箱）
 * 5. 各工具额外校验（文件大小、危险类型等）
 */
@Slf4j
public class PathSecurityUtils {

    /**
     * 安全解析文件路径，确保最终路径在项目根目录范围内
     * 禁止绝对路径、符号链接、路径遍历
     *
     * @param relativePath 相对路径（AI 传入）
     * @param appId        应用 ID，用于定位项目目录
     * @param codeGenType  代码生成类型，用于拼接目录名
     * @return 安全的绝对路径
     * @throws SecurityException 路径不安全时抛出
     */
    public static Path safeResolve(String relativePath, Long appId, String codeGenType) {
        // 1. 参数校验
        if (StrUtil.isBlank(relativePath)) {
            relativePath = "";
        }
        if (appId == null || appId <= 0) {
            throw new SecurityException("无效的应用 ID");
        }

        // 2. 禁止绝对路径（AI 不允许直接操作系统任意位置）
        Path rawPath = Paths.get(relativePath);
        if (rawPath.isAbsolute()) {
            throw new SecurityException("不允许使用绝对路径: " + relativePath);
        }

        // 3. 计算项目根目录
        String projectDirName = codeGenType + "_" + appId;
        Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName).toAbsolutePath().normalize();

        // 4. 解析并规范化路径（自动处理 . 和 ..）
        Path resolvedPath = projectRoot.resolve(relativePath).normalize();

        // 5. 核心校验：规范化后的路径必须仍在项目根目录内
        if (!resolvedPath.startsWith(projectRoot)) {
            log.warn("路径遍历攻击被拦截！appId={}, 原始路径={}, 解析后={}", appId, relativePath, resolvedPath);
            throw new SecurityException("路径超出项目目录范围: " + relativePath);
        }

        // 6. 检查符号链接（防止通过 symlink 跳出沙箱）
        try {
            checkNoSymlink(resolvedPath, projectRoot, relativePath, appId);
        } catch (Exception e) {
            // IOException 等转换为 SecurityException
            log.error("检查符号链接失败: {}", resolvedPath, e);
            throw new SecurityException("路径安全检查失败: " + relativePath);
        }

        return resolvedPath;
    }

    /**
     * 校验路径及其所有父目录都不是符号链接
     */
    private static void checkNoSymlink(Path resolvedPath, Path projectRoot, String relativePath, Long appId)
            throws java.io.IOException {
        // 检查文件本身
        if (Files.exists(resolvedPath) && Files.isSymbolicLink(resolvedPath)) {
            log.warn("符号链接访问被拦截！appId={}, 路径={}", appId, resolvedPath);
            throw new SecurityException("不允许访问符号链接文件: " + relativePath);
        }
        // 检查所有父目录（直到项目根目录）
        Path current = resolvedPath.getParent();
        while (current != null && !current.equals(projectRoot)) {
            if (Files.exists(current) && Files.isSymbolicLink(current)) {
                log.warn("父目录符号链接被拦截！appId={}, 路径={}", appId, current);
                throw new SecurityException("路径包含符号链接，不允许访问: " + relativePath);
            }
            current = current.getParent();
        }
    }

    /**
     * 校验目录路径安全（用于目录读取操作）
     *
     * @param relativePath 相对路径
     * @param appId        应用 ID
     * @param codeGenType  代码生成类型
     * @return 安全的目录绝对路径
     */
    public static Path safeResolveDir(String relativePath, Long appId, String codeGenType) {
        Path path = safeResolve(relativePath, appId, codeGenType);
        // 如果路径不存在，只做遍历校验即可（目录可能还没创建）
        try {
            if (Files.exists(path) && !Files.isDirectory(path)) {
                throw new SecurityException("指定路径不是目录: " + relativePath);
            }
        } catch (Exception e) {
            throw new SecurityException("目录检查失败: " + relativePath);
        }
        return path;
    }

    /**
     * 校验文件路径安全（用于文件读写操作）
     *
     * @param relativePath 相对路径
     * @param appId        应用 ID
     * @param codeGenType  代码生成类型
     * @return 安全的文件绝对路径
     */
    public static Path safeResolveFile(String relativePath, Long appId, String codeGenType) {
        Path path = safeResolve(relativePath, appId, codeGenType);
        try {
            // 文件如果存在，必须是普通文件（不是目录/符号链接等）
            if (Files.exists(path) && !Files.isRegularFile(path)) {
                throw new SecurityException("指定路径不是普通文件: " + relativePath);
            }
        } catch (Exception e) {
            throw new SecurityException("文件检查失败: " + relativePath);
        }
        return path;
    }
}

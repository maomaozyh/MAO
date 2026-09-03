package com.mao.maocodemother.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * AI 生成代码安全扫描器
 * <p>
 * 对 AI 生成的代码进行基础静态安全扫描，检测常见安全问题，
 * 防止 AI 生成包含严重安全漏洞的代码。
 * <p>
 * 扫描规则：
 * 1. 硬编码密钥 / 密码 / Token
 * 2. eval() / Function() 等动态代码执行（JS）
 * 3. innerHTML / document.write 等 DOM XSS 风险（JS）
 * 4. SQL 拼接风险
 * 5. 危险的文件操作（路径拼接）
 * 6. 禁用的协议（javascript: / data: text/html）
 */
@Slf4j
@Component
public class CodeSecurityScanner {

    /**
     * 扫描结果
     */
    public static class ScanResult {
        private final boolean hasRisk;
        private final List<String> issues;
        private final String scannedContent;

        public ScanResult(boolean hasRisk, List<String> issues, String scannedContent) {
            this.hasRisk = hasRisk;
            this.issues = issues;
            this.scannedContent = scannedContent;
        }

        public boolean hasRisk() {
            return hasRisk;
        }

        public List<String> getIssues() {
            return issues;
        }

        public String getScannedContent() {
            return scannedContent;
        }
    }

    // ===== 硬编码密钥检测 =====
    private static final Pattern HARDCODED_API_KEY = Pattern.compile(
            "(?i)(api[_-]?key|secret|password|token|auth[_-]?token)\\s*[:=]\\s*['\"][a-zA-Z0-9_\\-]{16,}['\"]");

    // ===== 动态代码执行（JavaScript）=====
    private static final Pattern DANGEROUS_EVAL = Pattern.compile(
            "(?i)\\b(eval|Function|setTimeout|setInterval)\\s*\\(");

    // ===== DOM XSS 风险（JavaScript）=====
    private static final Pattern DOM_XSS_RISK = Pattern.compile(
            "(?i)\\.(innerHTML|outerHTML|document\\.write)\\s*=");

    // ===== SQL 拼接风险 =====
    private static final Pattern SQL_INJECTION_RISK = Pattern.compile(
            "(?i)(SELECT|INSERT|UPDATE|DELETE|DROP|UNION).*\\+.*['\"]|['\"].*\\+.*(WHERE|FROM|AND|OR)");

    // ===== 危险协议（href/src 中的 javascript:）=====
    private static final Pattern DANGEROUS_PROTOCOL = Pattern.compile(
            "(?i)(href|src|action)\\s*=\\s*['\"]\\s*javascript:");

    // ===== 危险的文件路径拼接（路径遍历风险）=====
    private static final Pattern PATH_TRAVERSAL_RISK = Pattern.compile(
            "(?i)(fs\\.(readFile|writeFile|readFileSync|writeFileSync|unlink)|FileReader|require)\\s*\\(.*\\+");

    // ===== 明文密码 / 敏感信息日志输出 =====
    private static final Pattern SENSITIVE_LOG = Pattern.compile(
            "(?i)(console\\.(log|info|debug)|System\\.out\\.print).*(password|secret|token|apiKey)");

    /**
     * 扫描单个文件代码的安全性
     *
     * @param fileName    文件名（用于判断文件类型）
     * @param fileContent 文件内容
     * @return 扫描结果
     */
    public ScanResult scanFile(String fileName, String fileContent) {
        List<String> issues = new ArrayList<>();

        if (fileContent == null || fileContent.isEmpty()) {
            return new ScanResult(false, issues, fileContent);
        }

        // 根据文件类型选择扫描规则
        boolean isJsFile = fileName != null && (fileName.endsWith(".js") || fileName.endsWith(".vue")
                || fileName.endsWith(".ts") || fileName.endsWith(".tsx") || fileName.endsWith(".jsx"));
        boolean isHtmlFile = fileName != null && (fileName.endsWith(".html") || fileName.endsWith(".vue"));
        boolean isJavaFile = fileName != null && fileName.endsWith(".java");
        boolean isSqlFile = fileName != null && fileName.endsWith(".sql");

        // 1. 硬编码密钥检测（所有文件类型）
        if (HARDCODED_API_KEY.matcher(fileContent).find()) {
            issues.add("【高危】检测到硬编码密钥/密码/Token，请改用环境变量或配置文件注入");
        }

        // 2. JavaScript 安全检测
        if (isJsFile || isHtmlFile) {
            // eval 等动态代码执行
            if (DANGEROUS_EVAL.matcher(fileContent).find()) {
                issues.add("【高危】检测到 eval/Function 等动态代码执行，存在代码注入风险，建议改用安全的替代方案");
            }
            // DOM XSS 风险
            if (DOM_XSS_RISK.matcher(fileContent).find()) {
                issues.add("【中危】检测到 innerHTML/document.write 直接赋值，存在 XSS 风险，建议使用 textContent 或转义后再赋值");
            }
            // 危险协议
            if (DANGEROUS_PROTOCOL.matcher(fileContent).find()) {
                issues.add("【高危】检测到 javascript: 伪协议，存在 XSS 风险，禁止使用");
            }
            // 敏感信息日志
            if (SENSITIVE_LOG.matcher(fileContent).find()) {
                issues.add("【中危】检测到密码/密钥等敏感信息被输出到日志，存在信息泄露风险");
            }
        }

        // 3. 文件操作路径遍历风险（JS/TS）
        if (isJsFile) {
            if (PATH_TRAVERSAL_RISK.matcher(fileContent).find()) {
                issues.add("【中危】检测到文件路径拼接操作，存在路径遍历风险，建议使用 path.resolve 并校验路径合法性");
            }
        }

        // 4. SQL 注入风险
        if (isJavaFile || isSqlFile) {
            if (SQL_INJECTION_RISK.matcher(fileContent).find()) {
                issues.add("【高危】检测到 SQL 字符串拼接，存在 SQL 注入风险，建议使用预编译语句（PreparedStatement）或 ORM 框架");
            }
        }

        boolean hasRisk = !issues.isEmpty();
        if (hasRisk) {
            log.warn("[代码安全扫描] 文件 {} 发现 {} 个安全问题：{}", fileName, issues.size(), issues);
        }

        return new ScanResult(hasRisk, issues, fileContent);
    }

    /**
     * 批量扫描多个文件
     *
     * @param files 文件名 -> 文件内容 的映射
     * @return 所有问题汇总
     */
    public List<String> scanMultipleFiles(java.util.Map<String, String> files) {
        List<String> allIssues = new ArrayList<>();
        if (files == null || files.isEmpty()) {
            return allIssues;
        }
        for (var entry : files.entrySet()) {
            ScanResult result = scanFile(entry.getKey(), entry.getValue());
            if (result.hasRisk()) {
                for (String issue : result.getIssues()) {
                    allIssues.add("[" + entry.getKey() + "] " + issue);
                }
            }
        }
        if (!allIssues.isEmpty()) {
            log.warn("[代码安全扫描] 共扫描 {} 个文件，发现 {} 个安全问题", files.size(), allIssues.size());
        } else {
            log.info("[代码安全扫描] 共扫描 {} 个文件，未发现安全问题", files.size());
        }
        return allIssues;
    }
}

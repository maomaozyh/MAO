package com.mao.maocodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryCondition;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import com.mao.maocodemother.ai.AiCodeGenTypeRoutingService;
import com.mao.maocodemother.ai.AiCodeGenTypeRoutingServiceFactory;
import com.mao.maocodemother.ai.CodeSelfCheckService;
import com.mao.maocodemother.ai.SemanticSearchExpandService;
import com.mao.maocodemother.constant.AppConstant;
import com.mao.maocodemother.constant.UserConstant;
import com.mao.maocodemother.core.AiCodeGeneratorFacade;
import com.mao.maocodemother.core.ai.AiPromptBuilder;
import com.mao.maocodemother.core.builder.VueProjectBuilder;
import com.mao.maocodemother.core.handler.StreamHandlerExecutor;
import com.mao.maocodemother.exception.BusinessException;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.exception.ThrowUtils;
import com.mao.maocodemother.model.dto.app.AppAddRequest;
import com.mao.maocodemother.model.dto.app.AppQueryRequest;
import com.mao.maocodemother.model.dto.app.SemanticSearchRequest;
import com.mao.maocodemother.model.entity.App;
import com.mao.maocodemother.mapper.AppMapper;
import com.mao.maocodemother.model.entity.ChatHistory;
import com.mao.maocodemother.model.entity.Skill;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.enums.AppCategoryEnum;
import com.mao.maocodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.mao.maocodemother.model.enums.CodeGenTypeEnum;
import com.mao.maocodemother.model.enums.AssetTypeEnum;
import com.mao.maocodemother.model.enums.ExternalAssetType;
import com.mao.maocodemother.model.enums.SecondsBizTypeEnum;
import com.mao.maocodemother.model.vo.AppVO;
import com.mao.maocodemother.model.vo.SelfCheckResultVO;
import com.mao.maocodemother.model.vo.UserVO;
import com.mao.maocodemother.monitor.MonitorContext;
import com.mao.maocodemother.monitor.MonitorContextHolder;
import com.mao.maocodemother.service.AppService;
import com.mao.maocodemother.service.ChatHistoryService;
import com.mao.maocodemother.service.GeneratedAssetService;
import com.mao.maocodemother.service.ImageGenerationService;
import com.mao.maocodemother.service.Model3dGenerationService;
import com.mao.maocodemother.service.PptGenerationService;
import com.mao.maocodemother.service.VideoGenerationService;
import com.mao.maocodemother.service.ScreenshotService;
import com.mao.maocodemother.service.SecondsService;
import com.mao.maocodemother.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author <a href="https://github.com/liyupi">程序员mao</a>
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Value("${code.deploy-host:http://localhost}")
    private String deployHost;

    @Resource
    private UserService userService;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private AiPromptBuilder aiPromptBuilder;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private ScreenshotService screenshotService;

    @Resource
    private AiCodeGenTypeRoutingServiceFactory aiCodeGenTypeRoutingServiceFactory;

    @Resource
    private ImageGenerationService imageGenerationService;

    @Resource
    private VideoGenerationService videoGenerationService;

    @Resource
    private Model3dGenerationService model3dGenerationService;

    @Resource
    private PptGenerationService pptGenerationService;

    @Resource
    private GeneratedAssetService generatedAssetService;

    @Resource
    private SemanticSearchExpandService semanticSearchExpandService;

    @Resource
    private CodeSelfCheckService codeSelfCheckService;

    /**
     * 语义搜索 AI 扩展的最大关键词数量
     */
    private static final int MAX_SEARCH_KEYWORDS = 5;

    /**
     * 发送给 AI 自检的最大代码长度（字符）
     */
    private static final int MAX_SELFCHECK_CODE_CHARS = 8000;

    /**
     * 单个代码文件读取的最大长度（字符）
     */
    private static final int MAX_SINGLE_FILE_CHARS = 4000;

    /**
     * Vue 项目补充扫描源码文件的最大数量
     */
    private static final int MAX_EXTRA_SOURCE_FILES = 30;

    @Resource
    private SecondsService secondsService;

    @Resource
    private com.mao.maocodemother.service.SkillService skillService;

    @Override
    public Flux<String> chatToGenCode(Long appId, String message, String mode, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 错误");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "提示词不能为空");
        // 在「用户原始输入」边界限制长度（护栏作用在含历史的组装 prompt 上，不再判长，避免多轮对话被误杀）
        ThrowUtils.throwIf(message.length() > 1000, ErrorCode.PARAMS_ERROR, "输入内容过长，不要超过 1000 字");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 2.1 若该应用分类需要调用外部 AI 服务产出真实素材（图像/视频/3D/PPT），走专用生成分支
        ExternalAssetType externalAssetType = resolveExternalAssetType(app);
        if (externalAssetType != ExternalAssetType.NONE) {
            return generateExternalAsset(app, message, loginUser, externalAssetType);
        }
        // 3. 权限校验：仅本人可以和自己的应用对话；admin 可访问任意应用（用于后台预览/排查，普通用户不受影响）
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        if (!isAdmin && !app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }
        // 4. 获取应用的代码生成类型
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用代码生成类型错误");
        }
        // 5. 在调用 AI 前，先查询最近的对话历史（用于组装多轮上下文）
        List<ChatHistory> recentHistory = chatHistoryService.listRecentMessages(appId, loginUser.getId(), AiPromptBuilder.MAX_HISTORY_MESSAGES);
        // 6. 组装增强提示词：多轮对话上下文 + 迭代修改提示 + 自动技能匹配
        String enhancedMessage = aiPromptBuilder.buildEnhancedPrompt(message, recentHistory);
        // 6.0 若用户选择了生成模式（深度开发/快速开发），在提示词前拼接模式引导（默认深度开发，保持原有行为）
        String modeGuidance = buildModeGuidance(mode);
        if (modeGuidance != null) {
            enhancedMessage = modeGuidance + "\n\n" + enhancedMessage;
        }
        // 6.1 若存在应用分类，则在提示词前拼接分类专属生成引导（让同一套生成链路产出更贴合类型的应用）
        String categoryGuidance = buildCategoryGuidance(app);
        if (categoryGuidance != null) {
            enhancedMessage = categoryGuidance + "\n\n" + enhancedMessage;
        }
        // 6.2 若应用绑定了技能，注入技能专属系统提示词（角色设定、输出风格等），并累加使用次数
        Skill skill = null;
        if (app.getSkillId() != null) {
            skill = skillService.getById(app.getSkillId());
            if (skill != null && StrUtil.isNotBlank(skill.getSystemPrompt())) {
                String skillPrompt = "【技能设定】\n" + skill.getSystemPrompt()
                        + "\n\n请严格按照上述技能设定的角色和风格来完成用户的需求。";
                enhancedMessage = skillPrompt + "\n\n" + enhancedMessage;
            }
            // 异步累加使用次数（不影响主流程）
            if (skill != null) {
                try {
                    Skill updateSkill = new Skill();
                    updateSkill.setId(skill.getId());
                    updateSkill.setUsageCount((skill.getUsageCount() == null ? 0L : skill.getUsageCount()) + 1);
                    skillService.updateById(updateSkill);
                } catch (Exception e) {
                    log.warn("更新技能使用次数失败，skillId={}", app.getSkillId(), e);
                }
            }
        }
        // 6.1 预扣积分（AI 代码生成）：余额不足直接拒绝，不会进入生成链路
        long costRecordId = secondsService.deduct(loginUser.getId(), SecondsBizTypeEnum.GEN_CODE, null, appId);
        // 7. 保存用户消息到数据库（保存原始消息，便于前端展示）
        chatHistoryService.addChatMessage(appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());
        // 8. 设置监控上下文（用户 ID 和应用 ID）
        MonitorContextHolder.setContext(
                MonitorContext.builder()
                        .userId(loginUser.getId().toString())
                        .appId(appId.toString())
                        .build()
        );
        // 9. 调用 AI 生成代码（流式），传入增强后的提示词和技能（技能携带专属 MCP 工具集）
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(enhancedMessage, codeGenTypeEnum, appId, skill);
        // 10. 收集 AI 响应的内容，并且在完成后保存记录到对话历史
        return streamHandlerExecutor.doExecute(codeStream, chatHistoryService, appId, loginUser, codeGenTypeEnum)
                // 生成失败（抛错误信号）时退回预扣的积分，refund 幂等不会重复退
                .doOnError(e -> secondsService.refund(costRecordId))
                .doFinally(signalType -> {
                    // 流结束时清理（无论成功/失败/取消）
                    MonitorContextHolder.clearContext();
                });
    }

    /**
     * 根据应用分类构造生成引导词（拼在用户提示词前，约束 AI 产出类型）
     *
     * @param app 应用
     * @return 分类引导词；应用无分类或分类未知时返回 null
     */
    private String buildCategoryGuidance(App app) {
        if (app == null || StrUtil.isBlank(app.getCategory())) {
            return null;
        }
        AppCategoryEnum categoryEnum = AppCategoryEnum.getEnumByValue(app.getCategory());
        if (categoryEnum == null) {
            return null;
        }
        return "## 应用分类要求（" + categoryEnum.getText() + "）\n"
                + categoryEnum.getGenerationGuidance() + "\n"
                + "请严格按以上分类要求生成对应类型的应用，并保持界面美观、可直接运行。";
    }

    /**
     * 生成模式引导：深度开发（默认，保持原有行为）/ 快速开发（聚焦核心、控制篇幅）
     */
    private String buildModeGuidance(String mode) {
        if ("fast".equals(mode)) {
            return "## 生成模式：快速开发\n"
                    + "请优先产出可运行的核心代码，聚焦主流程与关键功能，控制篇幅，避免过度设计与冗余注释；"
                    + "在有限长度内保证结果能直接打开运行即可。";
        }
        // 默认深度开发
        return "## 生成模式：深度开发\n"
                + "请产出结构完整、健壮、细节丰富的实现，包含必要的边界处理、基础交互与适量注释，"
                + "保证界面美观且可直接运行。";
    }

    /**
     * 解析应用分类对应的外部素材生成类型；无对应则返回 NONE（走普通代码生成链路）
     */
    private ExternalAssetType resolveExternalAssetType(App app) {
        if (app == null || StrUtil.isBlank(app.getCategory())) {
            return ExternalAssetType.NONE;
        }
        AppCategoryEnum categoryEnum = AppCategoryEnum.getEnumByValue(app.getCategory());
        return categoryEnum != null ? categoryEnum.getExternalAssetType() : ExternalAssetType.NONE;
    }

    /**
     * 外部素材生成分支分发：按素材类型调用对应服务，产出真实素材并以 markdown 返回
     */
    private Flux<String> generateExternalAsset(App app, String message, User loginUser, ExternalAssetType type) {
        SecondsBizTypeEnum bizType = switch (type) {
            case IMAGE -> SecondsBizTypeEnum.GEN_IMAGE;
            case VIDEO -> SecondsBizTypeEnum.GEN_VIDEO;
            case MODEL_3D -> SecondsBizTypeEnum.GEN_3D;
            case PPT -> SecondsBizTypeEnum.GEN_PPT;
            default -> SecondsBizTypeEnum.GEN_IMAGE;
        };
        // 调用前预扣积分，余额不足直接拒绝（抛 BusinessException，不会进入生成）
        long costRecordId = secondsService.deduct(loginUser.getId(), bizType, null, app.getId());
        Flux<String> flux = switch (type) {
            case IMAGE -> generateExternalAssetMessage(app, message, loginUser);
            case VIDEO -> generateVideoMessage(app, message, loginUser);
            case MODEL_3D -> generateModel3dMessage(app, message, loginUser);
            case PPT -> generatePptMessage(app, message, loginUser);
            default -> generateExternalAssetMessage(app, message, loginUser);
        };
        // 失败退回：内部错误以「❌」开头的内容返回（不抛 error 信号），故需同时监听内容和错误信号
        // refund 以流水 status 保证幂等，重复调用不会多退
        return flux
                .doOnNext(chunk -> {
                    if (chunk != null && chunk.contains("❌")) {
                        secondsService.refund(costRecordId);
                    }
                })
                .doOnError(e -> secondsService.refund(costRecordId));
    }

    /**
     * 统一的错误回写：把错误文案存入聊天历史并返回单条消息流
     */
    private Flux<String> errorFlux(App app, User loginUser, String errMsg) {
        chatHistoryService.addChatMessage(app.getId(), errMsg, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
        return Flux.just(errMsg);
    }

    /**
     * 外部素材生成分支：调用对应 AI 服务产出真实素材，并以 markdown 图片消息返回（前端 MarkdownRenderer 直接渲染）
     *
     * @param app        应用
     * @param message    用户提示词
     * @param loginUser  登录用户
     * @return 图像 markdown 消息流
     */
    private Flux<String> generateExternalAssetMessage(App app, String message, User loginUser) {
        String imageUrl;
        try {
            imageUrl = imageGenerationService.generateImage(message);
        } catch (BusinessException e) {
            String errMsg = "❌ " + e.getMessage();
            chatHistoryService.addChatMessage(app.getId(), errMsg, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
            return Flux.just(errMsg);
        } catch (Exception e) {
            log.error("外部素材生成异常", e);
            String errMsg = "❌ 图像生成失败：" + e.getMessage();
            chatHistoryService.addChatMessage(app.getId(), errMsg, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
            return Flux.just(errMsg);
        }
        // 结合分类给出更友好的说明
        AppCategoryEnum categoryEnum = AppCategoryEnum.getEnumByValue(app.getCategory());
        String label = categoryEnum != null ? categoryEnum.getText() : "图片";
        String markdown = "![生成的" + label + "](" + imageUrl + ")";
        chatHistoryService.addChatMessage(app.getId(), markdown, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
        return Flux.just(markdown);
    }

    /**
     * 视频生成分支：调 DashScope 通义万相文生视频，返回可播放的 &lt;video&gt; + 下载链接（MarkdownRenderer 已开启 html 渲染）
     */
    private Flux<String> generateVideoMessage(App app, String message, User loginUser) {
        String videoUrl;
        try {
            videoUrl = videoGenerationService.generateVideo(message);
        } catch (BusinessException e) {
            return errorFlux(app, loginUser, "❌ " + e.getMessage());
        } catch (Exception e) {
            log.error("视频生成异常", e);
            return errorFlux(app, loginUser, "❌ 视频生成失败：" + e.getMessage());
        }
        AppCategoryEnum categoryEnum = AppCategoryEnum.getEnumByValue(app.getCategory());
        String label = categoryEnum != null ? categoryEnum.getText() : "视频";
        String markdown = "✅ 已生成" + label + "：\n\n"
                + "<video controls style=\"max-width:100%;border-radius:8px;\" src=\"" + videoUrl + "\"></video>\n\n"
                + "[下载视频](" + videoUrl + ")";
        generatedAssetService.saveAsset(app.getId(), loginUser.getId(), AssetTypeEnum.VIDEO.getValue(), videoUrl, message);
        chatHistoryService.addChatMessage(app.getId(), markdown, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
        return Flux.just(markdown);
    }

    /**
     * 3D 模型生成分支：调 Tripo3D 风格接口，返回 GLB 模型下载链接
     */
    private Flux<String> generateModel3dMessage(App app, String message, User loginUser) {
        String modelUrl;
        try {
            modelUrl = model3dGenerationService.generateModel(message);
        } catch (BusinessException e) {
            return errorFlux(app, loginUser, "❌ " + e.getMessage());
        } catch (Exception e) {
            log.error("3D 生成异常", e);
            return errorFlux(app, loginUser, "❌ 3D 模型生成失败：" + e.getMessage());
        }
        AppCategoryEnum categoryEnum = AppCategoryEnum.getEnumByValue(app.getCategory());
        String label = categoryEnum != null ? categoryEnum.getText() : "3D 模型";
        String markdown = "✅ 已生成" + label + "（GLB 格式）：\n\n[下载 3D 模型 (GLB)](" + modelUrl + ")";
        generatedAssetService.saveAsset(app.getId(), loginUser.getId(), AssetTypeEnum.MODEL_3D.getValue(), modelUrl, message);
        chatHistoryService.addChatMessage(app.getId(), markdown, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
        return Flux.just(markdown);
    }

    /**
     * PPT 生成分支：调大模型产出结构化幻灯片 JSON，前端用 pptxgenjs 导出 .pptx
     */
    private Flux<String> generatePptMessage(App app, String message, User loginUser) {
        String slidesJson;
        try {
            slidesJson = pptGenerationService.generateSlidesJson(message);
        } catch (BusinessException e) {
            return errorFlux(app, loginUser, "❌ " + e.getMessage());
        } catch (Exception e) {
            log.error("PPT 生成异常", e);
            return errorFlux(app, loginUser, "❌ PPT 大纲生成失败：" + e.getMessage());
        }
        // 大纲 JSON 存入聊天历史（含 pptx-slides 代码块，前端检测后展示「导出 PPTX」按钮）
        String markdown = "✅ 已生成 PPT 大纲，点击下方「导出为 PPTX」即可下载：\n\n```pptx-slides\n"
                + slidesJson + "\n```";
        generatedAssetService.saveAsset(app.getId(), loginUser.getId(), AssetTypeEnum.PPT.getValue(), null, message);
        chatHistoryService.addChatMessage(app.getId(), markdown, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
        return Flux.just(markdown);
    }

    @Override
    public Long createApp(AppAddRequest appAddRequest, User loginUser) {
        // 参数校验
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");
        // 构造入库对象
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        app.setUserId(loginUser.getId());
        // 记录应用分类（对应前端快捷入口；为空则忽略，走通用生成）
        if (StrUtil.isNotBlank(appAddRequest.getCategory())) {
            app.setCategory(appAddRequest.getCategory());
        }
        // 公开/私密开关：默认公开（传了 0 才设为私密）
        app.setIsPublic(appAddRequest.getIsPublic() != null ? appAddRequest.getIsPublic() : 1);
        // 应用名称暂时为 initPrompt 前 12 位
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 12)));
        // 使用 AI 智能选择代码生成类型（多例模式）
        AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService = aiCodeGenTypeRoutingServiceFactory.createAiCodeGenTypeRoutingService();
        CodeGenTypeEnum selectedCodeGenType = aiCodeGenTypeRoutingService.routeCodeGenType(initPrompt);
        app.setCodeGenType(selectedCodeGenType.getValue());
        // 插入数据库
        boolean result = this.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        log.info("应用创建成功，ID: {}, 类型: {}", app.getId(), selectedCodeGenType.getValue());
        return app.getId();
    }

    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 错误");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 权限校验，仅本人可以部署自己的应用
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");
        }
        // 4. 检查是否已有 deployKey
        String deployKey = app.getDeployKey();
        // 如果没有，则生成 6 位 deployKey（字母 + 数字）
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }
        // 5. 获取代码生成类型，获取原始代码生成路径（应用访问目录）
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 6. 检查路径是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码路径不存在，请先生成应用");
        }
        // 7. Vue 项目特殊处理：执行构建
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT) {
            // Vue 项目需要构建
            boolean buildSuccess = vueProjectBuilder.buildProject(sourceDirPath);
            ThrowUtils.throwIf(!buildSuccess, ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败，请重试");
            // 检查 dist 目录是否存在
            File distDir = new File(sourceDirPath, "dist");
            ThrowUtils.throwIf(!distDir.exists(), ErrorCode.SYSTEM_ERROR, "Vue 项目构建完成但未生成 dist 目录");
            // 构建完成后，需要将构建后的文件复制到部署目录
            sourceDir = distDir;
        }
        // 8. 复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用部署失败：" + e.getMessage());
        }
        // 9. 更新数据库
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        // 10. 构建应用访问 URL
        String appDeployUrl = String.format("%s/%s/", deployHost, deployKey);        // 11. 异步生成截图并且更新应用封面
        generateAppScreenshotAsync(appId, appDeployUrl);
        return appDeployUrl;
    }

    /**
     * 异步生成应用截图并更新封面
     *
     * @param appId  应用ID
     * @param appUrl 应用访问URL
     */
    @Override
    public void generateAppScreenshotAsync(Long appId, String appUrl) {
        // 使用虚拟线程并执行
        Thread.startVirtualThread(() -> {
            // 调用截图服务生成截图并上传
            String screenshotUrl = screenshotService.generateAndUploadScreenshot(appUrl);
            // 更新数据库的封面
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setCover(screenshotUrl);
            boolean updated = this.updateById(updateApp);
            ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新应用封面字段失败");
        });
    }

    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 关联查询用户信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        // 关联查询技能信息
        Long skillId = app.getSkillId();
        if (skillId != null && skillService != null) {
            try {
                Skill skill = skillService.getById(skillId);
                if (skill != null) {
                    com.mao.maocodemother.model.vo.SkillVO skillVO = new com.mao.maocodemother.model.vo.SkillVO();
                    BeanUtil.copyProperties(skill, skillVO);
                    appVO.setSkill(skillVO);
                }
            } catch (Exception e) {
                log.warn("查询应用关联的技能信息失败，appId={}, skillId={}", app.getId(), skillId, e);
            }
        }
        return appVO;
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userIds.isEmpty() ? new HashMap<>()
                : userService.listByIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, userService::getUserVO));
        // 批量获取技能信息，避免 N+1 查询问题
        Set<Long> skillIds = appList.stream()
                .map(App::getSkillId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, com.mao.maocodemother.model.vo.SkillVO> skillVOMap = new HashMap<>();
        if (!skillIds.isEmpty() && skillService != null) {
            try {
                skillVOMap = skillService.listByIds(skillIds).stream()
                        .collect(Collectors.toMap(Skill::getId, skill -> {
                            com.mao.maocodemother.model.vo.SkillVO vo = new com.mao.maocodemother.model.vo.SkillVO();
                            BeanUtil.copyProperties(skill, vo);
                            return vo;
                        }));
            } catch (Exception e) {
                log.warn("批量查询应用关联的技能信息失败", e);
            }
        }
        // 直接在循环里组装 VO：不再调用 getAppVO（其内部会按条 getById 用户，形成 N+1）
        final Map<Long, com.mao.maocodemother.model.vo.SkillVO> finalSkillVOMap = skillVOMap;
        return appList.stream().map(app -> {
            AppVO appVO = new AppVO();
            BeanUtil.copyProperties(app, appVO);
            appVO.setUser(userVOMap.get(app.getUserId()));
            appVO.setSkill(finalSkillVOMap.get(app.getSkillId()));
            return appVO;
        }).collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "app_detail", key = "#id")
    public App getAppById(long id) {
        return this.getById(id);
    }

    @Override
    public List<AppVO> listRecentApps(Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID错误");
        // 仅返回该用户最近打开过的应用（lastOpenTime 不为空），按最近打开时间倒序
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("userId", userId)
                .isNotNull("lastOpenTime")
                .orderBy("lastOpenTime", false);
        List<App> appList = this.list(queryWrapper);
        return this.getAppVOList(appList);
    }

    @Override
    @CacheEvict(value = "app_detail", key = "#appId")
    public boolean markAppOpened(User loginUser, Long appId) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID错误");
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 仅创建者本人或管理员可标记打开（与管理员可访问任意应用一致）
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        if (!app.getUserId().equals(loginUser.getId()) && !isAdmin) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限标记该应用的打开记录");
        }
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setLastOpenTime(LocalDateTime.now());
        boolean result = this.updateById(updateApp);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新最近打开时间失败");
        return true;
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String category = appQueryRequest.getCategory();
        Long skillId = appQueryRequest.getSkillId();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Integer status = appQueryRequest.getStatus();
        Integer isPublic = appQueryRequest.getIsPublic();
        Long userId = appQueryRequest.getUserId();
        LocalDateTime createTimeStart = appQueryRequest.getCreateTimeStart();
        LocalDateTime createTimeEnd = appQueryRequest.getCreateTimeEnd();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("category", category)
                .eq("skillId", skillId)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("status", status)
                .eq("isPublic", isPublic)
                .eq("userId", userId)
                .ge("createTime", createTimeStart)
                .le("createTime", createTimeEnd)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    /**
     * 删除应用时，关联删除对话历史
     *
     * @param id
     * @return
     */
    @Override
    @CacheEvict(value = "app_detail", key = "#id")
    public boolean removeById(Serializable id) {
        if (id == null) {
            return false;
        }
        long appId = Long.parseLong(id.toString());
        if (appId <= 0) {
            return false;
        }
        // 先删除关联的对话历史
        try {
            chatHistoryService.deleteByAppId(appId);
        } catch (Exception e) {
            log.error("删除应用关联的对话历史失败：{}", e.getMessage());
        }
        // 删除应用
        return super.removeById(id);
    }

    @Override
    public Page<AppVO> semanticSearchApps(SemanticSearchRequest semanticSearchRequest) {
        // 1. 参数校验
        ThrowUtils.throwIf(semanticSearchRequest == null || StrUtil.isBlank(semanticSearchRequest.getKeyword()),
                ErrorCode.PARAMS_ERROR, "搜索关键词不能为空");
        String keyword = semanticSearchRequest.getKeyword().trim();
        ThrowUtils.throwIf(keyword.length() > 100, ErrorCode.PARAMS_ERROR, "搜索词过长");
        long pageNum = semanticSearchRequest.getPageNum();
        long pageSize = semanticSearchRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        // 2. AI 扩展搜索词（解析失败时降级为只用原始关键词）
        List<String> keywords = expandSearchKeywords(keyword);
        log.info("语义搜索关键词扩展完成，原始: {}, 扩展: {}", keyword, keywords);
        // 3. 构建多关键词 OR 模糊匹配查询（appName / initPrompt），只搜精选且公开的应用
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("priority", AppConstant.GOOD_APP_PRIORITY)
                .eq("isPublic", 1)
                .and(buildSemanticSearchCondition(keywords))
                .orderBy("createTime", false);
        // 4. 分页查询
        Page<App> appPage = this.page(Page.of(pageNum, pageSize), queryWrapper);
        // 5. 封装 VO
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        appVOPage.setRecords(this.getAppVOList(appPage.getRecords()));
        return appVOPage;
    }

    @Override
    public SelfCheckResultVO selfCheckAppCode(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 错误");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 权限校验，仅本人可检查自己的应用
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限检查该应用");
        }
        // 4. 获取代码生成类型
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用代码生成类型错误");
        }
        // 5. 定位代码生成目录（tmp/code_output/{codeGenType}_{appId}）
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + codeGenType + "_" + appId;
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用代码不存在，请先生成代码");
        }
        // 6. 按类型读取代码内容
        String codeContent = readCodeContent(sourceDir, codeGenTypeEnum);
        if (StrUtil.isBlank(codeContent)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用代码内容为空，请先生成代码");
        }
        // 7. 截断到合理长度再发送给模型
        String truncatedCode = truncate(codeContent, MAX_SELFCHECK_CODE_CHARS);
        // 8. 调用 AI 自检并解析结果
        return doSelfCheck(truncatedCode);
    }

    /**
     * AI 扩展搜索关键词，解析失败时降级为只用原始关键词
     */
    private List<String> expandSearchKeywords(String keyword) {
        List<String> keywords = new ArrayList<>();
        // 原始关键词兜底，保证至少能搜
        keywords.add(keyword);
        try {
            String aiResult = semanticSearchExpandService.expandKeywords(keyword);
            String jsonText = extractJson(aiResult);
            List<String> expanded = JSONUtil.toList(jsonText, String.class);
            if (CollUtil.isNotEmpty(expanded)) {
                for (String kw : expanded) {
                    if (keywords.size() >= MAX_SEARCH_KEYWORDS) {
                        break;
                    }
                    if (StrUtil.isNotBlank(kw)) {
                        String trimmed = kw.trim();
                        if (!keywords.contains(trimmed)) {
                            keywords.add(trimmed);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("AI 扩展搜索关键词失败，降级为仅使用原始关键词: {}", e.getMessage());
        }
        return keywords;
    }

    /**
     * 构造多关键词 OR 匹配条件：
     * (appName LIKE k1 OR initPrompt LIKE k1) OR (appName LIKE k2 OR initPrompt LIKE k2) ...
     */
    private QueryCondition buildSemanticSearchCondition(List<String> keywords) {
        QueryCondition condition = buildKeywordCondition(keywords.get(0));
        for (int i = 1; i < keywords.size(); i++) {
            condition = condition.or(buildKeywordCondition(keywords.get(i)));
        }
        return condition;
    }

    /**
     * 单个关键词的匹配条件：appName LIKE kw OR initPrompt LIKE kw
     */
    private QueryCondition buildKeywordCondition(String keyword) {
        return QueryCondition.create(new QueryColumn("appName"), "like", keyword)
                .or(QueryCondition.create(new QueryColumn("initPrompt"), "like", keyword));
    }

    /**
     * 按代码生成类型读取关键文件内容
     */
    private String readCodeContent(File sourceDir, CodeGenTypeEnum codeGenTypeEnum) {
        return switch (codeGenTypeEnum) {
            // HTML 单文件：直接读取 index.html
            case HTML -> readFileWithHeader(sourceDir, "index.html");
            // 多文件：拼接 index.html + style.css + script.js
            case MULTI_FILE -> concatCode(
                    readFileWithHeader(sourceDir, "index.html"),
                    readFileWithHeader(sourceDir, "style.css"),
                    readFileWithHeader(sourceDir, "script.js"));
            // Vue 工程：读取关键文件 + 补充扫描 src 下源码
            case VUE_PROJECT -> readVueProjectCode(sourceDir);
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的代码生成类型");
        };
    }

    /**
     * 拼接多个代码片段
     */
    private String concatCode(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (StrUtil.isNotBlank(part)) {
                sb.append(part).append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * 读取单个文件内容并加上文件路径标记，文件不存在或为空时返回空串
     */
    private String readFileWithHeader(File baseDir, String relativePath) {
        File file = new File(baseDir, relativePath);
        if (!file.exists() || !file.isFile()) {
            return "";
        }
        String content = FileUtil.readUtf8String(file);
        if (StrUtil.isBlank(content)) {
            return "";
        }
        return "\n===== 文件: " + relativePath + " =====\n" + truncate(content, MAX_SINGLE_FILE_CHARS);
    }

    /**
     * Vue 项目：读取关键文件，不足时补充扫描 src 目录下的源码文件
     */
    private String readVueProjectCode(File projectDir) {
        List<String> keyFiles = List.of(
                "index.html",
                "package.json",
                "vite.config.js",
                "src/main.js",
                "src/main.ts",
                "src/App.vue",
                "src/router/index.js",
                "src/router/index.ts"
        );
        StringBuilder sb = new StringBuilder();
        for (String relativePath : keyFiles) {
            appendFileContent(sb, projectDir, relativePath);
        }
        File srcDir = new File(projectDir, "src");
        if (srcDir.isDirectory() && sb.length() < MAX_SELFCHECK_CODE_CHARS) {
            appendExtraSrcFiles(sb, srcDir);
        }
        return sb.toString();
    }

    private void appendFileContent(StringBuilder sb, File baseDir, String relativePath) {
        String content = readFileWithHeader(baseDir, relativePath);
        if (StrUtil.isNotBlank(content)) {
            sb.append(content).append('\n');
        }
    }

    /**
     * 补充扫描 src 目录下的 .vue/.js/.css 源码文件（跳过已在关键文件列表中读取过的）
     */
    private void appendExtraSrcFiles(StringBuilder sb, File srcDir) {
        List<File> files = FileUtil.loopFiles(srcDir, file -> {
            String name = file.getName();
            return name.endsWith(".vue") || name.endsWith(".js") || name.endsWith(".css");
        });
        files.sort(Comparator.comparing(File::getAbsolutePath));
        int count = 0;
        for (File file : files) {
            if (count >= MAX_EXTRA_SOURCE_FILES || sb.length() >= MAX_SELFCHECK_CODE_CHARS) {
                break;
            }
            String relativePath = relativePathOf(srcDir, file);
            // 跳过关键文件列表中已读取的文件（main.js、App.vue、router 下文件）
            if (relativePath == null || relativePath.startsWith("main.")
                    || relativePath.startsWith("App.vue") || relativePath.startsWith("router/")) {
                continue;
            }
            String content = FileUtil.readUtf8String(file);
            if (StrUtil.isBlank(content)) {
                continue;
            }
            sb.append("\n===== 文件: src/").append(relativePath).append(" =====\n")
                    .append(truncate(content, MAX_SINGLE_FILE_CHARS)).append('\n');
            count++;
        }
    }

    /**
     * 计算文件相对 src 目录的路径（统一斜杠）
     */
    private String relativePathOf(File baseDir, File file) {
        try {
            return baseDir.toPath().relativize(file.toPath()).toString().replace('\\', '/');
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 调用 AI 自检并解析 JSON 结果，解析失败时返回降级结果
     */
    private SelfCheckResultVO doSelfCheck(String codeContent) {
        SelfCheckResultVO result = new SelfCheckResultVO();
        try {
            String aiResult = codeSelfCheckService.checkAndFixCode(codeContent);
            String jsonText = extractJson(aiResult);
            JSONObject jsonObject = JSONUtil.parseObj(jsonText);
            result.setHasIssue(jsonObject.getBool("hasIssue", false));
            JSONArray issuesArray = jsonObject.getJSONArray("issues");
            result.setIssues(issuesArray == null ? new ArrayList<>() : JSONUtil.toList(issuesArray, String.class));
            result.setFixedCode(jsonObject.getStr("fixedCode", ""));
        } catch (Exception e) {
            log.warn("AI 代码自检结果解析失败，返回降级结果: {}", e.getMessage());
            result.setHasIssue(true);
            result.setIssues(List.of("AI 自检结果解析失败，请稍后重试"));
            result.setFixedCode("");
        }
        return result;
    }

    /**
     * 提取 AI 返回文本中的 JSON 内容（去除 ```json 代码块包裹等多余内容）
     */
    private String extractJson(String text) {
        if (StrUtil.isBlank(text)) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline >= 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            } else {
                trimmed = trimmed.substring(3);
            }
            int lastFence = trimmed.lastIndexOf("```");
            if (lastFence >= 0) {
                trimmed = trimmed.substring(0, lastFence);
            }
        }
        return trimmed.trim();
    }

    /**
     * 将内容截断到指定长度，超出时追加提示
     */
    private String truncate(String content, int maxChars) {
        if (content == null) {
            return "";
        }
        if (content.length() <= maxChars) {
            return content;
        }
        return content.substring(0, maxChars) + "\n...（内容过长已截断）";
    }
}

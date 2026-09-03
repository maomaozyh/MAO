package com.mao.maocodemother.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mao.maocodemother.ai.PromptExpandService;
import com.mao.maocodemother.annotation.AuthCheck;
import com.mao.maocodemother.common.BaseResponse;
import com.mao.maocodemother.common.DeleteRequest;
import com.mao.maocodemother.common.ResultUtils;
import com.mao.maocodemother.constant.AppConstant;
import com.mao.maocodemother.constant.UserConstant;
import com.mao.maocodemother.exception.BusinessException;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.exception.ThrowUtils;
import com.mao.maocodemother.model.dto.app.*;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.vo.AppVO;
import com.mao.maocodemother.model.vo.SelfCheckResultVO;
import com.mao.maocodemother.model.enums.SecondsBizTypeEnum;
import com.mao.maocodemother.service.SecondsService;
import com.mao.maocodemother.ratelimter.annotation.RateLimit;
import com.mao.maocodemother.ratelimter.enums.RateLimitType;
import com.mao.maocodemother.service.ProjectDownloadService;
import com.mao.maocodemother.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import com.mao.maocodemother.model.entity.App;
import com.mao.maocodemother.service.AppService;
import com.mao.maocodemother.service.OperationLogService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 应用 控制层。
 *
 * @author <a href="https://github.com/liyupi">程序员mao</a>
 */
@RestController
@RequestMapping("/app")
public class AppController {

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    @Resource
    private SecondsService secondsService;

    @Resource
    private ProjectDownloadService projectDownloadService;

    @Resource
    private PromptExpandService promptExpandService;

    @Resource
    private OperationLogService operationLogService;

    @GetMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimit(limitType = RateLimitType.USER, rate = 5, rateInterval = 60, message = "AI 对话请求过于频繁，请稍后再试")
    public Flux<ServerSentEvent<String>> chatToGenCode(@RequestParam Long appId,
                                                       @RequestParam String message,
                                                       @RequestParam(required = false) String mode,
                                                       HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 id 错误");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "提示词不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用服务生成代码（SSE 流式返回）
        Flux<String> contentFlux = appService.chatToGenCode(appId, message, mode, loginUser);
        return contentFlux
                .map(chunk -> {
                    Map<String, String> wrapper = Map.of("d", chunk);
                    String jsonData = JSONUtil.toJsonStr(wrapper);
                    return ServerSentEvent.<String>builder()
                            .data(jsonData)
                            .build();
                })
                .concatWith(Mono.just(
                        // 发送结束事件
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("")
                                .build()
                ));
    }

    /**
     * 描述智能扩写：将一句话需求扩写为详细需求描述
     *
     * @param promptExpandRequest 扩写请求
     * @param request             请求
     * @return 扩写后的需求描述
     */
    @PostMapping("/prompt/expand")
    @RateLimit(limitType = RateLimitType.USER, rate = 20, rateInterval = 60, message = "扩写请求过于频繁，请稍后再试")
    public BaseResponse<String> expandPrompt(@RequestBody PromptExpandRequest promptExpandRequest,
                                             HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(promptExpandRequest == null || StrUtil.isBlank(promptExpandRequest.getPrompt()),
                ErrorCode.PARAMS_ERROR, "提示词不能为空");
        String prompt = promptExpandRequest.getPrompt().trim();
        ThrowUtils.throwIf(prompt.length() > 2000, ErrorCode.PARAMS_ERROR, "提示词过长");
        // 校验登录
        User loginUser = userService.getLoginUser(request);
        // 预扣积分，失败自动退回
        long costRecordId = secondsService.deduct(loginUser.getId(), SecondsBizTypeEnum.EXPAND, null, null);
        try {
            String expandedPrompt = promptExpandService.expandPrompt(prompt);
            return ResultUtils.success(expandedPrompt);
        } catch (Exception e) {
            secondsService.refund(costRecordId);
            throw e;
        }
    }

    /**
     * 应用部署
     *
     * @param appDeployRequest 部署请求
     * @param request          请求
     * @return 部署 URL
     */
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
        // 检查部署请求是否为空
        ThrowUtils.throwIf(appDeployRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取应用 ID
        Long appId = appDeployRequest.getAppId();
        // 检查应用 ID 是否为空
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用服务部署应用
        String deployUrl = appService.deployApp(appId, loginUser);
        // 返回部署 URL
        return ResultUtils.success(deployUrl);
    }

    /**
     * 下载应用代码
     *
     * @param appId    应用ID
     * @param request  请求
     * @param response 响应
     */
    @GetMapping("/download/{appId}")
    public void downloadAppCode(@PathVariable Long appId,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        // 1. 基础校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        // 2. 查询应用信息
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 权限校验：只有应用创建者可以下载代码
        User loginUser = userService.getLoginUser(request);
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限下载该应用代码");
        }
        // 4. 构建应用代码目录路径（生成目录，非部署目录）
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 5. 检查代码目录是否存在
        File sourceDir = new File(sourceDirPath);
        ThrowUtils.throwIf(!sourceDir.exists() || !sourceDir.isDirectory(),
                ErrorCode.NOT_FOUND_ERROR, "应用代码不存在，请先生成代码");
        // 6. 生成下载文件名（不建议添加中文内容）
        String downloadFileName = String.valueOf(appId);
        // 7. 调用通用下载服务
        projectDownloadService.downloadProjectAsZip(sourceDirPath, downloadFileName, response);
    }

    /**
     * 创建应用
     *
     * @param appAddRequest 创建应用请求
     * @param request       请求
     * @return 应用 id
     */
    @CacheEvict(value = "good_app_page", allEntries = true)
    @PostMapping("/add")
    public BaseResponse<String> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        Long appId = appService.createApp(appAddRequest, loginUser);
        // 雪花 ID 超过 JS 安全整数范围，必须转字符串返回，否则前端 JSON.parse 会精度丢失
        return ResultUtils.success(String.valueOf(appId));
    }

    /**
     * 更新应用（用户只能更新自己的应用名称）
     *
     * @param appUpdateRequest 更新请求
     * @param request          请求
     * @return 更新结果
     */
    @Caching(evict = {
            @CacheEvict(value = "good_app_page", allEntries = true),
            @CacheEvict(value = "app_detail", key = "#appUpdateRequest.id")
    })
    @PostMapping("/update")
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        if (appUpdateRequest == null || appUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = appUpdateRequest.getId();
        // 判断是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人可更新
        if (!oldApp.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        App app = new App();
        app.setId(id);
        app.setAppName(appUpdateRequest.getAppName());
        // 公开/私密开关（不传则保持原值）
        if (appUpdateRequest.getIsPublic() != null) {
            app.setIsPublic(appUpdateRequest.getIsPublic());
        }
        // 设置编辑时间
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 删除应用（用户只能删除自己的应用）
     *
     * @param deleteRequest 删除请求
     * @param request       请求
     * @return 删除结果
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldApp.getUserId().equals(loginUser.getId()) && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = appService.removeById(id);
        // 记录操作日志
        try {
            operationLogService.recordSuccess(loginUser.getId(), loginUser.getUserName(), "app", "删除",
                    String.valueOf(id), "删除应用：" + (StrUtil.isNotBlank(oldApp.getAppName()) ? oldApp.getAppName() : id), request);
        } catch (Exception ignore) {
        }
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取应用详情
     *
     * @param id 应用 id
     * @return 应用详情
     */
    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 取登录用户（允许未登录，用于私密校验）
        User loginUser = userService.getLoginUserPermitNull(request);
        // 查询数据库（带缓存）
        App app = appService.getAppById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 私密校验：私密应用仅本人与管理员可见，其余人不可查看（不暴露应用存在性）
        boolean isAdmin = loginUser != null && UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isOwner = loginUser != null && app.getUserId().equals(loginUser.getId());
        if (app.getIsPublic() != null && app.getIsPublic() == 0 && !isOwner && !isAdmin) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "该应用已设为私密，无权限查看");
        }
        // 获取封装类（包含用户信息）
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 获取当前用户最近打开的应用列表（侧边栏"最近项目"）
     *
     * @param request 请求
     * @return 应用封装类列表（按最近打开时间倒序，含 lastOpenTime 字段）
     */
    @GetMapping("/recent")
    public BaseResponse<List<AppVO>> listRecentApps(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(appService.listRecentApps(loginUser.getId()));
    }

    /**
     * 记录某应用被打开，更新最近打开时间（进入对话页时由前端自动调用，侧边栏"最近项目"实时同步）
     *
     * @param appId  应用 id
     * @param request 请求
     * @return 是否成功
     */
    @PostMapping("/recent/{appId}")
    public BaseResponse<Boolean> markAppOpened(@PathVariable Long appId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(appService.markAppOpened(loginUser, appId));
    }

    /**
     * 分页获取当前用户创建的应用列表
     *
     * @param appQueryRequest 查询请求
     * @param request         请求
     * @return 应用列表
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<AppVO>> listMyAppVOByPage(@RequestBody AppQueryRequest appQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        // 限制每页最多 20 个
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        long pageNum = appQueryRequest.getPageNum();
        // 只查询当前用户的应用
        appQueryRequest.setUserId(loginUser.getId());
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 分页获取精选应用列表
     *
     * @param appQueryRequest 查询请求
     * @return 精选应用列表
     */
    @PostMapping("/good/list/page/vo")
    @Cacheable(
            value = "good_app_page",
            key = "T(com.mao.maocodemother.utils.CacheKeyUtils).generateKey(#appQueryRequest)",
            condition = "#appQueryRequest.pageNum <= 10"
    )
    public BaseResponse<Page<AppVO>> listGoodAppVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 限制每页最多 20 个
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        long pageNum = appQueryRequest.getPageNum();
        // 只查询精选的应用
        appQueryRequest.setPriority(AppConstant.GOOD_APP_PRIORITY);
        // 广场只展示公开应用（私密应用不外显）
        appQueryRequest.setIsPublic(1);
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        // 分页查询
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 语义搜索（AI 查询扩展）
     * 将自然语言搜索词交给 DeepSeek 扩展为多个关键词，再按 appName/initPrompt 做 OR 模糊匹配
     *
     * @param semanticSearchRequest 语义搜索请求
     * @return 精选应用分页列表
     */
    @PostMapping("/search/semantic")
    @RateLimit(limitType = RateLimitType.API, rate = 10, rateInterval = 60, message = "搜索请求过于频繁，请稍后再试")
    public BaseResponse<Page<AppVO>> semanticSearchApps(@RequestBody SemanticSearchRequest semanticSearchRequest,
                                                        HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        // 预扣积分，失败自动退回
        long costRecordId = secondsService.deduct(loginUser.getId(), SecondsBizTypeEnum.SEMANTIC_SEARCH, null, null);
        try {
            Page<AppVO> appVOPage = appService.semanticSearchApps(semanticSearchRequest);
            return ResultUtils.success(appVOPage);
        } catch (Exception e) {
            secondsService.refund(costRecordId);
            throw e;
        }
    }

    /**
     * 代码错误自检（AI 自查）
     * 读取应用生成的代码文件，交给 DeepSeek 检查常见问题并尝试修复
     *
     * @param selfCheckRequest 自检请求
     * @param request          请求
     * @return 自检结果
     */
    @PostMapping("/code/selfcheck")
    @RateLimit(limitType = RateLimitType.USER, rate = 10, rateInterval = 60, message = "自检请求过于频繁，请稍后再试")
    public BaseResponse<SelfCheckResultVO> selfCheckAppCode(@RequestBody SelfCheckRequest selfCheckRequest,
                                                            HttpServletRequest request) {
        ThrowUtils.throwIf(selfCheckRequest == null || selfCheckRequest.getAppId() == null
                        || selfCheckRequest.getAppId() <= 0,
                ErrorCode.PARAMS_ERROR, "应用 ID 错误");
        User loginUser = userService.getLoginUser(request);
        // 预扣积分，失败自动退回
        long costRecordId = secondsService.deduct(loginUser.getId(), SecondsBizTypeEnum.SELF_CHECK,
                null, selfCheckRequest.getAppId());
        try {
            SelfCheckResultVO result = appService.selfCheckAppCode(selfCheckRequest.getAppId(), loginUser);
            return ResultUtils.success(result);
        } catch (Exception e) {
            secondsService.refund(costRecordId);
            throw e;
        }
    }

    /**
     * 管理员删除应用
     *
     * @param deleteRequest 删除请求
     * @return 删除结果
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Caching(evict = {
            @CacheEvict(value = "good_app_page", allEntries = true),
            @CacheEvict(value = "app_detail", key = "#deleteRequest.id")
    })
    public BaseResponse<Boolean> deleteAppByAdmin(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        // 判断是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = appService.removeById(id);
        // 记录操作日志
        try {
            User loginUser = userService.getLoginUser(request);
            operationLogService.recordSuccess(loginUser.getId(), loginUser.getUserName(), "app", "管理员删除",
                    String.valueOf(id), "管理员删除应用：" + (StrUtil.isNotBlank(oldApp.getAppName()) ? oldApp.getAppName() : id), request);
        } catch (Exception ignore) {
        }
        return ResultUtils.success(result);
    }

    /**
     * 管理员更新应用
     *
     * @param appAdminUpdateRequest 更新请求
     * @return 更新结果
     */
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Caching(evict = {
            @CacheEvict(value = "good_app_page", allEntries = true),
            @CacheEvict(value = "app_detail", key = "#appAdminUpdateRequest.id")
    })
    public BaseResponse<Boolean> updateAppByAdmin(@RequestBody AppAdminUpdateRequest appAdminUpdateRequest) {
        if (appAdminUpdateRequest == null || appAdminUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = appAdminUpdateRequest.getId();
        // 判断是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        App app = new App();
        BeanUtil.copyProperties(appAdminUpdateRequest, app);
        // 设置编辑时间
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 管理员分页获取应用列表
     *
     * @param appQueryRequest 查询请求
     * @return 应用列表
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> listAppVOByPageByAdmin(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 管理员根据 id 获取应用详情
     *
     * @param id 应用 id
     * @return 应用详情
     */
    @GetMapping("/admin/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AppVO> getAppVOByIdByAdmin(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(appService.getAppVO(app));
    }
}

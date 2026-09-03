package com.mao.maocodemother.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.mao.maocodemother.model.dto.app.AppAddRequest;
import com.mao.maocodemother.model.dto.app.AppQueryRequest;
import com.mao.maocodemother.model.dto.app.SemanticSearchRequest;
import com.mao.maocodemother.model.entity.App;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.vo.AppVO;
import com.mao.maocodemother.model.vo.SelfCheckResultVO;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author <a href="https://github.com/liyupi">程序员mao</a>
 */
public interface AppService extends IService<App> {

    /**
     * 通过对话生成应用代码
     *
     * @param appId     应用 ID
     * @param message   提示词
     * @param loginUser 登录用户
     * @return
     */
    Flux<String> chatToGenCode(Long appId, String message, String mode, User loginUser);

    /**
     * 创建应用
     *
     * @param appAddRequest
     * @param loginUser
     * @return
     */
    Long createApp(AppAddRequest appAddRequest, User loginUser);

    /**
     * 应用部署
     *
     * @param appId     应用 ID
     * @param loginUser 登录用户
     * @return 可访问的部署地址
     */
    String deployApp(Long appId, User loginUser);

    /**
     * 异步生成应用截图并更新封面
     *
     * @param appId  应用ID
     * @param appUrl 应用访问URL
     */
    void generateAppScreenshotAsync(Long appId, String appUrl);

    /**
     * 获取应用封装类
     *
     * @param app
     * @return
     */
    AppVO getAppVO(App app);

    /**
     * 获取应用封装类列表
     *
     * @param appList
     * @return
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 按 id 获取应用实体（带缓存，按 id 失效）。
     * 私密校验在调用方（Controller）完成，本方法只负责取数。
     *
     * @param id 应用 id
     * @return 应用实体
     */
    App getAppById(long id);

    /**
     * 获取当前用户最近打开的应用列表（侧边栏"最近项目"）。
     * 返回该用户最近打开过的应用（按 lastOpenTime 倒序），仅本人可见。
     *
     * @param userId 用户 id
     * @return 应用封装类列表（含 lastOpenTime 字段）
     */
    List<AppVO> listRecentApps(Long userId);

    /**
     * 记录某应用被打开，更新其最近打开时间（侧边栏"最近项目"同步）。
     * 仅应用创建者本人（或管理员，与管理员可访问任意应用一致）可更新。
     *
     * @param loginUser 登录用户
     * @param appId     应用 id
     * @return 是否成功
     */
    boolean markAppOpened(User loginUser, Long appId);

    /**
     * 构造应用查询条件
     *
     * @param appQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 语义搜索（AI 查询扩展）：将自然语言关键词扩展为多个关键词，
     * 在 appName/initPrompt 上做多关键词 OR 模糊匹配，按创建时间倒序分页
     *
     * @param semanticSearchRequest 语义搜索请求
     * @return 应用分页列表
     */
    Page<AppVO> semanticSearchApps(SemanticSearchRequest semanticSearchRequest);

    /**
     * 代码错误自检（AI 自查）：读取应用生成的代码文件并调用 AI 检查常见问题
     *
     * @param appId     应用 ID
     * @param loginUser 登录用户（须为应用创建者）
     * @return 自检结果
     */
    SelfCheckResultVO selfCheckAppCode(Long appId, User loginUser);

}

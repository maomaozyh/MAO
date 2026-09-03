package com.mao.maocodemother.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.mao.maocodemother.model.dto.app.AppAddRequest;
import com.mao.maocodemother.model.dto.app.AppQueryRequest;
import com.mao.maocodemother.model.entity.App;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.vo.AppVO;
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
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

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
     * 构造应用查询条件
     *
     * @param appQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 获取当前用户最近打开的应用列表（用于“最近项目”）
     *
     * @param userId 用户 id
     * @param limit  数量上限
     * @return 应用封装类列表，按最近打开时间（无则按创建时间）倒序
     */
    List<AppVO> listRecentApps(Long userId, int limit);

    /**
     * 记录应用被打开，更新最近打开时间（用于“最近项目”同步）
     *
     * @param appId  应用 id
     * @param userId 用户 id
     * @return 是否更新成功
     */
    boolean markAppOpened(Long appId, Long userId);

}

package com.mao.maocodemother.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.mao.maocodemother.annotation.AuthCheck;
import com.mao.maocodemother.annotation.AuditLog;
import com.mao.maocodemother.common.BaseResponse;
import com.mao.maocodemother.common.DeleteRequest;
import com.mao.maocodemother.common.ResultUtils;
import com.mao.maocodemother.config.QQOauthProperties;
import com.mao.maocodemother.config.WechatOauthProperties;
import com.mao.maocodemother.constant.UserConstant;
import com.mao.maocodemother.exception.BusinessException;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.exception.ThrowUtils;
import com.mao.maocodemother.model.dto.user.*;
import com.mao.maocodemother.model.vo.LoginUserVO;
import com.mao.maocodemother.model.vo.UserVO;
import com.mao.maocodemother.model.vo.CaptchaVO;
import com.mao.maocodemother.utils.CaptchaUtil;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.service.OperationLogService;
import com.mao.maocodemother.service.UserService;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * 用户 控制层。
 *
 * @author <a href="https://github.com/liyupi">程序员mao</a>
 */
@RestController
@Slf4j
@RequestMapping("/user")
public class UserController {

    /**
     * 微信扫码登录成功后的前端跳转地址
     */
    private static final String WECHAT_LOGIN_SUCCESS_REDIRECT_URL = "http://localhost:5173";

    private static final String QQ_LOGIN_SUCCESS_REDIRECT_URL = "http://localhost:5173";

    /**
     * 图形验证码 key 前缀（与 UserServiceImpl 中的 CAPTCHA_CODE_PREFIX 保持一致，文本存 Redis 5 分钟）
     */
    private static final String CAPTCHA_CODE_PREFIX = "captcha:code:";

    /**
     * 图形验证码有效期（分钟）
     */
    private static final long CAPTCHA_TTL_MINUTES = 5;

    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private WechatOauthProperties wechatOauthProperties;

    @Resource
    private QQOauthProperties qqOauthProperties;

    @Resource
    private OperationLogService operationLogService;

    /**
     * 用户注册
     *
     * @param userRegisterRequest 用户注册请求
     * @return 注册结果
     */
    @PostMapping("/register")
    @AuditLog(actionType = "REGISTER", actionDesc = "用户注册")
    public BaseResponse<String> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String phone = userRegisterRequest.getPhone();
        String code = userRegisterRequest.getCode();
        long result = userService.userRegister(userAccount, userPassword, checkPassword, phone, code);
        // 雪花 ID 超过 JS 安全整数范围，转字符串返回避免前端精度丢失
        return ResultUtils.success(String.valueOf(result));
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest 用户登录请求
     * @param request          请求对象
     * @return 脱敏后的用户登录信息
     */
    @PostMapping("/login")
    @AuditLog(actionType = "LOGIN", actionDesc = "用户登录")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        String phone = userLoginRequest.getPhone();
        String code = userLoginRequest.getCode();
        try {
            LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, phone, code, request);
            // 记录登录成功日志
            try {
                operationLogService.recordSuccess(loginUserVO.getId(), loginUserVO.getUserName(), "user", "登录",
                        String.valueOf(loginUserVO.getId()), "用户登录成功：" + userAccount, request);
            } catch (Exception ignore) {
            }
            return ResultUtils.success(loginUserVO);
        } catch (BusinessException e) {
            // 记录登录失败日志
            try {
                operationLogService.recordFail(null, userAccount, "user", "登录", userAccount,
                        "用户登录失败：" + userAccount, e.getMessage(), request);
            } catch (Exception ignore) {
            }
            throw e;
        }
    }

    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 用户注销
     *
     * @param request 请求对象
     * @return
     */
    @PostMapping("/logout")
    @AuditLog(actionType = "LOGOUT", actionDesc = "用户登出", recordParams = false)
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 发送短信验证码
     *
     * @param smsCodeRequest 请求对象
     * @return 是否发送成功
     */
    @PostMapping("/login/sms/code")
    public BaseResponse<Boolean> sendSmsCode(@RequestBody SmsCodeRequest smsCodeRequest) {
        ThrowUtils.throwIf(smsCodeRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userService.sendSmsCode(
                smsCodeRequest.getPhone(), smsCodeRequest.getCaptchaKey(), smsCodeRequest.getCaptcha()));
    }

    /**
     * 获取图形验证码（公开发码接口前的人机校验，挡自动化轰炸）。
     * 返回 captchaKey + base64 图片，前端发码时回传校验。
     */
    @GetMapping("/captcha")
    public BaseResponse<CaptchaVO> getCaptcha() throws Exception {
        String code = CaptchaUtil.generateCode();
        String key = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set(CAPTCHA_CODE_PREFIX + key, code, Duration.ofMinutes(CAPTCHA_TTL_MINUTES));
        if (log.isDebugEnabled()) {
            log.debug("[CAPTCHA] key={} code={}", key, code);
        }
        return ResultUtils.success(new CaptchaVO(key, CaptchaUtil.generateBase64Image(code)));
    }

    /**
     * 短信验证码登录（不存在则自动注册）
     *
     * @param smsLoginRequest 请求对象
     * @param request         请求对象
     * @return 脱敏后的用户登录信息
     */
    @PostMapping("/login/sms")
    public BaseResponse<LoginUserVO> userLoginBySms(@RequestBody SmsLoginRequest smsLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(smsLoginRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userService.userLoginBySms(smsLoginRequest.getPhone(), smsLoginRequest.getCode(), request));
    }

    /**
     * 微信扫码登录：跳转微信授权页
     */
    @GetMapping("/login/wechat")
    public void userLoginByWechat(HttpServletResponse response) throws IOException {
        response.sendRedirect(userService.getWechatLoginUrl());
    }

    /**
     * 微信扫码登录回调
     */
    @GetMapping("/login/wechat/callback")
    public void wechatCallback(@RequestParam("code") String code,
                               @RequestParam(value = "state", required = false) String state,
                               HttpServletRequest request,
                               HttpServletResponse response) throws IOException {
        // mock 模式：未配置 app-id 时直接返回 JSON 提示，不抛 500
        if (StrUtil.isBlank(wechatOauthProperties.getAppId())) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JSONUtil.toJsonStr(ResultUtils.error(ErrorCode.PARAMS_ERROR, "微信登录未配置")));
            return;
        }
        // 登录成功，session cookie 随响应写入，重定向回前端
        userService.userLoginByWechat(code, state, request);
        response.sendRedirect(WECHAT_LOGIN_SUCCESS_REDIRECT_URL);
    }

    /**
     * QQ 互联扫码登录：跳转 QQ 授权页
     */
    @GetMapping("/login/qq")
    public void userLoginByQQ(HttpServletResponse response) throws IOException {
        // 未配置 app-id 时直接返回 JSON 提示，不抛 500（与生产未接入凭据时一致）
        if (StrUtil.isBlank(qqOauthProperties.getAppId())) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JSONUtil.toJsonStr(ResultUtils.error(ErrorCode.PARAMS_ERROR, "QQ 登录未配置")));
            return;
        }
        response.sendRedirect(userService.getQQLoginUrl());
    }

    /**
     * QQ 互联扫码登录回调
     */
    @GetMapping("/login/qq/callback")
    public void qqCallback(@RequestParam("code") String code,
                           @RequestParam(value = "state", required = false) String state,
                           HttpServletRequest request,
                           HttpServletResponse response) throws IOException {
        // 未配置 app-id 时直接返回 JSON 提示，不抛 500（与生产未接入凭据时一致）
        if (StrUtil.isBlank(qqOauthProperties.getAppId())) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JSONUtil.toJsonStr(ResultUtils.error(ErrorCode.PARAMS_ERROR, "QQ 登录未配置")));
            return;
        }
        // 登录成功，session cookie 随响应写入，重定向回前端
        userService.userLoginByQQ(code, state, request);
        response.sendRedirect(QQ_LOGIN_SUCCESS_REDIRECT_URL);
    }

    /**
     * 创建用户
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        // 默认密码 12345678
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 雪花 ID 超过 JS 安全整数范围，转字符串返回避免前端精度丢失
        return ResultUtils.success(String.valueOf(user.getId()));
    }

    /**
     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取包装类
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long userId = deleteRequest.getId();
        User oldUser = userService.getById(userId);
        boolean b = userService.removeById(userId);
        // 记录操作日志
        try {
            User loginUser = userService.getLoginUser(request);
            String targetName = oldUser != null ? StrUtil.blankToDefault(oldUser.getUserName(), oldUser.getUserAccount()) : String.valueOf(userId);
            operationLogService.recordSuccess(loginUser.getId(), loginUser.getUserName(), "user", "删除",
                    String.valueOf(userId), "删除用户：" + targetName, request);
        } catch (Exception ignore) {
        }
        return ResultUtils.success(b);
    }

    /**
     * 更新用户
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页获取用户封装列表（仅管理员）
     *
     * @param userQueryRequest 查询请求参数
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = userQueryRequest.getPageNum();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(Page.of(pageNum, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        // 数据脱敏
        Page<UserVO> userVOPage = new Page<>(pageNum, pageSize, userPage.getTotalRow());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

    /**
     * 管理员重置用户密码
     */
    @PostMapping("/reset/password")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> resetPassword(@RequestBody UserResetPasswordRequest request) {
        ThrowUtils.throwIf(request == null || request.getUserId() == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.resetPassword(request.getUserId(), request.getNewPassword());
        return ResultUtils.success(result);
    }

    /**
     * 用户修改自己的密码（需校验原密码）
     *
     * <p>与「管理员重置他人密码」的区别：重置不需要原密码且要求管理员权限；
     * 本接口任何已登录用户都可调用，但必须先正确输入原密码。
     */
    @PostMapping("/update/password")
    public BaseResponse<Boolean> updateMyPassword(
            @RequestBody UserUpdatePasswordRequest request,
            HttpServletRequest servletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(servletRequest);
        boolean result = userService.updateMyPassword(
                loginUser,
                request.getOldPassword(),
                request.getNewPassword(),
                request.getCheckPassword());
        return ResultUtils.success(result);
    }

    /**
     * 发送「找回密码」短信验证码（公开接口：未登录也可调用）
     */
    @PostMapping("/password/reset/sms/code")
    public BaseResponse<Boolean> sendResetSmsCode(@RequestBody SmsCodeRequest smsCodeRequest) {
        ThrowUtils.throwIf(smsCodeRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userService.sendResetSmsCode(
                smsCodeRequest.getPhone(), smsCodeRequest.getCaptchaKey(), smsCodeRequest.getCaptcha()));
    }

    /**
     * 手机号 + 验证码找回密码（公开接口：未登录也可调用）
     */
    @PostMapping("/password/reset/phone")
    public BaseResponse<Boolean> resetPasswordByPhone(@RequestBody UserResetPasswordByPhoneRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userService.resetPasswordByPhone(
                request.getPhone(), request.getCode(), request.getNewPassword(), request.getCheckPassword()));
    }

    /**
     * 发送「找回密码」邮箱验证码（公开接口：未登录也可调用）
     */
    @PostMapping("/password/reset/email/code")
    public BaseResponse<Boolean> sendResetEmailCode(@RequestBody UserEmailCodeRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userService.sendResetEmailCode(
                request.getEmail(), request.getCaptchaKey(), request.getCaptcha()));
    }

    /**
     * 邮箱 + 验证码找回密码（公开接口：未登录也可调用）
     */
    @PostMapping("/password/reset/email")
    public BaseResponse<Boolean> resetPasswordByEmail(@RequestBody UserResetPasswordByEmailRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userService.resetPasswordByEmail(
                request.getEmail(), request.getCode(), request.getNewPassword(), request.getCheckPassword()));
    }

    /**
     * 发送「绑定邮箱」验证码（需登录）
     */
    @PostMapping("/bind/email/code")
    public BaseResponse<Boolean> sendBindEmailCode(@RequestBody UserEmailCodeRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userService.sendBindEmailCode(request.getEmail()));
    }

    /**
     * 绑定邮箱（需登录：校验验证码后将邮箱写入当前账号）
     */
    @PostMapping("/bind/email")
    public BaseResponse<Boolean> bindEmail(@RequestBody UserBindEmailRequest request, HttpServletRequest servletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(servletRequest);
        boolean result = userService.bindEmail(loginUser, request.getEmail(), request.getCode());
        return ResultUtils.success(result);
    }

    /**
     * 管理员调整用户积分
     */
    @PostMapping("/adjust/balance")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> adjustBalance(@RequestBody UserAdjustBalanceRequest request) {
        ThrowUtils.throwIf(request == null || request.getUserId() == null
                || request.getAmount() == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.adjustBalance(
                request.getUserId(),
                request.getAmount(),
                request.getReason()
        );
        return ResultUtils.success(result);
    }

    /**
     * 管理员修改用户会员等级
     */
    @PostMapping("/update/membership")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateMembership(@RequestBody UserUpdateMembershipRequest request) {
        ThrowUtils.throwIf(request == null || request.getUserId() == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.updateMembership(
                request.getUserId(),
                request.getMembershipTier(),
                request.getMembershipExpireTime()
        );
        return ResultUtils.success(result);
    }

    /**
     * 管理员批量删除用户
     */
    @PostMapping("/batch/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> batchDelete(@RequestBody UserBatchDeleteRequest request) {
        ThrowUtils.throwIf(request == null || request.getIds() == null
                || request.getIds().isEmpty(), ErrorCode.PARAMS_ERROR);
        int count = userService.batchDelete(request.getIds());
        return ResultUtils.success(count);
    }
}

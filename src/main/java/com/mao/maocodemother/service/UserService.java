package com.mao.maocodemother.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.mao.maocodemother.model.dto.user.UserQueryRequest;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.vo.LoginUserVO;
import com.mao.maocodemother.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户 服务层。
 *
 * @author <a href="https://github.com/liyupi">程序员mao</a>
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword, String phone, String code);

    /**
     * 获取脱敏的已登录用户信息
     *
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 账号密码登录（强制手机号验证码二次验证）
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param phone        手机号（已绑定则须与绑定号一致；未绑定则凭此号码+验证码自动绑定）
     * @param code         短信验证码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, String phone, String code, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取当前登录用户（允许未登录）。
     * 未登录或会话失效时返回 null，不抛异常；用于需要区分「访客 / 已登录用户」的公开接口（如应用详情的私密校验）。
     *
     * @param request
     * @return 登录用户，未登录则为 null
     */
    User getLoginUserPermitNull(HttpServletRequest request);

    /**
     * 获取脱敏后的用户信息
     *
     * @param user 用户信息
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏后的用户信息（分页）
     *
     * @param userList 用户列表
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 用户注销
     *
     * @param request
     * @return 退出登录是否成功
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 根据查询条件构造数据查询参数
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 加密
     *
     * @param userPassword 用户密码
     * @return 加密后的用户密码
     */
    String getEncryptPassword(String userPassword);

    /**
     * 发送短信验证码
     *
     * @param phone 手机号
     * @return 是否发送成功
     */
    boolean sendSmsCode(String phone, String captchaKey, String captcha);

    /**
     * 短信验证码登录（不存在则自动注册）
     *
     * @param phone   手机号
     * @param code    短信验证码
     * @param request 请求对象
     * @return 脱敏后的用户登录信息
     */
    LoginUserVO userLoginBySms(String phone, String code, HttpServletRequest request);

    /**
     * 获取微信扫码授权页 URL
     *
     * @return 微信授权页 URL
     */
    String getWechatLoginUrl();

    /**
     * 微信扫码登录（不存在则自动注册）
     *
     * @param code    微信回调 code
     * @param state   随机状态参数
     * @param request 请求对象
     * @return 脱敏后的用户登录信息
     */
    LoginUserVO userLoginByWechat(String code, String state, HttpServletRequest request);

    /**
     * 获取 QQ 互联授权跳转地址
     *
     * @return 授权页 URL
     */
    String getQQLoginUrl();

    /**
     * QQ 互联扫码登录（不存在则自动注册）
     *
     * @param code    QQ 回调 code
     * @param state   随机状态参数
     * @param request 请求对象
     * @return 脱敏后的用户登录信息
     */
    LoginUserVO userLoginByQQ(String code, String state, HttpServletRequest request);

    /**
     * 订单支付成功后发放权益（会员升级 / 积分余额累加）。
     * 幂等：由调用方（订单服务）保证每笔订单只调用一次。
     *
     * @param userId      用户 id
     * @param productType 商品类型：MEMBERSHIP / SECONDS / CARD
     * @param productCode 商品编码
     * @param quantity    购买数量
     */
    void fulfillOrder(Long userId, String productType, String productCode, int quantity);

    /**
     * 管理员重置用户密码
     *
     * @param userId      用户 id
     * @param newPassword 新密码
     * @return 是否成功
     */
    boolean resetPassword(Long userId, String newPassword);

    /**
     * 用户修改自己的密码（需先校验原密码）
     *
     * @param loginUser     当前登录用户（从 session 取，不信任前端传的 userId）
     * @param oldPassword   原密码
     * @param newPassword   新密码
     * @param checkPassword 确认新密码
     * @return 是否成功
     */
    boolean updateMyPassword(User loginUser, String oldPassword, String newPassword, String checkPassword);

    /**
     * 发送「找回密码」短信验证码（公开接口，未登录也可调用）
     *
     * @param phone 手机号
     * @return 是否发送成功
     */
    boolean sendResetSmsCode(String phone, String captchaKey, String captcha);

    /**
     * 手机号 + 验证码找回密码（公开接口，未登录也可调用）
     *
     * @param phone        手机号
     * @param code         短信验证码
     * @param newPassword  新密码
     * @param checkPassword 确认新密码
     * @return 是否成功
     */
    boolean resetPasswordByPhone(String phone, String code, String newPassword, String checkPassword);

    /**
     * 发送「找回密码」邮箱验证码（公开接口，未登录也可调用）
     *
     * @param email 邮箱
     * @return 是否发送成功
     */
    boolean sendResetEmailCode(String email, String captchaKey, String captcha);

    /**
     * 邮箱 + 验证码找回密码（公开接口，未登录也可调用）
     *
     * @param email        邮箱
     * @param code         邮箱验证码
     * @param newPassword  新密码
     * @param checkPassword 确认新密码
     * @return 是否成功
     */
    boolean resetPasswordByEmail(String email, String code, String newPassword, String checkPassword);

    /**
     * 发送「绑定邮箱」验证码（登录态下，绑定当前账号的找回邮箱）
     *
     * @param email 要绑定的邮箱
     * @return 是否发送成功
     */
    boolean sendBindEmailCode(String email);

    /**
     * 绑定邮箱（登录态下，校验验证码后将邮箱写入当前账号）
     *
     * @param loginUser 当前登录用户
     * @param email     邮箱
     * @param code      邮箱验证码
     * @return 是否成功
     */
    boolean bindEmail(User loginUser, String email, String code);

    /**
     * 管理员调整用户积分余额
     *
     * @param userId 用户 id
     * @param amount 调整数额（正增负减）
     * @param reason 调整原因
     * @return 是否成功
     */
    boolean adjustBalance(Long userId, long amount, String reason);

    /**
     * 管理员修改用户会员等级
     *
     * @param userId               用户 id
     * @param membershipTier       会员等级
     * @param membershipExpireTime 会员到期时间
     * @return 是否成功
     */
    boolean updateMembership(Long userId, String membershipTier, java.time.LocalDateTime membershipExpireTime);

    /**
     * 管理员批量删除用户
     *
     * @param ids 用户 id 列表
     * @return 删除数量
     */
    int batchDelete(List<Long> ids);
}

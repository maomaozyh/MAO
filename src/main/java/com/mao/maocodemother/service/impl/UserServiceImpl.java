package com.mao.maocodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mao.maocodemother.config.SmsProperties;
import com.mao.maocodemother.config.MailProperties;
import com.mao.maocodemother.config.QQOauthProperties;
import com.mao.maocodemother.config.WechatOauthProperties;
import com.mao.maocodemother.exception.BusinessException;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.exception.ThrowUtils;
import com.mao.maocodemother.model.dto.user.UserQueryRequest;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.entity.SecondsRecord;
import com.mao.maocodemother.model.enums.SecondsBizTypeEnum;
import com.mao.maocodemother.mapper.UserMapper;
import com.mao.maocodemother.mapper.SecondsRecordMapper;
import com.mao.maocodemother.mapper.OrderMapper;
import com.mao.maocodemother.model.enums.UserRoleEnum;
import com.mao.maocodemother.model.vo.LoginUserVO;
import com.mao.maocodemother.model.vo.UserVO;
import com.mao.maocodemother.service.UserService;
import com.mao.maocodemother.service.MailService;
import com.mao.maocodemother.utils.RedisUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.DigestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.mao.maocodemother.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户 服务层实现。
 *
 * @author <a href="https://github.com/liyupi">程序员mao</a>
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     * 手机号格式
     */
    private static final Pattern PHONE_REGEX = Pattern.compile("^1[3-9]\\d{9}$");

    /**
     * 日志脱敏：手机号中间四位打码（138****8888），避免明文手机号进入日志
     */
    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 短信验证码频控 key 前缀
     */
    private static final String SMS_RATE_KEY_PREFIX = "sms:rate:";

    /**
     * 短信验证码 key 前缀
     */
    private static final String SMS_CODE_KEY_PREFIX = "sms:code:";

    /**
     * 短信验证码频控过期时间（秒）
     */
    private static final long SMS_RATE_TTL_SECONDS = 60;

    /**
     * 短信验证码过期时间（分钟）
     */
    private static final long SMS_CODE_TTL_MINUTES = 5;

    /**
     * 短信验证码错误次数上限（达到后临时封禁到验证码过期，防爆破）
     */
    private static final int SMS_TRY_MAX = 5;

    /**
     * 短信验证码错误计数 key 前缀
     */
    private static final String SMS_TRY_KEY_PREFIX = "sms:try:";

    /**
     * 同一手机号每日发送上限（防短信轰炸 / 资费滥用）
     */
    private static final int SMS_DAILY_MAX = 10;

    /**
     * 同一手机号每日发送计数 key 前缀
     */
    private static final String SMS_DAILY_KEY_PREFIX = "sms:daily:";

    /**
     * 全局每日短信发送上限（防攻击者轮换不同手机号轰炸导致资费失控）。
     * 按自然日清零；即使攻击者用成千上万个不同号码轮刷，总短信量也被锁死在此值，
     * 最坏资费 = SMS_GLOBAL_DAILY_MAX × 单价（约 0.045 元/条）。可按业务量调整。
     */
    private static final int SMS_GLOBAL_DAILY_MAX = 200;

    /**
     * 全局每日短信发送计数 key 前缀（后缀为当天日期，保证自然日维度）
     */
    private static final String SMS_GLOBAL_DAILY_KEY_PREFIX = "sms:global:daily:";

    /**
     * 全局每日邮件发送上限（对称防护，防止邮件验证码被轮换邮箱轰炸）
     */
    private static final int EMAIL_GLOBAL_DAILY_MAX = 500;

    /**
     * 全局每日邮件发送计数 key 前缀（后缀为当天日期）
     */
    private static final String EMAIL_GLOBAL_DAILY_KEY_PREFIX = "email:global:daily:";

    /**
     * 图形验证码 key 前缀（验证码文本存 Redis，5 分钟过期，一次性使用）
     */
    private static final String CAPTCHA_CODE_PREFIX = "captcha:code:";

    /**
     * 图形验证码有效期（分钟）
     */
    private static final long CAPTCHA_TTL_MINUTES = 5;

    /**
     * 登录态缓存 key 前缀（分布式会话，配合 Spring Session redis 共享登录态）
     */
    private static final String LOGIN_USER_KEY_PREFIX = "login:user:";

    // ===================== 找回密码：独立的验证码 key 前缀（与登录验证码隔离，防止登录码被复用重置密码） =====================

    private static final String RESET_PHONE_RATE_PREFIX = "reset:phone:rate:";
    private static final String RESET_PHONE_CODE_PREFIX = "reset:phone:code:";
    private static final String RESET_PHONE_TRY_PREFIX = "reset:phone:try:";
    private static final String RESET_PHONE_DAILY_PREFIX = "reset:phone:daily:";

    private static final String RESET_EMAIL_RATE_PREFIX = "reset:email:rate:";
    private static final String RESET_EMAIL_CODE_PREFIX = "reset:email:code:";
    private static final String RESET_EMAIL_TRY_PREFIX = "reset:email:try:";
    private static final String RESET_EMAIL_DAILY_PREFIX = "reset:email:daily:";

    // 绑定邮箱：独立的验证码 key 前缀（与找回密码隔离）
    private static final String BIND_EMAIL_RATE_PREFIX = "bind:email:rate:";
    private static final String BIND_EMAIL_CODE_PREFIX = "bind:email:code:";
    private static final String BIND_EMAIL_TRY_PREFIX = "bind:email:try:";
    private static final String BIND_EMAIL_DAILY_PREFIX = "bind:email:daily:";

    /**
     * 邮箱格式
     */
    private static final Pattern EMAIL_REGEX = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    /**
     * 登录态缓存过期时间（秒），与 session 超时一致（30 天）
     */
    private static final long LOGIN_USER_TTL_SECONDS = 2592000L;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private SecondsRecordMapper secondsRecordMapper;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private com.mao.maocodemother.service.SysConfigService sysConfigService;

    @Resource
    private SmsProperties smsProperties;

    @Resource
    private MailProperties mailProperties;

    @Resource
    private MailService mailService;

    @Resource
    private WechatOauthProperties wechatOauthProperties;

    @Resource
    private QQOauthProperties qqOauthProperties;

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private com.mao.maocodemother.service.LoginAttemptService loginAttemptService;

    /**
     * 旧版 MD5 盐值（仅用于兼容迁移，新密码一律使用 BCrypt）
     */
    private static final String LEGACY_MD5_SALT = "yupi";

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String phone, String code) {
        // 1. 校验参数
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 1.5 注册需手机号验证码校验（与登录共用频控 + 校验 + 一次性删除逻辑）
        if (StrUtil.isBlank(phone) || !PHONE_REGEX.matcher(phone).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请输入正确的手机号");
        }
        if (StrUtil.isBlank(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请输入手机号收到的短信验证码");
        }
        // 校验手机号是否已被其他账号绑定（短信登录可能已用该号自动建号）
        long phoneCount = this.mapper.selectCountByQuery(QueryWrapper.create().eq("userPhone", phone));
        if (phoneCount > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该手机号已被注册");
        }
        // 校验短信验证码
        verifySmsCode(phone, code);
        // 2. 查询用户是否已存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 3. 加密密码
        String encryptPassword = getEncryptPassword(userPassword);
        // 4. 创建用户，插入数据库（注册赠送积分，数量读 sys_config 的 user.giftSeconds）
        long giftSeconds = parseLongSafe(sysConfigService.getConfigValue("user.giftSeconds", "100"), 100L);
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("无名");
        user.setUserPhone(phone);
        user.setUserRole(UserRoleEnum.USER.getValue());
        user.setSecondsBalance(0L);
        user.setGiftSecondsBalance(giftSeconds);
        user.setMembershipTier("FREE");
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "注册失败，数据库错误");
        }
        // 记录赠送流水（insert 带 ignoreNulls=true，避免 null 的 createTime 显式插入报错）
        if (giftSeconds > 0) {
            secondsRecordMapper.insert(SecondsRecord.builder()
                    .userId(user.getId())
                    .amount(giftSeconds)
                    .balanceAfter(0L)
                    .giftAfter(giftSeconds)
                    .bizType(SecondsBizTypeEnum.GIFT.getValue())
                    .bizDesc("注册赠送")
                    .status(0)
                    .build(), true);
        }
        return user.getId();
    }

    /**
     * 解析 long，失败返回默认值
     */
    private long parseLongSafe(String raw, long defaultValue) {
        try {
            return Long.parseLong(raw == null ? "" : raw.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        loginUserVO.setUserPhone(user.getUserPhone());
        // 首购 8 折判断：是否已有「已支付」的会员订单 / 积分订单
        long paidMembershipCount = orderMapper.selectCountByQuery(QueryWrapper.create()
                .eq("userId", user.getId())
                .eq("productType", "MEMBERSHIP")
                .eq("status", "PAID"));
        loginUserVO.setHasPaidMembership(paidMembershipCount > 0);
        long paidSecondsCount = orderMapper.selectCountByQuery(QueryWrapper.create()
                .eq("userId", user.getId())
                .eq("productType", "SECONDS")
                .eq("status", "PAID"));
        loginUserVO.setHasPaidPoints(paidSecondsCount > 0);
        return loginUserVO;
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, String phone, String code, HttpServletRequest request) {
        // 1. 校验参数
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度过短");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度过短");
        }
        // 1.5 防暴力破解：检查是否被锁定
        String clientIp = getClientIp(request);
        if (loginAttemptService.isLocked(userAccount, clientIp)) {
            long remaining = loginAttemptService.getRemainingLockSeconds(userAccount, clientIp);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    String.format("登录失败次数过多，账号已临时锁定，请 %d 分钟后再试", (remaining + 59) / 60));
        }
        // 2. 查询用户是否存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        if (user == null) {
            // 记录失败（防止用户名枚举爆破，同样计数）
            loginAttemptService.recordFailure(userAccount, clientIp);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 验证密码（支持 BCrypt + 旧 MD5 兼容迁移）
        boolean passwordValid = verifyPasswordAndMigrate(user, userPassword);
        if (!passwordValid) {
            loginAttemptService.recordFailure(userAccount, clientIp);
            int failCount = loginAttemptService.getFailureCount(userAccount, clientIp);
            int remaining = 5 - failCount;
            String hint = remaining > 0
                    ? String.format("，还剩 %d 次机会", remaining)
                    : "，账号已临时锁定";
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误" + hint);
        }
        // 登录成功，清除失败计数
        loginAttemptService.recordSuccess(userAccount, clientIp);
        /*
         * 3.5 强制手机号验证码二次验证（已临时关闭 / 注释掉，代码保留以便按需重新开启）
         * [关闭原因] 产品要求先关闭「登录必须绑定手机号」的强制二次验证。
         * 已绑定手机号：校验绑定号，且密码登录传入的手机号须与绑定号一致
         * 未绑定手机号：用本次请求的手机号 + 验证码，校验通过后自动绑定（避免锁定老账号）
        if (StrUtil.isBlank(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请输入手机号收到的短信验证码");
        }
        String verifyPhone;
        boolean needBindPhone = false;
        if (StrUtil.isNotBlank(user.getUserPhone())) {
            verifyPhone = user.getUserPhone();
            if (StrUtil.isNotBlank(phone) && !phone.equals(user.getUserPhone())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "该账号已绑定手机号，请使用绑定手机号获取验证码");
            }
        } else {
            // 未绑定：要求本次提供手机号并完成绑定
            if (StrUtil.isBlank(phone) || !PHONE_REGEX.matcher(phone).matches()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "该账号尚未绑定手机号，请先填写手机号并获取验证码以完成绑定");
            }
            User phoneOwner = this.mapper.selectOneByQuery(QueryWrapper.create().eq("userPhone", phone));
            if (phoneOwner != null && !phoneOwner.getId().equals(user.getId())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "该手机号已绑定其他账号，无法用于登录");
            }
            verifyPhone = phone;
            needBindPhone = true;
        }
        // 校验验证码（与短信登录共用频控 + 校验 + 一次性删除逻辑）
        verifySmsCode(verifyPhone, code);
        if (needBindPhone) {
            user.setUserPhone(phone);
            this.updateById(user);
            log.info("[登录绑定手机号] 账号 {} 绑定手机号 {}", user.getUserAccount(), maskPhone(phone));
        }
         */
        // 4. 如果用户存在，记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // 写入 Redis 登录态缓存（与 session 一致 TTL），供多实例共享 & 减少 DB 查询
        redisUtil.set(LOGIN_USER_KEY_PREFIX + user.getId(), user, LOGIN_USER_TTL_SECONDS);
        // 5. 返回脱敏的用户信息
        return this.getLoginUserVO(user);
    }

    /**
     * 验证密码并自动迁移
     * - BCrypt 格式（$2a$ / $2b$ / $2y$ 开头）：直接 BCrypt 验证
     * - 旧 MD5 格式：MD5 验证通过后自动升级为 BCrypt 并存库
     *
     * @param user         用户
     * @param rawPassword  明文密码
     * @return 是否验证通过
     */
    private boolean verifyPasswordAndMigrate(User user, String rawPassword) {
        String storedPassword = user.getUserPassword();
        if (StrUtil.isBlank(storedPassword)) {
            return false;
        }
        // 判断是否为 BCrypt 格式
        if (isBcryptHash(storedPassword)) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        // 旧版 MD5 验证
        String legacyMd5 = legacyMd5Encrypt(rawPassword);
        if (legacyMd5.equals(storedPassword)) {
            // 验证通过，自动升级为 BCrypt
            String newBcryptHash = passwordEncoder.encode(rawPassword);
            user.setUserPassword(newBcryptHash);
            // MyBatis-Flex 的 BaseMapper 没有 update(entity, QueryWrapper) 重载，
            // 这里更新目标就是主键，直接用 ServiceImpl 的 updateById（忽略 null 字段）
            this.updateById(user);
            log.info("[密码迁移] 用户 {} 的密码已从 MD5 升级为 BCrypt", user.getUserAccount());
            return true;
        }
        return false;
    }

    /**
     * 判断是否为 BCrypt 哈希格式
     */
    private boolean isBcryptHash(String hash) {
        return hash != null && (hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"));
    }

    /**
     * 旧版 MD5 加密（仅用于兼容迁移）
     */
    private String legacyMd5Encrypt(String rawPassword) {
        return DigestUtils.md5DigestAsHex((rawPassword + LEGACY_MD5_SALT).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 获取客户端真实 IP（支持反向代理）
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断用户是否登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 优先从 Redis 登录态缓存读取，减少数据库查询；未命中再回源数据库并回填缓存
        long userId = currentUser.getId();
        User cachedUser = redisUtil.get(LOGIN_USER_KEY_PREFIX + userId, User.class);
        if (cachedUser != null) {
            return cachedUser;
        }
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        redisUtil.set(LOGIN_USER_KEY_PREFIX + userId, currentUser, LOGIN_USER_TTL_SECONDS);
        return currentUser;
    }

    @Override
    public User getLoginUserPermitNull(HttpServletRequest request) {
        // 未登录或会话失效直接返回 null，不抛异常
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        // 优先从 Redis 登录态缓存读取；未命中再回源数据库并回填缓存
        long userId = currentUser.getId();
        User cachedUser = redisUtil.get(LOGIN_USER_KEY_PREFIX + userId, User.class);
        if (cachedUser != null) {
            return cachedUser;
        }
        currentUser = this.getById(userId);
        if (currentUser == null) {
            return null;
        }
        redisUtil.set(LOGIN_USER_KEY_PREFIX + userId, currentUser, LOGIN_USER_TTL_SECONDS);
        return currentUser;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 先判断用户是否登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户未登录");
        }
        // 移除登录态
        User user = (User) userObj;
        redisUtil.delete(LOGIN_USER_KEY_PREFIX + user.getId());
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id) // where id = ${id}
                .eq("userRole", userRole) // and userRole = ${userRole}
                .like("userAccount", userAccount)
                .like("userName", userName)
                .like("userProfile", userProfile)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    @Override
    public String getEncryptPassword(String userPassword) {
        // 使用 BCrypt 加密（自带随机盐，安全强度远高于 MD5）
        return passwordEncoder.encode(userPassword);
    }

    @Override
    public boolean sendSmsCode(String phone, String captchaKey, String captcha) {
        // 0. 人机校验：发码前必须答对图形验证码，挡自动化轰炸
        verifyCaptcha(captchaKey, captcha);
        // 1. 校验手机号格式
        if (StrUtil.isBlank(phone) || !PHONE_REGEX.matcher(phone).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号格式不正确");
        }
        // 2. 频控：60 秒内同一手机号只能发送一次
        Boolean rateSet = stringRedisTemplate.opsForValue()
                .setIfAbsent(SMS_RATE_KEY_PREFIX + phone, "1", Duration.ofSeconds(SMS_RATE_TTL_SECONDS));
        if (!Boolean.TRUE.equals(rateSet)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "发送太频繁，请稍后再试");
        }
        // 3. 每日发送上限，防短信轰炸 / 资费滥用
        String dailyKey = SMS_DAILY_KEY_PREFIX + phone;
        Long dailyCount = stringRedisTemplate.opsForValue().increment(dailyKey);
        if (dailyCount != null && dailyCount == 1) {
            stringRedisTemplate.expire(dailyKey, Duration.ofDays(1));
        }
        if (dailyCount != null && dailyCount > SMS_DAILY_MAX) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "今日发送次数已达上限，请明日再试");
        }
        // 3.1 全局每日上限（防攻击者轮换不同手机号轰炸，资费失控）
        String globalKey = SMS_GLOBAL_DAILY_KEY_PREFIX + LocalDate.now();
        Long globalCount = stringRedisTemplate.opsForValue().increment(globalKey);
        if (globalCount != null && globalCount == 1) {
            stringRedisTemplate.expire(globalKey, Duration.ofDays(1));
        }
        if (globalCount != null && globalCount > SMS_GLOBAL_DAILY_MAX) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "系统繁忙，请稍后再试");
        }
        // 4. 生成 6 位随机验证码并缓存 5 分钟
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        stringRedisTemplate.opsForValue().set(SMS_CODE_KEY_PREFIX + phone, code, Duration.ofMinutes(SMS_CODE_TTL_MINUTES));
        // 5. 发送验证码（真实短信待接入；验证码绝不打进 INFO 日志，避免泄露）
        if (smsProperties.isEnabled()) {
            // TODO(接真实短信): 用 dysmsapi SDK 真实发送——accessKeyId/accessKeySecret 取自 smsProperties.getAccessKeyId()/getAccessKeySecret()，
            //   签名 smsProperties.getSignName()，模板 smsProperties.getTemplateCode()，endpoint=smsProperties.getEndpoint()，手机号=phone，验证码=code。
            log.info("[SMS] 已向 {} 发送验证码（真实短信服务启用，待接入）", maskPhone(phone));
        } else {
            // mock 模式：仅 DEBUG 级别打印验证码，供本地联调，生产不会输出
            log.debug("[SMS][mock] 手机号 {} 验证码 {}", maskPhone(phone), code);
        }
        return true;
    }

    @Override
    public LoginUserVO userLoginBySms(String phone, String code, HttpServletRequest request) {
        // 1. 校验参数 + 验证码（频控 / 校验 / 一次性删除，与账号密码登录共用）
        verifySmsCode(phone, code);
        // 4. 查询用户：优先按 userPhone，兼容「用户名恰为手机号」的历史账号
        User user = this.mapper.selectOneByQuery(QueryWrapper.create().eq("userPhone", phone));
        if (user == null) {
            user = this.mapper.selectOneByQuery(QueryWrapper.create().eq("userAccount", phone));
        }
        // 5. 用户不存在则自动注册；存在但 userPhone 为空则回补，避免重复建号
        if (user == null) {
            user = new User();
            user.setUserAccount(phone);
            user.setUserPassword(getEncryptPassword(generateRandomPassword()));
            user.setUserName("用户" + phone.substring(phone.length() - 4));
            user.setUserPhone(phone);
            user.setUserRole(UserRoleEnum.USER.getValue());
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "注册失败，数据库错误");
            }
        } else if (user.getUserPhone() == null) {
            user.setUserPhone(phone);
            this.updateById(user);
        }
        // 6. 记录登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // 写入 Redis 登录态缓存（与 session 一致 TTL）
        redisUtil.set(LOGIN_USER_KEY_PREFIX + user.getId(), user, LOGIN_USER_TTL_SECONDS);
        // 7. 返回脱敏的用户信息
        return this.getLoginUserVO(user);
    }

    /**
     * 校验短信验证码（账号密码登录 / 短信登录共用的核心校验）
     * - 参数非空 + 手机号格式
     * - 同一手机号验证码错误次数过多则临时封禁到验证码过期
     * - 验证码存在性 + 正确性（错误记一次，达上限临时封禁）
     * - 校验通过：删除验证码与错误计数，防止重放 / 爆破
     *
     * @param phone 手机号
     * @param code  验证码
     */
    private void verifySmsCode(String phone, String code) {
        if (StrUtil.hasBlank(phone, code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请输入手机号与验证码");
        }
        if (!PHONE_REGEX.matcher(phone).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号格式不正确");
        }
        // 防爆破：同一手机号验证码错误次数过多则临时封禁到验证码过期
        String tryKey = SMS_TRY_KEY_PREFIX + phone;
        String tryCount = stringRedisTemplate.opsForValue().get(tryKey);
        if (tryCount != null && Integer.parseInt(tryCount) >= SMS_TRY_MAX) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "验证码错误次数过多，请重新获取");
        }
        // 校验验证码
        String codeKey = SMS_CODE_KEY_PREFIX + phone;
        String savedCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (StrUtil.isBlank(savedCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码已过期，请重新获取");
        }
        if (!savedCode.equals(code)) {
            // 记录错误次数（与验证码同 TTL），达到上限后临时封禁
            Long n = stringRedisTemplate.opsForValue().increment(tryKey);
            if (n != null && n == 1) {
                stringRedisTemplate.expire(tryKey, Duration.ofMinutes(SMS_CODE_TTL_MINUTES));
            }
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
        }
        // 校验通过：删除验证码与错误计数，防止重放 / 爆破
        stringRedisTemplate.delete(codeKey);
        stringRedisTemplate.delete(tryKey);
    }

    @Override
    public String getWechatLoginUrl() {
        if (StrUtil.isBlank(wechatOauthProperties.getAppId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "微信登录未配置，请联系管理员");
        }
        String state = UUID.randomUUID().toString().replace("-", "");
        String redirectUri = URLEncoder.encode(wechatOauthProperties.getRedirectUri(), StandardCharsets.UTF_8);
        return String.format(
                "https://open.weixin.qq.com/connect/qrconnect?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_login&state=%s",
                wechatOauthProperties.getAppId(), redirectUri, state);
    }

    @Override
    public LoginUserVO userLoginByWechat(String code, String state, HttpServletRequest request) {
        // 1. 校验参数
        if (StrUtil.isBlank(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        String appId = wechatOauthProperties.getAppId();
        String appSecret = wechatOauthProperties.getAppSecret();
        if (StrUtil.isBlank(appId) || StrUtil.isBlank(appSecret)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "微信登录未配置");
        }
        // 2. 用 code 换取 access_token 和 openid
        String tokenUrl = String.format(
                "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code",
                appId, appSecret, code);
        JSONObject tokenJson = JSONUtil.parseObj(restTemplate.getForObject(tokenUrl, String.class));
        Integer tokenErrCode = tokenJson.getInt("errcode");
        if (tokenErrCode != null && tokenErrCode != 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取微信 access_token 失败：" + tokenJson.getStr("errmsg"));
        }
        String accessToken = tokenJson.getStr("access_token");
        String openid = tokenJson.getStr("openid");
        if (StrUtil.hasBlank(accessToken, openid)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取微信 access_token 失败");
        }
        // 3. 拉取微信用户信息
        String userInfoUrl = String.format(
                "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s", accessToken, openid);
        JSONObject userInfoJson = JSONUtil.parseObj(restTemplate.getForObject(userInfoUrl, String.class));
        Integer userInfoErrCode = userInfoJson.getInt("errcode");
        if (userInfoErrCode != null && userInfoErrCode != 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取微信用户信息失败：" + userInfoJson.getStr("errmsg"));
        }
        String nickname = userInfoJson.getStr("nickname");
        String avatar = userInfoJson.getStr("headimgurl");
        // 4. 按 wechatOpenId 查询用户
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("wechatOpenId", openid);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        // 5. 用户不存在则自动注册
        if (user == null) {
            user = new User();
            user.setUserAccount("wx_" + System.currentTimeMillis());
            user.setUserPassword(getEncryptPassword(generateRandomPassword()));
            user.setUserName(StrUtil.isBlank(nickname) ? "微信用户" : nickname);
            user.setUserAvatar(avatar);
            user.setWechatOpenId(openid);
            user.setUserRole(UserRoleEnum.USER.getValue());
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "注册失败，数据库错误");
            }
        }
        // 6. 记录登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // 写入 Redis 登录态缓存（与 session 一致 TTL）
        redisUtil.set(LOGIN_USER_KEY_PREFIX + user.getId(), user, LOGIN_USER_TTL_SECONDS);
        // 7. 返回脱敏的用户信息
        return this.getLoginUserVO(user);
    }

    @Override
    public String getQQLoginUrl() {
        if (StrUtil.isBlank(qqOauthProperties.getAppId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "QQ 登录未配置，请联系管理员");
        }
        String state = UUID.randomUUID().toString().replace("-", "");
        String redirectUri = URLEncoder.encode(qqOauthProperties.getRedirectUri(), StandardCharsets.UTF_8);
        return String.format(
                "https://graph.qq.com/oauth2.0/authorize?response_type=code&client_id=%s&redirect_uri=%s&state=%s&scope=get_user_info",
                qqOauthProperties.getAppId(), redirectUri, state);
    }

    @Override
    public LoginUserVO userLoginByQQ(String code, String state, HttpServletRequest request) {
        // 1. 校验参数
        if (StrUtil.isBlank(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        String appId = qqOauthProperties.getAppId();
        String appSecret = qqOauthProperties.getAppSecret();
        if (StrUtil.isBlank(appId) || StrUtil.isBlank(appSecret)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "QQ 登录未配置");
        }
        // 2. 用 code 换取 access_token（QQ 返回 key=value 表单格式，非 JSON）
        String tokenUrl = String.format(
                "https://graph.qq.com/oauth2.0/token?grant_type=authorization_code&client_id=%s&client_secret=%s&code=%s&redirect_uri=%s",
                appId, appSecret, code, qqOauthProperties.getRedirectUri());
        String tokenResp = restTemplate.getForObject(tokenUrl, String.class);
        if (StrUtil.isBlank(tokenResp) || tokenResp.contains("\"error\"")) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取 QQ access_token 失败");
        }
        String accessToken = null;
        for (String pair : tokenResp.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0 && "access_token".equals(pair.substring(0, idx))) {
                accessToken = pair.substring(idx + 1);
                break;
            }
        }
        if (StrUtil.isBlank(accessToken)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取 QQ access_token 失败");
        }
        // 3. 用 access_token 换取 openid（QQ 返回 callback({...}) 的 JSONP 包裹）
        String meUrl = String.format("https://graph.qq.com/oauth2.0/me?access_token=%s", accessToken);
        String meResp = restTemplate.getForObject(meUrl, String.class);
        String openid = parseQQOpenid(meResp);
        if (StrUtil.isBlank(openid)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取 QQ openid 失败");
        }
        // 4. 拉取 QQ 用户信息
        String userInfoUrl = String.format(
                "https://graph.qq.com/user/get_user_info?access_token=%s&oauth_consumer_key=%s&openid=%s",
                accessToken, appId, openid);
        JSONObject userInfoJson = JSONUtil.parseObj(restTemplate.getForObject(userInfoUrl, String.class));
        String nickname = userInfoJson.getStr("nickname");
        String avatar = userInfoJson.getStr("figureurl_qq_2");
        if (StrUtil.isBlank(avatar)) {
            avatar = userInfoJson.getStr("figureurl_qq_1");
        }
        if (StrUtil.isBlank(avatar)) {
            avatar = userInfoJson.getStr("figureurl");
        }
        // 5. 按 qqOpenId 查询用户
        User user = this.mapper.selectOneByQuery(QueryWrapper.create().eq("qqOpenId", openid));
        // 6. 用户不存在则自动注册
        if (user == null) {
            user = new User();
            user.setUserAccount("qq_" + openid);
            user.setUserPassword(getEncryptPassword(generateRandomPassword()));
            user.setUserName(StrUtil.isBlank(nickname) ? "QQ 用户" : nickname);
            user.setUserAvatar(avatar);
            user.setQqOpenId(openid);
            user.setUserRole(UserRoleEnum.USER.getValue());
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "注册失败，数据库错误");
            }
        }
        // 7. 记录登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // 写入 Redis 登录态缓存（与 session 一致 TTL）
        redisUtil.set(LOGIN_USER_KEY_PREFIX + user.getId(), user, LOGIN_USER_TTL_SECONDS);
        // 8. 返回脱敏的用户信息
        return this.getLoginUserVO(user);
    }

    /**
     * 解析 QQ openid 回调（格式为 callback( {"client_id":"...","openid":"..."} );）
     *
     * @param meResp QQ openid 接口原始返回
     * @return openid，解析失败返回 null
     */
    private String parseQQOpenid(String meResp) {
        if (StrUtil.isBlank(meResp)) {
            return null;
        }
        int start = meResp.indexOf('{');
        int end = meResp.lastIndexOf('}');
        if (start < 0 || end < 0 || end <= start) {
            return null;
        }
        JSONObject meJson = JSONUtil.parseObj(meResp.substring(start, end + 1));
        return meJson.getStr("openid");
    }

    /**
     * 生成随机密码（自动注册时使用）
     *
     * @return 随机密码
     */
    private String generateRandomPassword() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void fulfillOrder(Long userId, String productType, String productCode, int quantity) {
        if (userId == null) {
            return;
        }
        User user = this.getById(userId);
        if (user == null) {
            return;
        }
        long beforePoints = 0L;
        boolean isMembership = "MEMBERSHIP".equals(productType)
                || ("CARD".equals(productType) && productCode != null && productCode.startsWith("VIP"));
        boolean isPoints = "SECONDS".equals(productType)
                || ("CARD".equals(productType) && productCode != null && productCode.startsWith("POINT"));
        if (isMembership) {
            user.setMembershipTier(productCode);
            LocalDateTime base = user.getMembershipExpireTime() != null
                    && user.getMembershipExpireTime().isAfter(LocalDateTime.now())
                    ? user.getMembershipExpireTime()
                    : LocalDateTime.now();
            long days = productCode.contains("MONTH") ? 30 : 365;
            user.setMembershipExpireTime(base.plusDays(days));
        } else if (isPoints) {
            beforePoints = user.getSecondsBalance() == null ? 0L : user.getSecondsBalance();
            long addPoints = parsePointsFromCode(productCode) * Math.max(quantity, 1);
            user.setSecondsBalance(beforePoints + addPoints);
        }
        this.updateById(user);
        // 清除登录态缓存，使 getLoginUser 立即返回最新余额/会员等级/到期时间（支付后点数同步）
        redisUtil.delete(LOGIN_USER_KEY_PREFIX + userId);
        // 记录积分流水（仅积分/卡券类入账；此处直接用 Mapper 避免与 SecondsService 循环依赖）
        long creditedPoints = user.getSecondsBalance() == null ? 0L : user.getSecondsBalance();
        if (isPoints) {
            // 注意：直连 BaseMapper.insert 必须把 null 字段忽略（insert(entity, true)），
            // 否则 createTime 等 null 会被显式插入，MySQL strict 下报 Column 'createTime' cannot be null
            secondsRecordMapper.insert(SecondsRecord.builder()
                    .userId(userId)
                    .amount(creditedPoints - beforePoints)
                    .balanceAfter(creditedPoints)
                    .giftAfter(user.getGiftSecondsBalance() == null ? 0L : user.getGiftSecondsBalance())
                    .bizType(SecondsBizTypeEnum.PURCHASE.getValue())
                    .bizDesc("购买：" + productCode + " × " + Math.max(quantity, 1))
                    .status(0)
                    .build(), true);
        }
    }

    /**
     * 从商品编码中解析积分数量（SEC_1000 / POINT_1000 -> 1000）。解析失败返回 0。
     */
    private long parsePointsFromCode(String productCode) {
        if (productCode == null) {
            return 0;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+)").matcher(productCode);
        long last = 0;
        while (matcher.find()) {
            last = Long.parseLong(matcher.group(1));
        }
        return last;
    }

    @Override
    public boolean resetPassword(Long userId, String newPassword) {
        ThrowUtils.throwIf(userId == null || StrUtil.isBlank(newPassword), ErrorCode.PARAMS_ERROR);
        User user = this.getById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        // 加密密码
        String encryptPassword = getEncryptPassword(newPassword);
        user.setUserPassword(encryptPassword);
        boolean result = this.updateById(user);
        // 清除登录态缓存
        redisUtil.delete(LOGIN_USER_KEY_PREFIX + userId);
        return result;
    }

    @Override
    public boolean updateMyPassword(User loginUser, String oldPassword, String newPassword, String checkPassword) {
        ThrowUtils.throwIf(loginUser == null || loginUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(StrUtil.hasBlank(oldPassword, newPassword, checkPassword),
                ErrorCode.PARAMS_ERROR, "请填写完整信息");
        ThrowUtils.throwIf(newPassword.length() < 8, ErrorCode.PARAMS_ERROR, "新密码至少 8 位");
        ThrowUtils.throwIf(!newPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "两次输入的新密码不一致");
        ThrowUtils.throwIf(newPassword.equals(oldPassword), ErrorCode.PARAMS_ERROR, "新密码不能与原密码相同");

        // 必须用库里最新的密码比对，不能用 session 里的对象（可能已过期）
        User user = this.getById(loginUser.getId());
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        // 校验原密码：兼容 BCrypt 与历史遗留 MD5 两种存储格式
        String storedPassword = user.getUserPassword();
        boolean oldMatched = StrUtil.isNotBlank(storedPassword)
                && (isBcryptHash(storedPassword)
                ? passwordEncoder.matches(oldPassword, storedPassword)
                : legacyMd5Encrypt(oldPassword).equals(storedPassword));
        ThrowUtils.throwIf(!oldMatched, ErrorCode.PARAMS_ERROR, "原密码不正确");

        user.setUserPassword(getEncryptPassword(newPassword));
        boolean result = this.updateById(user);
        // 清除登录态缓存，避免拿到旧的用户对象
        redisUtil.delete(LOGIN_USER_KEY_PREFIX + user.getId());
        return result;
    }

    // ===================== 找回密码：手机号 / 邮箱 两种渠道 =====================

    @Override
    public boolean sendResetSmsCode(String phone, String captchaKey, String captcha) {
        if (StrUtil.isBlank(phone) || !PHONE_REGEX.matcher(phone).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号格式不正确");
        }
        // 0. 人机校验：发码前必须答对图形验证码，挡自动化轰炸
        verifyCaptcha(captchaKey, captcha);
        return sendResetCode(RESET_PHONE_RATE_PREFIX, RESET_PHONE_CODE_PREFIX, RESET_PHONE_TRY_PREFIX,
                RESET_PHONE_DAILY_PREFIX, phone, maskPhone(phone), true, true);
    }

    @Override
    public boolean resetPasswordByPhone(String phone, String code, String newPassword, String checkPassword) {
        if (StrUtil.isBlank(phone) || !PHONE_REGEX.matcher(phone).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号格式不正确");
        }
        validateNewPassword(newPassword, checkPassword);
        // 校验找回验证码（频控 / 校验 / 一次性删除，与登录验证码隔离）
        verifyResetCode(RESET_PHONE_TRY_PREFIX, RESET_PHONE_CODE_PREFIX, phone, code);
        // 该手机号必须已注册（绑定了 userPhone）
        User user = this.mapper.selectOneByQuery(QueryWrapper.create().eq("userPhone", phone));
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "该手机号未注册");
        return doResetPassword(user, newPassword);
    }

    @Override
    public boolean sendResetEmailCode(String email, String captchaKey, String captcha) {
        if (StrUtil.isBlank(email) || !EMAIL_REGEX.matcher(email).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式不正确");
        }
        // 0. 人机校验：发码前必须答对图形验证码，挡自动化轰炸
        verifyCaptcha(captchaKey, captcha);
        return sendResetCode(RESET_EMAIL_RATE_PREFIX, RESET_EMAIL_CODE_PREFIX, RESET_EMAIL_TRY_PREFIX,
                RESET_EMAIL_DAILY_PREFIX, email, maskEmail(email), false, true);
    }

    @Override
    public boolean resetPasswordByEmail(String email, String code, String newPassword, String checkPassword) {
        if (StrUtil.isBlank(email) || !EMAIL_REGEX.matcher(email).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式不正确");
        }
        validateNewPassword(newPassword, checkPassword);
        // 校验找回验证码（与登录 / 短信验证码隔离）
        verifyResetCode(RESET_EMAIL_TRY_PREFIX, RESET_EMAIL_CODE_PREFIX, email, code);
        // 该邮箱必须已绑定到某个账号
        User user = this.mapper.selectOneByQuery(QueryWrapper.create().eq("userEmail", email));
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "该邮箱未注册");
        return doResetPassword(user, newPassword);
    }

    /**
     * 通用：发送「找回密码」验证码（频控 / 每日上限 / 生成 6 位码 / 落 Redis / 发送）。
     * 短信与邮箱共用同一套频控与错误封禁策略，仅发送通道（isSms）不同。
     *
     * @param ratePrefix  频控 key 前缀
     * @param codePrefix  验证码 key 前缀
     * @param tryPrefix   错误计数 key 前缀
     * @param dailyPrefix 每日上限 key 前缀
     * @param target      目标（手机号或邮箱）
     * @param masked      脱敏后的目标（用于日志）
     * @param isSms       true=短信通道，false=邮箱通道
     */
    // ===================== 绑定邮箱（登录态下设置找回邮箱） =====================

    @Override
    public boolean sendBindEmailCode(String email) {
        if (StrUtil.isBlank(email) || !EMAIL_REGEX.matcher(email).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式不正确");
        }
        // 邮箱必须未被任何其他账号占用（唯一索引约束）
        long count = this.mapper.selectCountByQuery(QueryWrapper.create().eq("userEmail", email));
        ThrowUtils.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "该邮箱已被其他账号绑定");
        return sendResetCode(BIND_EMAIL_RATE_PREFIX, BIND_EMAIL_CODE_PREFIX, BIND_EMAIL_TRY_PREFIX,
                BIND_EMAIL_DAILY_PREFIX, email, maskEmail(email), false, false);
    }

    @Override
    public boolean bindEmail(User loginUser, String email, String code) {
        ThrowUtils.throwIf(loginUser == null || loginUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR);
        if (StrUtil.isBlank(email) || !EMAIL_REGEX.matcher(email).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式不正确");
        }
        // 校验绑定验证码（与找回密码验证码隔离）
        verifyResetCode(BIND_EMAIL_TRY_PREFIX, BIND_EMAIL_CODE_PREFIX, email, code);
        // 再次校验唯一性（排除自己）：防止并发/中途被占用
        User exist = this.mapper.selectOneByQuery(QueryWrapper.create().eq("userEmail", email));
        ThrowUtils.throwIf(exist != null && !exist.getId().equals(loginUser.getId()),
                ErrorCode.PARAMS_ERROR, "该邮箱已被其他账号绑定");
        // 写入当前账号
        User user = this.getById(loginUser.getId());
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        user.setUserEmail(email);
        boolean result = this.updateById(user);
        // 清除登录态缓存，让前端刷新出绑定后的邮箱
        redisUtil.delete(LOGIN_USER_KEY_PREFIX + user.getId());
        return result;
    }

    /**
     * 判断目标是否已注册（用于发码前的成本防护，避免给未注册号发短信）。
     */
    private boolean isTargetRegistered(String target, boolean isSms) {
        String column = isSms ? "userPhone" : "userEmail";
        return this.mapper.selectCountByQuery(QueryWrapper.create().eq(column, target).eq("isDelete", 0)) > 0;
    }

    /**
     * 校验图形验证码（公开发码接口的人机校验，挡自动化轰炸）。
     * 验证码一次性使用：校验成功后立即删除，防止重放。
     */
    private void verifyCaptcha(String captchaKey, String captcha) {
        if (StrUtil.hasBlank(captchaKey, captcha)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请先完成图形验证码");
        }
        String cached = stringRedisTemplate.opsForValue().get(CAPTCHA_CODE_PREFIX + captchaKey);
        if (cached == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图形验证码已过期，请点击刷新");
        }
        if (!cached.equalsIgnoreCase(captcha.trim())) {
            stringRedisTemplate.delete(CAPTCHA_CODE_PREFIX + captchaKey);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图形验证码错误");
        }
        // 校验通过，一次性删除
        stringRedisTemplate.delete(CAPTCHA_CODE_PREFIX + captchaKey);
    }

    private boolean sendResetCode(String ratePrefix, String codePrefix, String tryPrefix, String dailyPrefix,
                                  String target, String masked, boolean isSms, boolean requireRegistered) {
        // 0. 成本防护：找回密码只对「已注册」目标发码；未注册目标直接返回成功（不落 Redis、不发真实短信）。
        //    既把攻击者拿随机号轮刷的短信成本降到 0，又保持响应一致（不泄露账号是否注册）。绑定邮箱场景传 false。
        if (requireRegistered && !isTargetRegistered(target, isSms)) {
            log.debug("[CODE] 目标 {} 未注册，跳过发送（防轰炸资费滥用）", masked);
            return true;
        }
        // 1. 频控：60 秒内同一目标只能发送一次
        Boolean rateSet = stringRedisTemplate.opsForValue()
                .setIfAbsent(ratePrefix + target, "1", Duration.ofSeconds(SMS_RATE_TTL_SECONDS));
        if (!Boolean.TRUE.equals(rateSet)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "发送太频繁，请稍后再试");
        }
        // 2. 每日发送上限，防轰炸 / 资费滥用
        String dailyKey = dailyPrefix + target;
        Long dailyCount = stringRedisTemplate.opsForValue().increment(dailyKey);
        if (dailyCount != null && dailyCount == 1) {
            stringRedisTemplate.expire(dailyKey, Duration.ofDays(1));
        }
        if (dailyCount != null && dailyCount > SMS_DAILY_MAX) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "今日发送次数已达上限，请明日再试");
        }
        // 2.1 全局每日上限（防攻击者轮换不同目标轰炸，资费失控）
        // 短信走 sms:global:daily，邮件走 email:global:daily，分别限流
        String globalPrefix = isSms ? SMS_GLOBAL_DAILY_KEY_PREFIX : EMAIL_GLOBAL_DAILY_KEY_PREFIX;
        int globalMax = isSms ? SMS_GLOBAL_DAILY_MAX : EMAIL_GLOBAL_DAILY_MAX;
        String globalKey = globalPrefix + LocalDate.now();
        Long globalCount = stringRedisTemplate.opsForValue().increment(globalKey);
        if (globalCount != null && globalCount == 1) {
            stringRedisTemplate.expire(globalKey, Duration.ofDays(1));
        }
        if (globalCount != null && globalCount > globalMax) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "系统繁忙，请稍后再试");
        }
        // 3. 生成 6 位随机验证码并缓存 5 分钟
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        stringRedisTemplate.opsForValue().set(codePrefix + target, code, Duration.ofMinutes(SMS_CODE_TTL_MINUTES));
        // 4. 发送验证码（真实服务待接入；验证码绝不打进 INFO 日志）
        if (isSms) {
            if (smsProperties.isEnabled()) {
                // TODO(接真实短信): 同上，复用 smsProperties 的 accessKeyId/signName/templateCode/endpoint 真实发送重置验证码
                log.info("[SMS] 已向 {} 发送重置密码验证码（真实短信服务启用，待接入）", masked);
            } else {
                log.debug("[SMS][mock] 手机号 {} 重置密码验证码 {}", masked, code);
            }
        } else {
            mailService.sendResetCodeMail(target, code);
        }
        return true;
    }

    /**
     * 通用：校验「找回密码」验证码（防爆破 / 一次性删除），与登录验证码逻辑一致但 key 隔离。
     */
    private void verifyResetCode(String tryPrefix, String codePrefix, String target, String code) {
        if (StrUtil.hasBlank(target, code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请输入账号与验证码");
        }
        // 防爆破：同一目标验证码错误次数过多则临时封禁到验证码过期
        String tryKey = tryPrefix + target;
        String tryCount = stringRedisTemplate.opsForValue().get(tryKey);
        if (tryCount != null && Integer.parseInt(tryCount) >= SMS_TRY_MAX) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "验证码错误次数过多，请重新获取");
        }
        // 校验验证码
        String codeKey = codePrefix + target;
        String savedCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (StrUtil.isBlank(savedCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码已过期，请重新获取");
        }
        if (!savedCode.equals(code)) {
            // 记录错误次数（与验证码同 TTL），达到上限后临时封禁
            Long n = stringRedisTemplate.opsForValue().increment(tryKey);
            if (n != null && n == 1) {
                stringRedisTemplate.expire(tryKey, Duration.ofMinutes(SMS_CODE_TTL_MINUTES));
            }
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
        }
        // 校验通过：删除验证码与错误计数，防止重放 / 爆破
        stringRedisTemplate.delete(codeKey);
        stringRedisTemplate.delete(tryKey);
    }

    /**
     * 校验找回密码的新密码策略（≥8 位、两次一致），与「修改自己的密码」保持一致。
     */
    private void validateNewPassword(String newPassword, String checkPassword) {
        ThrowUtils.throwIf(StrUtil.hasBlank(newPassword, checkPassword), ErrorCode.PARAMS_ERROR, "请填写完整信息");
        ThrowUtils.throwIf(newPassword.length() < 8, ErrorCode.PARAMS_ERROR, "新密码至少 8 位");
        ThrowUtils.throwIf(!newPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "两次输入的新密码不一致");
    }

    /**
     * 执行密码重置：BCrypt 加密 + 更新 + 清除登录态缓存（强制旧会话失效）。
     */
    private boolean doResetPassword(User user, String newPassword) {
        user.setUserPassword(getEncryptPassword(newPassword));
        boolean result = this.updateById(user);
        // 清除登录态缓存，避免旧会话继续可用
        redisUtil.delete(LOGIN_USER_KEY_PREFIX + user.getId());
        return result;
    }

    /**
     * 日志脱敏：邮箱首字符保留、@ 之后全保留，中间打码（a***@qq.com），避免明文邮箱进入日志
     */
    private static String maskEmail(String email) {
        if (email == null) {
            return email;
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return email;
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean adjustBalance(Long userId, long amount, String reason) {
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR);
        User user = this.getById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        long beforeBalance = user.getSecondsBalance() == null ? 0L : user.getSecondsBalance();
        long newBalance = beforeBalance + amount;
        // 余额不能为负
        ThrowUtils.throwIf(newBalance < 0, ErrorCode.OPERATION_ERROR, "积分余额不足");
        user.setSecondsBalance(newBalance);
        boolean result = this.updateById(user);
        // 清除登录态缓存
        redisUtil.delete(LOGIN_USER_KEY_PREFIX + userId);
        // 记录积分流水
        String bizType = amount > 0
                ? SecondsBizTypeEnum.ADMIN_ADD.getValue()
                : SecondsBizTypeEnum.ADMIN_DEDUCT.getValue();
        String desc = StrUtil.isNotBlank(reason) ? reason : (amount > 0 ? "管理员增加积分" : "管理员扣除积分");
        secondsRecordMapper.insert(SecondsRecord.builder()
                .userId(userId)
                .amount(amount)
                .balanceAfter(newBalance)
                .giftAfter(user.getGiftSecondsBalance() == null ? 0L : user.getGiftSecondsBalance())
                .bizType(bizType)
                .bizDesc(desc)
                .status(0)
                .build(), true);
        return result;
    }

    @Override
    public boolean updateMembership(Long userId, String membershipTier, LocalDateTime membershipExpireTime) {
        ThrowUtils.throwIf(userId == null || StrUtil.isBlank(membershipTier), ErrorCode.PARAMS_ERROR);
        User user = this.getById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        user.setMembershipTier(membershipTier);
        user.setMembershipExpireTime(membershipExpireTime);
        boolean result = this.updateById(user);
        // 清除登录态缓存
        redisUtil.delete(LOGIN_USER_KEY_PREFIX + userId);
        return result;
    }

    @Override
    public int batchDelete(List<Long> ids) {
        ThrowUtils.throwIf(CollUtil.isEmpty(ids), ErrorCode.PARAMS_ERROR);
        // 逻辑删除
        return this.removeByIds(ids) ? ids.size() : 0;
    }
}

package com.mao.maocodeuser.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mao.maocodemother.exception.BusinessException;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.exception.ThrowUtils;
import com.mao.maocodemother.model.dto.user.UserQueryRequest;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodeuser.mapper.UserMapper;
import com.mao.maocodemother.model.enums.UserRoleEnum;
import com.mao.maocodemother.model.vo.LoginUserVO;
import com.mao.maocodemother.model.vo.MembershipVO;
import com.mao.maocodemother.model.vo.UserVO;
import com.mao.maocodemother.product.ProductBenefitResolver;
import com.mao.maocodeuser.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.mao.maocodemother.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户 服务层实现。
 *
 * @author <a href="https://github.com/liyupi">程序员mao</a>
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
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
        // 2. 查询用户是否已存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 3. 加密密码
        String encryptPassword = getEncryptPassword(userPassword);
        // 4. 创建用户，插入数据库
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("无名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        user.setMembershipTier("FREE");
        user.setSecondsBalance(0L);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "注册失败，数据库错误");
        }
        return user.getId();
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
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
        // 2. 加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 3. 查询用户是否存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 4. 如果用户存在，记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // 5. 返回脱敏的用户信息
        return this.getLoginUserVO(user);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断用户是否登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询当前用户信息
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
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
        // 盐值，混淆密码
        final String SALT = "yupi";
        return DigestUtils.md5DigestAsHex((userPassword + SALT).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void fulfillOrder(Long userId, String productType, String productCode, int quantity) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StrUtil.isBlank(productType) || StrUtil.isBlank(productCode), ErrorCode.PARAMS_ERROR);
        User user = getById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        int qty = Math.max(quantity, 1);
        switch (productType) {
            case "MEMBERSHIP" -> applyMembership(user, productCode, qty);
            case "SECONDS" -> addSeconds(user, ProductBenefitResolver.resolveSeconds(productCode) * qty);
            case "CARD" -> applyCard(user, productCode, qty);
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "未知商品类型");
        }
        boolean updated = updateById(user);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "权益发放失败");
    }

    @Override
    public MembershipVO getMembershipVO(User user) {
        if (user == null) {
            return null;
        }
        MembershipVO vo = new MembershipVO();
        String tier = StrUtil.blankToDefault(user.getMembershipTier(), "FREE");
        vo.setMembershipTier(tier);
        vo.setMembershipTierName(ProductBenefitResolver.resolveMembershipTierName(tier));
        vo.setSecondsBalance(user.getSecondsBalance() == null ? 0L : user.getSecondsBalance());
        vo.setMembershipExpireTime(user.getMembershipExpireTime());
        return vo;
    }

    private void applyMembership(User user, String productCode, int quantity) {
        String tier = ProductBenefitResolver.resolveMembershipTier(productCode);
        int days = ProductBenefitResolver.resolveMembershipDays(productCode);
        ThrowUtils.throwIf("FREE".equals(tier) || days <= 0, ErrorCode.PARAMS_ERROR, "无效的会员商品");
        user.setMembershipTier(tier);
        LocalDateTime base = user.getMembershipExpireTime();
        if (base == null || base.isBefore(LocalDateTime.now())) {
            base = LocalDateTime.now();
        }
        user.setMembershipExpireTime(base.plusDays((long) days * quantity));
    }

    private void addSeconds(User user, long seconds) {
        ThrowUtils.throwIf(seconds <= 0, ErrorCode.PARAMS_ERROR, "无效的秒点商品");
        long current = user.getSecondsBalance() == null ? 0L : user.getSecondsBalance();
        user.setSecondsBalance(current + seconds);
    }

    private void applyCard(User user, String productCode, int quantity) {
        long seconds = ProductBenefitResolver.resolveSeconds(productCode);
        if (seconds > 0) {
            addSeconds(user, seconds * quantity);
            return;
        }
        applyMembership(user, productCode, quantity);
    }
}

package com.mao.maocodemother.innerservice;

import com.mao.maocodemother.exception.BusinessException;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.innerservice.InnerUserService;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static com.mao.maocodemother.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 内部使用的用户服务
 */
public interface InnerUserService {

    List<User> listByIds(Collection<? extends Serializable> ids);

    User getById(Serializable id);

    UserVO getUserVO(User user);

    /**
     * 支付成功后发放会员/秒点等权益
     */
    void fulfillOrder(Long userId, String productType, String productCode, int quantity);

    /**
     * 用户总数（管理员仪表盘用）
     */
    long countUsers();

    /**
     * 某时间点之后新增的用户数
     */
    long countUsersSince(LocalDateTime since);

    /**
     * 某时间段内新增的用户数
     */
    long countUsersBetween(LocalDateTime start, LocalDateTime end);

    // 静态方法，避免跨服务调用
    static User getLoginUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }
}
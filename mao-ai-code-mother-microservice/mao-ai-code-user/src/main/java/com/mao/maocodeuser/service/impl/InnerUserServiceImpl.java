package com.mao.maocodeuser.service.impl;

import com.mao.maocodemother.innerservice.InnerUserService;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.vo.UserVO;
import com.mao.maocodeuser.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 内部服务实现类
 */
@DubboService
public class InnerUserServiceImpl implements InnerUserService {

    @Resource
    private UserService userService;

    @Override
    public List<User> listByIds(Collection<? extends Serializable> ids) {
        return userService.listByIds(ids);
    }

    @Override
    public User getById(Serializable id) {
        return userService.getById(id);
    }

    @Override
    public UserVO getUserVO(User user) {
        return userService.getUserVO(user);
    }

    @Override
    public void fulfillOrder(Long userId, String productType, String productCode, int quantity) {
        userService.fulfillOrder(userId, productType, productCode, quantity);
    }

    @Override
    public long countUsers() {
        return userService.count();
    }

    @Override
    public long countUsersSince(LocalDateTime since) {
        return userService.count(QueryWrapper.create().ge("createTime", since));
    }

    @Override
    public long countUsersBetween(LocalDateTime start, LocalDateTime end) {
        return userService.count(QueryWrapper.create().ge("createTime", start).le("createTime", end));
    }
}

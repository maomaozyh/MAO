package com.mao.maocodemother.aop;

import com.mao.maocodemother.annotation.AuthCheck;
import com.mao.maocodemother.exception.BusinessException;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.enums.UserRoleEnum;
import com.mao.maocodemother.service.SysPermissionService;
import com.mao.maocodemother.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    @Resource
    private SysPermissionService sysPermissionService;

    /**
     * 执行拦截
     *
     * @param joinPoint 切入点
     * @param authCheck 权限校验注解
     * @return
     * @throws Throwable
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        String mustPermission = authCheck.mustPermission();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 1. 角色校验
        if (mustRole != null && !mustRole.isEmpty()) {
            UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
            // 不需要角色权限，直接放行
            if (mustRoleEnum != null) {
                UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
                // 没有权限，直接拒绝
                if (userRoleEnum == null) {
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
                }
                // 要求必须有管理员权限，但当前登录用户没有
                if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)) {
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
                }
            }
        }

        // 2. 权限码校验（管理员自动拥有所有权限）
        if (mustPermission != null && !mustPermission.isEmpty()) {
            // 管理员直接放行
            if (!UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole())) {
                List<String> permissionCodes = sysPermissionService.getPermissionCodesByUserId(loginUser.getId());
                if (!permissionCodes.contains(mustPermission)) {
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "缺少权限：" + mustPermission);
                }
            }
        }

        return joinPoint.proceed();
    }
}

package com.mao.maocodemother.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryCondition;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mao.maocodemother.mapper.OperationLogMapper;
import com.mao.maocodemother.model.dto.log.OperationLogQueryRequest;
import com.mao.maocodemother.model.entity.OperationLog;
import com.mao.maocodemother.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志 服务层实现。
 */
@Service
@Slf4j
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    @Override
    public Page<OperationLog> listLogByPage(OperationLogQueryRequest operationLogQueryRequest) {
        long pageNum = operationLogQueryRequest.getPageNum();
        long pageSize = operationLogQueryRequest.getPageSize();
        QueryWrapper queryWrapper = getQueryWrapper(operationLogQueryRequest);
        return this.page(Page.of(pageNum, pageSize), queryWrapper);
    }

    @Override
    public List<OperationLog> listRecentLog(long limit) {
        long size = limit <= 0 ? 10 : Math.min(limit, 100);
        return this.list(QueryWrapper.create()
                .orderBy("createTime", false)
                .limit(size));
    }

    @Override
    public void record(Long userId, String userName, String module, String operation, String targetId,
                       String detail, Integer status, String errorMsg, HttpServletRequest request) {
        try {
            OperationLog operationLog = OperationLog.builder()
                    .userId(userId)
                    .userName(StrUtil.isBlank(userName) ? "未知用户" : userName)
                    .module(module)
                    .operation(operation)
                    .targetId(targetId)
                    .detail(detail)
                    .ip(getClientIp(request))
                    .status(status == null ? 1 : status)
                    .errorMsg(StrUtil.isNotBlank(errorMsg) ? StrUtil.sub(errorMsg, 0, 1000) : null)
                    .createTime(LocalDateTime.now())
                    .build();
            this.save(operationLog);
        } catch (Exception e) {
            // 日志记录失败不能影响主业务，仅打印告警
            log.warn("记录操作日志失败，module={}, operation={}, 原因={}", module, operation, e.getMessage());
        }
    }

    @Override
    public void recordSuccess(Long userId, String userName, String module, String operation, String targetId,
                              String detail, HttpServletRequest request) {
        record(userId, userName, module, operation, targetId, detail, 1, null, request);
    }

    @Override
    public void recordFail(Long userId, String userName, String module, String operation, String targetId,
                           String detail, String errorMsg, HttpServletRequest request) {
        record(userId, userName, module, operation, targetId, detail, 0, errorMsg, request);
    }

    @Override
    public QueryWrapper getQueryWrapper(OperationLogQueryRequest operationLogQueryRequest) {
        if (operationLogQueryRequest == null) {
            return QueryWrapper.create().orderBy("createTime", false);
        }
        Long userId = operationLogQueryRequest.getUserId();
        String module = operationLogQueryRequest.getModule();
        String operation = operationLogQueryRequest.getOperation();
        String keyword = operationLogQueryRequest.getKeyword();
        LocalDateTime startTime = operationLogQueryRequest.getStartTime();
        LocalDateTime endTime = operationLogQueryRequest.getEndTime();
        String sortField = operationLogQueryRequest.getSortField();
        String sortOrder = operationLogQueryRequest.getSortOrder();

        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("userId", userId)
                .eq("module", module)
                .eq("operation", operation)
                .ge("createTime", startTime)
                .le("createTime", endTime);

        // 关键词模糊搜索操作详情或操作人昵称
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.and(QueryCondition.create(new QueryColumn("detail"), "like", keyword)
                    .or(QueryCondition.create(new QueryColumn("userName"), "like", keyword)));
        }

        // 排序
        boolean isAsc = "ascend".equals(sortOrder);
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, isAsc);
        } else {
            queryWrapper.orderBy("createTime", false);
        }

        return queryWrapper;
    }

    /**
     * 获取客户端真实 IP，优先取 X-Forwarded-For
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能是多级代理，取第一个
        if (StrUtil.isNotBlank(ip) && ip.contains(",")) {
            ip = ip.substring(0, ip.indexOf(",")).trim();
        }
        return ip;
    }
}

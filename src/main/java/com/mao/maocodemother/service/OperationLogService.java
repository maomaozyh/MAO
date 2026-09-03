package com.mao.maocodemother.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mao.maocodemother.model.dto.log.OperationLogQueryRequest;
import com.mao.maocodemother.model.entity.OperationLog;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 操作日志 服务。
 */
public interface OperationLogService {

    /**
     * 分页查询操作日志
     *
     * @param operationLogQueryRequest 查询请求
     * @return 操作日志分页
     */
    Page<OperationLog> listLogByPage(OperationLogQueryRequest operationLogQueryRequest);

    /**
     * 查询最近的操作日志
     *
     * @param limit 条数
     * @return 操作日志列表
     */
    List<OperationLog> listRecentLog(long limit);

    /**
     * 记录一条操作日志（失败也不影响主业务）
     *
     * @param userId    操作人 ID
     * @param userName  操作人昵称
     * @param module    模块名
     * @param operation 操作类型
     * @param targetId  操作对象 ID
     * @param detail    操作详情
     * @param status    0 失败 1 成功
     * @param errorMsg  错误信息
     * @param request   请求对象，用于获取 IP
     */
    void record(Long userId, String userName, String module, String operation, String targetId,
                String detail, Integer status, String errorMsg, HttpServletRequest request);

    /**
     * 记录一条成功的操作日志
     */
    void recordSuccess(Long userId, String userName, String module, String operation, String targetId,
                       String detail, HttpServletRequest request);

    /**
     * 记录一条失败的操作日志
     */
    void recordFail(Long userId, String userName, String module, String operation, String targetId,
                    String detail, String errorMsg, HttpServletRequest request);

    /**
     * 获取查询条件
     *
     * @param operationLogQueryRequest 查询请求
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(OperationLogQueryRequest operationLogQueryRequest);
}

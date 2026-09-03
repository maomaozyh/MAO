package com.mao.maocodemother.controller;

import com.mybatisflex.core.paginate.Page;
import com.mao.maocodemother.annotation.AuthCheck;
import com.mao.maocodemother.common.BaseResponse;
import com.mao.maocodemother.common.ResultUtils;
import com.mao.maocodemother.constant.UserConstant;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.exception.ThrowUtils;
import com.mao.maocodemother.model.dto.log.OperationLogQueryRequest;
import com.mao.maocodemother.model.entity.OperationLog;
import com.mao.maocodemother.service.OperationLogService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 操作日志 控制层。
 */
@RestController
@RequestMapping("/admin/log")
public class OperationLogController {

    @Resource
    private OperationLogService operationLogService;

    /**
     * 分页查询操作日志（管理员）
     *
     * @param operationLogQueryRequest 查询请求
     * @return 操作日志分页
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<OperationLog>> listLogByPage(@RequestBody OperationLogQueryRequest operationLogQueryRequest) {
        ThrowUtils.throwIf(operationLogQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Page<OperationLog> logPage = operationLogService.listLogByPage(operationLogQueryRequest);
        return ResultUtils.success(logPage);
    }

    /**
     * 查询最近的操作日志（管理员，供仪表盘使用）
     *
     * @param limit 条数，默认 10
     * @return 操作日志列表
     */
    @GetMapping("/recent")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<OperationLog>> listRecentLog(@RequestParam(defaultValue = "10") long limit) {
        return ResultUtils.success(operationLogService.listRecentLog(limit));
    }
}

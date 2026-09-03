package com.mao.maocodemother.controller;

import com.mybatisflex.core.paginate.Page;
import com.mao.maocodemother.annotation.AuthCheck;
import com.mao.maocodemother.common.BaseResponse;
import com.mao.maocodemother.common.ResultUtils;
import com.mao.maocodemother.constant.UserConstant;
import com.mao.maocodemother.model.dto.seconds.SecondsRecordQueryRequest;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.vo.SecondsBalanceVO;
import com.mao.maocodemother.model.vo.SecondsCheckinVO;
import com.mao.maocodemother.model.vo.SecondsRecordVO;
import com.mao.maocodemother.service.SecondsService;
import com.mao.maocodemother.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 积分接口（余额 / 流水明细）
 */
@RestController
@RequestMapping("/points")
public class SecondsController {

    @Resource
    private SecondsService secondsService;

    @Resource
    private UserService userService;

    /**
     * 查询我的积分余额（购买余额 + 赠送额度）
     */
    @GetMapping("/balance")
    public BaseResponse<SecondsBalanceVO> getBalance(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(secondsService.getBalance(loginUser.getId()));
    }

    /**
     * 分页查询我的积分流水
     */
    @GetMapping("/list/page/vo")
    public BaseResponse<Page<SecondsRecordVO>> listMyRecords(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(secondsService.listMyRecords(loginUser.getId(), pageNum, pageSize));
    }

    /**
     * 每日签到（每天一次，送 seconds.checkinReward 配置的积分，计入赠送额度）
     */
    @PostMapping("/checkin")
    public BaseResponse<SecondsCheckinVO> checkin(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        long reward = secondsService.checkin(loginUser.getId());
        return ResultUtils.success(buildCheckinVO(loginUser.getId(), reward, true));
    }

    /**
     * 查询今日签到状态
     */
    @GetMapping("/checkin/status")
    public BaseResponse<SecondsCheckinVO> checkinStatus(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        boolean checked = secondsService.isCheckedInToday(loginUser.getId());
        return ResultUtils.success(buildCheckinVO(loginUser.getId(), null, checked));
    }

    private SecondsCheckinVO buildCheckinVO(Long userId, Long reward, boolean checkedToday) {
        SecondsCheckinVO vo = new SecondsCheckinVO();
        SecondsBalanceVO balance = secondsService.getBalance(userId);
        vo.setReward(reward);
        vo.setCheckedToday(checkedToday);
        vo.setSecondsBalance(balance.getSecondsBalance());
        vo.setGiftSecondsBalance(balance.getGiftSecondsBalance());
        vo.setTotalSeconds(balance.getTotalSeconds());
        return vo;
    }

    /**
     * 管理后台分页查询全部积分流水（支持用户 / 业务类型过滤，仅管理员）
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<SecondsRecordVO>> listRecordsByPage(@RequestBody SecondsRecordQueryRequest request) {
        return ResultUtils.success(secondsService.listRecordsByPage(request));
    }
}

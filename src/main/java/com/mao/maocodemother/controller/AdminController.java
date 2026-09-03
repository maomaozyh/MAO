package com.mao.maocodemother.controller;

import com.mao.maocodemother.annotation.AuthCheck;
import com.mao.maocodemother.common.BaseResponse;
import com.mao.maocodemother.common.ResultUtils;
import com.mao.maocodemother.constant.UserConstant;
import com.mao.maocodemother.model.vo.DashboardStatsVO;
import com.mao.maocodemother.service.AppService;
import com.mao.maocodemother.service.ChatHistoryService;
import com.mao.maocodemother.service.CommunityPostService;
import com.mao.maocodemother.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理员仪表盘控制器
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private UserService userService;

    @Resource
    private AppService appService;

    @Resource
    private CommunityPostService communityPostService;

    @Resource
    private ChatHistoryService chatHistoryService;

    /**
     * 获取仪表盘统计数据
     */
    @GetMapping("/dashboard/stats")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Cacheable(value = "admin_dashboard")
    public BaseResponse<DashboardStatsVO> getDashboardStats() {
        DashboardStatsVO stats = new DashboardStatsVO();

        // 总数统计
        stats.setTotalUsers(userService.count());
        stats.setTotalApps(appService.count());
        stats.setTotalPosts(communityPostService.count());
        stats.setTotalChats(chatHistoryService.count());

        // 今日新增
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        stats.setTodayNewUsers(userService.count(QueryWrapper.create()
                .ge("createTime", todayStart)));
        stats.setTodayNewPosts(communityPostService.count(QueryWrapper.create()
                .ge("createTime", todayStart)));

        // 本周新增（最近7天）
        LocalDateTime weekStart = LocalDateTime.of(LocalDate.now().minusDays(6), LocalTime.MIN);
        stats.setWeekNewUsers(userService.count(QueryWrapper.create()
                .ge("createTime", weekStart)));
        stats.setWeekNewPosts(communityPostService.count(QueryWrapper.create()
                .ge("createTime", weekStart)));

        // 最近7天趋势
        List<Long> userGrowth = new ArrayList<>();
        List<Long> postGrowth = new ArrayList<>();
        List<String> dateLabels = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);
            dateLabels.add(date.format(formatter));

            long userCount = userService.count(QueryWrapper.create()
                    .ge("createTime", dayStart)
                    .le("createTime", dayEnd));
            long postCount = communityPostService.count(QueryWrapper.create()
                    .ge("createTime", dayStart)
                    .le("createTime", dayEnd));

            userGrowth.add(userCount);
            postGrowth.add(postCount);
        }

        stats.setUserGrowthTrend(userGrowth);
        stats.setPostGrowthTrend(postGrowth);
        stats.setDateLabels(dateLabels);

        return ResultUtils.success(stats);
    }
}

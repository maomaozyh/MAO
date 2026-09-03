package com.mao.maocodemother.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 仪表盘统计信息
 */
@Data
public class DashboardStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 总用户数
     */
    private Long totalUsers;

    /**
     * 总应用数
     */
    private Long totalApps;

    /**
     * 总帖子数
     */
    private Long totalPosts;

    /**
     * 总对话数
     */
    private Long totalChats;

    /**
     * 今日新增用户
     */
    private Long todayNewUsers;

    /**
     * 今日新增帖子
     */
    private Long todayNewPosts;

    /**
     * 本周新增用户
     */
    private Long weekNewUsers;

    /**
     * 本周新增帖子
     */
    private Long weekNewPosts;

    /**
     * 用户增长趋势（最近7天）
     */
    private List<Long> userGrowthTrend;

    /**
     * 帖子增长趋势（最近7天）
     */
    private List<Long> postGrowthTrend;

    /**
     * 日期标签（最近7天）
     */
    private List<String> dateLabels;
}

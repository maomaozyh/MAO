package com.mao.maocodemother.service;

import com.mybatisflex.core.paginate.Page;
import com.mao.maocodemother.model.dto.seconds.SecondsRecordQueryRequest;
import com.mao.maocodemother.model.entity.SecondsRecord;
import com.mao.maocodemother.model.enums.SecondsBizTypeEnum;
import com.mao.maocodemother.model.vo.SecondsBalanceVO;
import com.mao.maocodemother.model.vo.SecondsRecordVO;

/**
 * 积分服务（扣费 / 入账 / 退回 / 流水查询）
 */
public interface SecondsService {

    /**
     * 获取某业务的扣费单价（点/次），从 sys_config 的 seconds.price 读取
     *
     * @param bizType 业务类型
     * @return 单价，配置缺失时取默认值
     */
    long getPrice(SecondsBizTypeEnum bizType);

    /**
     * 预扣积分：校验余额 → 扣除 → 写流水。
     * 扣除顺序：先扣赠送额度，不足部分再扣购买余额。
     *
     * @param userId  用户 id
     * @param bizType 业务类型
     * @param bizDesc 业务描述（可空，默认用业务类型文案）
     * @param appId   关联应用 id（可空）
     * @return 扣费流水 id，生成失败时需用其调用 {@link #refund(Long)}
     */
    long deduct(Long userId, SecondsBizTypeEnum bizType, String bizDesc, Long appId);

    /**
     * 退回某笔扣费（幂等，已退回的不会重复退）
     *
     * @param recordId 扣费流水 id
     */
    void refund(Long recordId);

    /**
     * 积分入账（购买 / 赠送发放）
     *
     * @param userId  用户 id
     * @param amount  入账数量（正数）
     * @param bizType 业务类型
     * @param bizDesc 业务描述
     */
    void credit(Long userId, long amount, SecondsBizTypeEnum bizType, String bizDesc);

    /**
     * 查询我的积分余额（购买余额 + 赠送额度）
     *
     * @param userId 用户 id
     * @return 余额视图
     */
    SecondsBalanceVO getBalance(Long userId);

    /**
     * 分页查询我的积分流水
     *
     * @param userId   用户 id
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 流水分页
     */
    Page<SecondsRecordVO> listMyRecords(Long userId, long pageNum, long pageSize);

    /**
     * 每日签到：送出 seconds.checkinReward 配置的积分（计入赠送额度）。
     * 当天已签到则抛出业务异常。
     *
     * @param userId 用户 id
     * @return 本次签到送出的积分数
     */
    long checkin(Long userId);

    /**
     * 今天是否已签到
     *
     * @param userId 用户 id
     * @return true 表示今天已签过
     */
    boolean isCheckedInToday(Long userId);

    /**
     * 管理后台分页查询积分流水（支持按用户 / 业务类型过滤）
     *
     * @param request 查询条件（含分页参数）
     * @return 流水分页
     */
    Page<SecondsRecordVO> listRecordsByPage(SecondsRecordQueryRequest request);
}

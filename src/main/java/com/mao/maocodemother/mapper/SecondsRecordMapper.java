package com.mao.maocodemother.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mao.maocodemother.model.entity.SecondsRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 积分流水 Mapper
 */
public interface SecondsRecordMapper extends BaseMapper<SecondsRecord> {

    /**
     * 原子抢占退款权：仅当流水仍为「有效(0)」时置为「已退回(1)」。
     *
     * <p>并发调用同一流水的 refund 时，只有一个请求影响 1 行，其余影响 0 行直接返回，
     * 保证退款幂等不依赖事务隔离级别。
     *
     * @param id 流水 id
     * @return 影响行数（0 = 已被其它请求退回/不存在）
     */
    @Update("UPDATE seconds_record SET status = 1, updateTime = NOW() WHERE id = #{id} AND status = 0")
    int markRefunded(@Param("id") Long id);
}

package com.mao.maocodemother.model.dto.seconds;

import com.mao.maocodemother.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 管理后台积分流水分页查询请求
 *
 * @author 元知AI
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SecondsRecordQueryRequest extends PageRequest implements Serializable {

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 业务类型（SecondsBizTypeEnum 的 value，如 PURCHASE / GEN_CODE）
     */
    private String bizType;

    private static final long serialVersionUID = 1L;
}

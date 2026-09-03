package com.mao.maocodemother.model.dto.order;

import com.mao.maocodemother.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 管理后台订单分页查询请求
 *
 * @author 元知AI
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class OrderQueryRequest extends PageRequest implements Serializable {

    /**
     * 订单号（精确匹配）
     */
    private String orderNo;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 商品类型：MEMBERSHIP / SECONDS
     */
    private String productType;

    /**
     * 订单状态：PENDING / PAID / EXPIRED
     */
    private String status;

    private static final long serialVersionUID = 1L;
}

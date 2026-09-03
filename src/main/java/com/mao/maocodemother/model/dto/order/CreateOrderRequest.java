package com.mao.maocodemother.model.dto.order;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建订单请求
 */
@Data
public class CreateOrderRequest implements Serializable {

    /**
     * 商品类型：MEMBERSHIP / SECONDS / CARD
     */
    private String productType;

    /**
     * 商品编码（见 ProductCatalog）
     */
    private String productCode;

    /**
     * 购买数量（卡券类可大于 1，其余默认 1）
     */
    private Integer quantity;
}

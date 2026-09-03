-- 支付相关表结构迁移（幂等，可重复执行）
USE yu_ai_code_mother;

-- 交易订单表（支付：MOCK 沙箱 / REAL 真实渠道）
CREATE TABLE IF NOT EXISTS trade_order
(
    id           BIGINT                             NOT NULL COMMENT 'id（雪花 ID）' PRIMARY KEY,
    orderNo      VARCHAR(64)                        NOT NULL COMMENT '商户订单号',
    userId       BIGINT                             NOT NULL COMMENT '下单用户 id',
    productType  VARCHAR(32)                        NOT NULL COMMENT '商品类型：MEMBERSHIP / SECONDS / CARD',
    productCode  VARCHAR(64)                        NOT NULL COMMENT '商品编码（见 ProductCatalog）',
    productName  VARCHAR(256)                       NULL COMMENT '商品名称',
    quantity     INT      DEFAULT 1                 NOT NULL COMMENT '购买数量',
    amount       DECIMAL(10, 2)                     NOT NULL COMMENT '订单金额（元）',
    currency     VARCHAR(8)  DEFAULT 'CNY'          NOT NULL COMMENT '币种',
    status       VARCHAR(16)                        NOT NULL COMMENT '订单状态：PENDING / PAID / EXPIRED',
    channel      VARCHAR(16)                        NOT NULL COMMENT '支付渠道：MOCK / REAL',
    payTradeNo   VARCHAR(64)                        NULL COMMENT '渠道交易流水号',
    expireTime   DATETIME                           NULL COMMENT '订单过期时间',
    payTime      DATETIME                           NULL COMMENT '支付时间',
    createTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete     TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    UNIQUE KEY uk_orderNo (orderNo),
    INDEX idx_userId (userId),
    INDEX idx_status_expire (status, expireTime)
) COMMENT '交易订单' COLLATE = utf8mb4_unicode_ci;

-- 用户权益字段（会员等级 / 会员过期时间 / 秒点余额）已由 alter_user_membership.sql 在 user 表上创建，
-- 对应列：membershipTier / secondsBalance / membershipExpireTime，这里无需再添加。

# 数据库初始化
# @author <a href="https://github.com/liyupi">程序员mao</a>
# @from <a href="https://codefather.cn">编程导航学习圈</a>

-- 创建库
create database if not exists yu_ai_code_mother;

-- 切换库
use yu_ai_code_mother;

-- 用户表
-- 以下是建表语句

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
) comment '用户' collate = utf8mb4_unicode_ci;

-- 应用表
create table app
(
    id           bigint auto_increment comment 'id' primary key,
    appName      varchar(256)                       null comment '应用名称',
    cover        varchar(512)                       null comment '应用封面',
    initPrompt   text                               null comment '应用初始化的 prompt',
    codeGenType  varchar(64)                        null comment '代码生成类型（枚举）',
    category     varchar(64)                        null comment '应用分类（对应前端快捷入口，如 miniprogram/image/ppt 等）',
    deployKey    varchar(64)                        null comment '部署标识',
    deployedTime datetime                           null comment '部署时间',
    lastOpenTime datetime                           null comment '最近打开时间（用于“最近项目”排序与同步）',
    priority     int      default 0                 not null comment '优先级',
    status       int                                null comment '应用状态（0 正常 / 其他为下架或审核中等）',
    isPublic     tinyint   default 1                 not null comment '是否公开：1-公开，0-私密',
    userId       bigint                             not null comment '创建用户id',
    editTime     datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '是否删除',
    UNIQUE KEY uk_deployKey (deployKey), -- 确保部署标识唯一
    INDEX idx_appName (appName),         -- 提升基于应用名称的查询性能
    INDEX idx_category (category),       -- 提升基于应用分类的查询性能
    INDEX idx_userId (userId),           -- 提升基于用户 ID 的查询性能
    INDEX idx_isPublic (isPublic)        -- 提升按公开状态过滤的查询性能
) comment '应用' collate = utf8mb4_unicode_ci;

-- 对话历史表
create table chat_history
(
    id          bigint auto_increment comment 'id' primary key,
    message     text                               not null comment '消息',
    messageType varchar(32)                        not null comment 'user/ai',
    appId       bigint                             not null comment '应用id',
    userId      bigint                             not null comment '创建用户id',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除',
    INDEX idx_appId (appId),                       -- 提升基于应用的查询性能
    INDEX idx_createTime (createTime),             -- 提升基于时间的查询性能
    INDEX idx_appId_createTime (appId, createTime) -- 游标查询核心索引
) comment '对话历史' collate = utf8mb4_unicode_ci;

-- 生成的外部素材表（图片/视频/3D 模型/PPT 等真实产出，供前端预览/下载）
create table generated_asset
(
    id         bigint                             not null comment 'id（雪花 ID）' primary key,
    appId      bigint                             not null comment '所属应用 id',
    userId     bigint                             not null comment '生成用户 id',
    assetType  varchar(32)                        not null comment '素材类型（image/video/model_3d/ppt）',
    url        varchar(1024)                      null comment '素材可访问 URL',
    localPath  varchar(1024)                      null comment '本地/COS 存储路径（预留）',
    prompt     text                               null comment '生成该素材使用的提示词',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    INDEX idx_appId (appId),
    INDEX idx_userId (userId),
    INDEX idx_assetType (assetType)
) comment '生成的外部素材' collate = utf8mb4_unicode_ci;

-- 交易订单表（支付：MOCK 沙箱 / REAL 真实渠道）
create table if not exists trade_order
(
    id           bigint                             not null comment 'id（雪花 ID）' primary key,
    orderNo      varchar(64)                        not null comment '商户订单号',
    userId       bigint                             not null comment '下单用户 id',
    productType  varchar(32)                        not null comment '商品类型：MEMBERSHIP / SECONDS / CARD',
    productCode  varchar(64)                        not null comment '商品编码（见 ProductCatalog）',
    productName  varchar(256)                       null comment '商品名称',
    quantity     int      default 1                 not null comment '购买数量',
    amount       decimal(10, 2)                     not null comment '订单金额（元）',
    currency     varchar(8)  default 'CNY'          not null comment '币种',
    status       varchar(16)                        not null comment '订单状态：PENDING / PAID / EXPIRED',
    channel      varchar(16)                        not null comment '支付渠道：MOCK / REAL',
    payTradeNo   varchar(64)                        null comment '渠道交易流水号',
    expireTime   datetime                           null comment '订单过期时间',
    payTime      datetime                           null comment '支付时间',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '是否删除',
    UNIQUE KEY uk_orderNo (orderNo),
    INDEX idx_userId (userId),
    INDEX idx_status_expire (status, expireTime)
) comment '交易订单' collate = utf8mb4_unicode_ci;

-- 用户权益字段（会员等级 / 会员过期时间 / 秒点余额）对应列 membershipTier / secondsBalance / membershipExpireTime，
-- 已随 user 表一并创建（见上方 user 表定义），无需额外 ALTER。

-- 秒点流水表（购买入账 / 注册赠送 / 会员每月发放 / AI 能力扣费 / 失败退回）
create table if not exists seconds_record
(
    id           bigint                             not null comment 'id（雪花 ID）' primary key,
    userId       bigint                             not null comment '用户 id',
    amount       bigint                             not null comment '变动秒点（正数=获取，负数=消耗）',
    balanceAfter bigint  default 0                  not null comment '变动后的购买余额',
    giftAfter    bigint  default 0                  not null comment '变动后的赠送额度',
    bizType      varchar(32)                        not null comment '业务类型：PURCHASE/GIFT/GEN_CODE/GEN_IMAGE/GEN_VIDEO/GEN_3D/GEN_PPT/EXPAND/SEMANTIC_SEARCH/SELF_CHECK/REFUND',
    bizDesc      varchar(256)                       null comment '业务描述',
    appId        bigint                             null comment '关联应用 id（可空）',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '是否删除',
    INDEX idx_userId (userId),
    INDEX idx_bizType (bizType),
    INDEX idx_createTime (createTime)
) comment '秒点流水' collate = utf8mb4_unicode_ci;

-- user 表的赠送额度字段（giftSecondsBalance / lastGiftMonth），
-- 已随 user 表一并创建（见上方 user 表定义），存量库需执行 seconds_migration.sql。


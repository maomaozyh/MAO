-- mao AI 代码生成器 - 数据库初始化脚本
-- 执行方式：mysql -u root -p < init.sql
-- 或将该文件挂载到 MySQL 容器的 /docker-entrypoint-initdb.d/ 目录自动执行

CREATE DATABASE IF NOT EXISTS `yu_ai_code_mother` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `yu_ai_code_mother`;
SET NAMES utf8mb4;

-- ----------------------------
-- 用户表
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`
(
    `id`           BIGINT       NOT NULL COMMENT 'id',
    `userAccount`  VARCHAR(256) NOT NULL COMMENT '账号',
    `userPassword` VARCHAR(512) NOT NULL COMMENT '密码',
    `userName`     VARCHAR(256) NULL     COMMENT '用户昵称',
    `userAvatar`   VARCHAR(1024) NULL    COMMENT '用户头像',
    `userProfile`  VARCHAR(512)  NULL    COMMENT '用户简介',
    `userRole`     VARCHAR(64)  NOT NULL DEFAULT 'user' COMMENT '用户角色：user/admin',
    `membershipTier` VARCHAR(64) NOT NULL DEFAULT 'FREE' COMMENT '会员等级',
    `secondsBalance` BIGINT NOT NULL DEFAULT 0 COMMENT '秒点余额',
    `membershipExpireTime` DATETIME NULL COMMENT '会员到期时间',
    `editTime`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '编辑时间',
    `createTime`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_userAccount` (`userAccount`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户';

-- ----------------------------
-- 应用表
-- ----------------------------
DROP TABLE IF EXISTS `app`;
CREATE TABLE `app`
(
    `id`           BIGINT       NOT NULL COMMENT 'id',
    `appName`      VARCHAR(256) NULL     COMMENT '应用名称',
    `cover`        VARCHAR(1024) NULL    COMMENT '应用封面',
    `initPrompt`   TEXT         NULL     COMMENT '应用初始化的 prompt',
    `codeGenType`  VARCHAR(64)  NULL     COMMENT '代码生成类型（枚举）',
    `deployKey`    VARCHAR(256) NULL     COMMENT '部署标识',
    `deployedTime` DATETIME     NULL     COMMENT '部署时间',
    `lastOpenTime` DATETIME     NULL     COMMENT '最近打开时间（用于“最近项目”排序与同步）',
    `priority`     INT          NOT NULL DEFAULT 0 COMMENT '优先级',
    `userId`       BIGINT       NULL     COMMENT '创建用户id',
    `editTime`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '编辑时间',
    `createTime`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_userId` (`userId`),
    KEY `idx_priority` (`priority`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '应用';

-- ----------------------------
-- 对话历史表
-- ----------------------------
DROP TABLE IF EXISTS `chat_history`;
CREATE TABLE `chat_history`
(
    `id`          BIGINT       NOT NULL COMMENT 'id',
    `message`     TEXT         NULL     COMMENT '消息',
    `messageType` VARCHAR(64)  NULL     COMMENT '消息类型：user/ai',
    `appId`       BIGINT       NULL     COMMENT '应用id',
    `userId`      BIGINT       NULL     COMMENT '创建用户id',
    `createTime`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_appId` (`appId`),
    KEY `idx_userId` (`userId`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '对话历史';

-- ----------------------------
-- 技能表
-- ----------------------------
DROP TABLE IF EXISTS `skill`;
CREATE TABLE `skill`
(
    `id`            BIGINT       NOT NULL COMMENT '主键',
    `skillName`     VARCHAR(256) NOT NULL COMMENT '技能名称',
    `skillDesc`     VARCHAR(1024) NULL COMMENT '技能描述',
    `icon`          VARCHAR(64)  NULL COMMENT '图标',
    `category`      VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '分类',
    `price`         VARCHAR(32)  NULL DEFAULT '免费' COMMENT '价格',
    `originalPrice` VARCHAR(32)  NULL COMMENT '原价',
    `priceUnit`     VARCHAR(32)  NULL COMMENT '价格单位',
    `tags`          VARCHAR(512) NULL COMMENT '标签(逗号分隔)',
    `usageCount`    BIGINT       NOT NULL DEFAULT 0 COMMENT '使用次数',
    `status`        INT          NOT NULL DEFAULT 1 COMMENT '状态(0-下架 1-上架)',
    `userId`        BIGINT       NOT NULL COMMENT '创建用户id',
    `editTime`      DATETIME     NULL COMMENT '编辑时间',
    `createTime`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category`),
    KEY `idx_userId` (`userId`),
    KEY `idx_skillName` (`skillName`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '技能';

DROP TABLE IF EXISTS `material`;
CREATE TABLE `material`
(
    `id`         BIGINT       NOT NULL COMMENT '主键',
    `name`       VARCHAR(256) NOT NULL COMMENT '素材名称',
    `type`       VARCHAR(32)  NOT NULL DEFAULT 'other' COMMENT '素材类型(image/video/audio/3d/other)',
    `url`        VARCHAR(1024) NOT NULL COMMENT '素材访问地址',
    `size`       BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
    `userId`     BIGINT       NOT NULL COMMENT '上传用户id',
    `createTime` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_userId` (`userId`),
    KEY `idx_type` (`type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '素材';

-- ----------------------------
-- 素材文件夹表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `material_folder` (
  `id` BIGINT NOT NULL COMMENT 'id',
  `name` VARCHAR(128) NULL DEFAULT NULL COMMENT '文件夹名称',
  `userId` BIGINT NULL DEFAULT NULL COMMENT '创建用户 id',
  `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='素材文件夹';

-- 素材表增加 folderId 字段
ALTER TABLE `material` ADD COLUMN `folderId` BIGINT NULL DEFAULT NULL COMMENT '所属文件夹 id';

-- ----------------------------
-- 初始化管理员账号
-- 密码：12345678（加密规则为 md5(password + "yupi")）
-- ----------------------------
INSERT INTO `user` (`id`, `userAccount`, `userPassword`, `userName`, `userRole`, `createTime`, `updateTime`)
VALUES (1, 'admin', '10670d38ec32fa8102be6a37f8cb52bf', '管理员', 'admin', NOW(), NOW());

-- ----------------------------
-- 交易订单表（支付沙箱：当前渠道固定 MOCK）
-- ----------------------------
DROP TABLE IF EXISTS `trade_order`;
CREATE TABLE `trade_order`
(
    `id`          BIGINT       NOT NULL COMMENT 'id（雪花）',
    `orderNo`     VARCHAR(64)  NOT NULL COMMENT '订单号',
    `userId`      BIGINT       NULL     COMMENT '下单用户 id',
    `productType` VARCHAR(32)  NULL     COMMENT '商品类型：MEMBERSHIP/SECONDS/CARD',
    `productCode` VARCHAR(64)  NULL     COMMENT '商品编码',
    `productName` VARCHAR(128) NULL     COMMENT '商品名称',
    `quantity`    INT          NULL     COMMENT '购买数量',
    `amount`      DECIMAL(12, 2) NULL   COMMENT '订单金额',
    `currency`    VARCHAR(8)   NULL     DEFAULT 'CNY' COMMENT '币种',
    `status`      VARCHAR(16)  NULL     COMMENT '订单状态：PENDING/PAID/CANCELLED/EXPIRED',
    `channel`     VARCHAR(16)  NULL     COMMENT '支付渠道',
    `payTradeNo`  VARCHAR(64)  NULL     COMMENT '支付流水号',
    `expireTime`  DATETIME     NULL     COMMENT '订单过期时间',
    `payTime`     DATETIME     NULL     COMMENT '支付时间',
    `createTime`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_orderNo` (`orderNo`),
    KEY `idx_userId` (`userId`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '交易订单';

-- ----------------------------
-- 社区 - 帖子表
-- ----------------------------
DROP TABLE IF EXISTS `community_post`;
CREATE TABLE `community_post`
(
    `id`           BIGINT       NOT NULL COMMENT '主键（雪花算法）',
    `title`        VARCHAR(256) NOT NULL COMMENT '帖子标题',
    `content`      TEXT         NOT NULL COMMENT '帖子内容',
    `category`     VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '分类(official_tips/suggest/feedback/other)',
    `tags`         VARCHAR(512) NULL     DEFAULT NULL COMMENT '标签(逗号分隔)',
    `coverImage`   VARCHAR(1024) NULL    DEFAULT NULL COMMENT '封面图片',
    `viewCount`    INT          NOT NULL DEFAULT 0 COMMENT '浏览数',
    `likeCount`    INT          NOT NULL DEFAULT 0 COMMENT '点赞数',
    `commentCount` INT          NOT NULL DEFAULT 0 COMMENT '评论数',
    `status`       INT          NOT NULL DEFAULT 1 COMMENT '状态(0-隐藏 1-公开)',
    `userId`       BIGINT       NOT NULL COMMENT '发布用户ID',
    `editTime`     DATETIME     NULL DEFAULT NULL COMMENT '编辑时间',
    `createTime`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_userId` (`userId`),
    KEY `idx_category` (`category`),
    KEY `idx_createTime` (`createTime`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '社区帖子表';

-- ----------------------------
-- 社区 - 评论表
-- ----------------------------
DROP TABLE IF EXISTS `community_comment`;
CREATE TABLE `community_comment`
(
    `id`         BIGINT       NOT NULL COMMENT '主键（雪花算法）',
    `content`    TEXT         NOT NULL COMMENT '评论内容',
    `postId`     BIGINT       NOT NULL COMMENT '帖子ID',
    `parentId`   BIGINT       NULL DEFAULT 0 COMMENT '父评论ID(回复，一级评论为0)',
    `userId`     BIGINT       NOT NULL COMMENT '评论用户ID',
    `createTime` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_postId` (`postId`),
    KEY `idx_userId` (`userId`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '社区评论表';

-- ----------------------------
-- 社区 - 点赞表
-- ----------------------------
DROP TABLE IF EXISTS `community_like`;
CREATE TABLE `community_like`
(
    `id`         BIGINT   NOT NULL COMMENT '主键（雪花算法）',
    `postId`     BIGINT   NOT NULL COMMENT '帖子ID',
    `userId`     BIGINT   NOT NULL COMMENT '用户ID',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_user` (`postId`, `userId`),
    KEY `idx_userId` (`userId`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '社区点赞表';

-- ----------------------------
-- 社区 - 浏览足迹表
-- ----------------------------
DROP TABLE IF EXISTS `community_footprint`;
CREATE TABLE `community_footprint`
(
    `id`         BIGINT   NOT NULL COMMENT '主键（雪花算法）',
    `postId`     BIGINT   NOT NULL COMMENT '帖子ID',
    `userId`     BIGINT   NOT NULL COMMENT '用户ID',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_post` (`postId`, `userId`),
    KEY `idx_userId` (`userId`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '社区浏览足迹表';

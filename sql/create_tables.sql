-- 技能中心 - 技能表
CREATE TABLE IF NOT EXISTS `skill` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `skillName` VARCHAR(256) NOT NULL COMMENT '技能名称',
    `skillDesc` VARCHAR(1024) DEFAULT NULL COMMENT '技能描述',
    `icon` VARCHAR(64) DEFAULT NULL COMMENT '图标',
    `category` VARCHAR(64) NOT NULL COMMENT '分类(kling/create/understand/voice/search/office/design/pay/auth/map/billing)',
    `price` VARCHAR(32) DEFAULT '免费' COMMENT '价格',
    `originalPrice` VARCHAR(32) DEFAULT NULL COMMENT '原价',
    `priceUnit` VARCHAR(32) DEFAULT NULL COMMENT '价格单位',
    `tags` VARCHAR(512) DEFAULT NULL COMMENT '标签(逗号分隔)',
    `usageCount` BIGINT DEFAULT 0 COMMENT '使用次数',
    `status` INT DEFAULT 1 COMMENT '状态(0-下架 1-上架)',
    `userId` BIGINT NOT NULL COMMENT '创建用户ID',
    `editTime` DATETIME DEFAULT NULL COMMENT '编辑时间',
    `createTime` DATETIME DEFAULT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT NULL COMMENT '更新时间',
    `isDelete` TINYINT DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    INDEX `idx_category` (`category`),
    INDEX `idx_userId` (`userId`),
    INDEX `idx_skillName` (`skillName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='技能表';

-- 素材库 - 素材表
CREATE TABLE IF NOT EXISTS `material` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `materialName` VARCHAR(256) NOT NULL COMMENT '素材名称',
    `fileUrl` VARCHAR(1024) NOT NULL COMMENT '文件URL',
    `fileType` VARCHAR(32) NOT NULL COMMENT '文件类型(image/video/audio/3d/other)',
    `fileSize` BIGINT DEFAULT 0 COMMENT '文件大小(字节)',
    `folderId` BIGINT DEFAULT NULL COMMENT '文件夹ID',
    `tags` VARCHAR(512) DEFAULT NULL COMMENT '标签(逗号分隔)',
    `userId` BIGINT NOT NULL COMMENT '上传用户ID',
    `editTime` DATETIME DEFAULT NULL COMMENT '编辑时间',
    `createTime` DATETIME DEFAULT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT NULL COMMENT '更新时间',
    `isDelete` TINYINT DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    INDEX `idx_userId` (`userId`),
    INDEX `idx_fileType` (`fileType`),
    INDEX `idx_folderId` (`folderId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='素材表';

-- 素材库 - 文件夹表
CREATE TABLE IF NOT EXISTS `material_folder` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `folderName` VARCHAR(256) NOT NULL COMMENT '文件夹名称',
    `userId` BIGINT NOT NULL COMMENT '创建用户ID',
    `createTime` DATETIME DEFAULT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT NULL COMMENT '更新时间',
    `isDelete` TINYINT DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    INDEX `idx_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='素材文件夹表';

-- 社区 - 帖子表
CREATE TABLE IF NOT EXISTS `community_post` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title` VARCHAR(256) NOT NULL COMMENT '帖子标题',
    `content` TEXT NOT NULL COMMENT '帖子内容',
    `category` VARCHAR(32) NOT NULL COMMENT '分类(official_tips/suggest/feedback/other)',
    `tags` VARCHAR(512) DEFAULT NULL COMMENT '标签(逗号分隔)',
    `coverImage` VARCHAR(1024) DEFAULT NULL COMMENT '封面图片',
    `viewCount` INT DEFAULT 0 COMMENT '浏览数',
    `likeCount` INT DEFAULT 0 COMMENT '点赞数',
    `commentCount` INT DEFAULT 0 COMMENT '评论数',
    `status` INT DEFAULT 1 COMMENT '状态(0-隐藏 1-公开)',
    `userId` BIGINT NOT NULL COMMENT '发布用户ID',
    `editTime` DATETIME DEFAULT NULL COMMENT '编辑时间',
    `createTime` DATETIME DEFAULT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT NULL COMMENT '更新时间',
    `isDelete` TINYINT DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    INDEX `idx_userId` (`userId`),
    INDEX `idx_category` (`category`),
    INDEX `idx_createTime` (`createTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区帖子表';

-- 社区 - 评论表
CREATE TABLE IF NOT EXISTS `community_comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `postId` BIGINT NOT NULL COMMENT '帖子ID',
    `parentId` BIGINT DEFAULT NULL COMMENT '父评论ID(回复)',
    `userId` BIGINT NOT NULL COMMENT '评论用户ID',
    `createTime` DATETIME DEFAULT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT NULL COMMENT '更新时间',
    `isDelete` TINYINT DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    INDEX `idx_postId` (`postId`),
    INDEX `idx_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区评论表';

-- 社区 - 点赞表
CREATE TABLE IF NOT EXISTS `community_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `postId` BIGINT NOT NULL COMMENT '帖子ID',
    `userId` BIGINT NOT NULL COMMENT '用户ID',
    `createTime` DATETIME DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_post_user` (`postId`, `userId`),
    INDEX `idx_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区点赞表';

-- 社区 - 浏览足迹表（主键为雪花 ID，由 MyBatis-Flex 的 id 生成策略写入，不用 AUTO_INCREMENT）
CREATE TABLE IF NOT EXISTS `community_footprint` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `postId` BIGINT NOT NULL COMMENT '帖子ID',
    `userId` BIGINT NOT NULL COMMENT '用户ID',
    `createTime` DATETIME DEFAULT NULL COMMENT '浏览时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_user_post` (`postId`, `userId`),
    INDEX `idx_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区浏览足迹表';

-- 交易订单（支付沙箱 / 真实支付渠道共用）
CREATE TABLE IF NOT EXISTS `trade_order` (
    `id` BIGINT NOT NULL COMMENT '主键（雪花算法）',
    `orderNo` VARCHAR(64) NOT NULL COMMENT '商户订单号',
    `userId` BIGINT NOT NULL COMMENT '用户ID',
    `productType` VARCHAR(64) DEFAULT NULL COMMENT '商品类型：MEMBERSHIP / SECONDS / CARD',
    `productCode` VARCHAR(64) DEFAULT NULL COMMENT '商品编码',
    `productName` VARCHAR(256) DEFAULT NULL COMMENT '商品名称',
    `quantity` INT DEFAULT NULL COMMENT '购买数量',
    `amount` DECIMAL(10,2) DEFAULT NULL COMMENT '订单金额',
    `currency` VARCHAR(16) DEFAULT NULL COMMENT '币种',
    `status` VARCHAR(32) DEFAULT NULL COMMENT '订单状态：PENDING / PAID',
    `channel` VARCHAR(32) DEFAULT NULL COMMENT '支付渠道：MOCK / REAL',
    `payTradeNo` VARCHAR(128) DEFAULT NULL COMMENT '渠道交易流水号',
    `expireTime` DATETIME DEFAULT NULL COMMENT '订单过期时间',
    `payTime` DATETIME DEFAULT NULL COMMENT '支付时间',
    `createTime` DATETIME DEFAULT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT NULL COMMENT '更新时间',
    `isDelete` TINYINT DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_orderNo` (`orderNo`),
    INDEX `idx_userId` (`userId`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='交易订单表';

-- 用户表新增字段：手机短信验证码登录 + 微信扫码登录（2026-08-31）
ALTER TABLE `user` ADD COLUMN `userPhone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    ADD UNIQUE KEY `uk_userPhone` (`userPhone`);
ALTER TABLE `user` ADD COLUMN `wechatOpenId` VARCHAR(64) DEFAULT NULL COMMENT '微信OpenID',
    ADD UNIQUE KEY `uk_wechatOpenId` (`wechatOpenId`);
ALTER TABLE `user` ADD COLUMN `qqOpenId` VARCHAR(64) DEFAULT NULL COMMENT 'QQ OpenID',
    ADD UNIQUE KEY `uk_qqOpenId` (`qqOpenId`);

-- 用户表新增字段：邮箱（用于邮箱验证码找回密码，2026-09-03）
ALTER TABLE `user` ADD COLUMN `userEmail` VARCHAR(256) DEFAULT NULL COMMENT '邮箱（用于邮箱验证码找回密码）',
    ADD UNIQUE KEY `uk_userEmail` (`userEmail`);

-- 操作日志表（2026-09-01 新增，管理后台操作审计 / 仪表盘最近动态）
CREATE TABLE IF NOT EXISTS `operation_log` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `userId` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `userName` VARCHAR(64) DEFAULT NULL COMMENT '操作人昵称',
    `module` VARCHAR(64) DEFAULT NULL COMMENT '模块名，如 user/app/post',
    `operation` VARCHAR(128) DEFAULT NULL COMMENT '操作类型，如 新增/删除/审核',
    `targetId` VARCHAR(64) DEFAULT NULL COMMENT '操作对象ID',
    `detail` VARCHAR(1024) DEFAULT NULL COMMENT '操作详情',
    `ip` VARCHAR(64) DEFAULT NULL COMMENT '操作IP',
    `status` TINYINT DEFAULT 1 COMMENT '0失败 1成功',
    `errorMsg` VARCHAR(1024) DEFAULT NULL COMMENT '错误信息',
    `createTime` DATETIME DEFAULT NULL COMMENT '操作时间',
    PRIMARY KEY (`id`),
    INDEX `idx_userId` (`userId`),
    INDEX `idx_createTime` (`createTime`),
    INDEX `idx_module` (`module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志表';

-- 系统配置表（2026-09-01 新增，后台系统设置）
CREATE TABLE IF NOT EXISTS `sys_config` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `configKey` VARCHAR(128) NOT NULL COMMENT '配置键',
    `configValue` VARCHAR(2048) DEFAULT NULL COMMENT '配置值',
    `configName` VARCHAR(128) DEFAULT NULL COMMENT '配置名称',
    `configType` VARCHAR(32) DEFAULT 'string' COMMENT '值类型：string/number/boolean',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '说明',
    `createTime` DATETIME DEFAULT NULL,
    `updateTime` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_configKey` (`configKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统配置表';

-- 系统配置初始化数据（id 固定 1~9 仅为初始化方便，后续新增配置走雪花 ID；重复执行用 ON DUPLICATE KEY 兜底）
INSERT INTO `sys_config` (`id`,`configKey`,`configValue`,`configName`,`configType`,`description`,`createTime`,`updateTime`) VALUES
(1,'site.name','秒哒','站点名称','string','平台显示名称',NOW(),NOW()),
(2,'site.registerEnabled','true','开放注册','boolean','是否允许新用户注册',NOW(),NOW()),
(3,'community.enabled','true','开放社区','boolean','是否开放社区功能',NOW(),NOW()),
(4,'user.giftSeconds','100','新用户赠送秒点','number','注册时赠送的秒点数量',NOW(),NOW()),
(5,'ai.modelName','deepseek-chat','AI 模型','string','当前使用的对话模型',NOW(),NOW()),
(6,'upload.maxSizeMb','20','单文件上传上限(MB)','number','素材上传大小限制',NOW(),NOW()),
(7,'skill.banners','[{"title":"可灵专区 · 限时 8.5 折","desc":"秒哒 × 可灵联合推出限时优惠，8月15日-9月15日期间，调用可灵技能享 8.5 折。","emoji":"🎀"},{"title":"three.js 3D 创作","desc":"用 three.js 在网页里构建三维场景、动态视觉与沉浸式交互体验，社区技能免费使用。","emoji":"🧊"},{"title":"登录能力免费接入","desc":"支持用户名、邮箱、手机号、第三方账号及微信登录，一键接入你的应用，完全免费。","emoji":"🔐"}]','技能中心 Banner','json','技能中心轮播配置，JSON 数组（title/desc/emoji）',NOW(),NOW()),
(8,'skill.categories','[{"key":"all","label":"全部"},{"key":"kling","label":"可灵专区"},{"key":"create","label":"内容创作与生成"},{"key":"understand","label":"内容理解与处理"},{"key":"voice","label":"语音交互"},{"key":"search","label":"搜索查询"},{"key":"office","label":"办公提效"},{"key":"design","label":"设计美化"},{"key":"pay","label":"支付交易"},{"key":"auth","label":"登录验证"},{"key":"map","label":"地图出行"},{"key":"billing","label":"计费"},{"key":"allMore","label":"全部 ▾"}]','技能中心分类','json','技能中心分类配置，JSON 数组（key/label）',NOW(),NOW()),
(9,'skill.quota','[{"label":"视频生成类","used":1,"total":1},{"label":"图片生成类","used":5,"total":5},{"label":"其他类","used":100,"total":100}]','技能中心免费额度','json','技能中心右侧额度卡配置，JSON 数组（label/used/total）',NOW(),NOW())
ON DUPLICATE KEY UPDATE configKey=configKey;

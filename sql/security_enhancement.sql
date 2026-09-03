-- 安全增强 SQL 脚本
-- 1. 审计日志表
-- 2. 密码迁移兼容说明（MD5 -> BCrypt 自动迁移，无需手动 SQL）

-- ============================================
-- 1. 审计日志表
-- ============================================
CREATE TABLE IF NOT EXISTS `audit_log` (
  `id` BIGINT NOT NULL COMMENT '主键 ID',
  `actionType` VARCHAR(50) NOT NULL COMMENT '操作类型：LOGIN/LOGOUT/REGISTER/PASSWORD_CHANGE/ADMIN_OP 等',
  `actionDesc` VARCHAR(200) DEFAULT '' COMMENT '操作描述',
  `userId` BIGINT DEFAULT NULL COMMENT '操作人用户 ID',
  `userAccount` VARCHAR(64) DEFAULT '' COMMENT '操作人账号',
  `clientIp` VARCHAR(64) DEFAULT '' COMMENT '客户端 IP',
  `userAgent` VARCHAR(500) DEFAULT '' COMMENT 'User-Agent',
  `requestMethod` VARCHAR(10) DEFAULT '' COMMENT '请求方法（GET/POST）',
  `requestUri` VARCHAR(255) DEFAULT '' COMMENT '请求 URI',
  `requestParams` TEXT COMMENT '请求参数（JSON 格式，脱敏）',
  `resultStatus` VARCHAR(20) DEFAULT '' COMMENT '操作结果：SUCCESS/FAIL',
  `failReason` VARCHAR(500) DEFAULT '' COMMENT '失败原因',
  `costMs` BIGINT DEFAULT 0 COMMENT '操作耗时（毫秒）',
  `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_userId` (`userId`),
  KEY `idx_actionType` (`actionType`),
  KEY `idx_createTime` (`createTime`),
  KEY `idx_clientIp` (`clientIp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='安全审计日志表';

-- ============================================
-- 2. 密码迁移说明
-- ============================================
-- 密码加密方式已从 MD5 + 固定盐 升级为 BCrypt
-- 无需手动迁移：
-- - 新注册用户：直接使用 BCrypt 加密
-- - 已有用户：首次登录时自动检测 MD5 格式，验证通过后自动升级为 BCrypt
-- - 回滚方案：如遇问题可降级，代码保留了 MD5 验证逻辑

-- ============================================
-- 3. 登录失败计数（用于防暴力破解）
-- ============================================
-- 登录失败次数使用 Redis 存储，无需建表
-- 规则：同一账号 + IP 5 分钟内连续失败 5 次，锁定 15 分钟

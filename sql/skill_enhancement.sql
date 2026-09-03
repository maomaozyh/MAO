-- ============================================
-- 技能增强：做实技能能力
-- 1. skill 表新增字段：系统提示词、模型类型、温度、功能介绍、使用说明、技能编码
-- 2. app 表新增 skillId 字段，绑定创建时使用的技能
-- ============================================

-- 技能表新增字段
ALTER TABLE `skill`
    ADD COLUMN `skillCode` VARCHAR(64) DEFAULT NULL COMMENT '技能编码（唯一标识，用于程序调用）' AFTER `skillName`,
    ADD COLUMN `systemPrompt` TEXT DEFAULT NULL COMMENT '系统提示词（技能专属角色设定）' AFTER `tags`,
    ADD COLUMN `modelType` VARCHAR(32) DEFAULT 'DEFAULT' COMMENT '模型类型：DEFAULT-默认 REASONING-推理模型' AFTER `systemPrompt`,
    ADD COLUMN `temperature` DECIMAL(3,2) DEFAULT NULL COMMENT '采样温度，0-1，null 用模型默认值' AFTER `modelType`,
    ADD COLUMN `featureDesc` VARCHAR(2048) DEFAULT NULL COMMENT '功能介绍' AFTER `temperature`,
    ADD COLUMN `usageDesc` VARCHAR(2048) DEFAULT NULL COMMENT '使用说明' AFTER `featureDesc`;

-- 技能编码唯一索引
CREATE UNIQUE INDEX `idx_skillCode` ON `skill` (`skillCode`);

-- app 表新增 skillId
ALTER TABLE `app`
    ADD COLUMN `skillId` BIGINT DEFAULT NULL COMMENT '使用的技能ID' AFTER `category`;

CREATE INDEX `idx_skillId` ON `app` (`skillId`);

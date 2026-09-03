-- 用户表新增字段：QQ 互联扫码登录（2026-09-03）
-- 说明：MySQL BTREE 唯一索引允许多个 NULL 值，历史 qqOpenId 为 NULL 的行不会受影响。
-- 前提：执行前请确保不存在两个非 NULL 且相同的 qqOpenId（QQ 用户按 openid 唯一识别，正常不会出现）。
ALTER TABLE `user` ADD COLUMN `qqOpenId` VARCHAR(64) DEFAULT NULL COMMENT 'QQ OpenID',
    ADD UNIQUE KEY `uk_qqOpenId` (`qqOpenId`);

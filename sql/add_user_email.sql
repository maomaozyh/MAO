-- 用户表新增邮箱字段（用于「邮箱验证码找回密码」功能）
-- 2026-09-03
-- 说明：userEmail 允许为空（存量账号未绑定邮箱），因此 UNIQUE KEY 不会因多个 NULL 报错（MySQL 将 NULL 视为互异）。

USE `yu_ai_code_mother`;

ALTER TABLE `user` ADD COLUMN `userEmail` VARCHAR(256) DEFAULT NULL COMMENT '邮箱（用于邮箱验证码找回密码）';
ALTER TABLE `user` ADD UNIQUE KEY `uk_userEmail` (`userEmail`);

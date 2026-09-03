-- 为已有数据库追加会员/秒点字段（在 yu_ai_code_mother 库中执行，重复执行若列已存在会报错可忽略）
USE `yu_ai_code_mother`;

ALTER TABLE `user`
    ADD COLUMN `membershipTier` VARCHAR(64) NOT NULL DEFAULT 'FREE' COMMENT '会员等级' AFTER `userRole`,
    ADD COLUMN `secondsBalance` BIGINT NOT NULL DEFAULT 0 COMMENT '秒点余额' AFTER `membershipTier`,
    ADD COLUMN `membershipExpireTime` DATETIME NULL COMMENT '会员到期时间' AFTER `secondsBalance`;

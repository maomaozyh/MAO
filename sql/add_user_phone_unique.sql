-- 为 user 表的 userPhone 增加唯一索引，支撑短信登录「按手机号去重」与登录热查询加速。
-- 说明：MySQL BTREE 唯一索引允许多个 NULL 值，历史 userPhone 为 NULL 的行不会受影响。
-- 前提：执行前请确保不存在两个非 NULL 且相同的 userPhone（代码层已做去重，正常不会出现）。
--       若 ALTER 因重复值报错，先清理重复数据再执行。
ALTER TABLE `user` ADD UNIQUE KEY `uk_user_phone` (`userPhone`);

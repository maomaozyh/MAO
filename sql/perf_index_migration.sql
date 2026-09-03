-- 性能优化：索引补齐迁移脚本
-- 注意：MySQL 8.0 的 ADD INDEX 不支持 IF NOT EXISTS，重复执行前请先 SHOW INDEX 确认。
-- 执行方式：mysql -uroot -p yu_ai_code_mother < perf_index_migration.sql

-- 1. 会员月度发放调度（GiftSecondsScheduler 每小时跑一次）：
--    按 membershipTier != 'FREE' AND membershipExpireTime > now() AND lastGiftMonth 过滤，
--    原来三列均无索引，每次全表扫描 user 表。
ALTER TABLE `user` ADD INDEX idx_membership_gift (membershipTier, membershipExpireTime, lastGiftMonth);

-- 2. 帖子评论列表：查「某帖子的一级评论按时间倒序」（WHERE postId=? AND parentId=0 ORDER BY createTime DESC），
--    原来只有单列 idx_postId，排序需额外 filesort。
ALTER TABLE community_comment ADD INDEX idx_post_parent_time (postId, parentId, createTime);

-- 3. 秒点流水分页：查「我的流水按时间倒序分页」（WHERE userId=? ORDER BY createTime DESC），
--    原来只有单列 idx_userId，排序需额外 filesort。
ALTER TABLE seconds_record ADD INDEX idx_user_time (userId, createTime);

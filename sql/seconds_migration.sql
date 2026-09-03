-- 秒点扣费功能：存量数据库迁移脚本（幂等，可重复执行）
-- 新建库已随 create_table.sql 建好，只需执行本文件的 sys_config 部分。

-- 1. user 表新增赠送额度字段
--    giftSecondsBalance：赠送的秒点额度，消费时优先于购买的 secondsBalance 扣除
--    lastGiftMonth：上次发放赠送秒点的月份（YYYY-MM），保证每月发放只执行一次
ALTER TABLE user ADD COLUMN giftSecondsBalance BIGINT DEFAULT 0 NOT NULL COMMENT '赠送秒点额度（优先于购买余额扣除）';
ALTER TABLE user ADD COLUMN lastGiftMonth VARCHAR(7) NULL COMMENT '上次发放赠送秒点的月份(YYYY-MM)';

-- 2. 秒点计费单价配置（JSON，单位：点/次，可在「系统设置」后台调整）
--    键说明：genCode=代码生成 image=图片 video=视频 model3d=3D模型 ppt=PPT大纲
--            expand=描述扩写 semanticSearch=语义搜索 selfCheck=代码自查
INSERT INTO sys_config (`id`,`configKey`,`configValue`,`configName`,`configType`,`description`,`createTime`,`updateTime`) VALUES
(10,'seconds.price','{"genCode":10,"image":20,"video":100,"model3d":150,"ppt":30,"expand":2,"semanticSearch":2,"selfCheck":2}','秒点计费单价','json','各 AI 能力的秒点扣费单价（点/次），JSON',NOW(),NOW())
ON DUPLICATE KEY UPDATE `configValue`=VALUES(`configValue`);

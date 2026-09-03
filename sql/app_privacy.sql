-- 应用公开/私密开关
-- 需求：用户可逐应用设置「公开 / 私密」，私密应用仅本人与管理员可见，
--       不会出现在「广场 / 发现 / 语义搜索」中（默认公开）。
-- 1. 新增字段（历史数据默认置为公开 1）
ALTER TABLE `app`
    ADD COLUMN `isPublic` TINYINT NOT NULL DEFAULT 1 COMMENT '是否公开：1-公开，0-私密'
    AFTER `status`;

-- 2. 历史数据兜底（极端情况下 ALTER 未自动回填时）
UPDATE `app` SET `isPublic` = 1 WHERE `isPublic` IS NULL;

-- 3. 提升「广场 / 发现 / 语义搜索」按公开状态过滤的查询性能
CREATE INDEX `idx_isPublic` ON `app` (`isPublic`);

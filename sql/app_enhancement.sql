-- 应用管理完善：新增 status 字段，补充分类和技能的查询能力
-- 1. 新增应用状态字段（0=禁用/下架，1=启用/正常，默认 1）
ALTER TABLE `app`
    ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT '应用状态：0-禁用，1-启用'
    AFTER `priority`;

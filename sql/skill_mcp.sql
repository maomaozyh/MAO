-- 技能 MCP 集成：skill 表新增 mcpServers 字段
-- 每个技能可以独立配置一组 MCP 服务器，AI 生成时自动注入对应工具

ALTER TABLE `skill`
    ADD COLUMN `mcpServers` TEXT NULL COMMENT 'MCP 服务配置（JSON 数组，每个元素含 name/type/url/headers）'
    AFTER `usageDesc`;

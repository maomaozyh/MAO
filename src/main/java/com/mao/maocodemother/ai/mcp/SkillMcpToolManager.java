package com.mao.maocodemother.ai.mcp;

import cn.hutool.core.util.StrUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mao.maocodemother.model.entity.Skill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 技能 MCP 工具管理器
 * 根据技能的 mcpServers 配置，动态加载 MCP 服务器提供的工具
 * 按技能 ID 缓存，避免重复连接
 */
@Slf4j
@Component
public class SkillMcpToolManager {

    /**
     * MCP 工具提供者缓存：skillId -> SkillMcpToolProvider
     * 最大缓存 50 个技能的 MCP 连接，访问后 30 分钟过期
     */
    private final Cache<Long, SkillMcpToolProvider> mcpCache = Caffeine.newBuilder()
            .maximumSize(50)
            .expireAfterAccess(Duration.ofMinutes(30))
            .removalListener((key, value, cause) -> {
                log.info("MCP 连接被移除，skillId={}, 原因={}", key, cause);
                if (value instanceof SkillMcpToolProvider provider) {
                    try {
                        provider.close();
                    } catch (Exception e) {
                        log.warn("关闭 MCP 工具提供者失败，skillId={}", key, e);
                    }
                }
            })
            .build();

    /**
     * 获取技能对应的 MCP 工具提供者
     *
     * @param skill 技能实体
     * @return SkillMcpToolProvider，若无 MCP 配置则返回 null
     */
    public SkillMcpToolProvider getMcpToolProvider(Skill skill) {
        if (skill == null || skill.getId() == null || StrUtil.isBlank(skill.getMcpServers())) {
            return null;
        }
        try {
            return mcpCache.get(skill.getId(), id -> createMcpToolProvider(skill));
        } catch (Exception e) {
            log.error("获取技能 MCP 工具提供者失败，skillId={}", skill.getId(), e);
            return null;
        }
    }

    /**
     * 刷新技能的 MCP 连接（配置变更时调用）
     */
    public void refresh(Long skillId) {
        if (skillId != null) {
            mcpCache.invalidate(skillId);
            log.info("已刷新技能 MCP 缓存，skillId={}", skillId);
        }
    }

    /**
     * 根据技能配置创建 MCP 工具提供者
     */
    private SkillMcpToolProvider createMcpToolProvider(Skill skill) {
        String mcpServersJson = skill.getMcpServers();
        if (StrUtil.isBlank(mcpServersJson)) {
            return null;
        }

        try {
            SkillMcpToolProvider provider = new SkillMcpToolProvider(skill);
            // 不在此处初始化（建立 SSE 连接），延迟到首次 provideTools 时执行
            // 避免服务启动或缓存加载时阻塞
            log.info("技能 {} 的 MCP 工具提供者创建成功（待懒加载初始化）", skill.getSkillName());
            return provider;
        } catch (Exception e) {
            log.error("创建技能 MCP 工具提供者失败，skillId={}", skill.getId(), e);
            return null;
        }
    }
}

package com.mao.maocodemother.core.ai;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mao.maocodemother.mapper.SkillMapper;
import com.mao.maocodemother.model.entity.ChatHistory;
import com.mao.maocodemother.model.entity.Skill;
import com.mao.maocodemother.model.enums.ChatHistoryMessageTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 提示词构建器
 * 统一组装：
 * 1. 多轮对话上下文（【对话历史】 + 【当前需求】）
 * 2. 迭代修改提示（历史中存在 AI 生成内容时追加）
 * 3. 自动技能匹配（优先查询技能表，无数据时使用内置关键词映射兜底）
 */
@Slf4j
@Component
public class AiPromptBuilder {

    /**
     * 单次携带的最大历史消息条数
     */
    public static final int MAX_HISTORY_MESSAGES = 10;

    /**
     * 单条消息计入上下文的最大长度（字符），避免上下文膨胀
     */
    private static final int MAX_MESSAGE_LENGTH = 2000;

    /**
     * 最多附加到提示词的匹配技能数量
     */
    private static final int MAX_MATCHED_SKILLS = 5;

    /**
     * 内置「关键词 -> 技能说明」映射（兜底：技能表无数据或无法匹配时使用）
     */
    private static final Map<String, String> BUILT_IN_SKILL_KEYWORDS = new LinkedHashMap<>();

    static {
        BUILT_IN_SKILL_KEYWORDS.put("视频", "可灵视频生成能力：可根据文本或图片生成/编辑短视频");
        BUILT_IN_SKILL_KEYWORDS.put("图片", "图像生成能力：可根据文本描述生成精美图片");
        BUILT_IN_SKILL_KEYWORDS.put("图像", "图像生成能力：可根据文本描述生成精美图片");
        BUILT_IN_SKILL_KEYWORDS.put("海报", "海报设计能力：可一键生成营销海报");
        BUILT_IN_SKILL_KEYWORDS.put("PPT", "演示文稿生成能力：可根据主题生成结构化 PPT");
        BUILT_IN_SKILL_KEYWORDS.put("演示文稿", "演示文稿生成能力：可根据主题生成结构化 PPT");
        BUILT_IN_SKILL_KEYWORDS.put("3D", "three.js 三维能力：可在页面中渲染 3D 场景");
        BUILT_IN_SKILL_KEYWORDS.put("三维", "three.js 三维能力：可在页面中渲染 3D 场景");
        BUILT_IN_SKILL_KEYWORDS.put("地图", "地图服务能力：可集成地图展示与位置标注");
        BUILT_IN_SKILL_KEYWORDS.put("动画", "动画生成能力：可生成/播放动画效果");
        BUILT_IN_SKILL_KEYWORDS.put("音乐", "音乐生成能力：可生成背景音乐");
        BUILT_IN_SKILL_KEYWORDS.put("配音", "语音合成能力：可将文本转换为自然语音");
        BUILT_IN_SKILL_KEYWORDS.put("语音", "语音合成能力：可将文本转换为自然语音");
        BUILT_IN_SKILL_KEYWORDS.put("logo", "Logo 生成能力：可为品牌生成 Logo 标识");
        BUILT_IN_SKILL_KEYWORDS.put("图表", "图表生成能力：可生成数据可视化图表");
        BUILT_IN_SKILL_KEYWORDS.put("数据可视化", "图表生成能力：可生成数据可视化图表");
    }

    @Resource
    private SkillMapper skillMapper;

    /**
     * 组装最终传给 AI 生成器的增强提示词
     * （多轮上下文 + 迭代提示 + 自动技能匹配）
     *
     * @param currentMessage 用户当前需求
     * @param history        历史对话（按时间正序），可为空
     * @return 增强后的提示词
     */
    public String buildEnhancedPrompt(String currentMessage, List<ChatHistory> history) {
        List<String> matchedSkills = matchSkills(currentMessage, history);
        return buildPrompt(currentMessage, history, matchedSkills);
    }

    /**
     * 组装提示词正文
     */
    public String buildPrompt(String currentMessage, List<ChatHistory> history, List<String> matchedSkills) {
        StringBuilder sb = new StringBuilder();
        List<ChatHistory> limitedHistory = limitHistory(history);
        // 1. 可用能力（自动技能匹配结果）
        if (CollUtil.isNotEmpty(matchedSkills)) {
            sb.append("【可用能力】\n");
            for (String skillText : matchedSkills) {
                sb.append("- ").append(skillText).append('\n');
            }
            sb.append('\n');
        }
        // 2. 对话历史（多轮上下文记忆）
        if (CollUtil.isNotEmpty(limitedHistory)) {
            sb.append("【对话历史】\n");
            for (ChatHistory chatHistory : limitedHistory) {
                String role = ChatHistoryMessageTypeEnum.USER.getValue().equals(chatHistory.getMessageType()) ? "用户" : "AI";
                String message = sanitizeMessage(chatHistory.getMessage());
                if (StrUtil.isBlank(message)) {
                    continue;
                }
                sb.append(role).append(": ").append(message).append('\n');
            }
            sb.append('\n');
        }
        // 3. 迭代修改提示（历史中存在 AI 生成内容时追加）
        if (hasAiMessage(limitedHistory)) {
            sb.append("【迭代提示】\n");
            sb.append("用户正在对已生成的应用进行迭代修改，请在现有方案上调整，而不是重新生成整套。\n\n");
        }
        // 4. 当前需求
        sb.append("【当前需求】\n");
        sb.append(sanitizeMessage(currentMessage)).append('\n');
        return sb.toString();
    }

    /**
     * 自动匹配技能：
     * 扫描当前消息 + 历史消息中的关键词，优先从技能表匹配，无匹配或表无数据时使用内置映射兜底
     *
     * @param currentMessage 用户当前需求
     * @param history        历史对话
     * @return 匹配到的技能说明列表
     */
    public List<String> matchSkills(String currentMessage, List<ChatHistory> history) {
        // 扫描 message + 历史里的关键词
        StringBuilder textBuilder = new StringBuilder(StrUtil.blankToDefault(currentMessage, ""));
        if (CollUtil.isNotEmpty(history)) {
            for (ChatHistory chatHistory : history) {
                textBuilder.append('\n').append(StrUtil.blankToDefault(chatHistory.getMessage(), ""));
            }
        }
        String text = textBuilder.toString();
        List<String> matched = new ArrayList<>();
        // 1. 优先从技能表匹配（单体内存在 skill 表）
        List<Skill> dbSkills = queryEnabledSkills();
        if (CollUtil.isNotEmpty(dbSkills)) {
            for (Skill skill : dbSkills) {
                if (matched.size() >= MAX_MATCHED_SKILLS) {
                    break;
                }
                if (skillMatches(text, skill)) {
                    matched.add(buildSkillText(skill));
                }
            }
            if (!matched.isEmpty()) {
                return matched;
            }
        }
        // 2. 内置关键词映射兜底
        for (Map.Entry<String, String> entry : BUILT_IN_SKILL_KEYWORDS.entrySet()) {
            if (matched.size() >= MAX_MATCHED_SKILLS) {
                break;
            }
            if (text.contains(entry.getKey())) {
                matched.add(entry.getValue());
            }
        }
        return matched;
    }

    /**
     * 查询技能表中上架（status=1）的技能
     */
    private List<Skill> queryEnabledSkills() {
        try {
            return skillMapper.selectListByQuery(
                    QueryWrapper.create().eq("status", 1).limit(0, 100));
        } catch (Exception e) {
            // 技能表异常时降级为内置关键词匹配，不影响代码生成主流程
            log.warn("查询技能表失败，改用内置关键词匹配: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 判断技能是否命中文本（按技能名、标签、分类匹配）
     */
    private boolean skillMatches(String text, Skill skill) {
        if (skill == null) {
            return false;
        }
        String skillName = skill.getSkillName();
        if (StrUtil.isNotBlank(skillName) && StrUtil.containsIgnoreCase(text, skillName)) {
            return true;
        }
        String tags = skill.getTags();
        if (StrUtil.isNotBlank(tags)) {
            for (String tag : tags.split("[,，、;；/\\\\s]+")) {
                if (StrUtil.isNotBlank(tag) && text.contains(tag.trim())) {
                    return true;
                }
            }
        }
        String category = skill.getCategory();
        return StrUtil.isNotBlank(category) && text.contains(category);
    }

    /**
     * 构造技能的提示词描述
     */
    private String buildSkillText(Skill skill) {
        String name = StrUtil.blankToDefault(skill.getSkillName(), "技能");
        String desc = StrUtil.blankToDefault(skill.getSkillDesc(), "已接入的 AI 能力");
        return name + "：" + desc;
    }

    /**
     * 限制历史消息条数（只保留最近的 MAX_HISTORY_MESSAGES 条）
     */
    private List<ChatHistory> limitHistory(List<ChatHistory> history) {
        if (CollUtil.isEmpty(history)) {
            return new ArrayList<>();
        }
        if (history.size() <= MAX_HISTORY_MESSAGES) {
            return history;
        }
        return new ArrayList<>(history.subList(history.size() - MAX_HISTORY_MESSAGES, history.size()));
    }

    /**
     * 历史中是否包含 AI 消息（用于追加迭代修改提示）
     */
    private boolean hasAiMessage(List<ChatHistory> history) {
        if (CollUtil.isEmpty(history)) {
            return false;
        }
        return history.stream()
                .anyMatch(h -> ChatHistoryMessageTypeEnum.AI.getValue().equals(h.getMessageType()));
    }

    /**
     * 消息清洗：去掉首尾空白、统一换行符、限制单条长度，避免上下文格式错乱
     */
    private String sanitizeMessage(String message) {
        if (StrUtil.isBlank(message)) {
            return "";
        }
        String trimmed = message.trim();
        if (trimmed.length() > MAX_MESSAGE_LENGTH) {
            trimmed = trimmed.substring(0, MAX_MESSAGE_LENGTH) + "…";
        }
        return trimmed.replace("\r\n", "\n");
    }
}

package com.mao.maocodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mao.maocodemother.exception.BusinessException;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.exception.ThrowUtils;
import com.mao.maocodemother.mapper.SkillMapper;
import com.mao.maocodemother.model.dto.skill.SkillAddRequest;
import com.mao.maocodemother.model.dto.skill.SkillQueryRequest;
import com.mao.maocodemother.model.dto.skill.SkillUpdateRequest;
import com.mao.maocodemother.model.entity.Skill;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.vo.SkillVO;
import com.mao.maocodemother.model.vo.UserVO;
import com.mao.maocodemother.service.SkillService;
import com.mao.maocodemother.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 技能 服务层实现（单体版）。
 * <p>
 * 与微服务版差异：登录态 / 用户资料直接走本地 {@link UserService}（单体无 Dubbo），
 * 去掉了向量入库（VectorSearchService）依赖——技能列表/详情展示不受影响。
 */
@Slf4j
@Service
public class SkillServiceImpl extends ServiceImpl<SkillMapper, Skill> implements SkillService {

    @Resource
    private UserService userService;

    @Override
    public Long createSkill(SkillAddRequest skillAddRequest, User loginUser) {
        ThrowUtils.throwIf(skillAddRequest == null, ErrorCode.PARAMS_ERROR);
        String skillName = skillAddRequest.getSkillName();
        ThrowUtils.throwIf(StrUtil.isBlank(skillName), ErrorCode.PARAMS_ERROR, "技能名称不能为空");
        skillName = skillName.trim(); // 去除首尾空格，避免"同名带空格"造成重复假象（ci collation 已大小写不敏感）
        // 防重复提交：同名且未删除的技能已存在则拒绝（管理员「新增技能」与「上传技能」共用此入口）
        long existCount = this.count(QueryWrapper.create().eq("skillName", skillName));
        ThrowUtils.throwIf(existCount > 0,
                ErrorCode.OPERATION_ERROR, "技能「" + skillName + "」已存在，请勿重复提交");
        Skill skill = new Skill();
        BeanUtil.copyProperties(skillAddRequest, skill);
        skill.setSkillName(skillName); // 使用 trim 后的名称落库
        skill.setUserId(loginUser.getId());
        // 默认上架、使用次数为 0
        skill.setStatus(skill.getStatus() == null ? 1 : skill.getStatus());
        skill.setUsageCount(skill.getUsageCount() == null ? 0L : skill.getUsageCount());
        skill.setEditTime(LocalDateTime.now());
        boolean result = this.save(skill);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return skill.getId();
    }

    @Override
    public Boolean updateSkill(SkillUpdateRequest skillUpdateRequest, User loginUser) {
        ThrowUtils.throwIf(skillUpdateRequest == null || skillUpdateRequest.getId() == null,
                ErrorCode.PARAMS_ERROR);
        Long id = skillUpdateRequest.getId();
        Skill oldSkill = this.getById(id);
        ThrowUtils.throwIf(oldSkill == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅管理员或创建者可更新
        if (!oldSkill.getUserId().equals(loginUser.getId())
                && !com.mao.maocodemother.constant.UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限修改该技能");
        }
        Skill skill = new Skill();
        BeanUtil.copyProperties(skillUpdateRequest, skill);
        skill.setEditTime(LocalDateTime.now());
        boolean result = this.updateById(skill);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return true;
    }

    @Override
    public Boolean deleteSkill(Long id, User loginUser) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Skill oldSkill = this.getById(id);
        ThrowUtils.throwIf(oldSkill == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅管理员或创建者可删除
        if (!oldSkill.getUserId().equals(loginUser.getId())
                && !com.mao.maocodemother.constant.UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限删除该技能");
        }
        boolean result = this.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return true;
    }

    @Override
    public SkillVO getSkillVOById(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Skill skill = this.getById(id);
        ThrowUtils.throwIf(skill == null, ErrorCode.NOT_FOUND_ERROR);
        return this.getSkillVO(skill);
    }

    @Override
    public Page<SkillVO> listSkillVOByPage(SkillQueryRequest skillQueryRequest) {
        ThrowUtils.throwIf(skillQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = skillQueryRequest.getPageNum();
        long pageSize = skillQueryRequest.getPageSize();
        QueryWrapper queryWrapper = this.getQueryWrapper(skillQueryRequest);
        Page<Skill> skillPage = this.page(Page.of(pageNum, pageSize), queryWrapper);
        Page<SkillVO> skillVOPage = new Page<>(pageNum, pageSize, skillPage.getTotalRow());
        skillVOPage.setRecords(this.getSkillVOList(skillPage.getRecords()));
        return skillVOPage;
    }

    @Override
    public SkillVO getSkillVO(Skill skill) {
        if (skill == null) {
            return null;
        }
        SkillVO skillVO = new SkillVO();
        BeanUtil.copyProperties(skill, skillVO);
        Long userId = skill.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            if (user != null) {
                skillVO.setUser(userService.getUserVO(user));
            }
        }
        return skillVO;
    }

    @Override
    public List<SkillVO> getSkillVOList(List<Skill> skillList) {
        if (CollUtil.isEmpty(skillList)) {
            return new ArrayList<>();
        }
        Set<Long> userIds = skillList.stream()
                .map(Skill::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return skillList.stream().map(skill -> {
            SkillVO skillVO = this.getSkillVO(skill);
            skillVO.setUser(userVOMap.get(skill.getUserId()));
            return skillVO;
        }).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(SkillQueryRequest skillQueryRequest) {
        if (skillQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = skillQueryRequest.getId();
        String skillName = skillQueryRequest.getSkillName();
        String category = skillQueryRequest.getCategory();
        String tags = skillQueryRequest.getTags();
        Integer status = skillQueryRequest.getStatus();
        Long userId = skillQueryRequest.getUserId();
        return QueryWrapper.create()
                .eq("id", id)
                .like("skillName", skillName)
                .eq("category", category)
                .like("tags", tags)
                .eq("status", status)
                .eq("userId", userId)
                .orderBy("createTime", false)
                .orderBy("id", true);
    }

    /**
     * 上传技能文件并自动解析创建（.json / .zip / .skill）。
     * <p>
     * - .json：直接反序列化为 {@link SkillAddRequest} 后创建；
     * - .zip / .skill：解压后在包内查找 SKILL.md，解析其 YAML frontmatter 与正文，映射为技能字段后创建。
     */
    @Override
    public Long uploadSkill(MultipartFile file, User loginUser) {
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        String fileName = file.getOriginalFilename();
        ThrowUtils.throwIf(StrUtil.isBlank(fileName), ErrorCode.PARAMS_ERROR, "文件名不能为空");
        String lower = fileName.toLowerCase();
        ThrowUtils.throwIf(
                !(lower.endsWith(".json") || lower.endsWith(".zip") || lower.endsWith(".skill")),
                ErrorCode.PARAMS_ERROR, "仅支持 .json / .zip / .skill 格式的技能文件");
        ThrowUtils.throwIf(file.getSize() > 10 * 1024 * 1024, ErrorCode.PARAMS_ERROR, "文件大小不能超过 10MB");

        try {
            SkillAddRequest req;
            if (lower.endsWith(".json")) {
                String text = new String(file.getBytes(), StandardCharsets.UTF_8);
                req = JSONUtil.toBean(text, SkillAddRequest.class);
            } else {
                req = parseSkillArchive(file);
            }
            ThrowUtils.throwIf(StrUtil.isBlank(req.getSkillName()),
                    ErrorCode.PARAMS_ERROR, "技能名称缺失，请在文件（SKILL.md 的 name 或 JSON 的 skillName）中提供");
            // 上传的技能默认进入「待审核」，由管理员在后台「上传技能」管理页上架后才进入发现页
            req.setStatus(0);
            return createSkill(req, loginUser);
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("[Skill] 上传技能解析失败: {}", fileName, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "技能文件解析失败：" + e.getMessage());
        }
    }

    /** 解压 zip/skill 包，定位 SKILL.md 并解析为创建请求 */
    private SkillAddRequest parseSkillArchive(MultipartFile file) throws IOException {
        File tempZip = File.createTempFile("skill_upload_", ".zip");
        File extractDir = new File(tempZip.getParent(), "skill_extract_" + System.nanoTime());
        extractDir.mkdirs();
        try {
            file.transferTo(tempZip);
            ZipUtil.unzip(tempZip, extractDir);
            File skillMd = findSkillMd(extractDir);
            ThrowUtils.throwIf(skillMd == null, ErrorCode.PARAMS_ERROR, "压缩包内未找到 SKILL.md 文件");
            String md = FileUtil.readUtf8String(skillMd);
            return parseSkillMd(md);
        } finally {
            FileUtil.del(tempZip);
            FileUtil.del(extractDir);
        }
    }

    /** 递归查找 SKILL.md（大小写不敏感） */
    private File findSkillMd(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.isDirectory()) {
                File found = findSkillMd(f);
                if (found != null) return found;
            } else if ("skill.md".equalsIgnoreCase(f.getName())) {
                return f;
            }
        }
        return null;
    }

    /** 解析 SKILL.md：分离 YAML frontmatter 与正文，映射为技能字段 */
    private SkillAddRequest parseSkillMd(String md) {
        SkillAddRequest req = new SkillAddRequest();
        String front = null;
        String body = md;
        // frontmatter 以单独一行的 --- 起止
        Pattern p = Pattern.compile("^---\\s*\\r?\\n(.*?)\\r?\\n---\\s*\\r?\\n?(.*)$", Pattern.DOTALL);
        Matcher m = p.matcher(md);
        if (m.find()) {
            front = m.group(1);
            body = m.group(2);
        }
        if (StrUtil.isNotBlank(front)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> fm = (Map<String, Object>) new Yaml().load(
                    new ByteArrayInputStream(front.getBytes(StandardCharsets.UTF_8)));
            req.setSkillName(str(fm, "name", "skillName"));
            req.setSkillDesc(str(fm, "description", "desc", "skillDesc"));
            req.setIcon(str(fm, "icon"));
            req.setCategory(str(fm, "category"));
            req.setTags(str(fm, "tags"));
            req.setFeatureDesc(str(fm, "featureDesc", "feature_desc"));
            req.setUsageDesc(str(fm, "usageDesc", "usage_desc"));
            req.setSkillCode(str(fm, "skillCode", "skill_code"));
            req.setModelType(str(fm, "modelType", "model_type"));
            req.setMcpServers(str(fm, "mcpServers", "mcp_servers"));
            req.setPrice(str(fm, "price"));
            req.setOriginalPrice(str(fm, "originalPrice", "original_price"));
            req.setPriceUnit(str(fm, "priceUnit", "price_unit"));

            String sysPrompt = str(fm, "systemPrompt", "system_prompt");
            if (StrUtil.isBlank(sysPrompt)) {
                sysPrompt = body.trim();
            } else if (StrUtil.isNotBlank(body.trim())) {
                sysPrompt = sysPrompt + "\n\n" + body.trim();
            }
            req.setSystemPrompt(sysPrompt);

            Object temp = fm.get("temperature");
            if (temp != null) {
                try {
                    req.setTemperature(new BigDecimal(temp.toString()));
                } catch (NumberFormatException ignored) {
                    // 非法温度值忽略，使用默认值
                }
            }
        } else {
            // 无 frontmatter：整篇正文作为系统提示词
            req.setSystemPrompt(body.trim());
        }
        return req;
    }

    /** 从 frontmatter Map 中取第一个存在的非空字段值（兼容 snake_case / camelCase） */
    private static String str(Map<String, Object> fm, String... keys) {
        if (fm == null) return null;
        for (String key : keys) {
            Object v = fm.get(key);
            if (v == null) continue;
            if (v instanceof Map || v instanceof List) {
                return JSONUtil.toJsonStr(v);
            }
            String s = v.toString().trim();
            if (StrUtil.isNotBlank(s) && !"null".equalsIgnoreCase(s)) {
                return s;
            }
        }
        return null;
    }
}

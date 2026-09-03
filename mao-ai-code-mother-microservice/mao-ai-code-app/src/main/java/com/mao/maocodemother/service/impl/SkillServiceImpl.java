package com.mao.maocodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mao.maocodemother.exception.BusinessException;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.exception.ThrowUtils;
import com.mao.maocodemother.innerservice.InnerUserService;
import com.mao.maocodemother.mapper.SkillMapper;
import com.mao.maocodemother.model.dto.skill.SkillAddRequest;
import com.mao.maocodemother.model.dto.skill.SkillQueryRequest;
import com.mao.maocodemother.model.dto.skill.SkillUpdateRequest;
import com.mao.maocodemother.model.entity.Skill;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.vo.SkillVO;
import com.mao.maocodemother.model.vo.UserVO;
import com.mao.maocodemother.service.SkillService;
import com.mao.maocodemother.ai.service.VectorSearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 技能 服务层实现。
 */
@Service
@Slf4j
public class SkillServiceImpl extends ServiceImpl<SkillMapper, Skill> implements SkillService {

    @DubboReference
    private InnerUserService userService;

    @Autowired(required = false)
    private VectorSearchService vectorSearchService;

    @Override
    public Long createSkill(SkillAddRequest skillAddRequest, User loginUser) {
        ThrowUtils.throwIf(skillAddRequest == null, ErrorCode.PARAMS_ERROR);
        String skillName = skillAddRequest.getSkillName();
        ThrowUtils.throwIf(StrUtil.isBlank(skillName), ErrorCode.PARAMS_ERROR, "技能名称不能为空");
        Skill skill = new Skill();
        BeanUtil.copyProperties(skillAddRequest, skill);
        skill.setUserId(loginUser.getId());
        // 默认上架、使用次数为 0
        skill.setStatus(skill.getStatus() == null ? 1 : skill.getStatus());
        skill.setUsageCount(skill.getUsageCount() == null ? 0L : skill.getUsageCount());
        skill.setEditTime(LocalDateTime.now());
        boolean result = this.save(skill);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        ingestSkillVector(skill);
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
            UserVO userVO = userService.getUserVO(user);
            skillVO.setUser(userVO);
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
                .orderBy("createTime", false);
    }

    private void ingestSkillVector(Skill skill) {
        if (vectorSearchService == null) {
            return;
        }
        try {
            String text = skill.getSkillName() + "\n" + StrUtil.blankToDefault(skill.getSkillDesc(), "");
            vectorSearchService.ingest(
                    "skill-" + skill.getId(),
                    text,
                    Map.of(
                            "type", "skill",
                            "skillId", skill.getId(),
                            "userId", skill.getUserId(),
                            "category", StrUtil.blankToDefault(skill.getCategory(), "")
                    ));
        } catch (Exception e) {
            log.warn("技能向量入库失败, skillId={}", skill.getId(), e);
        }
    }
}

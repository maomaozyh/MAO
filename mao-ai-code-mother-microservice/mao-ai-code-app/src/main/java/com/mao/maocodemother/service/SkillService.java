package com.mao.maocodemother.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.mao.maocodemother.model.dto.skill.SkillAddRequest;
import com.mao.maocodemother.model.dto.skill.SkillQueryRequest;
import com.mao.maocodemother.model.dto.skill.SkillUpdateRequest;
import com.mao.maocodemother.model.entity.Skill;
import com.mao.maocodemother.model.entity.User;
import com.mao.maocodemother.model.vo.SkillVO;

import java.util.List;

/**
 * 技能 服务层。
 */
public interface SkillService extends IService<Skill> {

    /**
     * 创建技能（记录创建人）
     *
     * @param skillAddRequest 创建请求
     * @param loginUser       登录用户
     * @return 新技能 id
     */
    Long createSkill(SkillAddRequest skillAddRequest, User loginUser);

    /**
     * 更新技能
     *
     * @param skillUpdateRequest 更新请求
     * @param loginUser          登录用户
     * @return 是否成功
     */
    Boolean updateSkill(SkillUpdateRequest skillUpdateRequest, User loginUser);

    /**
     * 删除技能
     *
     * @param id        技能 id
     * @param loginUser 登录用户
     * @return 是否成功
     */
    Boolean deleteSkill(Long id, User loginUser);

    /**
     * 根据 id 获取技能详情（脱敏封装）
     *
     * @param id 技能 id
     * @return 技能详情
     */
    SkillVO getSkillVOById(Long id);

    /**
     * 分页获取技能列表（脱敏封装）
     *
     * @param skillQueryRequest 查询请求
     * @return 技能分页
     */
    Page<SkillVO> listSkillVOByPage(SkillQueryRequest skillQueryRequest);

    /**
     * 转换为脱敏视图
     *
     * @param skill 技能
     * @return 技能视图
     */
    SkillVO getSkillVO(Skill skill);

    /**
     * 批量转换为脱敏视图
     *
     * @param skillList 技能列表
     * @return 技能视图列表
     */
    List<SkillVO> getSkillVOList(List<Skill> skillList);

    /**
     * 构造查询条件
     *
     * @param skillQueryRequest 查询请求
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(SkillQueryRequest skillQueryRequest);
}

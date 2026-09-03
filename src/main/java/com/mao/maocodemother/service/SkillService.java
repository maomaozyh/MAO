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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SkillService extends IService<Skill> {

    Long createSkill(SkillAddRequest skillAddRequest, User loginUser);

    /**
     * 上传技能文件并自动解析创建（.json / .zip / .skill）。
     * 返回新创建的技能 id。
     */
    Long uploadSkill(MultipartFile file, User loginUser);

    Boolean updateSkill(SkillUpdateRequest skillUpdateRequest, User loginUser);

    Boolean deleteSkill(Long id, User loginUser);

    SkillVO getSkillVO(Skill skill);

    List<SkillVO> getSkillVOList(List<Skill> skillList);

    QueryWrapper getQueryWrapper(SkillQueryRequest skillQueryRequest);

    SkillVO getSkillVOById(Long id);

    Page<SkillVO> listSkillVOByPage(SkillQueryRequest skillQueryRequest);
}

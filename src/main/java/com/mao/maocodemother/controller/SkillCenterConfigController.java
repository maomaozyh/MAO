package com.mao.maocodemother.controller;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.mao.maocodemother.common.BaseResponse;
import com.mao.maocodemother.common.ResultUtils;
import com.mao.maocodemother.service.SysConfigService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 技能中心公开配置（无需登录）。
 * <p>
 * Banner 轮播、分类、右侧免费额度均可通过 sys_config 表配置：
 * key 分别为 skill.banners / skill.categories / skill.quota（JSON 字符串）。
 * 未配置或配置缺失时使用内置默认值。
 */
@RestController
@RequestMapping("/config")
public class SkillCenterConfigController {

    /** 默认 Banner（与前端内置默认一致） */
    private static final String DEFAULT_BANNERS = "["
            + "{\"title\":\"限时福利・调用即享 8折\",\"desc\":\"限时福利活动期间，调用任意技能即享 8 折优惠，先到先得。\",\"emoji\":\"🎀\"},"
            + "{\"title\":\"three.js 3D 创作\",\"desc\":\"用 three.js 在网页里构建三维场景、动态视觉与沉浸式交互体验，社区技能免费使用。\",\"emoji\":\"🧊\"},"
            + "{\"title\":\"登录能力免费接入\",\"desc\":\"支持用户名、邮箱、手机号、第三方账号及微信登录，一键接入你的应用，完全免费。\",\"emoji\":\"🔐\"}"
            + "]";

    /** 默认分类 */
    private static final String DEFAULT_CATEGORIES = "["
            + "{\"key\":\"all\",\"label\":\"全部\"},"
            + "{\"key\":\"kling\",\"label\":\"可灵专区\"},"
            + "{\"key\":\"create\",\"label\":\"内容创作与生成\"},"
            + "{\"key\":\"understand\",\"label\":\"内容理解与处理\"},"
            + "{\"key\":\"voice\",\"label\":\"语音交互\"},"
            + "{\"key\":\"search\",\"label\":\"搜索查询\"},"
            + "{\"key\":\"office\",\"label\":\"办公提效\"},"
            + "{\"key\":\"design\",\"label\":\"设计美化\"},"
            + "{\"key\":\"pay\",\"label\":\"支付交易\"},"
            + "{\"key\":\"auth\",\"label\":\"登录验证\"},"
            + "{\"key\":\"map\",\"label\":\"地图出行\"},"
            + "{\"key\":\"billing\",\"label\":\"计费\"},"
            + "{\"key\":\"allMore\",\"label\":\"全部 ▾\"}"
            + "]";

    /** 默认免费额度 */
    private static final String DEFAULT_QUOTA = "["
            + "{\"label\":\"视频生成类\",\"used\":1,\"total\":1},"
            + "{\"label\":\"图片生成类\",\"used\":5,\"total\":5},"
            + "{\"label\":\"其他类\",\"used\":100,\"total\":100}"
            + "]";

    @Resource
    private SysConfigService sysConfigService;

    /**
     * 技能中心公开配置：Banner / 分类 / 免费额度
     *
     * @return 配置 Map
     */
    @GetMapping("/skill-center")
    public BaseResponse<Map<String, Object>> getSkillCenterConfig() {
        String banners = sysConfigService.getConfigValue("skill.banners", DEFAULT_BANNERS);
        String categories = sysConfigService.getConfigValue("skill.categories", DEFAULT_CATEGORIES);
        String quota = sysConfigService.getConfigValue("skill.quota", DEFAULT_QUOTA);
        Map<String, Object> result = new HashMap<>();
        result.put("banners", parseJsonArray(banners));
        result.put("categories", parseJsonArray(categories));
        result.put("quota", parseJsonArray(quota));
        return ResultUtils.success(result);
    }

    /**
     * 安全解析 JSON 数组，失败返回空数组（前端对空数组回退默认值）
     */
    private JSONArray parseJsonArray(String json) {
        try {
            JSONArray array = JSONUtil.parseArray(json);
            return array == null ? new JSONArray() : array;
        } catch (Exception e) {
            return new JSONArray();
        }
    }
}

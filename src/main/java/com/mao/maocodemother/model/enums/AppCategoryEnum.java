package com.mao.maocodemother.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 应用分类枚举
 * <p>
 * 分类分为两个层级：
 * 1. 首页快捷入口分类（粗分类）：游戏、工具、教育、网站、电商、办公、营销、研究、问答、财务经营、办公协同、营销增长、其他 等
 * 2. AI 生成引导分类（细分类）：图片生成、营销文案、数据可视化等，用于约束 AI 代码生成
 * <p>
 * 两套分类共用同一个枚举类和同一个 category 字段存储。
 * 粗分类用于首页筛选，细分类用于 AI 生成指导。
 * 每个分类都携带一段「生成引导词」，用于在 AI 代码生成时约束产出类型。
 *
 * @author mao
 */
@Getter
public enum AppCategoryEnum {

    MINI_PROGRAM("小程序", "miniprogram",
            "请生成一个【微信小程序风格】的单页 Web 应用：采用移动端竖屏布局（max-width 420px 居中模拟手机），"
                    + "包含底部 TabBar（至少 2 个 tab）、卡片列表与基础交互，使用文字/emoji/渐变充当图标，"
                    + "整体风格简洁、轻量，可直接在浏览器预览。"),
    IMAGE("图片生成", "image",
            "请生成一个【图片生成 / 图像工具】类 Web 应用：提供提示词输入框、风格/尺寸/比例选项、生成按钮与结果展示区，"
                    + "可用前端占位图或 SVG 演示生成效果，UI 突出视觉表现，配色鲜明。"),
    RESEARCH("深度研究", "research",
            "请生成一个【深度研究 / 研究报告】类 Web 应用：提供主题输入与大纲展示，"
                    + "输出结构化的研究报告版面（带目录、章节、引用样式），排版专业、阅读体验好。"),
    PPT("PPT生成", "ppt",
            "请生成一个【PPT / 演示文稿】类 Web 应用：提供主题与大纲输入，"
                    + "以幻灯片形式（一屏一页 + 翻页/缩略图导航）展示内容，支持键盘/按钮切换，样式接近演示文稿。"),
    VIDEO("视频生成", "video",
            "请生成一个【视频生成 / 视频工具】类 Web 应用：提供脚本/主题输入、参数设置与播放预览区，"
                    + "可用占位视频或 canvas 动画演示，突出播放器与控制条交互。"),
    WEB("网页抓取", "web",
            "请生成一个【网页抓取 / 内容提取】类 Web 应用：提供 URL 输入、抓取字段配置与结果展示（结构化列表/表格），"
                    + "说明抓取逻辑，UI 偏工具型、信息密度高。"),
    POSTER("海报设计", "poster",
            "请生成一个【海报设计】类 Web 应用：提供文案、配色、尺寸选项与实时预览画布，"
                    + "强调构图与排版美观，支持一键切换模板风格。"),
    LOGO("Logo 设计", "logo",
            "请生成一个【Logo 设计】类 Web 应用：提供品牌名、行业、风格关键词输入与 Logo 预览区，"
                    + "用 SVG 矢量展示多个 Logo 方案，强调简洁与配色。"),
    CHART("数据可视化", "chart",
            "请生成一个【数据可视化】类 Web 应用：提供数据集/示例数据输入与多种图表（柱状/折线/饼图）切换，"
                    + "图表美观、交互（hover 提示）完善。"),
    COPY("营销文案", "copy",
            "请生成一个【营销文案】类 Web 应用：提供产品卖点输入，输出多版本营销文案（标题/正文/社交媒体短文），"
                    + "强调可读性与排版，可直接复制。"),
    RESUME("简历优化", "resume",
            "请生成一个【简历优化】类 Web 应用：提供经历输入，输出结构化、专业的简历版面（左右两栏：个人信息/经历），"
                    + "排版接近真实简历，支持打印样式。"),
    TRANSLATE("智能翻译", "translate",
            "请生成一个【智能翻译】类 Web 应用：提供源语言/目标语言选择与双向输入框，"
                    + "支持即时翻译展示与语言切换，UI 简洁。"),
    LESSON("教学课件", "lesson",
            "请生成一个【教学课件】类 Web 应用：提供主题输入，输出分页课件（章节 + 讲解 + 互动问答），"
                    + "支持翻页导航，排版清晰、适合教学。"),
    MODEL_3D("3D 模型", "model3d",
            "请生成一个【3D 模型 / 3D 预览】类 Web 应用：提供模型描述输入与 3D 预览画布（可用 Three.js 等前端 3D 库），"
                    + "支持旋转/缩放交互，突出 3D 展示。"),
    AVATAR("头像生成", "avatar",
            "请生成一个【头像生成】类 Web 应用：提供风格/特征输入与头像预览区，"
                    + "用 SVG/Canvas 生成个性化头像，强调趣味与可定制。"),
    INFOGRAPHIC("信息图表", "infographic",
            "请生成一个【信息图表】类 Web 应用：提供数据/要点输入，输出一张信息图表版面（图标 + 数据块 + 说明），"
                    + "布局清晰、视觉化强。"),
    FINANCE("财务经营", "finance",
            "请生成一个【财务经营】类 Web 应用：提供数据输入与可视化报表，包含收支统计、利润分析、预算管理、"
                    + "趋势图表等模块，配色专业稳重，数据展示清晰直观，支持数据录入与结果展示。"),
    OFFICE_COLLAB("办公协同", "office_collab",
            "请生成一个【办公协同】类 Web 应用：提供任务/文档/会议管理功能，包含待办列表、日程安排、"
                    + "会议纪要、团队协作看板等模块，界面简洁高效，支持拖拽、筛选、状态切换等交互。"),
    MARKETING_GROWTH("营销增长", "marketing_growth",
            "请生成一个【营销增长】类 Web 应用：提供营销文案生成、活动策划、数据分析、获客转化等功能模块，"
                    + "包含输入配置区与结果展示区，风格活泼、强调转化效果，支持多模板切换与一键复制。"),
    // ===== 首页粗分类（用于首页 tab 筛选）=====
    APP_MINI("应用类手", "app_mini",
            "请生成一个【小程序风格应用】：移动端竖屏布局，卡片列表，交互简洁。"),
    GAME("游戏", "game",
            "请生成一个【小游戏】类 Web 应用：包含游戏画面、得分系统、操作按钮，玩法有趣、视觉活泼。"),
    TOOL("工具", "tool",
            "请生成一个【效率工具】类 Web 应用：功能明确、操作简单、结果清晰，UI 偏工具型。"),
    EDUCATION("教育", "education",
            "请生成一个【教育学习】类 Web 应用：包含知识讲解、练习测试、进度追踪，适合学习场景。"),
    WEBSITE("网站", "website",
            "请生成一个【企业/官网风格】的 Web 应用：包含导航栏、banner、内容板块、页脚，排版专业。"),
    ECOMMERCE("电商", "ecommerce",
            "请生成一个【电商/购物】类 Web 应用：包含商品展示、购物车、结算等模块，视觉突出商品。"),
    OFFICE_CAT("办公", "office_cat",
            "请生成一个【办公效率】类 Web 应用：包含文档/表格/待办等办公功能，界面简洁高效。"),
    MARKETING_CAT("营销", "marketing_cat",
            "请生成一个【营销推广】类 Web 应用：包含活动页、营销文案、转化按钮，风格吸引眼球。"),
    RESEARCH_CAT("研究", "research_cat",
            "请生成一个【研究分析】类 Web 应用：包含数据输入、分析图表、结论输出，排版专业。"),
    QA("问答", "qa",
            "请生成一个【智能问答】类 Web 应用：包含对话界面、输入框、消息列表，支持多轮问答交互。"),
    OTHER("其他", "other",
            "请生成一个创意 Web 应用，类型不限，发挥你的想象力，做出有趣有用的功能。");

    private final String text;
    private final String value;
    private final String generationGuidance;

    AppCategoryEnum(String text, String value, String generationGuidance) {
        this.text = text;
        this.value = value;
        this.generationGuidance = generationGuidance;
    }

    /**
     * 根据 value 获取枚举
     */
    public static AppCategoryEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (AppCategoryEnum anEnum : AppCategoryEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

    /**
     * 该分类是否需要调用外部 AI 服务产出真实素材（而非走代码生成链路）
     * <p>
     * 图像类（图片/头像/Logo/海报）统一走图像生成服务；其余走 DeepSeek 代码生成。
     */
    public ExternalAssetType getExternalAssetType() {
        return switch (value) {
            case "image", "avatar", "logo", "poster" -> ExternalAssetType.IMAGE;
            case "video" -> ExternalAssetType.VIDEO;
            case "model3d" -> ExternalAssetType.MODEL_3D;
            case "ppt" -> ExternalAssetType.PPT;
            default -> ExternalAssetType.NONE;
        };
    }
}

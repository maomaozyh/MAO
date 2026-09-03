# mao AI 代码生成器（mao-ai-code-mother）

> 一个基于大模型的**全栈 AI 代码生成平台**：用户用自然语言描述需求，平台通过 AI 对话流式生成可运行的网站应用，并支持应用管理、部署预览、技能中心、素材库、社区与会员体系等完整功能。

- 单体后端：`mao-ai-code-mother`（Spring Boot 3 + Java 21，端口 `8123`，上下文 `/api`）
- 前端：`mao-ai-code-mother-frontend`（Vue 3 + Vite 7 + TypeScript + Ant Design Vue，端口 `5173`）
- 微服务后端（可选部署）：`mao-ai-code-mother-microservice`（Spring Cloud Alibaba + Dubbo + Nacos）

---

## 一、核心特性

### 创作与生成
- 💬 **AI 对话生成代码**：通过 `AppController.chatToGenCode`（SSE 流式）与 AI 多轮对话，实时生成并预览网页应用；内置 Agent 工具（`writeFile` / `readFile` / `modifyFile` / `deleteFile` / `readDir` / `exit`），可自动读写项目文件。
- 🧠 **多模型编排**：默认使用 DeepSeek（`deepseek-chat` / `deepseek-reasoner`）做生成与路由；DashScope（通义千问）负责图像生成、视频生成与 PPT 大纲。
- 🖼️ **富媒体能力**：文生图 / 文生视频（通义万相）、文生 3D 模型（Tripo3D）、PPT 导出（`pptxgenjs`）、Pexels 图片搜索。
- 📸 **自动截图**：基于 Selenium 生成应用封面（部署后截图服务）。

### 应用管理
- 📦 **应用 CRUD**：创建 / 编辑 / 删除 / 查询自己的应用，支持分页与按名称检索。
- 🚀 **一键部署与预览**：将生成的静态站点部署到本地目录，前端通过 `/api/static/{deployKey}/` 实时预览。
- ⭐ **精选应用**：管理员可置顶 / 设为精选。

### 平台能力
- 🧩 **技能中心（Skill Center）**：可发现、上传、管理自定义技能（`status` 区分「已上架 / 待审核」）。
- 🗂️ **素材库**：上传 / 管理素材文件（单文件 ≤ 200MB，存储于腾讯云 COS）。
- 👥 **社区**：帖子（Post）与评论（Comment）的发布、浏览与互动。
- 🪙 **会员与秒点**：`seconds`（秒点）为平台虚拟积分，支持赠送 / 购买、消耗明细（`seconds_record`）；消费走 CAS 并发安全写入。
- 💰 **支付**：内置 MOCK 沙箱支付，可切换真实支付宝电脑网站支付。
- 🛡️ **安全与治理**：敏感词过滤、登录手机号验证码二次验证、微信 / QQ 扫码登录、操作日志、RBAC 权限（角色 / 菜单 / 权限）。
- 🖥️ **管理后台**：应用、用户、技能、素材、敏感词、菜单、权限、角色、系统配置等统一管理（`/admin`）。

---

## 二、技术架构

### 部署形态 A：单体后端（默认，推荐快速上手）
```
浏览器
  │  http://localhost:5173
  ▼
前端 (Vite :5173)
  │  /api/**  →  proxy  http://localhost:8123
  ▼
单体后端 (Spring Boot :8123, context /api)
  ├── MySQL 8        (localhost:3306 / yu_ai_code_mother)
  ├── Redis 7        (localhost:6379，Session + 缓存 + Redisson 分布式锁)
  ├── 大模型 API     (DeepSeek / DashScope / Tripo3D …)
  └── 腾讯云 COS      (素材 / 封面图对象存储)
```

### 部署形态 B：微服务后端（高可用，可选）
前端对接 `mao-ai-code-gateway :8123`；`user` / `app` 等服务通过 Dubbo 经 Nacos 注册发现。详见 [`mao-ai-code-mother-microservice/README.md`](mao-ai-code-mother-microservice/README.md)。

### 技术栈
| 层 | 技术 |
| --- | --- |
| 后端（单体） | Spring Boot 3.5.4、Java 21、MyBatis-Flex 1.11、Spring Session + Redis、Redisson、LangChain4j 1.1、LangGraph4j 1.6、Spring Security Crypto（BCrypt） |
| AI / 多模态 | DeepSeek（对话/推理）、阿里云 DashScope（通义万相 文生图/视频、千问 PPT 大纲）、Tripo3D（文生 3D）、Selenium（截图） |
| 存储 / 中间件 | MySQL 8、Redis 7、Nacos 2.x（微服务注册中心）、RabbitMQ（可选） |
| 前端 | Vue 3.5、Vite 7、TypeScript 5.8、Ant Design Vue 4、Pinia、Vue Router 4、Axios、markdown-it + highlight.js、dompurify、pptxgenjs |
| 工程 / 运维 | Maven 3.9+、Knife4j / SpringDoc（API 文档）、Actuator + Micrometer + Prometheus、Docker / K8s |

---

## 三、目录结构

```
yu-ai-code-mother/                 # 仓库根（本文件所在目录）
├── pom.xml                        # 单体后端 Maven 工程
├── src/                           # 单体后端源码（com.mao.maocodemother）
├── sql/                           # 数据库建表 / 迁移脚本（含管理员密码重置脚本）
├── docker-compose.yml             # Redis / Nacos / RabbitMQ 基础设施
├── k8s/  prometheus.yml  grafana/ # 生产运维相关配置
├── mao-ai-code-mother-frontend/   # 前端工程（Vue3 + Vite）
└── mao-ai-code-mother-microservice/  # 微服务后端（Spring Cloud + Dubbo + Nacos）
```

---

## 四、环境要求

| 依赖 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 21 | 单体与微服务后端均需要 |
| Maven | 3.9+ | 后端构建 |
| Node.js | 18+（推荐 22） | 前端构建 |
| MySQL | 8.x | 库名 `yu_ai_code_mother`，默认 `root` / `123456` |
| Redis | 7.x | 端口 `6379` |
| 大模型 API Key | — | `DEEPSEEK_API_KEY`（生成能力必需），其余按需 |

---

## 五、快速开始（单体后端）

### 1. 准备数据库与中间件
```bash
# MySQL：创建库并执行建表脚本
mysql -uroot -p123456 < sql/create_table.sql        # 建库 + 核心表（用户 / 应用 / 对话历史等）
mysql -uroot -p123456 yu_ai_code_mother < sql/create_tables.sql   # 技能 / 素材 / 社区 / 订单 / 系统配置等表
# 其余迁移脚本按需执行（rbac_migration / seconds_migration / payment_migration / sensitive_word / security_enhancement）

# Redis（或用 docker compose 一键起基础设施）
docker compose up -d        # 启动 redis(6379) / nacos(8848) / rabbitmq(5672)
```

### 2. 配置密钥（二选一）
- **环境变量**（推荐）：设置 `DEEPSEEK_API_KEY` 等；`application.yml` 中以 `${VAR:}` 占位，未设置时使用默认值。
- **本地配置文件**：新建 `src/main/resources/application-local.yml`（已被 `.gitignore` 忽略）写入真实密钥；默认激活的 profile 为 `local`。

| 配置项（环境变量） | 用途 | 是否必需 |
| --- | --- | --- |
| `DEEPSEEK_API_KEY` | AI 对话 / 代码生成 | ✅ 必需 |
| `DASHSCOPE_API_KEY` | 文生图 / 文生视频 / PPT 大纲 | 可选 |
| `COS_SECRET_ID` / `COS_SECRET_KEY` | 腾讯云 COS 素材与封面存储 | 可选（未配会导致上传封面报错） |
| `TRIPO_API_KEY` | 文生 3D 模型 | 可选 |
| `PEXELS_API_KEY` | Pexels 图片搜索 | 可选 |
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号 | 默认 root / 123456 |
| `REDIS_PASSWORD` | Redis 密码 | 默认空 |
| `WECHAT_*` / `QQ_*` / `SMS_*` / `ALIPAY_*` | 第三方登录 / 支付 | 可选 |

> 生产环境请用 `application-prod.yml` + 环境变量，不要把密钥写进仓库。

### 3. 启动后端
```bash
# 开发态直接运行
mvn -o spring-boot:run
# 或打成可执行 jar
mvn -o clean package -DskipTests && java -jar target/mao-ai-code-mother-0.0.1-SNAPSHOT.jar
```
启动后访问：
- 接口根路径：`http://localhost:8123/api`
- 健康检查：`http://localhost:8123/api/actuator/health`
- API 文档（Knife4j）：`http://localhost:8123/api/doc.html`

### 4. 启动前端
```bash
cd mao-ai-code-mother-frontend
npm install
npm run dev          # 默认 http://localhost:5173，/api 代理到 http://localhost:8123
```
浏览器打开 `http://localhost:5173` 即可使用。前端 `vite.config.ts` 已配置 `/api` → `8123` 代理；如需自定义可改 `.env.development` 的 `VITE_API_BASE_URL`。

### 5. 默认账号
- 管理员账号 `admin`（忘记密码可执行 `sql/reset_admin_password.sql` 重置为强口令后登录，再自行修改）。
- 普通用户通过注册 / 手机号验证码 / 微信 / QQ 登录。

---

## 六、微服务部署（可选）

若采用 Spring Cloud + Dubbo 微服务架构，请参见 [`mao-ai-code-mother-microservice/README.md`](mao-ai-code-mother-microservice/README.md)：

```bash
cd mao-ai-code-mother-microservice
docker compose up -d                 # MySQL + Redis + Nacos（该目录自带 compose）
mvn clean install -DskipTests
mvn -pl mao-ai-code-user   -am spring-boot:run
mvn -pl mao-ai-code-app    -am spring-boot:run
mvn -pl mao-ai-code-gateway -am spring-boot:run   # 网关统一入口 :8123
```
前端 `/api` 同样指向网关 `8123`。

---

## 七、常用脚本与构建

| 命令 | 说明 |
| --- | --- |
| `mvn -o clean compile` | 后端离线编译校验 |
| `mvn -o spring-boot:run` | 运行单体后端 |
| `cd mao-ai-code-mother-frontend && npm run dev` | 前端开发服务器 |
| `npm run build` | 前端生产构建（Vite build） |
| `npm run build:verify` | 沙箱安全构建（输出到 `.verify-dist`，不清理旧目录） |
| `npm run lint` / `npm run type-check` | 前端 ESLint / 类型检查 |
| `docker compose up -d` | 启动 Redis / Nacos / RabbitMQ 基础设施 |

---

## 八、主要接口模块（单体后端）

| Controller | 职责 |
| --- | --- |
| `AppController` | 应用管理 + AI 对话流式生成代码（`/app/chat/gen/code` SSE） |
| `ChatHistoryController` | 对话历史 |
| `SkillController` / `SkillCenterConfigController` | 技能中心（发现 / 上传 / 审核） |
| `MaterialController` | 素材库（COS 上传 / 管理） |
| `CommunityPostController` / `CommunityCommentController` | 社区帖子与评论 |
| `SecondsController` | 秒点（会员积分）余额与流水 |
| `PaymentController` | 下单与支付（MOCK / 支付宝） |
| `UserController` | 注册 / 登录 / 第三方登录 / 个人信息 |
| `SensitiveWordController` | 敏感词管理 |
| `AdminController` / `Sys*Controller` | 管理后台 + RBAC（角色 / 菜单 / 权限） |
| `StaticResourceController` | 已部署静态站点预览 |
| `WorkflowSseController` | 工作流 SSE |
| `HealthController` | 健康检查 |

---

## 九、注意事项

- **AI 生成依赖外网 Key**：不配置 `DEEPSEEK_API_KEY` 时对话生成不可用，其余功能不受影响。
- **对象存储**：封面图默认走腾讯云 COS，未配置 `COS_SECRET_ID/KEY` 时相关上传会报错，可改为本地存储。
- **会话与缓存**：登录态基于 Redis Session（cookie 30 天）；修改会员 / 权益字段后缓存会自动失效，无需手动清理。
- **ID 精度**：数据库主键为雪花 ID（19 位），前端一律以字符串处理，禁止用 `Number()` 解析。
- **微服务与单体二选一**：两者都监听 `8123`，不要同时启动以免端口冲突。

---

## 十、许可与鸣谢

- 本项目以 MIT 许可证开源，详见各模块 `LICENSE`。


---

> 文档随项目演进维护；如发现与代码不一致，欢迎提交 Issue / PR 修正。
"# maomaoAI" 

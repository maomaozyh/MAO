# mao AI 代码生成器 - 微服务后端

基于 Spring Boot 3 + Spring Cloud + Dubbo + MyBatis Flex + LangChain4j 的微服务后端。

## 模块说明

| 模块 | 端口 | 说明 |
| --- | --- | --- |
| `mao-ai-code-gateway` | 8123 | 网关，统一入口，转发 `/api` 到各服务（**前端对接的就是这个端口**） |
| `mao-ai-code-user` | 8124 | 用户服务（注册/登录/管理） |
| `mao-ai-code-app` | 8125 | 应用服务（应用 CRUD、AI 对话生成代码、部署、静态资源预览） |
| `mao-ai-code-ai` | - | AI 代码生成核心（LangChain4j + DeepSeek） |
| `mao-ai-code-screenshot` | - | 应用截图服务（部署后生成封面） |
| `mao-ai-code-client` | - | 内部 RPC 接口定义（Dubbo） |
| `mao-ai-code-common` | - | 公共依赖（BaseResponse / 异常 / 常量 / COS 等） |
| `mao-ai-code-model` | - | 实体、DTO、VO、枚举 |

## 架构与请求链路

```
浏览器 (前端 :5173)
   │  /api/**
   ▼
网关 mao-ai-code-gateway (:8123)
   ├── /api/user/**        → mao-ai-code-user   (:8124)
   └── /api/app/**         → mao-ai-code-app    (:8125)
       /api/chatHistory/**    │
       /api/static/**         │
                              ▼
        app 服务 通过 Dubbo(@DubboReference) 调用 user 服务的 InnerUserService
        （注册中心：Nacos :8848）
```

## 环境依赖

- JDK 21
- Maven 3.9+
- MySQL 8（库名 `yu_ai_code_mother`，账号 `root` / `123456`）
- Redis 7（端口 6379）
- Nacos 2.x（端口 8848，Dubbo 注册中心）
- 大模型 API Key（已在 `mao-ai-code-app/src/main/resources/application.yml` 中配置 DeepSeek / 通义千问，可替换为你自己的 key）

## 快速启动

### 1. 启动基础设施

```bash
docker compose up -d
```

该命令会启动 MySQL（自动执行 `sql/init.sql` 建表并初始化管理员）、Redis、Nacos。
默认管理员账号：`admin` / `12345678`。

> 若不使用 Docker，请自行安装 MySQL / Redis / Nacos，并手动执行 `sql/init.sql`。

### 2. 启动微服务

建议按依赖顺序启动（Nacos 需先就绪）：

```bash
mvn clean install -DskipTests

# 终端 1
mvn -pl mao-ai-code-user -am spring-boot:run
# 终端 2
mvn -pl mao-ai-code-app -am spring-boot:run
# 终端 3（可选，应用截图）
mvn -pl mao-ai-code-screenshot -am spring-boot:run
# 终端 4
mvn -pl mao-ai-code-gateway -am spring-boot:run
```

启动后访问 `http://localhost:8123/api/health/` 应返回 `{"code":0,"data":"ok","message":"ok"}`。

### 3. 前端联调

前端 `vite.config.ts` 将 `/api` 代理到 `http://localhost:8123`，与网关端口一致。
将前端根目录的 `.env.development` 中 `VITE_API_BASE_URL` 设置为 `http://localhost:8123/api` 即可。

## 重要目录说明

- 应用生成代码目录：`{启动目录}/tmp/code_output`（由 `AppConstant.CODE_OUTPUT_ROOT_DIR` 决定）
- 应用部署目录：`{启动目录}/tmp/code_deploy`（`CODE_DEPLOY_ROOT_DIR`）
- 前端通过 `http://localhost:8123/api/static/{deployKey}/` 预览已部署的静态页面

## 部署配置

- 应用部署域名：在 `mao-ai-code-app` 的 `application.yml` 中通过 `code.deploy-host` 配置（默认 `http://localhost`）。
- AI Key：同文件 `langchain4j` 配置节。
- 对象存储 COS：在 `mao-ai-code-common` 的 `CosClientConfig` 中配置（封面图默认走 COS，未配置时会报错，可改为本地存储）。

# Kubernetes 部署（本地开发集群）

面向 **minikube / kind** 的本地部署方案，包含全部微服务与基础设施。

## 架构

```
                          ┌─────────────────────────────┐
        浏览器 / 前端 ─────▶│  mao-ai-code-gateway (8123)  │  NodePort / port-forward
                          └──────────────┬──────────────┘
                                         │ 路由 /api/user/**  /api/app/**
                         ┌───────────────┴───────────────┐
                  ┌──────▼──────┐                 ┌───────▼──────┐
                  │ user (8124) │                 │  app (8125)  │
                  └──────┬──────┘                 └───┬──────┬────┘
                         │  Dubbo (tri)               │      │ 发布/消费
                         │      │                     │      │
                  ┌──────▼──────┴──────┐       ┌──────▼──────▼──────┐
                  │      nacos (8848)   │       │   rabbitmq (5672)  │
                  └────────────────────┘       └─────────────────────┘

   screenshot (8127) 也通过 Dubbo 注册到 nacos。
   mysql(3306) / redis(6379) 由 user、app 直接通过 Service DNS 访问。
```

## 组件清单

| 组件        | 镜像                          | Service (ClusterIP) | 说明                         |
|-------------|-------------------------------|---------------------|------------------------------|
| mysql       | mysql:8.0                     | `mysql:3306`        | 启动时自动建库建表（ConfigMap initdb） |
| redis       | redis:7.0                     | `redis:6379`        | Session / 缓存               |
| nacos       | nacos/nacos-server:v2.3.2     | `nacos:8848/9848/9849` | Dubbo 注册中心（standalone） |
| rabbitmq    | rabbitmq:3-management         | `rabbitmq:5672/15672` | 消息队列 + 管理台            |
| user        | yu-ai-code/user:latest        | `mao-ai-code-user:8124` | 用户服务                     |
| app         | yu-ai-code/app:latest         | `mao-ai-code-app:8125` | 应用/对话/素材服务（含 MQ 集成） |
| screenshot  | yu-ai-code/screenshot:latest | `mao-ai-code-screenshot:8127` | 截图服务                |
| gateway     | yu-ai-code/gateway:latest     | `mao-ai-code-gateway:8123` (NodePort 31223) | 网关 |

> Milvus 为向量检索（知识库）可选组件，未部署时 App 仍可启动，仅在真正调用向量检索时才会连接。
> 如需启用，将 `MILVUS_HOST`/`MILVUS_PORT` 指向可用的 Milvus 实例即可。

## 一键部署

```powershell
# 1. 启动本地集群（首次）
minikube start --memory=4g --cpus=4

# 2. 构建镜像并部署
.\k8s\deploy.ps1
```

脚本会：使用 minikube 内置 Docker 构建镜像 → `kubectl apply` 全部清单。

## 本地访问

```powershell
# 前端开发服务器（在宿主机运行，vite 代理 /api -> localhost:8123）
cd mao-ai-code-mother-frontend; npm run dev

# 将网关端口转发到本地 8123，使前端代理无需改动即可联通
kubectl -n yu-ai-code port-forward svc/mao-ai-code-gateway 8123:8123
```

- 前端：http://localhost:5173/
- 后端网关：http://localhost:8123/api
- RabbitMQ 管理台：`kubectl -n yu-ai-code port-forward svc/rabbitmq 15672:15672` → http://localhost:15672 （guest/guest）
- Nacos 控制台：`kubectl -n yu-ai-code port-forward svc/nacos 8848:8848` → http://localhost:8848/nacos

## RabbitMQ 代码集成（已在 app 服务实现）

- 依赖：`mao-ai-code-app/pom.xml` 增加 `spring-boot-starter-amqp`
- 配置：`application.yml` 中 `spring.rabbitmq.*` 支持环境变量注入（`RABBITMQ_HOST/PORT/USERNAME/PASSWORD`）
- 交换机/队列：`mq/RabbitMQConfig.java` 声明 topic 交换机 `mao-ai-code-exchange`、队列 `codegen.task.queue`
- 生产者：`mq/CodeGenerationProducer.java`，在 `AppController.addApp` 创建应用后发送 `GenerationTaskMessage`
- 消费者：`mq/CodeGenerationConsumer.java` 异步处理任务（目前为骨架，可扩展为素材预生成/知识库写入/缓存预热等）

## 配置参数化

所有服务均通过环境变量覆盖连接地址，方便多环境切换：

- `MYSQL_HOST/PORT/DATABASE/USERNAME/PASSWORD`
- `REDIS_HOST/PORT/PASSWORD`
- `DUBBO_REGISTRY_ADDRESS`（统一指向 `nacos://nacos:8848`）
- `RABBITMQ_HOST/PORT/USERNAME/PASSWORD`
- `CODE_DEPLOY_HOST`、`USER_SERVICE_URI`、`APP_SERVICE_URI`

持续化说明：mysql/redis 使用 `emptyDir`，Pod 重建会清空数据，仅供本地开发；生产请将 `volumes` 改为 `PersistentVolumeClaim`。

## 销毁

```powershell
kubectl delete namespace yu-ai-code
```
